package com.myiptv.clean.data.repository

import com.myiptv.clean.ui.compose.MediaKind
import com.myiptv.clean.ui.compose.MediaPlaybackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MediaPlaylistTest {
    @Test
    fun emptyPlaylistHasNoInventedContent() {
        val catalog = MediaPlaylistParser.parse("#EXTM3U\n")
        assertTrue(catalog.items.isEmpty())
        assertTrue(catalog.sources.isEmpty())
        assertEquals(0, catalog.ignoredEntries)
    }

    @Test
    fun simpleSeasonGroupsShareOneSeriesAndKeepTheirOwnEpisodeLinks() {
        val catalog = MediaPlaylistParser.parse(
            """
            #EXTM3U
            #EXTINF:-1 tvg-logo="" group-title="Peliculas",Marsupilami (2026)
            https://nupload.my/watch/movie
            #EXTINF:-1 tvg-logo="" group-title="La Brea Temporada 1",Capítulo 2
            https://nupload.my/watch/t1e2
            #EXTINF:-1 tvg-logo="" group-title="La Brea Temporada 1",Capítulo 1
            [https://nupload.my/watch/t1e1](https://nupload.my/watch/t1e1)
            #EXTINF:-1 tvg-logo="" group-title="La Brea Temporada 2",Capítulo 1
            https://nupload.my/watch/t2e1
            """.trimIndent()
        )
        assertEquals(0, catalog.ignoredEntries)
        assertEquals(2, catalog.items.size)
        val movie = catalog.items.first { it.kind == MediaKind.MOVIE }
        val series = catalog.items.first { it.kind == MediaKind.SERIES }
        assertEquals("Marsupilami (2026)", movie.title)
        assertEquals(2026, movie.year)
        assertEquals("La Brea", series.title)
        assertEquals(2, series.seasonCount)
        assertEquals(3, series.episodeCount)
        assertEquals(listOf(1, 2), catalog.seasons(series.id))
        assertEquals(listOf(1, 2), catalog.episodes(series.id, 1).map { it.number })
        assertEquals(listOf(1), catalog.episodes(series.id, 2).map { it.number })
        assertEquals(PlaylistSource("https://nupload.my/watch/t1e1", MediaPlaybackType.IFRAME), catalog.source(episodeSourceId(series.id, 1, 1)))
        assertEquals(PlaylistSource("https://nupload.my/watch/t2e1", MediaPlaybackType.IFRAME), catalog.source(episodeSourceId(series.id, 2, 1)))
        assertEquals(MediaPlaybackType.IFRAME, catalog.source(movie.id)?.type)
    }

    @Test
    fun explicitSourceTypeWinsAndObviousDirectFilesStillUseVlc() {
        val catalog = MediaPlaylistParser.parse(
            """
            #EXTM3U
            #EXTINF:-1 group-title="Peliculas",Reproductor web
            https://video.example/embed/123
            #EXTINF:-1 group-title="Peliculas",Archivo directo
            https://video.example/watch/film.m3u8?token=ok
            #EXTINF:-1 group-title="Peliculas" playback-type="direct",Directo explícito
            https://video.example/watch/stream
            #EXTINF:-1 group-title="Peliculas",Directo sin extensión
            https://video.example/get.php?id=1
            """.trimIndent()
        )
        assertEquals(listOf(MediaPlaybackType.IFRAME, MediaPlaybackType.DIRECT, MediaPlaybackType.DIRECT, MediaPlaybackType.DIRECT),
            catalog.items.map { catalog.source(it.id)?.type })
    }

    @Test
    fun contradictoryOrUnnumberedSeasonEntriesAreIgnored() {
        val catalog = MediaPlaylistParser.parse(
            """
            #EXTM3U
            #EXTINF:-1 group-title="La Brea Temporada 1" season="2" episode="1",Capítulo 1
            https://video.example/watch/one
            #EXTINF:-1 group-title="La Brea Temporada 1" episode="2",Capítulo 1
            https://video.example/watch/two
            #EXTINF:-1 group-title="La Brea Temporada 1",Capítulo sin número
            https://video.example/watch/three
            #EXTINF:-1 group-title="La Brea Temporada 1",Capítulo 0
            https://video.example/watch/four
            """.trimIndent()
        )
        assertTrue(catalog.items.isEmpty())
        assertEquals(4, catalog.ignoredEntries)
    }

    @Test
    fun packagedCatalogHasNoMalformedEntries() {
        val asset = listOf(File("src/main/assets/samplay_catalog.m3u"), File("app/src/main/assets/samplay_catalog.m3u"))
            .firstOrNull(File::isFile) ?: error("No se encontró el catálogo incluido")
        val catalog = MediaPlaylistParser.parse(asset.readText(Charsets.UTF_8))
        assertEquals(0, catalog.ignoredEntries)
        assertTrue(catalog.sources.values.all { it.url.startsWith("https://") || it.url.startsWith("http://") })
        assertEquals(catalog.items.size, catalog.items.map { it.id }.distinct().size)
    }

    @Test
    fun playlistTakesPrecedenceWithoutDiscardingOldLocalSources() {
        val catalog = PlaylistSource("https://media.example/catalog.m3u8", MediaPlaybackType.DIRECT)
        val old = PlaylistSource("https://embed.example/legacy", MediaPlaybackType.IFRAME)
        val fallback = PlaylistSource("https://media.example/fallback.mp4", MediaPlaybackType.DIRECT)
        assertEquals(catalog, resolvePlaybackSource(catalog, old, fallback))
        assertEquals(old, resolvePlaybackSource(null, old, fallback))
        assertEquals(fallback, resolvePlaybackSource(null, null, fallback))
        assertNull(resolvePlaybackSource(PlaylistSource("", MediaPlaybackType.DIRECT), null))
    }

    @Test
    fun movieSourceUsesExplicitTmdbIdentityAndPreservesMetadata() {
        val catalog = MediaPlaylistParser.parse(
            """
            #EXTM3U
            #EXTINF:-1 tmdb-id="42" media-type="movie" year="2022" genre="Drama|Acción" tvg-logo="https://images.example/poster.jpg",Película autorizada
            https://media.example/movie.m3u8
            """.trimIndent()
        )
        val item = catalog.items.single()
        assertEquals("tmdb_movie_42", item.id)
        assertEquals(MediaKind.MOVIE, item.kind)
        assertEquals(listOf("Drama", "Acción"), item.genres)
        assertEquals("https://images.example/poster.jpg", item.posterUrl)
        assertEquals(PlaylistSource("https://media.example/movie.m3u8", MediaPlaybackType.DIRECT), catalog.source(item.id))
        assertEquals(0, catalog.ignoredEntries)
    }

    @Test
    fun episodesAreGroupedBySeriesAndIframeMarkupUsesOnlyItsUrl() {
        val catalog = MediaPlaylistParser.parse(
            """
            #EXTM3U
            #EXTINF:-1 tvg-id="tmdb_tv_91_s2_e3" series-title="Serie autorizada" episode-title="Capítulo tres",Capítulo tres
            <iframe src="https://embed.example/3?a=1&amp;b=2" allowfullscreen></iframe>
            #EXTINF:-1 tvg-id="tmdb_tv_91" season="2" episode="1" series-title="Serie autorizada" playback-type="iframe",Capítulo uno
            https://embed.example/1
            """.trimIndent()
        )
        val series = catalog.items.single()
        assertEquals("tmdb_tv_91", series.id)
        assertEquals("Serie autorizada", series.title)
        assertEquals(listOf(2), catalog.seasons(series.id))
        assertEquals(listOf(1, 3), catalog.episodes(series.id, 2).map { it.number })
        assertEquals(2, series.episodeCount)
        assertEquals(PlaylistSource("https://embed.example/3?a=1&b=2", MediaPlaybackType.IFRAME), catalog.source("tmdb_tv_91_s2_e3"))
        assertEquals(MediaPlaybackType.IFRAME, catalog.source("tmdb_tv_91_s2_e1")?.type)
        assertNull(catalog.source(series.id))
    }

    @Test
    fun multilineIframeUsesTheActualSrcRatherThanDataSrc() {
        val catalog = MediaPlaylistParser.parse(
            """
            #EXTM3U
            #EXTINF:-1 media-type="movie",Embed autorizado
            <iframe data-src="https://wrong.example/video"
                src="https://embed.example/authorized?a=1&amp;b=2"
                allowfullscreen></iframe>
            """.trimIndent()
        )
        assertEquals(
            PlaylistSource("https://embed.example/authorized?a=1&b=2", MediaPlaybackType.IFRAME),
            catalog.source(catalog.items.single().id)
        )
    }

    @Test
    fun playlistOnlyIdsStayStableWhenUrlsChangeAndEpisodesShareTheirSeries() {
        fun catalog(url: String) = MediaPlaylistParser.parse(
            """
            #EXTM3U
            #EXTINF:-1 media-type="series" series-title="Serie independiente" season="1" episode="1",Uno
            $url
            #EXTINF:-1 media-type="series" series-title="Serie independiente" season="1" episode="2",Dos
            https://media.example/two.mp4
            """.trimIndent()
        )
        val first = catalog("https://media.example/old.mp4")
        val second = catalog("https://media.example/new.mp4")
        assertEquals(1, first.items.size)
        assertEquals(first.items.single().id, second.items.single().id)
        assertTrue(first.items.single().id.startsWith("m3u_tv_"))
        assertEquals(listOf(1, 2), first.episodes(first.items.single().id, 1).map { it.number })
        assertEquals("https://media.example/new.mp4", second.source(episodeSourceId(second.items.single().id, 1, 1))?.url)
    }

    @Test
    fun malformedAmbiguousAndNonHttpEntriesAreNotPublished() {
        val catalog = MediaPlaylistParser.parse(
            """
            #EXTM3U
            #EXTINF:-1,Sin tipo
            https://media.example/file.mp4
            #EXTINF:-1 media-type="movie",Esquema no admitido
            javascript:alert(1)
            #EXTINF:-1 media-type="series" season="2",Falta episodio
            https://media.example/file.mp4
            #EXTINF:-1 tvg-id="tmdb_tv_12" media-type="movie",Tipos incompatibles
            https://media.example/file.mp4
            #EXTINF:-1 media-type="movie",Falta URL
            """.trimIndent()
        )
        assertTrue(catalog.items.isEmpty())
        assertTrue(catalog.sources.isEmpty())
        assertEquals(5, catalog.ignoredEntries)
    }

    @Test
    fun episodeIdsRoundTripForPlaylistAndExistingTmdbSeries() {
        val id = "m3u_tv_0123456789abcdef"
        assertEquals(EpisodeSourceReference(id, 2, 8), parseEpisodeSourceId(episodeSourceId(id, 2, 8)))
        assertEquals(EpisodeSourceReference("tmdb_tv_91", 1, 3), parseEpisodeSourceId("tmdb_tv_91_s1_e3"))
        assertEquals(EpisodeSourceReference("tmdb_tv_91", 0, 3), parseEpisodeSourceId("tmdb_tv_91_s0_e3"))
        assertNull(parseEpisodeSourceId("tmdb_tv_91_s1_e0"))
        val special = MediaPlaylistParser.parse(
            "#EXTM3U\n#EXTINF:-1 tvg-id=\"tmdb_tv_91_s0_e3\" series-title=\"Serie\",Especial\nhttps://media.example/special.mp4"
        )
        assertEquals(listOf(0), special.seasons("tmdb_tv_91"))
        assertEquals(listOf(3), special.episodes("tmdb_tv_91", 0).map { it.number })
    }

    @Test
    fun duplicateEntriesKeepTheFirstValidSource() {
        val catalog = MediaPlaylistParser.parse(
            """
            #EXTM3U
            #EXTINF:-1 tvg-id="tmdb_movie_42",Título
            https://media.example/first.mp4
            #EXTINF:-1 tvg-id="tmdb_movie_42" playback-type="iframe",Título
            https://embed.example/second
            """.trimIndent()
        )
        assertEquals(1, catalog.items.size)
        assertEquals(PlaylistSource("https://media.example/first.mp4", MediaPlaybackType.DIRECT), catalog.source("tmdb_movie_42"))
    }

    @Test
    fun conflictingTmdbIdentifiersAndNumbersAreRejected() {
        val catalog = MediaPlaylistParser.parse(
            """
            #EXTM3U
            #EXTINF:-1 tvg-id="tmdb_movie_42" tmdb-id="43",Identidades distintas
            https://media.example/one.mp4
            #EXTINF:-1 media-type="movie" tmdb-id="no-es-numero",ID inválido
            https://media.example/two.mp4
            #EXTINF:-1 tvg-id="tmdb_tv_91_s2_e3" season="1",Temporada incompatible
            https://media.example/three.mp4
            #EXTINF:-1 media-type="movie" tvg-id="tmdb_movie_invalido",ID reservado incorrecto
            https://media.example/four.mp4
            """.trimIndent()
        )
        assertTrue(catalog.items.isEmpty())
        assertEquals(4, catalog.ignoredEntries)
    }

    @Test
    fun explicitIdentityPreventsTitleBasedMatchesAndRemoteMetadataEnrichesExactIds() {
        val catalog = MediaPlaylistParser.parse(
            """
            #EXTM3U
            #EXTINF:-1 media-type="movie",Mismo nombre
            https://media.example/local.mp4
            #EXTINF:-1 tvg-id="tmdb_movie_42",Otro título
            https://media.example/tmdb.mp4
            """.trimIndent()
        )
        val local = catalog.items.first()
        val tmdb = catalog.items.last()
        val remote = tmdb.copy(title = "Nombre de TMDb", rating = 8.0)
        val merged = mergeMediaCatalog(catalog.items, listOf(remote))
        assertEquals(2, merged.size)
        assertEquals(local, merged.first())
        assertEquals(remote, merged.last())
        assertFalse(local.id.startsWith("tmdb_"))
    }

    @Test
    fun enrichmentKeepsSuppliedArtworkAndEpisodesWhenRemoteFieldsAreMissing() {
        val catalog = MediaPlaylistParser.parse(
            """
            #EXTM3U
            #EXTINF:-1 tvg-id="tmdb_tv_91_s1_e1" series-title="Serie" tvg-logo="https://images.example/cover.jpg" description="Descripción local",Episodio
            https://media.example/one.mp4
            """.trimIndent()
        )
        val local = catalog.items.single()
        val remote = local.copy(title = "Serie de TMDb", posterUrl = null, synopsis = "Sinopsis no disponible.", seasonCount = null)
        val enriched = mergeMediaMetadata(local, remote)
        assertEquals("Serie de TMDb", enriched.title)
        assertEquals(local.posterUrl, enriched.posterUrl)
        assertEquals(local.synopsis, enriched.synopsis)
        assertEquals(local.seasonCount, enriched.seasonCount)

        val localEpisode = catalog.episodes(local.id, 1).single()
        val remoteEpisode = localEpisode.copy(stillUrl = null, synopsis = "Sinopsis no disponible.")
        val merged = mergeSeasonEpisodes(listOf(localEpisode), listOf(remoteEpisode)).single()
        assertEquals(localEpisode.stillUrl, merged.stillUrl)
        assertEquals(localEpisode.synopsis, merged.synopsis)
    }
}
