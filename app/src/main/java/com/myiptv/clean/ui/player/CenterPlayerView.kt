package com.myiptv.clean.ui.player

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.R as ExoUiR
import kotlin.math.abs

/**
 * A tap on the video, or the remote's center key when no control has focus, changes playback and
 * reveals the controls. Touches and key events on playback controls still belong to ExoPlayer.
 */
@Suppress("DEPRECATION")
class CenterPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var trackingCenter = false
    private var moved = false
    private var downX = 0f
    private var downY = 0f
    private var centerClick = false
    private var consumingCenterKey = false

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                trackingCenter = isPlaybackCenter(event.x, event.y, width, height) && !touchesPlaybackControl(event)
                if (trackingCenter) {
                    moved = false
                    downX = event.x
                    downY = event.y
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> if (trackingCenter) {
                if (abs(event.x - downX) > touchSlop || abs(event.y - downY) > touchSlop) moved = true
                return true
            }
            MotionEvent.ACTION_UP -> if (trackingCenter) {
                trackingCenter = false
                if (!moved && isPlaybackCenter(event.x, event.y, width, height)) {
                    centerClick = true
                    performClick()
                    centerClick = false
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> if (trackingCenter) {
                trackingCenter = false
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> if (trackingCenter) {
                moved = true
                return true
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun performClick(): Boolean {
        val handled = super.performClick() // Preserve accessibility click notifications.
        if (centerClick) {
            togglePlayback()
            showController()
            return true
        }
        return handled
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val isCenterKey = event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
        if (isCenterKey) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 &&
                (!isControllerVisible || findFocus() == null || findFocus() === this)) {
                consumingCenterKey = true
                togglePlayback()
                showController()
            }
            if (consumingCenterKey) {
                if (event.action == KeyEvent.ACTION_UP) consumingCenterKey = false
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun togglePlayback() {
        val current = player ?: return
        when (centerPlaybackAction(
            isIdle = current.playbackState == Player.STATE_IDLE,
            isEnded = current.playbackState == Player.STATE_ENDED,
            playWhenReady = current.playWhenReady
        )) {
            CenterPlaybackAction.RESTART -> {
                current.seekToDefaultPosition()
                current.play()
            }
            CenterPlaybackAction.PREPARE -> {
                current.prepare()
                current.play()
            }
            CenterPlaybackAction.PAUSE -> current.pause()
            CenterPlaybackAction.PLAY -> current.play()
        }
    }

    private fun touchesPlaybackControl(event: MotionEvent): Boolean {
        if (!isControllerVisible) return false
        val controls = intArrayOf(ExoUiR.id.exo_prev, ExoUiR.id.exo_rew, ExoUiR.id.exo_play, ExoUiR.id.exo_pause, ExoUiR.id.exo_ffwd, ExoUiR.id.exo_next, ExoUiR.id.exo_progress)
        val location = IntArray(2)
        return controls.any { id ->
            val view = findViewById<View>(id)
            if (view == null || !view.isShown) false else {
                view.getLocationOnScreen(location)
                event.rawX >= location[0] && event.rawX < location[0] + view.width &&
                    event.rawY >= location[1] && event.rawY < location[1] + view.height
            }
        }
    }
}
