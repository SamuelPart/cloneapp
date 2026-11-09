package com.myiptv.clean.ads

import org.junit.Assert.assertEquals
import org.junit.Test

class PendingRewardOfferTest {
    @Test fun leavingTheOriginOrSwitchingChannelsCancelsEvenIfAnAdIsReady() {
        assertEquals(PendingRewardDecision.CANCEL, pendingRewardDecision(false, true, true))
        assertEquals(PendingRewardDecision.CANCEL, pendingRewardDecision(true, false, true))
        assertEquals(PendingRewardDecision.CANCEL, pendingRewardDecision(false, false, false))
    }

    @Test fun expiredAdNewPassOrLostConsentContinuesFreeWithoutCharging() {
        assertEquals(PendingRewardDecision.CONTINUE_FREE, pendingRewardDecision(true, true, false))
        assertEquals(PendingRewardDecision.AVAILABLE, pendingRewardDecision(true, true, true))
    }

    @Test fun redemptionOnlyHappensForAStillAvailableInvitation() {
        val initial = CoinState(balance = 20)
        fun spendIfAvailable(sameOrigin: Boolean, sameContent: Boolean, eligible: Boolean): CoinState =
            when (pendingRewardDecision(sameOrigin, sameContent, eligible)) {
                PendingRewardDecision.AVAILABLE -> CoinRules.buyPlaybackPass(initial, AdSection.CHANNELS, "channel-a", 1_000).state
                PendingRewardDecision.CANCEL, PendingRewardDecision.CONTINUE_FREE -> initial
            }
        assertEquals(initial, spendIfAvailable(false, true, true))
        assertEquals(initial, spendIfAvailable(true, false, true))
        assertEquals(initial, spendIfAvailable(true, true, false))
        assertEquals(10, spendIfAvailable(true, true, true).balance)
    }
}
