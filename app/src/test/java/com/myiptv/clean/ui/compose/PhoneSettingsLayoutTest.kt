package com.myiptv.clean.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneSettingsLayoutTest {
    @Test
    fun sectionKeysAreUniqueAndKeepTheIntendedOrder() {
        val keys = PhoneSettingsSection.values().map { it.key }
        assertEquals(listOf("catalog", "notifications", "updates", "ad_privacy", "about"), keys)
        assertEquals(keys.size, keys.distinct().size)
    }

    @Test
    fun openingAnotherSectionReplacesTheCurrentOneAndTappingItAgainClosesIt() {
        assertEquals("catalog", togglePhoneSettingsSection(null, PhoneSettingsSection.CATALOG))
        assertEquals("notifications", togglePhoneSettingsSection("catalog", PhoneSettingsSection.NOTIFICATIONS))
        assertNull(togglePhoneSettingsSection("notifications", PhoneSettingsSection.NOTIFICATIONS))
        assertEquals("updates", togglePhoneSettingsSection("about", PhoneSettingsSection.UPDATES))
    }

    @Test
    fun anUpdateNotificationAlwaysTargetsTheUpdateControls() {
        for (current in listOf(null, "catalog", "notifications", "updates", "ad_privacy", "about")) {
            assertEquals("updates", phoneSettingsSectionForUpdate(current, 1L))
            assertEquals(current, phoneSettingsSectionForUpdate(current, null))
        }
    }

    @Test
    fun onlyTheActiveEntryReceivesARequestAndOldCompletionsCannotClearANewerOne() {
        assertNull(phoneSettingsUpdateForEntry(41L, isCurrentEntry = false))
        assertEquals(41L, phoneSettingsUpdateForEntry(41L, isCurrentEntry = true))
        assertNull(phoneSettingsUpdateForEntry(null, isCurrentEntry = true))
        assertEquals(42L, phoneSettingsUpdateAfterHandled(42L, handled = 41L))
        assertNull(phoneSettingsUpdateAfterHandled(42L, handled = 42L))
        assertNull(phoneSettingsUpdateAfterHandled(null, handled = 42L))
    }

    @Test
    fun notificationWaitsForTheAnimationButRespectsUserClosingThePanel() {
        assertFalse(phoneSettingsUpdateWaitFinished("updates", 0f))
        assertFalse(phoneSettingsUpdateWaitFinished("updates", 0.99f))
        assertTrue(phoneSettingsUpdateWaitFinished("updates", 1f))
        assertTrue(phoneSettingsUpdateWaitFinished("catalog", 0.25f))
        assertTrue(phoneSettingsUpdateWaitFinished(null, 0.5f))
    }

    @Test
    fun animatedContentHeightIsBoundedAndClosesCompletely() {
        assertEquals(0, phoneSettingsVisibleHeight(200, 0f))
        assertEquals(50, phoneSettingsVisibleHeight(200, 0.25f))
        assertEquals(100, phoneSettingsVisibleHeight(200, 0.5f))
        assertEquals(200, phoneSettingsVisibleHeight(200, 1f))
        assertEquals(0, phoneSettingsVisibleHeight(200, -1f))
        assertEquals(200, phoneSettingsVisibleHeight(200, 2f))
        assertEquals(0, phoneSettingsVisibleHeight(0, 1f))
        assertTrue(phoneSettingsVisibleHeight(321, 0.5f) in 160..161)
        for (height in listOf(1, 10, 200, 321, 10_000)) {
            var previous = 0
            for (step in 0..100) {
                val visible = phoneSettingsVisibleHeight(height, step / 100f)
                assertTrue(visible in previous..height)
                previous = visible
            }
            assertEquals(height, previous)
        }
    }
}
