package com.myiptv.clean.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveChannelHealthTest {
    @Test fun slowOrInconclusiveConnectionsNeverTriggerASkip() {
        val opening = LivePlaybackEvidence().withEvent(PlaybackEvent.Opening)
        for (probe in ChannelProbeResult.values()) {
            assertEquals(ChannelHealthDecision.WAIT, channelHealthDecision(3_500, opening, probe, false))
            assertEquals(ChannelHealthDecision.WAIT, channelHealthDecision(60_000, opening, probe, false))
        }
        val buffering = opening.withEvent(PlaybackEvent.Buffering(2f))
        assertEquals(ChannelHealthDecision.HEALTHY, channelHealthDecision(4_000, buffering, ChannelProbeResult.UNAVAILABLE, false))
        val playing = opening.withEvent(PlaybackEvent.Playing)
        assertEquals(ChannelHealthDecision.HEALTHY, channelHealthDecision(4_000, playing, ChannelProbeResult.INCONCLUSIVE, false))
    }

    @Test fun noSecondHttpSessionIsOpenedWhileVlcIsStillLoadingOrPlaying() {
        assertFalse(shouldProbeLiveChannel(LivePlaybackEvidence().withEvent(PlaybackEvent.Opening)))
        assertFalse(shouldProbeLiveChannel(LivePlaybackEvidence().withEvent(PlaybackEvent.Buffering(2f))))
        assertFalse(shouldProbeLiveChannel(LivePlaybackEvidence().withEvent(PlaybackEvent.Playing)))
        assertTrue(shouldProbeLiveChannel(LivePlaybackEvidence().withEvent(PlaybackEvent.Error)))
    }

    @Test fun aUserPauseAndAManualRetryDoNotCauseAnAutomaticSkip() {
        val paused = LivePlaybackEvidence().withEvent(PlaybackEvent.UserPaused).withEvent(PlaybackEvent.Error)
        assertEquals(ChannelHealthDecision.WAIT, channelHealthDecision(30_000, paused, ChannelProbeResult.UNAVAILABLE, true))
        assertFalse(shouldProbeLiveChannel(paused))
        val retrying = paused.withEvent(PlaybackEvent.UserResumed).withEvent(PlaybackEvent.Opening)
        assertEquals(ChannelHealthDecision.WAIT, channelHealthDecision(30_000, retrying, ChannelProbeResult.INCONCLUSIVE, true))
        assertEquals(ChannelHealthDecision.HEALTHY, channelHealthDecision(30_000, retrying.withEvent(PlaybackEvent.Playing), ChannelProbeResult.INCONCLUSIVE, true))
    }

    @Test fun aTerminalVlcFailureWaitsForTheCheckAndMovesToTheNextChannel() {
        val failed = LivePlaybackEvidence().withEvent(PlaybackEvent.Error)
        assertEquals(ChannelHealthDecision.WAIT, channelHealthDecision(3_499, failed, ChannelProbeResult.UNAVAILABLE, false))
        assertEquals(ChannelHealthDecision.NEXT, channelHealthDecision(3_500, failed, ChannelProbeResult.UNAVAILABLE, false))
        assertEquals(ChannelHealthDecision.RETRY, channelHealthDecision(4_000, failed, ChannelProbeResult.INCONCLUSIVE, false))
        assertEquals(ChannelHealthDecision.WAIT, channelHealthDecision(40_000, failed, ChannelProbeResult.INCONCLUSIVE, true))
        assertEquals(ChannelHealthDecision.NEXT, channelHealthDecision(4_000, LivePlaybackEvidence().withEvent(PlaybackEvent.Ended), ChannelProbeResult.UNAVAILABLE, false))
    }

    @Test fun responsiveUrlOrPreviouslyPlayingSignalGetsOneRetryBeforeSkipping() {
        val failed = LivePlaybackEvidence().withEvent(PlaybackEvent.Error)
        assertEquals(ChannelHealthDecision.RETRY, channelHealthDecision(4_000, failed, ChannelProbeResult.RESPONDING, false))
        assertEquals(ChannelHealthDecision.NEXT, channelHealthDecision(4_000, failed, ChannelProbeResult.RESPONDING, true))
        val interrupted = LivePlaybackEvidence().withEvent(PlaybackEvent.Playing).withEvent(PlaybackEvent.Error)
        assertEquals(ChannelHealthDecision.RETRY, channelHealthDecision(4_000, interrupted, ChannelProbeResult.INCONCLUSIVE, false))
        assertEquals(ChannelHealthDecision.WAIT, channelHealthDecision(4_000, interrupted, ChannelProbeResult.INCONCLUSIVE, true))
        assertEquals(ChannelHealthDecision.NEXT, channelHealthDecision(4_000, interrupted, ChannelProbeResult.UNAVAILABLE, true))
    }

    @Test fun failedChannelsKeepOrderAndNeverLoopForever() {
        val ids = listOf("a", "b", "c")
        assertEquals(1, nextUnfailedChannelIndex(ids, "a", setOf("a")))
        assertEquals(2, nextUnfailedChannelIndex(ids, "b", setOf("a", "b")))
        assertNull(nextUnfailedChannelIndex(ids, "c", setOf("a", "b", "c")))
        assertEquals(0, nextUnfailedChannelIndex(ids, "c", setOf("b", "c")))
        assertNull(nextUnfailedChannelIndex(emptyList(), "a", emptySet()))
    }

    @Test fun remoteKeysMoveInBothDirectionsAndWrap() {
        assertEquals(1, adjacentChannelIndex(3, 0, 1))
        assertEquals(2, adjacentChannelIndex(3, 0, -1))
        assertEquals(0, adjacentChannelIndex(3, 2, 1))
        assertEquals(0, adjacentChannelIndex(3, -1, 1))
        assertEquals(2, adjacentChannelIndex(3, -1, -1))
        assertNull(adjacentChannelIndex(0, 0, 1))
        assertNull(adjacentChannelIndex(3, 0, 0))
    }
}
