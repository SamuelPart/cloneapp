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

    @Test fun appendedCatalogEntriesHaveNumericTmdbIdsAndNoConflictingDuplicateTitles() {
        val asset = listOf(File("src/main/assets/samplay_catalog.m3u"), File("app/src/main/assets/samplay_catalog.m3u"))
            .firstOrNull(File::isFile) ?: error("No se encontró el catálogo incluido")
        val text = asset.readText()
        val lines = text.lineSequence().toList()
        val marker = lines.indexOf("# SamPlay additions: direct playable M3U entries; each must have a numeric TMDb match ID.")
        assertTrue("Falta el marcador de entradas verificadas", marker >= 0)
        val additions = lines.drop(marker + 1).filter(String::isNotBlank)
        val metadata = additions.filter { it.startsWith("#EXTINF:") }
        assertTrue("No hay películas añadidas", metadata.isNotEmpty())
        metadata.forEach { entry ->
            val id = Regex("""\btmdb-match-id="([1-9]\d*)"""").find(entry)?.groupValues?.get(1)
            assertNotNull("Falta un ID TMDb válido: $entry", id)
        }
        assertEquals("Cada entrada debe tener una fuente", metadata.size, additions.count { it.startsWith("http://") || it.startsWith("https://") })
        assertTrue("No se admiten enlaces torrent", additions.none { ".torrent" in it.lowercase() })
        val parsed = parseRemoteCatalog(text)
        assertNotNull("El catálogo remoto no debe descartarse", parsed)
        assertEquals("No debe haber filas ignoradas en la actualización", 0, parsed!!.ignoredEntries)
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

    @Test fun movieWithoutOptionalTmdbIdIsPublishedWithoutLosingPreviousTitles() {
        val original = catalog(movie("Película anterior"))
        val updated = catalog(
            movie("Película anterior"),
            "#EXTINF:-1 tvg-logo=\"\" group-title=\"Peliculas\",Nueva sin ID TMDb\nhttps://video.example/new.mp4",
            "#EXTINF:-1 group-title=\"Peliculas\" tmdb-match-id=\"123\",Nueva con ID TMDb\nhttps://video.example/other.mp4",
        )
        val saved = updateStoredCatalog(StoredCatalog(original), updated)!!
        val parsed = parseRemoteCatalog(saved.content)!!
        assertEquals(listOf("Película anterior", "Nueva sin ID TMDb", "Nueva con ID TMDb"), parsed.items.map { it.title })
        assertEquals(listOf("Nueva sin ID TMDb", "Nueva con ID TMDb"), saved.pending.map { it.title })
        val withoutId = parsed.items.single { it.title == "Nueva sin ID TMDb" }
        assertNull(withoutId.tmdbId)
        assertEquals("https://video.example/new.mp4", parsed.source(withoutId.id)?.url)
    }

    @Test fun blankOptionalTmdbIdsAllowUpdatesAndKeepNewNotificationTargetsExact() {
        val previous = catalog(
            "#EXTINF:-1 group-title=\"Peliculas\" tmdb-match-id=\"\",Anterior\nhttps://video.example/old.mp4",
            episode(1)
        )
        val next = catalog(
            "#EXTINF:-1 group-title=\"Peliculas\" tmdb-match-id=\" \" tmdb-id=\"\",Anterior\nhttps://video.example/replaced.mp4",
            episode(1),
            "#EXTINF:-1 group-title=\"La Brea Temporada 1\" tmdb-match-id=\"\",Capítulo 2\nhttps://video.example/s1e2.mp4",
            "#EXTINF:-1 group-title=\"Peliculas\" tmdb-match-id=\"\",Nueva sin ID\nhttps://video.example/new.mp4",
            "#EXTINF:-1 group-title=\"Serie nueva Temporada 1\" tmdb-id=\"\",Capítulo 1\nhttps://video.example/new-s1e1.mp4"
        )
        val before = parseRemoteCatalog(previous) ?: error("La lista anterior debe ser válida")
        val saved = updateStoredCatalog(StoredCatalog(previous), next) ?: error("La actualización debe ser válida")
        val after = parseRemoteCatalog(saved.content) ?: error("La lista guardada debe ser válida")
        assertEquals(before.items.first().id, after.items.first().id)
        assertEquals("https://video.example/replaced.mp4", after.source(after.items.first().id)?.url)
        assertEquals(listOf("La Brea", "Nueva sin ID", "Serie nueva"), saved.pending.map { it.title })
        val episodes = saved.pending.first()
        val oldSeries = after.items.single { it.title == "La Brea" }
        assertEquals(oldSeries.id, episodes.mediaId)
        assertEquals(1, episodes.season)
        assertEquals(listOf(2), episodes.episodes)
        assertEquals("https://video.example/s1e2.mp4", after.source(episodeSourceId(oldSeries.id, 1, 2))?.url)
        for (addition in saved.pending.drop(1)) {
            assertEquals(after.items.single { it.title == addition.title }.id, addition.mediaId)
        }
        assertEquals(saved.pending, updateStoredCatalog(saved, next)!!.pending)
    }

    @Test fun distinctM3uOptionsWithTheSameTmdbIdAreAnnouncedSeparately() {
        fun option(id: String, url: String) =
            "#EXTINF:-1 group-title=\"Peliculas\" media-id=\"$id\" tmdb-match-id=\"8697\",quien soy?(1998)\n$url"
        val first = option("option-1", "https://video.example/one.mp4")
        val second = option("option-2", "https://video.example/two.mp4")
        val third = option("option-3", "https://video.example/three.mp4")
        val previous = catalog(first)
        val next = catalog(first, second, third)
        val updated = updateStoredCatalog(StoredCatalog(previous), next)!!
        val playlist = parseRemoteCatalog(next)!!
        val expectedNew = playlist.items.drop(1)
        assertEquals(3, playlist.items.map { it.id }.distinct().size)
        assertEquals(expectedNew.map { it.id }, updated.pending.map { it.mediaId })
        assertEquals(listOf("https://video.example/two.mp4", "https://video.example/three.mp4"),
            updated.pending.map { playlist.source(it.mediaId)?.url })
        // Pending notices follow the exact card, not another one with the same TMDb metadata.
        val removedSecond = updateStoredCatalog(updated, catalog(first, third))!!
        assertEquals(listOf(expectedNew.last().id), removedSecond.pending.map { it.mediaId })
    }

    @Test fun aRetainedTmdbAliasDoesNotHideAnotherDistinctNewCard() {
        val explicit = "#EXTINF:-1 tvg-id=\"tmdb_movie_42\",Una obra\nhttps://video.example/explicit.mp4"
        val separate = "#EXTINF:-1 group-title=\"Peliculas\" media-id=\"otra-opcion\" tmdb-match-id=\"42\",Una obra\nhttps://video.example/separate.mp4"
        for ((old, added) in listOf(explicit to separate, separate to explicit)) {
            val before = catalog(old)
            val after = catalog(old, added)
            val expected = parseRemoteCatalog(after)!!.items.last()
            val pending = updateStoredCatalog(StoredCatalog(before), after)!!.pending
            assertEquals(listOf(expected.id), pending.map { it.mediaId })
        }
    }

    @Test fun replacingAnIdentityWithItsVerifiedTmdbAliasIsNotANewTitle() {
        val local = catalog("#EXTINF:-1 group-title=\"Peliculas\" tmdb-match-id=\"42\",Una obra\nhttps://video.example/one.mp4")
        val explicit = catalog("#EXTINF:-1 tvg-id=\"tmdb_movie_42\",Una obra\nhttps://video.example/one.mp4")
        assertTrue(updateStoredCatalog(StoredCatalog(local), explicit)!!.pending.isEmpty())
        assertTrue(updateStoredCatalog(StoredCatalog(explicit), local)!!.pending.isEmpty())
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
