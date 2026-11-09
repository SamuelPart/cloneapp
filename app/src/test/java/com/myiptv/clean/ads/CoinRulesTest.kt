package com.myiptv.clean.ads

import java.util.GregorianCalendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinRulesTest {
    @Test fun dailyOpenAndFiveDayStreakAreAwardedOncePerDistinctDay() {
        var state = CoinState()
        for (day in 10L..14L) state = CoinRules.open(state, day)
        assertEquals(20, state.balance) // 5 x 2 plus the five-day bonus.
        assertEquals(5, state.consecutiveOpenDays)
        assertEquals(state, CoinRules.open(state, 14L))
        assertEquals(state, CoinRules.open(state, 12L)) // A backwards clock cannot double-award.
        for (day in 15L..19L) state = CoinRules.open(state, day)
        assertEquals(40, state.balance)
        assertEquals(10, state.consecutiveOpenDays)
        state = CoinRules.open(state, 21L)
        assertEquals(1, state.consecutiveOpenDays)
        assertEquals(42, state.balance)
    }

    @Test fun consecutiveDatesSurviveDaylightSavingTimeChanges() {
        val zone = TimeZone.getTimeZone("America/New_York")
        val dayBefore = GregorianCalendar(zone).apply { clear(); set(2026, 2, 7, 12, 0) }.timeInMillis
        val dayAfter = GregorianCalendar(zone).apply { clear(); set(2026, 2, 8, 12, 0) }.timeInMillis
        assertEquals(1L, CoinRules.localDay(dayAfter, zone) - CoinRules.localDay(dayBefore, zone))
        assertEquals(-1L, CoinRules.localDay(-1, TimeZone.getTimeZone("UTC")))
    }

    @Test fun onlyANewFavoriteCanEarnAndAtMostOncePerDay() {
        var state = CoinRules.rememberFavorites(CoinState(), setOf("saved", "old-alias"))
        state = CoinRules.addFavorite(state, setOf("saved", "old-alias"), 1)
        assertEquals(0, state.balance)
        state = CoinRules.addFavorite(state, setOf("movie", "alias"), 1)
        assertEquals(3, state.balance)
        state = CoinRules.addFavorite(state, setOf("another"), 1)
        assertEquals(3, state.balance)
        // Removing and re-adding the same title or one of its aliases is not a new favorite.
        state = CoinRules.addFavorite(state, setOf("alias"), 2)
        state = CoinRules.addFavorite(state, setOf("another"), 2)
        assertEquals(3, state.balance)
        state = CoinRules.addFavorite(state, setOf("truly-new"), 2)
        assertEquals(6, state.balance)
        state = CoinRules.addFavorite(state, setOf("new-but-clock-went-back"), 1)
        assertEquals(6, state.balance)
    }

    @Test fun foregroundPlaybackAccumulatesAcrossSessionsAndRewardsOncePerDay() {
        var state = CoinRules.watch(CoinState(), 1, 600_000L)
        assertEquals(0, state.balance)
        state = CoinRules.watch(state, 1, 599_999L)
        assertEquals(0, state.balance)
        state = CoinRules.watch(state, 1, 1L)
        assertEquals(5, state.balance)
        state = CoinRules.watch(state, 1, 600_000L)
        assertEquals(5, state.balance)
        state = CoinRules.watch(state, 0, 600_000L)
        assertEquals(5, state.balance)
        state = CoinRules.watch(state, 2, 1_200_000L)
        assertEquals(10, state.balance)
    }

    @Test fun webPlaybackMustAdvanceWithoutASeekOrStall() {
        assertEquals(0L, observedWebPlaybackMs(null, 5000, 5000))
        assertEquals(0L, observedWebPlaybackMs(1000, 1000, 5000))
        assertEquals(0L, observedWebPlaybackMs(5000, 1000, 5000))
        assertEquals(0L, observedWebPlaybackMs(1000, 60_000, 5000))
        assertEquals(3_000L, observedWebPlaybackMs(1000, 4000, 5000))
        assertEquals(5_000L, observedWebPlaybackMs(1000, 7000, 5000))
    }

    @Test fun sdkRewardIsTenCoinsAndBalanceCannotOverflow() {
        assertEquals(10, CoinRules.completedRewardedAd(CoinState()).balance)
        assertEquals(Int.MAX_VALUE, CoinRules.completedRewardedAd(CoinState(balance = Int.MAX_VALUE - 1)).balance)
    }

    @Test fun rewardCallbackCreditsOnceAndFailureCannotCredit() {
        val completed = RewardCallbackGate()
        var balance = 0
        // Closing or skipping without an earned callback does not credit anything.
        assertEquals(0, balance)
        completed.onEarned { balance += 10 }
        completed.onEarned { balance += 10 }
        assertEquals(10, balance)
        val failed = RewardCallbackGate()
        failed.onFailedToShow()
        failed.onEarned { balance += 10 }
        assertEquals(10, balance)
    }

    @Test fun sectionPassOnlyHidesItsOwnAdsAndExpiresAt24Hours() {
        val purchase = CoinRules.buySectionPass(CoinState(balance = 50), AdSection.MOVIES, 1_000)
        assertEquals(CoinPurchaseStatus.ACTIVATED, purchase.status)
        assertEquals(10, purchase.state.balance)
        val expiry = 1_000 + CoinRules.SECTION_DURATION_MS
        assertTrue(CoinRules.isSectionAdFree(purchase.state, AdSection.MOVIES, expiry - 1))
        assertFalse(CoinRules.isSectionAdFree(purchase.state, AdSection.SERIES, expiry - 1))
        assertFalse(CoinRules.isSectionAdFree(purchase.state, AdSection.MOVIES, expiry))
        assertEquals(CoinPurchaseStatus.ALREADY_ACTIVE, CoinRules.buySectionPass(purchase.state, AdSection.MOVIES, 1_001).status)
        assertTrue(CoinRules.pruneExpired(purchase.state, expiry).sectionAdFreeUntil.isEmpty())
    }

    @Test fun appPassCoversAllSectionsAndCannotBeRepurchasedWhileActive() {
        val result = CoinRules.buyAppPass(CoinState(balance = 100), 1_000)
        assertEquals(CoinPurchaseStatus.ACTIVATED, result.status)
        assertEquals(0, result.state.balance)
        AdSection.values().forEach { assertTrue(CoinRules.isSectionAdFree(result.state, it, 1_001)) }
        assertEquals(CoinPurchaseStatus.ALREADY_ACTIVE, CoinRules.buyAppPass(result.state, 1_001).status)
        assertEquals(CoinPurchaseStatus.INSUFFICIENT, CoinRules.buyAppPass(CoinState(balance = 99), 1_000).status)
        assertFalse(CoinRules.isAppAdFree(result.state, 1_000 + CoinRules.SECTION_DURATION_MS))
    }

    @Test fun moviePassIsPerPlayAndChannelPassIsOneHourAndPerChannel() {
        val movie = CoinRules.buyPlaybackPass(CoinState(balance = 20), AdSection.MOVIES, null, 1_000)
        assertEquals(10, movie.state.balance)
        assertTrue(movie.state.channelOfferFreeUntil.isEmpty())
        val channel = CoinRules.buyPlaybackPass(movie.state, AdSection.CHANNELS, "channel-a", 1_000)
        assertEquals(0, channel.state.balance)
        assertTrue(CoinRules.isChannelOfferFree(channel.state, "channel-a", 1_000 + CoinRules.CHANNEL_DURATION_MS - 1))
        assertFalse(CoinRules.isChannelOfferFree(channel.state, "channel-b", 1_001))
        assertFalse(CoinRules.isChannelOfferFree(channel.state, "channel-a", 1_000 + CoinRules.CHANNEL_DURATION_MS))
        assertEquals(CoinPurchaseStatus.ALREADY_ACTIVE, CoinRules.buyPlaybackPass(channel.state, AdSection.CHANNELS, "channel-a", 1_001).status)
        assertEquals(CoinPurchaseStatus.INSUFFICIENT, CoinRules.buyPlaybackPass(channel.state, AdSection.MOVIES, null, 1_001).status)
    }

    @Test fun passesAreNotChargedWhenAnOverlappingPassAlreadyApplies() {
        val app = CoinRules.buyAppPass(CoinState(balance = 200), 1_000).state
        val section = CoinRules.buySectionPass(app, AdSection.CHANNELS, 1_001)
        assertEquals(CoinPurchaseStatus.ALREADY_ACTIVE, section.status)
        val channel = CoinRules.buyPlaybackPass(app, AdSection.CHANNELS, "c", 1_001)
        assertEquals(CoinPurchaseStatus.ALREADY_ACTIVE, channel.status)
        assertEquals(100, channel.state.balance)
        val nearOverflow = CoinRules.buySectionPass(CoinState(balance = 40), AdSection.HOME, Long.MAX_VALUE - 1)
        assertEquals(Long.MAX_VALUE, nearOverflow.state.sectionAdFreeUntil[AdSection.HOME])
    }

    @Test fun childrenAndFamilyGenresRemainProtected() {
        listOf("Kids", "Niños", "Familia", "Family").forEach { assertTrue(isKidsGenre(listOf("Drama", it))) }
        assertFalse(isKidsGenre(listOf("Drama", "Action")))
    }
}
