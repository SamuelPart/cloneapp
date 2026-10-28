package com.myiptv.clean.ui.player

import android.webkit.WebView
import org.json.JSONObject

internal data class EmbedBotState(
    val controllable: Boolean = false,
    val isPlaying: Boolean = false,
    val ended: Boolean = false,
    val timeMs: Long = 0,
    val durationMs: Long = 0,
    val isSeekable: Boolean = false,
    val needsGesture: Boolean = false
)

/** Runs a bundled, deterministic DOM bot inside the current WebView, without a JS-to-native bridge. */
internal class EmbedPlaybackBot(
    private val view: WebView,
    private val script: String,
    private val onState: (EmbedBotState) -> Unit
) {
    private var generation = 0
    private var installed = false
    private var disposed = false
    private var polling = false
    private var foreground = false

    fun onPageStarted() {
        generation++
        installed = false
        polling = false
        if (!disposed) onState(EmbedBotState())
    }

    fun onPageFinished() {
        if (disposed) return
        val page = generation
        evaluate(script) {
            if (!disposed && page == generation) {
                installed = it == "true"
                if (installed && foreground) tick()
            }
        }
    }

    fun tick() {
        if (disposed || !installed || !foreground || polling) return
        polling = true
        evaluateState("tick()") { polling = false }
    }

    fun play() = evaluateState("command('play', 0)")
    fun pause() = evaluateState("command('pause', 0)")
    fun seekTo(timeMs: Long) = evaluateState("command('seek', ${timeMs.coerceAtLeast(0L) / 1000.0})")
    fun setMasked(masked: Boolean) = evaluateState("setMasked($masked)")

    fun background() {
        if (disposed) return
        foreground = false
        evaluateCommand("background()")
    }

    fun foreground() {
        if (disposed) return
        foreground = true
        evaluateCommand("foreground()")
        tick()
    }

    fun dispose() {
        if (disposed) return
        evaluateCommand("dispose()")
        disposed = true
        generation++
        installed = false
        foreground = false
        polling = false
    }

    private fun evaluateState(expression: String, after: () -> Unit = {}) {
        if (disposed || !installed) { after(); return }
        val page = generation
        evaluate("(function(){try{return window.__samplayBot ? window.__samplayBot.$expression : null;}catch(e){return null;}})()") { result ->
            after()
            if (!disposed && page == generation) parseState(result)?.let(onState)
        }
    }

    private fun evaluateCommand(expression: String) {
        if (disposed || !installed) return
        evaluate("(function(){try{if(window.__samplayBot) window.__samplayBot.$expression;}catch(e){}})()") {}
    }

    private fun evaluate(code: String, callback: (String?) -> Unit) {
        try {
            view.evaluateJavascript(code, callback)
        } catch (_: IllegalStateException) {
            callback(null)
        }
    }

    private fun parseState(value: String?): EmbedBotState? = runCatching {
        val json = JSONObject(value ?: return null)
        fun secondsToMs(key: String): Long {
            val seconds = json.optDouble(key, 0.0)
            return if (seconds.isFinite() && seconds > 0.0) (seconds * 1_000.0).coerceAtMost(Long.MAX_VALUE.toDouble()).toLong() else 0L
        }
        val duration = secondsToMs("duration")
        val time = secondsToMs("time").let { if (duration > 0L) it.coerceAtMost(duration) else it }
        val controllable = json.optBoolean("controllable", false)
        EmbedBotState(
            controllable = controllable,
            isPlaying = controllable && json.optBoolean("playing", false),
            ended = controllable && json.optBoolean("ended", false),
            timeMs = time,
            durationMs = duration,
            isSeekable = controllable && duration > 0L && json.optBoolean("seekable", false),
            needsGesture = controllable && json.optBoolean("needsGesture", false)
        )
    }.getOrNull()
}
