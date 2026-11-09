package com.myiptv.clean.ads

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Local, non-transferable wallet. Call its methods on the main thread. */
internal class CoinWallet(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("samplay_coins", Context.MODE_PRIVATE)
    var state by mutableStateOf(readState())
        private set

    fun recordOpen(nowMs: Long = System.currentTimeMillis()) =
        update(CoinRules.open(state, CoinRules.localDay(nowMs)))

    fun rememberFavorites(aliases: Set<String>) = update(CoinRules.rememberFavorites(state, aliases))

    fun recordNewFavorite(aliases: Set<String>, nowMs: Long = System.currentTimeMillis()) =
        update(CoinRules.addFavorite(state, aliases, CoinRules.localDay(nowMs)))

    fun recordPlayback(observedMs: Long, nowMs: Long = System.currentTimeMillis()) =
        update(CoinRules.watch(state, CoinRules.localDay(nowMs), observedMs.coerceIn(0L, MAX_PLAYBACK_SAMPLE_MS)))

    /** Only AdsController's once-per-impression SDK reward callback calls this. */
    fun creditCompletedAd() = update(CoinRules.completedRewardedAd(state))

    fun buyAppPass(nowMs: Long = System.currentTimeMillis()): CoinPurchaseStatus =
        purchase(CoinRules.buyAppPass(state, nowMs))

    fun buySectionPass(section: AdSection, nowMs: Long = System.currentTimeMillis()): CoinPurchaseStatus =
        purchase(CoinRules.buySectionPass(state, section, nowMs))

    fun buyPlaybackPass(section: AdSection, channelId: String?, nowMs: Long = System.currentTimeMillis()): CoinPurchaseStatus =
        purchase(CoinRules.buyPlaybackPass(state, section, channelId, nowMs))

    fun isAppAdFree(nowMs: Long = System.currentTimeMillis()): Boolean = CoinRules.isAppAdFree(state, nowMs)
    fun isSectionAdFree(section: AdSection, nowMs: Long = System.currentTimeMillis()): Boolean =
        CoinRules.isSectionAdFree(state, section, nowMs)
    fun isChannelOfferFree(channelId: String, nowMs: Long = System.currentTimeMillis()): Boolean =
        CoinRules.isChannelOfferFree(state, channelId, nowMs)
    fun refreshExpired(nowMs: Long = System.currentTimeMillis()) = update(CoinRules.pruneExpired(state, nowMs))

    private fun purchase(result: CoinPurchase): CoinPurchaseStatus {
        if (result.status == CoinPurchaseStatus.ACTIVATED) update(result.state)
        return result.status
    }

    private fun update(next: CoinState) {
        if (next == state) return
        state = next
        // One atomic preferences update keeps the balance and entitlement together across restarts.
        prefs.edit()
            .putInt("balance", next.balance)
            .putLong("last_open_day", next.lastOpenDay)
            .putInt("open_streak", next.consecutiveOpenDays)
            .putLong("last_favorite_day", next.lastFavoriteRewardDay)
            .putStringSet("known_favorites", next.knownFavorites.toSet())
            .putLong("watch_day", next.watchDay)
            .putLong("watched_ms", next.watchedMs)
            .putLong("last_watch_day", next.lastWatchRewardDay)
            .putLong("app_ad_free_until", next.appAdFreeUntil)
            .putStringSet("section_ad_free_until", next.sectionAdFreeUntil.map { "${it.key.name}|${it.value}" }.toSet())
            .putStringSet("channel_offer_free_until", next.channelOfferFreeUntil.map { "${it.value}|${it.key}" }.toSet())
            .apply()
    }

    private fun readState(): CoinState {
        val sections = prefs.getStringSet("section_ad_free_until", emptySet()).orEmpty().mapNotNull { entry ->
            val pieces = entry.split('|', limit = 2)
            if (pieces.size != 2) return@mapNotNull null
            val section = AdSection.values().firstOrNull { it.name == pieces[0] } ?: return@mapNotNull null
            val until = pieces[1].toLongOrNull() ?: return@mapNotNull null
            section to until
        }.toMap()
        val channels = prefs.getStringSet("channel_offer_free_until", emptySet()).orEmpty().mapNotNull { entry ->
            val pieces = entry.split('|', limit = 2)
            if (pieces.size != 2 || pieces[1].isBlank()) return@mapNotNull null
            val until = pieces[0].toLongOrNull() ?: return@mapNotNull null
            pieces[1] to until
        }.toMap()
        return CoinState(
            balance = prefs.getInt("balance", 0).coerceAtLeast(0),
            lastOpenDay = prefs.getLong("last_open_day", CoinState.NO_DAY),
            consecutiveOpenDays = prefs.getInt("open_streak", 0).coerceIn(0, 1_000_000),
            lastFavoriteRewardDay = prefs.getLong("last_favorite_day", CoinState.NO_DAY),
            knownFavorites = prefs.getStringSet("known_favorites", emptySet()).orEmpty().toSet(),
            watchDay = prefs.getLong("watch_day", CoinState.NO_DAY),
            watchedMs = prefs.getLong("watched_ms", 0).coerceIn(0, CoinRules.WATCH_TARGET_MS),
            lastWatchRewardDay = prefs.getLong("last_watch_day", CoinState.NO_DAY),
            appAdFreeUntil = prefs.getLong("app_ad_free_until", 0),
            sectionAdFreeUntil = sections,
            channelOfferFreeUntil = channels,
        )
    }

    private companion object { const val MAX_PLAYBACK_SAMPLE_MS = 6_000L }
}
