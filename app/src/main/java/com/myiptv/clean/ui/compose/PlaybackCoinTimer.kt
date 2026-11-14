package com.myiptv.clean.ui.compose

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.myiptv.clean.CleanIPTVApp
import com.myiptv.clean.ads.observedWebPlaybackMs
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Counts only foreground, confirmed playback; a paused/open player never accumulates time. */
@Composable
internal fun PlaybackCoinTimer(active: Boolean, isTv: Boolean, webPositionMs: Long? = null) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val currentPosition = rememberUpdatedState(webPositionMs)
    LaunchedEffect(active, isTv, context, owner) {
        if (!active || isTv || context.isTelevisionDevice()) return@LaunchedEffect
        val coins = (context.applicationContext as CleanIPTVApp).coins
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var lastTime = SystemClock.elapsedRealtime()
            var lastPosition = currentPosition.value
            while (isActive) {
                delay(5_000L)
                val now = SystemClock.elapsedRealtime()
                val elapsed = now - lastTime
                val position = currentPosition.value
                // Ignore long pauses/suspension rather than crediting unobserved time.
                if (elapsed in 1L..6_000L) {
                    val played = if (position == null && lastPosition == null) elapsed
                    else observedWebPlaybackMs(lastPosition, position, elapsed)
                    if (played > 0L) coins.recordPlayback(played)
                }
                lastTime = now
                lastPosition = position
            }
        }
    }
}
