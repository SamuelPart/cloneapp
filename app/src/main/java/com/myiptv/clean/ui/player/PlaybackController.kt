package com.myiptv.clean.ui.player

/** Platform-independent coordination of playback, foreground lifecycle and player controls. */
internal data class PlayerAudioTrack(val id: Int, val name: String)

internal data class PlaybackProgress(val timeMs: Long, val durationMs: Long, val seekable: Boolean)

internal data class PlaybackState(
    val isPlaying: Boolean = true,
    val isBuffering: Boolean = true,
    val canPause: Boolean? = null,
    val isSeekable: Boolean = false,
    val timeMs: Long = 0L,
    val durationMs: Long = 0L,
    val ended: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val audioTracks: List<PlayerAudioTrack> = emptyList(),
    val selectedAudioTrack: Int? = null
)

internal sealed interface PlaybackEvent {
    data object Opening : PlaybackEvent
    data class Buffering(val percent: Float) : PlaybackEvent
    data object Playing : PlaybackEvent
    data object Paused : PlaybackEvent
    data object Ended : PlaybackEvent
    data object Error : PlaybackEvent
    data object ProgressChanged : PlaybackEvent
    data class PausableChanged(val pausable: Boolean) : PlaybackEvent
    data object AudioTracksChanged : PlaybackEvent
}

/** Implementations and callbacks are used on Android's main thread. */
internal interface PlaybackEngine {
    fun setListener(listener: ((PlaybackEvent) -> Unit)?)
    fun attach()
    fun detach()
    fun load()
    fun play()
    fun pause()
    fun stop()
    fun isPlaying(): Boolean
    fun progress(): PlaybackProgress
    fun seekTo(timeMs: Long)
    fun audioTracks(): List<PlayerAudioTrack>
    fun selectedAudioTrack(): Int?
    fun selectAudioTrack(id: Int): Boolean
    fun release()
}

internal class PlaybackController(
    private val engine: PlaybackEngine,
    private val onStateChanged: (PlaybackState) -> Unit
) {
    var state: PlaybackState = PlaybackState()
        private set

    private var foreground = false
    private var surfaceReady = false
    private var active = false
    private var attached = false
    private var released = false
    private var wantsToPlay = true
    private var resumePositionMs = 0L
    private var pendingRestoreMs: Long? = null
    private var resumeAudioTrack: Int? = null
    private var pendingAudioTrack: Int? = null

    fun onSurfaceReady() {
        if (released) return
        surfaceReady = true
        activateIfReady()
    }

    fun onForeground() {
        if (released) return
        foreground = true
        activateIfReady()
    }

    /** Stop network/audio in the background; restore seekable video and the user's pause state. */
    fun onBackground() {
        if (released) return
        foreground = false
        if (active) {
            val progress = runCatching { engine.progress() }.getOrNull()
            resumePositionMs = if (progress?.seekable == true) progress.timeMs.coerceAtLeast(0L) else 0L
            resumeAudioTrack = runCatching { engine.selectedAudioTrack() }.getOrNull() ?: state.selectedAudioTrack
            pendingRestoreMs = null
            pendingAudioTrack = null
            deactivate()
        }
        update(state.copy(isPlaying = false, isBuffering = false))
    }

    fun togglePlayback() {
        when (centerPlaybackAction(state.error != null, state.ended, wantsToPlay)) {
            CenterPlaybackAction.RETRY, CenterPlaybackAction.RESTART -> restart()
            CenterPlaybackAction.PAUSE -> pause()
            CenterPlaybackAction.PLAY -> play()
        }
    }

    fun play() {
        if (released) return
        if (state.error != null || state.ended) {
            restart()
            return
        }
        wantsToPlay = true
        update(state.copy(isPlaying = active, notice = null))
        if (active) runEngine { engine.play() }
    }

    fun pause() {
        if (released || !wantsToPlay) return
        if (state.canPause == false) {
            update(state.copy(notice = "Esta señal no permite pausar."))
            return
        }
        wantsToPlay = false
        update(state.copy(isPlaying = false, isBuffering = false, notice = null))
        // If VLC is still opening, the Playing event below will apply the requested pause.
        if (active) runEngine { if (engine.isPlaying()) engine.pause() }
    }

    fun seekBy(deltaMs: Long) {
        if (!active) return
        val progress = runCatching { engine.progress() }.getOrNull() ?: return
        if (!progress.seekable || progress.durationMs <= 0L) return
        val current = progress.timeMs.coerceIn(0L, progress.durationMs)
        val target = when {
            deltaMs > 0 && deltaMs > progress.durationMs - current -> progress.durationMs
            deltaMs < 0 && deltaMs < -current -> 0L
            else -> current + deltaMs
        }
        seekTo(target)
    }

    fun seekTo(timeMs: Long) {
        if (!active) return
        val progress = runCatching { engine.progress() }.getOrNull() ?: return
        if (!progress.seekable || progress.durationMs <= 0L) return
        val target = timeMs.coerceIn(0L, progress.durationMs)
        runEngine { engine.seekTo(target) }
        update(state.copy(timeMs = target, durationMs = progress.durationMs, isSeekable = true))
    }

    fun refreshProgress() {
        if (!active || released) return
        val progress = runCatching { engine.progress() }.getOrNull() ?: return
        restorePositionIfPossible(progress)
        val duration = progress.durationMs.coerceAtLeast(0L)
        val position = progress.timeMs.coerceAtLeast(0L).let { if (duration > 0L) it.coerceAtMost(duration) else it }
        update(state.copy(timeMs = position, durationMs = duration, isSeekable = progress.seekable && duration > 0L))
    }

    fun refreshAudioTracks() {
        if (!active || released) return
        val tracks = runCatching { engine.audioTracks() }.getOrDefault(emptyList())
            .filter { it.id != -1 } // LibVLC's -1 entry disables audio; it is not an audio stream.
            .distinctBy { it.id }
            .mapIndexed { index, track ->
                track.copy(name = track.name.trim().ifEmpty { "Pista de audio ${index + 1}" })
            }
        var selected = runCatching { engine.selectedAudioTrack() }.getOrNull()?.takeIf { id -> tracks.any { it.id == id } }
        val pending = pendingAudioTrack
        if (pending != null && tracks.any { it.id == pending }) {
            if (selected == pending || runCatching { engine.selectAudioTrack(pending) }.getOrDefault(false)) {
                selected = pending
                pendingAudioTrack = null
                resumeAudioTrack = null
            }
        }
        update(state.copy(audioTracks = tracks, selectedAudioTrack = selected))
    }

    fun selectAudioTrack(id: Int) {
        if (!active || released) return
        // An explicit choice takes precedence over any track being restored after backgrounding.
        pendingAudioTrack = null
        resumeAudioTrack = null
        refreshAudioTracks()
        if (state.audioTracks.none { it.id == id }) {
            update(state.copy(notice = "La pista de audio ya no está disponible."))
            return
        }
        val selected = runCatching { engine.selectAudioTrack(id) }.getOrDefault(false)
        if (selected) {
            update(state.copy(selectedAudioTrack = id, notice = null))
        } else {
            update(state.copy(notice = "No se pudo cambiar la pista de audio."))
        }
    }

    fun clearNotice() {
        if (!released && state.notice != null) update(state.copy(notice = null))
    }

    fun release() {
        if (released) return
        released = true
        foreground = false
        if (active || attached) deactivate()
        runCatching { engine.release() }
    }

    private fun activateIfReady() {
        if (released || active || !foreground || !surfaceReady || state.error != null || state.ended) return
        try {
            engine.setListener(::onEvent)
            // If attach fails partway through, detach any surface VLC may already have created.
            attached = true
            engine.attach()
            active = true
            pendingRestoreMs = resumePositionMs.takeIf { it > 0L }
            pendingAudioTrack = resumeAudioTrack
            engine.load()
            update(state.copy(isPlaying = wantsToPlay, isBuffering = true, canPause = null, isSeekable = false,
                audioTracks = emptyList(), selectedAudioTrack = null, notice = null))
            // VLC must start decoding to restore a paused frame as well. Playing immediately pauses
            // it again if the user had paused before the app went to the background.
            engine.play()
        } catch (_: Exception) {
            failToStart()
        } catch (_: LinkageError) {
            failToStart()
        }
    }

    private fun restart() {
        if (released) return
        wantsToPlay = true
        resumePositionMs = 0L
        pendingRestoreMs = null
        resumeAudioTrack = null
        pendingAudioTrack = null
        if (active || attached) deactivate()
        update(PlaybackState())
        activateIfReady()
    }

    private fun deactivate() {
        // Removing the listener also clears queued VLC callbacks before stop/release.
        runCatching { engine.setListener(null) }
        if (active) runCatching { engine.stop() }
        if (attached) runCatching { engine.detach() }
        active = false
        attached = false
    }

    private fun failToStart() {
        if (active || attached) deactivate() else runCatching { engine.setListener(null) }
        wantsToPlay = false
        update(state.copy(isPlaying = false, isBuffering = false, error = PLAYBACK_ERROR))
    }

    private fun runEngine(block: () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
            failToStart()
        } catch (_: LinkageError) {
            failToStart()
        }
    }

    private fun onEvent(event: PlaybackEvent) {
        if (released || !active || !foreground || state.error != null) return
        when (event) {
            PlaybackEvent.Opening -> update(state.copy(isBuffering = wantsToPlay))
            is PlaybackEvent.Buffering -> update(state.copy(isBuffering = wantsToPlay && event.percent < 100f))
            PlaybackEvent.Playing -> {
                refreshProgress()
                refreshAudioTracks()
                if (!wantsToPlay) {
                    if (state.canPause == false) {
                        wantsToPlay = true
                        update(state.copy(isPlaying = true, isBuffering = false, notice = "Esta señal no permite pausar."))
                    } else {
                        runEngine { engine.pause() }
                        update(state.copy(isPlaying = false, isBuffering = false))
                    }
                } else {
                    update(state.copy(isPlaying = true, isBuffering = false, ended = false, error = null))
                }
            }
            PlaybackEvent.Paused -> {
                if (wantsToPlay) runEngine { engine.play() }
                else update(state.copy(isPlaying = false, isBuffering = false))
            }
            PlaybackEvent.Ended -> {
                wantsToPlay = false
                update(state.copy(isPlaying = false, isBuffering = false, ended = true))
            }
            PlaybackEvent.Error -> {
                wantsToPlay = false
                update(state.copy(isPlaying = false, isBuffering = false, error = PLAYBACK_ERROR))
            }
            PlaybackEvent.ProgressChanged -> refreshProgress()
            is PlaybackEvent.PausableChanged -> {
                if (!event.pausable && !wantsToPlay && !state.ended && state.error == null) {
                    wantsToPlay = true
                    update(state.copy(canPause = false, isPlaying = true, notice = "Esta señal no permite pausar."))
                    runEngine { engine.play() }
                } else update(state.copy(canPause = event.pausable))
            }
            PlaybackEvent.AudioTracksChanged -> refreshAudioTracks()
        }
    }

    private fun restorePositionIfPossible(progress: PlaybackProgress) {
        val pending = pendingRestoreMs ?: return
        if (!progress.seekable || progress.durationMs <= 0L) return
        pendingRestoreMs = null
        val target = pending.coerceIn(0L, progress.durationMs)
        runEngine { engine.seekTo(target) }
        resumePositionMs = 0L
    }

    private fun update(next: PlaybackState) {
        if (next != state) {
            state = next
            onStateChanged(next)
        }
    }

    private companion object {
        const val PLAYBACK_ERROR = "No se pudo reproducir la fuente con VLC. Comprueba el enlace y su disponibilidad."
    }
}
