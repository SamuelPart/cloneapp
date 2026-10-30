package com.myiptv.clean.notifications

import com.myiptv.clean.data.repository.CatalogAddition
import com.myiptv.clean.ui.compose.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogNotificationTargetTest {
    @Test fun eachMovieSeriesAndEpisodeBatchHasAStableDistinctIdentity() {
        val movie = CatalogAddition("movie_1", "Una película", MediaKind.MOVIE)
        val series = CatalogAddition("series_1", "Una serie", MediaKind.SERIES)
        val episodes = CatalogAddition("series_1", "Una serie", MediaKind.SERIES, season = 2, episodes = listOf(4, 2))
        assertNotEquals(catalogNotificationIdentity(movie), catalogNotificationIdentity(series))
        assertNotEquals(catalogNotificationIdentity(series), catalogNotificationIdentity(episodes))
        assertNotEquals(catalogNotificationIdentity(episodes), catalogNotificationIdentity(episodes.copy(season = 3)))
        assertNotEquals(catalogNotificationIdentity(episodes), catalogNotificationIdentity(episodes.copy(episodes = listOf(5))))
        assertEquals(catalogNotificationIdentity(episodes), catalogNotificationIdentity(episodes.copy(episodes = listOf(2, 4, 2))))
    }

    @Test fun coldStartAndNewIntentsKeepTheNewestUnconsumedDestination() {
        val requests = CatalogNotificationRequests()
        assertNull(requests.current)
        assertNull(requests.accept(null))
        assertNull(requests.accept("   "))
        val coldStart = requests.accept("movie_1")!!
        assertEquals("movie_1", coldStart.mediaId)
        val whileOpen = requests.accept("series_1")!!
        assertEquals("series_1", whileOpen.mediaId)
        assertFalse(requests.consume(coldStart.sequence))
        assertEquals(whileOpen, requests.current)
        assertTrue(requests.consume(whileOpen.sequence))
        assertNull(requests.current)
        val repeatedTap = requests.accept("series_1")!!
        assertNotEquals(whileOpen.sequence, repeatedTap.sequence)
    }

    @Test fun notificationTargetsKeepExactMediaIdsAndRepeatedTapsDistinct() {
        val firstTap = CatalogNotificationTarget("series_1", 1)
        val secondTap = CatalogNotificationTarget("series_1", 2)
        assertEquals("series_1", firstTap.mediaId)
        assertNotEquals(firstTap, secondTap)
        val specialId = CatalogAddition("a|title", "Con separadores", MediaKind.MOVIE)
        val other = CatalogAddition("a", "Otra", MediaKind.MOVIE)
        assertNotEquals(catalogNotificationIdentity(specialId), catalogNotificationIdentity(other))
    }
}
