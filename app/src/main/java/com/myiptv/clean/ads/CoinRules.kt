package com.myiptv.clean.ads

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/** A section is a SamPlay placement, not advertising embedded by a content provider. */
internal enum class AdSection(val label: String) {
    HOME("Inicio"), MOVIES("Películas"), SERIES("Series"), CHANNELS("Canales"),
    SEARCH("Búsqueda"), FAVORITES("Mi lista");

    companion object {
        fun forBannerRoute(route: String): AdSection? = when (route) {
            "movies" -> MOVIES
            "series" -> SERIES
            "search" -> SEARCH
            "favorites" -> FAVORITES
            else -> null
        }
    }
}

internal data class CoinState(
    val balance: Int = 0,
    val lastOpenDay: Long = NO_DAY,
    val consecutiveOpenDays: Int = 0,
    val lastFavoriteRewardDay: Long = NO_DAY,
    val knownFavorites: Set<String> = emptySet(),
    val watchDay: Long = NO_DAY,
    val watchedMs: Long = 0,
    val lastWatchRewardDay: Long = NO_DAY,
    val appAdFreeUntil: Long = 0,
    val sectionAdFreeUntil: Map<AdSection, Long> = emptyMap(),
    val channelOfferFreeUntil: Map<String, Long> = emptyMap(),
) {
    companion object { const val NO_DAY = Long.MIN_VALUE }
}

internal enum class CoinPurchaseStatus { ACTIVATED, INSUFFICIENT, ALREADY_ACTIVE }
internal data class CoinPurchase(val state: CoinState, val status: CoinPurchaseStatus)

/** Pure rules are shared by the persistent wallet and unit tests. All values are local. */
internal object CoinRules {
    const val REWARDED_COINS = 10
    const val DAILY_OPEN_COINS = 2
    const val FIVE_DAY_STREAK_COINS = 10
    const val FAVORITE_COINS = 3
    const val WATCH_COINS = 5
    const val WATCH_TARGET_MS = 20 * 60 * 1000L
    const val PLAYBACK_PRICE = 10
    const val SECTION_PRICE = 40
    const val APP_PRICE = 100
    const val SECTION_DURATION_MS = 24 * 60 * 60 * 1000L
    const val CHANNEL_DURATION_MS = 60 * 60 * 1000L

    /** Date in the device's timezone. UTC arithmetic keeps consecutive dates correct across DST. */
    fun localDay(nowMs: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val local = Calendar.getInstance(zone).apply { timeInMillis = nowMs }
        val dateInUtc = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }
        // Math.floorDiv(long, long) is unavailable on Android API 21–23.
        val millis = dateInUtc.timeInMillis
        val dayMs = 24 * 60 * 60 * 1000L
        return if (millis >= 0) millis / dayMs else (millis + 1) / dayMs - 1
    }

    fun open(state: CoinState, day: Long): CoinState {
        // A clock moved backwards must not award the same day again.
        if (day <= state.lastOpenDay) return state
        val streak = if (state.lastOpenDay != CoinState.NO_DAY && day - state.lastOpenDay == 1L) {
            state.consecutiveOpenDays.coerceIn(0, 999_999) + 1
        } else 1
        val award = DAILY_OPEN_COINS + if (streak % 5 == 0) FIVE_DAY_STREAK_COINS else 0
        return state.copy(balance = plusCoins(state.balance, award), lastOpenDay = day, consecutiveOpenDays = streak)
    }

    fun rememberFavorites(state: CoinState, aliases: Set<String>): CoinState =
        state.copy(knownFavorites = state.knownFavorites + aliases.filter(String::isNotBlank))

    fun addFavorite(state: CoinState, aliases: Set<String>, day: Long): CoinState {
        val clean = aliases.filter(String::isNotBlank).toSet()
        if (clean.isEmpty()) return state
        val wasKnown = clean.any { it in state.knownFavorites }
        val remembered = rememberFavorites(state, clean)
        if (wasKnown || day <= state.lastFavoriteRewardDay) return remembered
        return remembered.copy(
            balance = plusCoins(remembered.balance, FAVORITE_COINS),
            lastFavoriteRewardDay = day,
        )
    }

    /** The caller supplies observed foreground playback, not time spent in the player screen. */
    fun watch(state: CoinState, day: Long, observedMs: Long): CoinState {
        if (observedMs <= 0 || day < state.watchDay) return state
        val previous = if (day == state.watchDay) state.watchedMs.coerceIn(0, WATCH_TARGET_MS) else 0L
        val watched = previous + observedMs.coerceAtMost(WATCH_TARGET_MS - previous)
        val award = watched >= WATCH_TARGET_MS && day > state.lastWatchRewardDay
        return state.copy(
            watchDay = day,
            watchedMs = watched,
            lastWatchRewardDay = if (award) day else state.lastWatchRewardDay,
            balance = if (award) plusCoins(state.balance, WATCH_COINS) else state.balance,
        )
    }

    fun completedRewardedAd(state: CoinState): CoinState =
        state.copy(balance = plusCoins(state.balance, REWARDED_COINS))

    fun isAppAdFree(state: CoinState, nowMs: Long): Boolean = isActive(state.appAdFreeUntil, nowMs)
    fun isSectionAdFree(state: CoinState, section: AdSection, nowMs: Long): Boolean =
        isAppAdFree(state, nowMs) || isActive(state.sectionAdFreeUntil[section] ?: 0L, nowMs)
    fun isChannelOfferFree(state: CoinState, channelId: String, nowMs: Long): Boolean =
        isSectionAdFree(state, AdSection.CHANNELS, nowMs) || isActive(state.channelOfferFreeUntil[channelId] ?: 0L, nowMs)

    private fun isActive(until: Long, nowMs: Long): Boolean = until > 0L && until > nowMs

    fun buyAppPass(state: CoinState, nowMs: Long): CoinPurchase = when {
        isAppAdFree(state, nowMs) -> CoinPurchase(state, CoinPurchaseStatus.ALREADY_ACTIVE)
        state.balance < APP_PRICE -> CoinPurchase(state, CoinPurchaseStatus.INSUFFICIENT)
        else -> CoinPurchase(state.copy(balance = state.balance - APP_PRICE, appAdFreeUntil = expiry(nowMs, SECTION_DURATION_MS)), CoinPurchaseStatus.ACTIVATED)
    }

    fun buySectionPass(state: CoinState, section: AdSection, nowMs: Long): CoinPurchase = when {
        isSectionAdFree(state, section, nowMs) -> CoinPurchase(state, CoinPurchaseStatus.ALREADY_ACTIVE)
        state.balance < SECTION_PRICE -> CoinPurchase(state, CoinPurchaseStatus.INSUFFICIENT)
        else -> CoinPurchase(state.copy(
            balance = state.balance - SECTION_PRICE,
            sectionAdFreeUntil = state.sectionAdFreeUntil + (section to expiry(nowMs, SECTION_DURATION_MS)),
        ), CoinPurchaseStatus.ACTIVATED)
    }

    /** For a movie/episode the pass is consumed by this playback; for a channel it lasts one hour. */
    fun buyPlaybackPass(state: CoinState, section: AdSection, channelId: String?, nowMs: Long): CoinPurchase {
        val alreadyFree = if (channelId != null) isChannelOfferFree(state, channelId, nowMs)
        else isSectionAdFree(state, section, nowMs)
        return when {
            alreadyFree -> CoinPurchase(state, CoinPurchaseStatus.ALREADY_ACTIVE)
            state.balance < PLAYBACK_PRICE -> CoinPurchase(state, CoinPurchaseStatus.INSUFFICIENT)
            else -> CoinPurchase(state.copy(
                balance = state.balance - PLAYBACK_PRICE,
                channelOfferFreeUntil = if (channelId == null) state.channelOfferFreeUntil
                else state.channelOfferFreeUntil + (channelId to expiry(nowMs, CHANNEL_DURATION_MS)),
            ), CoinPurchaseStatus.ACTIVATED)
        }
    }

    fun pruneExpired(state: CoinState, nowMs: Long): CoinState = state.copy(
        appAdFreeUntil = state.appAdFreeUntil.takeIf { it > nowMs } ?: 0L,
        sectionAdFreeUntil = state.sectionAdFreeUntil.filterValues { it > nowMs },
        channelOfferFreeUntil = state.channelOfferFreeUntil.filterValues { it > nowMs },
    )

    private fun plusCoins(balance: Int, award: Int): Int =
        (balance.coerceAtLeast(0).toLong() + award).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    private fun expiry(nowMs: Long, duration: Long): Long =
        if (nowMs > Long.MAX_VALUE - duration) Long.MAX_VALUE else nowMs + duration
}
