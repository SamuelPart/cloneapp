package com.myiptv.clean.ui.player

import android.content.Context
import android.net.Uri
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

/** The only native playback engine for direct channel and video URLs. */
internal class VlcPlaybackEngine(
    context: Context,
    private val sourceUrl: String,
    private val userAgent: String?,
    private val referrer: String?
) : PlaybackEngine {
    private val libVlc = run {
        // LibVLC 3.x calls System.exit if loading a native library fails. Preflight the same
        // libraries so an unsupported device can show an error instead of terminating the app.
        listOf("c++_shared", "vlc", "vlcjni").forEach(System::loadLibrary)
        LibVLC(context.applicationContext, arrayListOf("--network-caching=1000"))
    }
    private val player = try {
        MediaPlayer(libVlc)
    } catch (error: Throwable) {
        libVlc.release()
        throw error
    }

    var videoLayout: VLCVideoLayout? = null

    override fun setListener(listener: ((PlaybackEvent) -> Unit)?) {
        if (listener == null) {
            player.setEventListener(null)
            return
        }
        // LibVLC posts its listener callbacks to Android's main thread.
        player.setEventListener { event ->
            val mapped = when (event.type) {
                MediaPlayer.Event.Opening -> PlaybackEvent.Opening
                MediaPlayer.Event.Buffering -> PlaybackEvent.Buffering(event.buffering)
                MediaPlayer.Event.Playing -> PlaybackEvent.Playing
                MediaPlayer.Event.Paused -> PlaybackEvent.Paused
                MediaPlayer.Event.EndReached -> PlaybackEvent.Ended
                MediaPlayer.Event.EncounteredError -> PlaybackEvent.Error
                MediaPlayer.Event.TimeChanged, MediaPlayer.Event.LengthChanged,
                MediaPlayer.Event.PositionChanged, MediaPlayer.Event.SeekableChanged -> PlaybackEvent.ProgressChanged
                MediaPlayer.Event.PausableChanged -> PlaybackEvent.PausableChanged(event.pausable)
                MediaPlayer.Event.ESAdded, MediaPlayer.Event.ESDeleted,
                MediaPlayer.Event.ESSelected -> PlaybackEvent.AudioTracksChanged
                else -> null
            }
            if (mapped != null) listener(mapped)
        }
    }

    override fun attach() {
        player.attachViews(checkNotNull(videoLayout), null, true, false)
    }

    override fun detach() {
        player.detachViews()
    }

    override fun load() {
        val media = Media(libVlc, Uri.parse(sourceUrl))
        try {
            vlcMediaOptions(userAgent, referrer).forEach(media::addOption)
            player.media = media
        } finally {
            media.release()
        }
    }

    override fun play() = player.play()
    override fun pause() = player.pause()
    override fun stop() = player.stop()
    override fun isPlaying(): Boolean = player.isPlaying

    override fun progress(): PlaybackProgress = PlaybackProgress(
        timeMs = player.time,
        durationMs = player.length,
        seekable = player.isSeekable
    )

    override fun seekTo(timeMs: Long) {
        player.setTime(timeMs)
    }

    override fun audioTracks(): List<PlayerAudioTrack> = player.audioTracks?.map { track ->
        PlayerAudioTrack(track.id, track.name.orEmpty())
    }.orEmpty()

    override fun selectedAudioTrack(): Int? = player.audioTrack.takeIf { it != -1 }
    override fun selectAudioTrack(id: Int): Boolean = player.setAudioTrack(id)

    override fun release() {
        try {
            player.release()
        } finally {
            libVlc.release()
            videoLayout = null
        }
    }
}
