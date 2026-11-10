package com.myiptv.clean.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneCatalogLayoutTest {
    @Test
    fun catalogDesignOnlyAppliesOnPhonesAndTablets() {
        PHONE_CATALOG_ROUTES.forEach { route ->
            assertTrue(usePhoneCatalogLayout(isTvDevice = false, route = route))
            assertFalse(usePhoneCatalogLayout(isTvDevice = true, route = route))
        }
        listOf("channels", "favorites", "search", "settings", "player/{id}").forEach { route ->
            assertFalse(usePhoneCatalogLayout(isTvDevice = false, route = route))
            assertFalse(usePhoneCatalogLayout(isTvDevice = true, route = route))
        }
    }

    @Test
    fun tvKeepsItsLayoutAndWidePhonesKeepTheirPhoneCatalog() {
        for (width in listOf(320f, 720f, 840f, 1200f)) {
            PHONE_CATALOG_ROUTES.forEach { route ->
                assertTrue(useSamplayTvLayout(true, route, false, false, width))
                assertFalse(useSamplayTvLayout(false, route, false, false, width))
            }
        }
        // Other existing layouts, including live playback, keep their previous policy.
        assertTrue(useSamplayTvLayout(false, "settings", false, false, 840f))
        assertFalse(useSamplayTvLayout(false, "channels", false, true, 1200f))
        assertFalse(useSamplayTvLayout(false, "player/{id}", true, false, 1200f))
    }

    @Test
    fun eachCatalogTabKeepsInicioSelectedInTheFiveItemBar() {
        PHONE_CATALOG_ROUTES.forEach { assertEquals("home", phoneBottomCatalogRoute(it)) }
        assertEquals("channels", phoneBottomCatalogRoute("channels"))
        assertEquals("favorites", phoneBottomCatalogRoute("favorites"))
        assertEquals("settings", phoneBottomCatalogRoute("settings"))
    }

    @Test
    fun posterGridFitsCompactPhonesAndWideScreens() {
        assertEquals(2, phonePosterColumns(279f))
        assertEquals(3, phonePosterColumns(280f))
        assertEquals(3, phonePosterColumns(393f))
        assertEquals(4, phonePosterColumns(600f))
        assertEquals(4, phonePosterColumns(839f))
        assertEquals(5, phonePosterColumns(840f))
    }
}
