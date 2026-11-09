package com.myiptv.clean.ads

import java.util.GregorianCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdsRulesTest {
    private val today = GregorianCalendar(2026, 9, 28)

    @Test fun neutralBirthDateClassificationAndBoundaries() {
        assertEquals(AdAudience.ADULT, AdsRules.audienceForBirthDate(28, 10, 2008, today))
        assertEquals(AdAudience.ADULT, AdsRules.audienceForBirthDate(27, 10, 2008, today))
        assertEquals(AdAudience.UNDER_AGE, AdsRules.audienceForBirthDate(29, 10, 2008, today))
        assertEquals(AdAudience.UNDER_AGE, AdsRules.audienceForBirthDate(28, 10, 2026, today))
    }

    @Test fun invalidOrFutureBirthDatesCannotEnableAds() {
        assertNull(AdsRules.audienceForBirthDate(29, 2, 2025, today))
        assertNull(AdsRules.audienceForBirthDate(0, 1, 2000, today))
        assertNull(AdsRules.audienceForBirthDate(1, 13, 2000, today))
        assertNull(AdsRules.audienceForBirthDate(29, 10, 2026, today))
        assertNull(AdsRules.audienceForBirthDate(1, 1, 1899, today))
    }

    @Test fun requestRequiresAdultConsentSdkAndNoRewardOrTv() {
        assertTrue(AdsRules.canRequest(false, AdAudience.ADULT, true, true, 0, 100))
        assertFalse(AdsRules.canRequest(true, AdAudience.ADULT, true, true, 0, 100))
        assertFalse(AdsRules.canRequest(false, AdAudience.UNDER_AGE, true, true, 0, 100))
        assertFalse(AdsRules.canRequest(false, AdAudience.UNKNOWN, true, true, 0, 100))
        assertFalse(AdsRules.canRequest(false, AdAudience.ADULT, false, true, 0, 100))
        assertFalse(AdsRules.canRequest(false, AdAudience.ADULT, true, false, 0, 100))
        assertFalse(AdsRules.canRequest(false, AdAudience.ADULT, true, true, 101, 100))
        assertTrue(AdsRules.canRequest(false, AdAudience.ADULT, true, true, 100, 100))
    }

    @Test fun placementsExcludeLivePlaybackKidsAndTelevision() {
        listOf("movies", "series", "search", "favorites").forEach { route ->
            assertTrue(AdsRules.canPlaceBanner(false, false, route))
            assertFalse(AdsRules.canPlaceBanner(true, false, route))
            assertFalse(AdsRules.canPlaceBanner(false, true, route))
        }
        listOf("home", "channels", "kids", "anime", "settings", "player/{id}", "media/{id}", "channel/{id}").forEach {
            assertFalse(AdsRules.canPlaceBanner(false, false, it))
        }
        assertTrue(AdsRules.canPlaceNativeOnHome(false, false))
        assertFalse(AdsRules.canPlaceNativeOnHome(true, false))
        assertFalse(AdsRules.canPlaceNativeOnHome(false, true))
        listOf("home", "movies", "series", "anime", "search", "favorites", "downloads", "history", "settings").forEach {
            assertTrue(AdsRules.canPresentConsent(false, it, false))
            assertFalse(AdsRules.canPresentConsent(true, it, false))
            assertFalse(AdsRules.canPresentConsent(false, it, true))
        }
        listOf("channels", "kids", "player/{id}", "media/{id}", "channel/{id}", "future-player").forEach {
            assertFalse(AdsRules.canPresentConsent(false, it, false))
        }
    }

    @Test fun adUnitsAreSeparatedBetweenDebugAndRelease() {
        val debug = adUnitsForBuild(true)
        val release = adUnitsForBuild(false)
        listOf(debug.banner, debug.appOpen, debug.rewardedInterstitial, debug.native, debug.rewarded).forEach {
            assertTrue(it.startsWith("ca-app-pub-3940256099942544/"))
        }
        assertEquals("ca-app-pub-8124327134735952/9929993078", release.banner)
        assertEquals("ca-app-pub-8124327134735952/3632003026", release.appOpen)
        assertEquals("ca-app-pub-8124327134735952/1706733175", release.rewardedInterstitial)
        assertEquals("ca-app-pub-8124327134735952/7913048878", release.native)
        assertEquals("ca-app-pub-8124327134735952/5286885530", release.rewarded)
    }

    @Test fun rewardExpiresAfterThirtyMinutesAndCannotOverflow() {
        val expiry = AdsRules.rewardExpiry(10_000)
        assertEquals(1_810_000, expiry)
        assertTrue(AdsRules.isAdFree(expiry, expiry - 1))
        assertFalse(AdsRules.isAdFree(expiry, expiry))
        assertEquals(Long.MAX_VALUE, AdsRules.rewardExpiry(Long.MAX_VALUE - 1))
    }

    @Test fun appOpenRequiresPreviousUseFreshAdAndCooldown() {
        val now = 10_000_000L
        assertFalse(AdsRules.canShowAppOpen(false, now - 1, 0, now))
        assertFalse(AdsRules.canShowAppOpen(true, 0, 0, now))
        assertFalse(AdsRules.canShowAppOpen(true, now + 1, 0, now))
        assertFalse(AdsRules.canShowAppOpen(true, now - AdsRules.APP_OPEN_MAX_AGE_MS, 0, now))
        assertFalse(AdsRules.canShowAppOpen(true, now - 1, now - AdsRules.APP_OPEN_COOLDOWN_MS + 1, now))
        assertFalse(AdsRules.canShowAppOpen(true, now - 1, now + 1, now))
        assertTrue(AdsRules.canShowAppOpen(true, now - 1, 0, now))
        assertTrue(AdsRules.canShowAppOpen(true, now - 1, now - AdsRules.APP_OPEN_COOLDOWN_MS, now))
    }
}
