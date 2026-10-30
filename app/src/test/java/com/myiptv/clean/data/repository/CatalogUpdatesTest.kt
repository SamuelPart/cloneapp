package com.myiptv.clean.data.repository

import com.myiptv.clean.ui.compose.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogUpdatesTest {
    private fun catalog(vararg entries: String) = "#EXTM3U\n" + entries.joinToString("\n") + "\n"
    private fun movie(title: String, url: String = "https://video.example/watch/movie") =
        "#EXTINF:-1 group-title=\"Peliculas\",$title\n$url"
    private fun episode(number: Int, season: Int = 1, url: String = "https://video.example/watch/s${season}e$number") =
        "#EXTINF:-1 group-title=\"La Brea Temporada $season\",Capítulo $number\n$url"

    @Test fun bundledCatalogIsAValidBaselineWithNoInventedInitialNotifications() {
        val asset = listOf(File("src/main/assets/samplay_catalog.m3u"), File("app/src/main/assets/samplay_catalog.m3u"))
            .firstOrNull(File::isFile) ?: error("No se encontró el catálogo incluido")
        val bundled = asset.readText()
        val parsed = parseRemoteCatalog(bundled)
        assertNotNull(parsed)
        // The owner edits this real list over time (and may intentionally leave it empty).
        // Do not hard-code their current title or source counts in a baseline test.
        assertEquals(bundled, updateStoredCatalog(null, bundled)!!.content)
        assertTrue(updateStoredCatalog(null, bundled)!!.pending.isEmpty())
        assertTrue(updateStoredCatalog(StoredCatalog(bundled), bundled)!!.pending.isEmpty())
    }

    @Test fun newMovieAndSeriesAreIdentifiedOnceByTheirRealTitles() {
        val before = catalog(movie("Marsupilami (2026)"))
        val after = catalog(movie("Marsupilami (2026)"), movie("Otra película"), episode(1), episode(2))
        val updated = updateStoredCatalog(StoredCatalog(before), after)!!
        assertEquals(listOf("Otra película", "La Brea"), updated.pending.map { it.title })
        assertEquals(listOf(MediaKind.MOVIE, MediaKind.SERIES), updated.pending.map { it.kind })
        assertEquals(listOf("Nueva película: Otra película", "Nueva serie: La Brea"), updated.pending.map(::catalogAdditionLabel))
        assertEquals(updated.pending, updateStoredCatalog(updated, after)!!.pending)
    }

    @Test fun newEpisodesAreGroupedWithoutTreatingChangedUrlsAsNew() {
        val before = catalog(episode(1), episode(2))
        val after = catalog(episode(1, url = "https://video.example/watch/replaced"), episode(2), episode(3), episode(4))
        val pending = updateStoredCatalog(StoredCatalog(before), after)!!.pending.single()
        assertEquals("La Brea", pending.title)
        assertEquals(1, pending.season)
        assertEquals(listOf(3, 4), pending.episodes)
        assertEquals("La Brea · T1 E3, E4", catalogAdditionLabel(pending))
        val next = catalog(episode(1), episode(2), episode(3), episode(4), episode(5))
        val merged = updateStoredCatalog(StoredCatalog(after, listOf(pending)), next)!!.pending.single()
        assertEquals(listOf(3, 4, 5), merged.episodes)
        assertEquals("La Brea · T1 E1, E2, E3, E4, E5, E6 y 2 más", catalogAdditionLabel(merged.copy(episodes = (1..8).toList())))
    }

    @Test fun deniedPermissionCanBeRetriedButRemovedContentIsNotAnnounced() {
        val base = StoredCatalog(catalog(episode(1)))
        val first = updateStoredCatalog(base, catalog(episode(1), episode(2), movie("Nueva")))!!
        assertEquals(2, first.pending.size)
        val removed = updateStoredCatalog(first, catalog(episode(1), episode(2)))!!
        assertEquals(listOf("La Brea"), removed.pending.map { it.title })
        val delivered = removed.copy(pending = emptyList())
        assertTrue(updateStoredCatalog(delivered, delivered.content)!!.pending.isEmpty())
        val readded = updateStoredCatalog(delivered, catalog(episode(1), episode(2), movie("Nueva")))!!
        assertEquals(listOf("Nueva"), readded.pending.map { it.title })
    }

    @Test fun invalidOrPartialRemoteFilesNeverReplaceThePreviousCopy() {
        val saved = StoredCatalog(catalog(movie("Original")))
        assertNull(updateStoredCatalog(saved, "<html>error</html>"))
        assertNull(updateStoredCatalog(saved, "#EXTM3U\nhttps://video.example/orphan"))
        assertNull(updateStoredCatalog(saved, saved.content + "https://video.example/orphan\n"))
        assertNull(updateStoredCatalog(saved, "#EXTM3U\n#EXTINF Película incompleta\n"))
        assertNull(updateStoredCatalog(saved, "#EXTM3U\n#EXTINF:-1 group-title=\"Peliculas\",Rota\n"))
        assertNull(updateStoredCatalog(saved, "#EXTM3U\n#EXTINF:-1 group-title=\"Peliculas\",Rota\nnot-a-url"))
        assertNotNull(parseRemoteCatalog("\uFEFF#EXTM3U x-tvg-url=\"https://example.com/epg\"\n"))
        assertNotNull(parseRemoteCatalog(catalog("#EXTINF:-1 group-title=\"Peliculas\",Iframe\n<iframe\n src=\"https://video.example/embed/1\"\n>\n</iframe>")))
        val intentionallyEmpty = updateStoredCatalog(saved, "#EXTM3U\n# sin entradas\n")!!
        assertTrue(parseRemoteCatalog(intentionallyEmpty.content)!!.items.isEmpty())
    }

    @Test fun addingVerifiedMetadataToAnExistingTitleIsNotAnAddition() {
        val local = catalog(movie("Una obra (2026)"))
        val pinned = catalog("#EXTINF:-1 group-title=\"Peliculas\" tmdb-match-id=\"123\",Una obra (2026)\nhttps://video.example/watch/movie")
        val explicitId = catalog("#EXTINF:-1 tvg-id=\"tmdb_movie_123\",Una obra (2026)\nhttps://video.example/watch/movie")
        assertTrue(updateStoredCatalog(StoredCatalog(local), pinned)!!.pending.isEmpty())
        assertTrue(updateStoredCatalog(StoredCatalog(pinned), explicitId)!!.pending.isEmpty())
    }

    @Test fun addingSeriesEpisodesWhileItsNewTitleIsPendingDoesNotDuplicateTheNotice() {
        val baseline = StoredCatalog(catalog())
        val series = updateStoredCatalog(baseline, catalog(episode(1)))!!
        val later = updateStoredCatalog(series, catalog(episode(1), episode(2)))!!
        assertEquals(1, later.pending.size)
        assertEquals("Nueva serie: La Brea", catalogAdditionLabel(later.pending.single()))
    }
}
