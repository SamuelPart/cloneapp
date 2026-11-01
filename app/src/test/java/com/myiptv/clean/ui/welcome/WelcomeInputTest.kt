package com.myiptv.clean.ui.welcome

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class WelcomeInputTest {
    @Test fun tvConfirmKeysOpenOnReleaseAndConsumeCanceledReleases() {
        val confirmKeys = listOf(
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_BUTTON_A,
        )
        confirmKeys.forEach { key ->
            assertEquals(WelcomeKeyResult.CONSUME, welcomeKeyResult(true, key, KeyEvent.ACTION_DOWN, false))
            assertEquals(WelcomeKeyResult.OPEN_CONTENT, welcomeKeyResult(true, key, KeyEvent.ACTION_UP, false))
            assertEquals(WelcomeKeyResult.CONSUME, welcomeKeyResult(true, key, KeyEvent.ACTION_UP, true))
            assertEquals(WelcomeKeyResult.PASS_THROUGH, welcomeKeyResult(false, key, KeyEvent.ACTION_UP, false))
        }
    }

    @Test fun otherKeysAndActionsRemainAvailableToAndroid() {
        assertEquals(
            WelcomeKeyResult.PASS_THROUGH,
            welcomeKeyResult(true, KeyEvent.KEYCODE_BACK, KeyEvent.ACTION_UP, false),
        )
        assertEquals(
            WelcomeKeyResult.PASS_THROUGH,
            welcomeKeyResult(true, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.ACTION_UP, false),
        )
        assertEquals(
            WelcomeKeyResult.PASS_THROUGH,
            welcomeKeyResult(true, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.ACTION_MULTIPLE, false),
        )
    }
}
