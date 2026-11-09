package com.myiptv.clean.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRoutesTest {
    @Test fun coinsRemainWithinSettingsForNavigationSelection() {
        assertEquals("settings", SettingsRoutes.topLevel(SettingsRoutes.COINS))
        assertEquals("settings", SettingsRoutes.topLevel(SettingsRoutes.SETTINGS))
        assertEquals("channels", SettingsRoutes.topLevel("channels"))
        assertEquals("settings/other", SettingsRoutes.topLevel("settings/other"))
    }
}
