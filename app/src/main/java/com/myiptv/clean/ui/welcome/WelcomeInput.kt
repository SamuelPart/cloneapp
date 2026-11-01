package com.myiptv.clean.ui.welcome

import android.view.KeyEvent

internal enum class WelcomeKeyResult { PASS_THROUGH, CONSUME, OPEN_CONTENT }

/** Only intercept a TV confirm key; leave Back and directional navigation to Android. */
internal fun welcomeKeyResult(
    isTelevision: Boolean,
    keyCode: Int,
    action: Int,
    canceled: Boolean,
): WelcomeKeyResult {
    val isConfirm = keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
        keyCode == KeyEvent.KEYCODE_ENTER ||
        keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
        keyCode == KeyEvent.KEYCODE_BUTTON_A
    if (!isTelevision || !isConfirm) return WelcomeKeyResult.PASS_THROUGH
    return when (action) {
        KeyEvent.ACTION_DOWN -> WelcomeKeyResult.CONSUME
        KeyEvent.ACTION_UP -> if (canceled) WelcomeKeyResult.CONSUME else WelcomeKeyResult.OPEN_CONTENT
        else -> WelcomeKeyResult.PASS_THROUGH
    }
}
