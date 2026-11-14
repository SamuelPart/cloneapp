package com.myiptv.clean.ui.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Owns only the mobile player's temporary orientation and system-bar changes. */
internal class MobilePlayerFullscreen(private val activity: Activity) {
    private data class PreviousWindow(
        val orientation: Int,
        val systemUiVisibility: Int,
        val statusBarVisible: Boolean,
        val navigationBarVisible: Boolean,
        val behavior: Int,
        val cutoutMode: Int?
    )

    private var previous: PreviousWindow? = null

    @Suppress("DEPRECATION")
    fun enter(): Boolean {
        if (previous != null) return true
        val window = activity.window
        val decor = window.decorView
        val controller = WindowCompat.getInsetsController(window, decor)
        val insets = ViewCompat.getRootWindowInsets(decor)
        val oldVisibility = decor.systemUiVisibility
        previous = PreviousWindow(
            orientation = activity.requestedOrientation,
            systemUiVisibility = oldVisibility,
            statusBarVisible = insets?.isVisible(WindowInsetsCompat.Type.statusBars())
                ?: (oldVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0),
            navigationBarVisible = insets?.isVisible(WindowInsetsCompat.Type.navigationBars())
                ?: (oldVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0),
            behavior = controller.systemBarsBehavior,
            cutoutMode = if (Build.VERSION.SDK_INT >= 28) window.attributes.layoutInDisplayCutoutMode else null
        )
        return try {
            if (Build.VERSION.SDK_INT >= 28) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            // Layout flags extend the picture to the screen edges, including behind the status bar.
            decor.systemUiVisibility = oldVisibility or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            true
        } catch (_: Exception) {
            exit()
            false
        }
    }

    @Suppress("DEPRECATION")
    fun exit() {
        val snapshot = previous ?: return
        previous = null
        val window = activity.window
        val decor = window.decorView
        val controller = WindowCompat.getInsetsController(window, decor)
        decor.systemUiVisibility = snapshot.systemUiVisibility
        controller.systemBarsBehavior = snapshot.behavior
        if (snapshot.statusBarVisible) controller.show(WindowInsetsCompat.Type.statusBars())
        else controller.hide(WindowInsetsCompat.Type.statusBars())
        if (snapshot.navigationBarVisible) controller.show(WindowInsetsCompat.Type.navigationBars())
        else controller.hide(WindowInsetsCompat.Type.navigationBars())
        if (Build.VERSION.SDK_INT >= 28 && snapshot.cutoutMode != null) {
            window.attributes = window.attributes.apply { layoutInDisplayCutoutMode = snapshot.cutoutMode }
        }
        // Returning to the previous requested orientation also respects the device's rotation lock.
        runCatching { activity.requestedOrientation = snapshot.orientation }
    }
}

internal class PlayerFullscreenState(private val controller: MobilePlayerFullscreen?) {
    var active by mutableStateOf(false)
        private set
    val available: Boolean get() = controller != null

    fun set(enabled: Boolean) {
        if (enabled) active = controller?.enter() == true
        else {
            controller?.exit()
            active = false
        }
    }
}

@Composable
internal fun rememberPlayerFullscreen(isTv: Boolean, contentId: String): PlayerFullscreenState {
    val activity = LocalContext.current.findActivity()
    val state = remember(activity, isTv, contentId) {
        PlayerFullscreenState(if (!isTv && activity != null) MobilePlayerFullscreen(activity) else null)
    }
    DisposableEffect(state) { onDispose { state.set(false) } }
    return state
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext === this) null else baseContext.findActivity()
    else -> null
}
