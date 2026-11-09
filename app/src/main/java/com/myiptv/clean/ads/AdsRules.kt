package com.myiptv.clean.ads

import java.util.Calendar
import java.util.GregorianCalendar

/** We retain only the resulting category, not a user's date of birth. */
internal enum class AdAudience { UNKNOWN, ADULT, UNDER_AGE }

internal object AdsRules {
    const val REWARD_DURATION_MS = 30 * 60 * 1000L
    const val APP_OPEN_COOLDOWN_MS = 60 * 60 * 1000L
    const val APP_OPEN_MAX_AGE_MS = 4 * 60 * 60 * 1000L

    /** Returns null for malformed or future dates; all three fields start out blank. */
    fun audienceForBirthDate(day: Int, month: Int, year: Int, today: Calendar): AdAudience? {
        if (year < 1900 || month !in 1..12 || day !in 1..31) return null
        val birth = GregorianCalendar(today.timeZone).apply {
            isLenient = false
            clear()
            set(year, month - 1, day)
        }
        val validBirth = try { birth.timeInMillis } catch (_: IllegalArgumentException) { return null }
        val currentDay = (today.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (validBirth > currentDay.timeInMillis) return null
        val cutoff = (currentDay.clone() as Calendar).apply { add(Calendar.YEAR, -18) }
        return if (validBirth <= cutoff.timeInMillis) AdAudience.ADULT else AdAudience.UNDER_AGE
    }

    fun canRequest(
        isTv: Boolean,
        audience: AdAudience,
        consentAllowsRequests: Boolean,
        sdkReady: Boolean,
        adFreeUntilMs: Long,
        nowMs: Long,
    ): Boolean = !isTv && audience == AdAudience.ADULT && consentAllowsRequests && sdkReady &&
        !isAdFree(adFreeUntilMs, nowMs)

    /** Keep ad placements away from Kids, live TV, details and every player route. */
    fun canPlaceBanner(isTvDevice: Boolean, isTvLayout: Boolean, route: String): Boolean =
        !isTvDevice && !isTvLayout && route in setOf("movies", "series", "search", "favorites")

    fun canPlaceNativeOnHome(isTvDevice: Boolean, isTvLayout: Boolean): Boolean = !isTvDevice && !isTvLayout

    /** Only known browsing routes may open UMP; unknown/future player routes fail closed. */
    fun canPresentConsent(isTvDevice: Boolean, route: String, notificationPending: Boolean): Boolean =
        !isTvDevice && !notificationPending && route in setOf(
            "home", "movies", "series", "anime", "search", "favorites", "downloads", "history", "settings"
        )

    fun isAdFree(untilMs: Long, nowMs: Long): Boolean = untilMs > 0L && nowMs < untilMs

    /** Reward from one completed ad is exactly 30 minutes; never grant it when an ad is skipped. */
    fun rewardExpiry(nowMs: Long): Long = if (nowMs > Long.MAX_VALUE - REWARD_DURATION_MS) {
        Long.MAX_VALUE
    } else nowMs + REWARD_DURATION_MS

    fun canShowAppOpen(hasUsedApp: Boolean, loadedAtMs: Long, lastShownAtMs: Long, nowMs: Long): Boolean {
        if (!hasUsedApp || loadedAtMs <= 0L || nowMs < loadedAtMs || nowMs - loadedAtMs >= APP_OPEN_MAX_AGE_MS) return false
        // If the wall clock was set back, do not circumvent the frequency cap.
        return lastShownAtMs <= 0L || (nowMs >= lastShownAtMs && nowMs - lastShownAtMs >= APP_OPEN_COOLDOWN_MS)
    }
}
