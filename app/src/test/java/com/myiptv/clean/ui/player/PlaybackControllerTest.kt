package com.myiptv.clean.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackControllerTest {
    private class FakeEngine : PlaybackEngine {
        var callback: ((PlaybackEvent) -> Unit)? = null
        var attached = 0
        var detached = 0
        var loaded = 0
        var played = 0
        var paused = 0
        var stopped = 0
        var releases = 0
        var playing = false
        var attachFails = false
        var loadFails = false
        var position = PlaybackProgress(0, 0, false)
        var lastSeek: Long? = null
        var tracks: List<PlayerAudioTrack> = emptyList()
        var selected: Int? = null
        var selectionWorks = true

        override fun setListener(listener: ((PlaybackEvent) -> Unit)?) { this.callback = listener }
        override fun attach() { attached++; if (attachFails) throw IllegalStateException("attach failed") }
        override fun detach() { detached++ }
        override fun load() { loaded++; if (loadFails) throw IllegalStateException("load failed") }
        override fun play() { played++; playing = true }
        override fun pause() { paused++; playing = false }
        override fun stop() { stopped++; playing = false }
        override fun isPlaying() = playing
        override fun progress() = position
        override fun seekTo(timeMs: Long) { lastSeek = timeMs; position = position.copy(timeMs = timeMs) }
        override fun audioTracks() = tracks
        override fun selectedAudioTrack() = selected
        override fun selectAudioTrack(id: Int): Boolean {
            if (selectionWorks) selected = id
            return selectionWorks
        }
        override fun release() { releases++ }
        fun emit(event: PlaybackEvent) { callback?.invoke(event) }
    }

    private fun start(engine: FakeEngine): PlaybackController = PlaybackController(engine) {}.also {
        it.onForeground()
        it.onSurfaceReady()
    }

    @Test
    fun waitsForForegroundAndSurfaceAndDoesNotStartTwice() {
        val engine = FakeEngine()
        val controller = PlaybackController(engine) {}
        controller.onSurfaceReady()
        assertEquals(0, engine.loaded)
        controller.onForeground()
        controller.onForeground()
        controller.onSurfaceReady()
        assertEquals(1, engine.attached)
        assertEquals(1, engine.loaded)
        assertEquals(1, engine.played)
        engine.emit(PlaybackEvent.Playing)
        assertTrue(controller.state.isPlaying)
        assertFalse(controller.state.isBuffering)
    }

    @Test
    fun backgroundStopsAudioAndRestoresPositionAndPausedState() {
        val engine = FakeEngine()
        engine.position = PlaybackProgress(65_000, 200_000, true)
        val controller = start(engine)
        engine.emit(PlaybackEvent.Playing)
        controller.pause()
        assertEquals(1, engine.paused)
        controller.onBackground()
        assertEquals(1, engine.stopped)
        assertEquals(1, engine.detached)
        assertNull(engine.callback)

        engine.position = PlaybackProgress(0, 200_000, true)
        controller.onForeground()
        assertEquals(2, engine.loaded)
        engine.emit(PlaybackEvent.ProgressChanged)
        assertEquals(65_000L, engine.lastSeek)
        engine.emit(PlaybackEvent.Playing)
        assertEquals(2, engine.paused)
        assertFalse(controller.state.isPlaying)
        controller.play()
        assertTrue(controller.state.isPlaying)
        assertEquals(3, engine.played)
    }

    @Test
    fun pendingPauseIsAppliedAfterOpeningAndCannotPauseSignalIsHonest() {
        val engine = FakeEngine()
        val controller = start(engine)
        engine.playing = false // The source is still opening.
        controller.pause()
        assertEquals(0, engine.paused)
        engine.playing = true
        engine.emit(PlaybackEvent.Playing)
        assertEquals(1, engine.paused)
        assertFalse(controller.state.isPlaying)

        controller.play()
        engine.emit(PlaybackEvent.PausableChanged(false))
        controller.pause()
        assertEquals(1, engine.paused)
        assertTrue(controller.state.isPlaying)
        assertNotNull(controller.state.notice)
    }

    @Test
    fun selectedAudioTrackIsRestoredWhenItAppearsAfterBackgrounding() {
        val engine = FakeEngine()
        engine.tracks = listOf(PlayerAudioTrack(4, "Principal"), PlayerAudioTrack(9, "Alternativa"))
        engine.selected = 4
        val controller = start(engine)
        controller.refreshAudioTracks()
        controller.selectAudioTrack(9)
        controller.onBackground()

        // Tracks can arrive asynchronously after VLC has restarted.
        engine.selected = 4
        engine.tracks = listOf(PlayerAudioTrack(4, "Principal"))
        controller.onForeground()
        engine.emit(PlaybackEvent.Playing)
        assertEquals(4, engine.selected)
        engine.tracks = listOf(PlayerAudioTrack(4, "Principal"), PlayerAudioTrack(9, "Alternativa"))
        engine.emit(PlaybackEvent.AudioTracksChanged)
        assertEquals(9, engine.selected)
        assertEquals(9, controller.state.selectedAudioTrack)
    }

    @Test
    fun backgroundingAnEndedVideoDoesNotStartItAgainUntilRequested() {
        val engine = FakeEngine()
        val controller = start(engine)
        engine.emit(PlaybackEvent.Ended)
        controller.onBackground()
        controller.onForeground()
        assertEquals(1, engine.loaded)
        assertTrue(controller.state.ended)
        controller.togglePlayback()
        assertEquals(2, engine.loaded)
    }

    @Test
    fun liveStreamDoesNotExposeOrPerformSeeking() {
        val engine = FakeEngine()
        engine.position = PlaybackProgress(15_000, 0, false)
        val controller = start(engine)
        engine.emit(PlaybackEvent.ProgressChanged)
        controller.seekBy(10_000)
        controller.seekTo(20_000)
        assertFalse(controller.state.isSeekable)
        assertNull(engine.lastSeek)
        controller.onBackground()
        controller.onForeground()
        engine.emit(PlaybackEvent.ProgressChanged)
        assertNull(engine.lastSeek)
    }

    @Test
    fun seekingIsClampedToTheAvailableDuration() {
        val engine = FakeEngine()
        engine.position = PlaybackProgress(3_000, 100_000, true)
        val controller = start(engine)
        controller.seekBy(-10_000)
        assertEquals(0L, engine.lastSeek)
        controller.seekTo(500_000)
        assertEquals(100_000L, engine.lastSeek)
        controller.seekBy(10_000)
        assertEquals(100_000L, engine.lastSeek)
        assertTrue(controller.state.isSeekable)
    }

    @Test
    fun audioTracksUseVlcIdsAndHandleUnavailableOrFailedSelections() {
        val engine = FakeEngine()
        engine.tracks = listOf(PlayerAudioTrack(-1, "Disable"), PlayerAudioTrack(12, "Español"), PlayerAudioTrack(25, ""), PlayerAudioTrack(25, "Duplicate"))
        engine.selected = 12
        val controller = start(engine)
        controller.refreshAudioTracks()
        assertEquals(listOf(PlayerAudioTrack(12, "Español"), PlayerAudioTrack(25, "Pista de audio 2")), controller.state.audioTracks)
        assertEquals(12, controller.state.selectedAudioTrack)
        controller.selectAudioTrack(25)
        assertEquals(25, engine.selected)
        assertEquals(25, controller.state.selectedAudioTrack)
        engine.selectionWorks = false
        controller.selectAudioTrack(12)
        assertNotNull(controller.state.notice)
        controller.clearNotice()
        controller.selectAudioTrack(99)
        assertNotNull(controller.state.notice)
    }

    @Test
    fun startupFailuresDetachSurfacesAndCanBeRetried() {
        val engine = FakeEngine().apply { attachFails = true }
        val controller = start(engine)
        assertNotNull(controller.state.error)
        assertEquals(1, engine.detached)
        assertNull(engine.callback)

        engine.attachFails = false
        engine.loadFails = true
        controller.togglePlayback()
        assertNotNull(controller.state.error)
        assertEquals(2, engine.detached)
        assertEquals(1, engine.stopped)

        engine.loadFails = false
        controller.togglePlayback()
        assertNull(controller.state.error)
        assertEquals(2, engine.loaded)
        assertEquals(1, engine.played)
    }

    @Test
    fun errorAndEndAllowRetryWithoutLeakingSurfaces() {
        val engine = FakeEngine()
        val controller = start(engine)
        engine.emit(PlaybackEvent.Error)
        assertNotNull(controller.state.error)
        assertFalse(controller.state.isPlaying)
        engine.emit(PlaybackEvent.Playing) // An old callback must not clear an error.
        assertNotNull(controller.state.error)
        controller.togglePlayback()
        assertNull(controller.state.error)
        assertEquals(2, engine.loaded)
        assertEquals(1, engine.stopped)
        assertEquals(1, engine.detached)
        engine.emit(PlaybackEvent.Ended)
        assertTrue(controller.state.ended)
        controller.togglePlayback()
        assertFalse(controller.state.ended)
        assertEquals(3, engine.loaded)
    }

    @Test
    fun releaseIsIdempotentAndPreventsLatePlayback() {
        val engine = FakeEngine()
        val controller = start(engine)
        controller.release()
        controller.release()
        controller.onForeground()
        controller.onSurfaceReady()
        assertEquals(1, engine.releases)
        assertEquals(1, engine.stopped)
        assertEquals(1, engine.detached)
        assertEquals(1, engine.loaded)
        assertNull(engine.callback)
    }
}
