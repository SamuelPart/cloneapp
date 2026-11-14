package com.myiptv.clean.ui.compose

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.myiptv.clean.CleanIPTVApp
import com.myiptv.clean.ads.AdSection
import com.myiptv.clean.ads.AdsRules
import com.myiptv.clean.ads.canOfferMediaAds
import com.myiptv.clean.ads.isKidsGenre
import com.myiptv.clean.data.repository.ChannelCatalogRepository
import com.myiptv.clean.data.repository.LocalMediaSources
import com.myiptv.clean.data.repository.MediaPlaylist
import com.myiptv.clean.data.repository.PlaylistSource
import com.myiptv.clean.data.repository.TmdbRepository
import com.myiptv.clean.data.repository.episodeSourceId
import com.myiptv.clean.data.repository.mergeMediaCatalog
import com.myiptv.clean.data.repository.mergeMediaMetadata
import com.myiptv.clean.data.repository.mergePlaylistTmdbMetadata
import com.myiptv.clean.data.repository.mergeSeasonEpisodes
import com.myiptv.clean.data.repository.resolvePlaybackSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun ChannelDetailScreen(
    channelId: String,
    isTv: Boolean,
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    isFavorite: (String) -> Boolean,
    onToggleFavorite: (String) -> Unit
) {
    val channel = ChannelCatalogRepository.cachedChannel(channelId)
    if (channel == null) {
        TvUnavailableDetail(Icons.Filled.LiveTv, "Canal no disponible", "Actualiza las fuentes de canales y vuelve a intentarlo.", onBack)
        return
    }
    TvChannelDetail(
        channel = channel,
        isTv = isTv,
        favorite = isFavorite(channel.id),
        onBack = onBack,
        onPlay = { onPlay(channel.id) },
        onToggleFavorite = { onToggleFavorite(channel.id) }
    )
}

@Composable
fun MediaDetailScreen(
    mediaId: String,
    isTv: Boolean,
    playlist: MediaPlaylist,
    fromKids: Boolean,
    onBack: () -> Unit,
    onOpenMedia: (String) -> Unit,
    onPlay: (String, MediaKind, Boolean, () -> Boolean) -> Unit,
    isFavorite: (String) -> Boolean,
    onToggleFavorite: (String) -> Unit
) {
    val playlistItem = playlist.item(mediaId)
    val linkedTmdbId = playlistItem?.tmdbId?.let { "tmdb_${playlistItem.tmdbMediaType}_$it" }
    val cachedItem = TmdbRepository.cachedMedia(mediaId) ?: linkedTmdbId?.let(TmdbRepository::cachedMedia)
    val initialItem = if (cachedItem != null && playlistItem != null) {
        if (cachedItem.id == playlistItem.id) mergeMediaMetadata(playlistItem, cachedItem)
        else mergePlaylistTmdbMetadata(playlistItem, cachedItem)
    } else cachedItem ?: playlistItem
    val canConsultTmdb = TmdbRepository.isConfigured && (playlistItem != null || mediaId.startsWith("tmdb_"))
    var detailLoading by remember(mediaId) { mutableStateOf(initialItem == null && canConsultTmdb) }
    var hasTmdbMetadata by remember(mediaId) { mutableStateOf(cachedItem != null) }
    var metadataNotice by remember(mediaId) { mutableStateOf<String?>(null) }
    var remoteSeasonCount by remember(mediaId) { mutableStateOf(cachedItem?.seasonCount?.coerceAtLeast(0) ?: 0) }
    val loadedItem by key(mediaId, playlistItem) {
        produceState<MediaItem?>(initialValue = initialItem) {
            // The playlist can arrive or be enriched after this screen has opened.
            val cached = TmdbRepository.cachedMedia(mediaId) ?: linkedTmdbId?.let(TmdbRepository::cachedMedia)
            val local = playlistItem
            value = if (cached != null && local != null) {
                if (cached.id == local.id) mergeMediaMetadata(local, cached) else mergePlaylistTmdbMetadata(local, cached)
            } else cached ?: local
            if (cached != null) {
                hasTmdbMetadata = true
                remoteSeasonCount = value?.seasonCount?.coerceAtLeast(0) ?: 0
            }
            metadataNotice = null
            if (canConsultTmdb) {
                detailLoading = true
                try {
                    val detail = if (local != null) TmdbRepository.playlistMetadata(local) else TmdbRepository.details(mediaId)
                    if (detail != null) {
                        value = detail
                        hasTmdbMetadata = true
                        remoteSeasonCount = detail.seasonCount?.coerceAtLeast(0) ?: 0
                    } else if (local != null && !hasTmdbMetadata) {
                        metadataNotice = "No se encontró una coincidencia única en TMDb. Revisa el título o añade su tmdb-id."
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    if (!hasTmdbMetadata) metadataNotice = "No se pudieron consultar los datos de TMDb. Comprueba la conexión y la clave."
                }
            } else if (local != null && !TmdbRepository.isConfigured) {
                metadataNotice = "Configura una clave de TMDb para cargar la ficha y las imágenes."
            }
            detailLoading = false
        }
    }
    val item = loadedItem
    if (item == null) {
        val loading = detailLoading || !playlist.loaded
        if (!isTv && loading) {
            SamplayLoadingScreen("Cargando ficha…", onBack = onBack)
        } else {
            TvUnavailableDetail(
                Icons.Filled.Movie,
                if (loading) "Cargando ficha" else "Título no disponible",
                if (loading) "Consultando el catálogo…" else "No se encontró este título en el catálogo M3U ni en TMDb.",
                onBack
            )
        }
        return
    }

    val context = LocalContext.current
    val ads = (context.applicationContext as CleanIPTVApp).ads
    val lifecycleOwner = LocalLifecycleOwner.current
    val safeMedia = canOfferMediaAds(item.genres)
    val latestSafeMedia = rememberUpdatedState(safeMedia)
    val latestMediaId = rememberUpdatedState(item.id)
    val section = if (item.kind == MediaKind.MOVIE) AdSection.MOVIES else AdSection.SERIES
    // Do not request ads for child-directed or unclassified media while TMDb is still loading.
    val canPreloadReward = AdsRules.canPreloadMediaReward(context.isTelevisionDevice(), fromKids, item.genres) &&
        ads.canShowAdsIn(section)
    LaunchedEffect(ads, lifecycleOwner, item.id, canPreloadReward, ads.fullscreenShowing) {
        if (canPreloadReward && !ads.fullscreenShowing) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { ads.loadRewards() }
        }
    }
    val canStillAdvertise: () -> Boolean = { latestMediaId.value == item.id && latestSafeMedia.value }
    val savedSource = remember(item.id, playlist) { LocalMediaSources.get(context, item.id, playlist) }
    val hasPlayback = resolvePlaybackSource(
        catalog = playlist.source(item.id),
        legacy = savedSource?.let { PlaylistSource(it.url, it.type) },
        fallback = item.streamUrl?.let { PlaylistSource(it, item.playbackType) }
    ) != null
    val localSeasons = playlist.seasons(item.id)
    val remoteSeasons = if (remoteSeasonCount > 0) (1..remoteSeasonCount).toList() else emptyList()
    val seasons = remember(localSeasons, remoteSeasons) { (localSeasons + remoteSeasons).distinct().sorted() }
    var selectedSeason by remember(item.id) { mutableStateOf(seasons.firstOrNull() ?: 1) }
    LaunchedEffect(seasons) {
        if (seasons.isNotEmpty() && selectedSeason !in seasons) selectedSeason = seasons.first()
    }
    val localEpisodes = playlist.episodes(item.id, selectedSeason)
    val canLoadRemoteEpisodes = item.kind == MediaKind.SERIES && item.tmdbId != null &&
        item.tmdbMediaType == "tv" && TmdbRepository.isConfigured &&
        (selectedSeason in remoteSeasons || (selectedSeason == 0 && selectedSeason in localSeasons))
    var episodeNotice by remember(item.id, item.tmdbId, selectedSeason, localEpisodes, remoteSeasonCount) {
        mutableStateOf(if (canLoadRemoteEpisodes) "Cargando episodios desde TMDb…" else "Episodios del catálogo M3U.")
    }
    val episodes by key(item.id, item.tmdbId, selectedSeason, localEpisodes, remoteSeasonCount) {
        produceState(initialValue = localEpisodes) {
            value = localEpisodes
            if (canLoadRemoteEpisodes) {
                try {
                    val remote = TmdbRepository.seasonEpisodes(item.tmdbId!!, selectedSeason)
                    value = mergeSeasonEpisodes(localEpisodes, remote)
                    episodeNotice = when {
                        remote.isNotEmpty() && localEpisodes.isNotEmpty() -> "Episodios de TMDb y del catálogo M3U."
                        remote.isNotEmpty() -> "Episodios cargados desde TMDb."
                        localEpisodes.isNotEmpty() -> "Episodios del catálogo M3U."
                        else -> "TMDb no devolvió episodios para esta temporada."
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    episodeNotice = if (localEpisodes.isNotEmpty()) "No se pudo consultar TMDb; se muestran los episodios del catálogo M3U."
                    else "No se pudieron cargar los episodios desde TMDb."
                }
            } else {
                episodeNotice = if (localEpisodes.isNotEmpty()) "Episodios del catálogo M3U."
                else if (!TmdbRepository.isConfigured) "No hay episodios en el catálogo M3U. Configura TMDb para consultar sus metadatos."
                else "No hay episodios disponibles para esta temporada."
            }
        }
    }
    val related = mergeMediaCatalog(playlist.items, TmdbRepository.cachedCatalog())
        .filter { it.id != item.id }.sortedByDescending(MediaItem::rating).take(8)
    if (!isTv && item.kind == MediaKind.MOVIE) {
        PhoneMovieDetailScreen(
            item = item,
            related = related,
            hasPlayback = hasPlayback,
            favorite = isFavorite(item.id),
            onBack = onBack,
            onPlay = { onPlay(item.id, item.kind, isKidsGenre(item.genres), canStillAdvertise) },
            onToggleFavorite = { onToggleFavorite(item.id) },
            onOpenMedia = onOpenMedia,
        )
        return
    }
    if (!isTv && item.kind == MediaKind.SERIES) {
        val playableEpisodeIds = remember(item.id, selectedSeason, episodes, playlist, context) {
            episodes.mapNotNull { episode ->
                val sourceId = episodeSourceId(item.id, selectedSeason, episode.number)
                val saved = LocalMediaSources.get(context, sourceId, playlist)
                sourceId.takeIf {
                    resolvePlaybackSource(playlist.source(sourceId), saved?.let { PlaylistSource(it.url, it.type) }) != null
                }
            }
        }
        val firstPlayableEpisodeId = playableEpisodeIds.firstOrNull()
        PhoneSeriesDetailScreen(
            item = item,
            related = related,
            seasons = seasons,
            selectedSeason = selectedSeason,
            episodes = episodes,
            episodeNotice = episodeNotice,
            canPlay = hasPlayback || firstPlayableEpisodeId != null,
            favorite = isFavorite(item.id),
            onBack = onBack,
            onPlay = {
                val target = if (hasPlayback) item.id else firstPlayableEpisodeId
                target?.let { onPlay(it, item.kind, isKidsGenre(item.genres), canStillAdvertise) }
            },
            onSelectSeason = { selectedSeason = it },
            isEpisodePlayable = { episode ->
                episodeSourceId(item.id, selectedSeason, episode.number) in playableEpisodeIds
            },
            onPlayEpisode = { episode ->
                val sourceId = episodeSourceId(item.id, selectedSeason, episode.number)
                onPlay(sourceId, item.kind, isKidsGenre(item.genres), canStillAdvertise)
            },
            onToggleFavorite = { onToggleFavorite(item.id) },
            onOpenMedia = onOpenMedia,
        )
        return
    }
    val horizontalPadding = if (isTv) 36.dp else 18.dp

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
        MediaDetailHero(
            item = item,
            isTv = isTv,
            hasPlayback = hasPlayback,
            favorite = isFavorite(item.id),
            onBack = onBack,
            onPlay = { onPlay(item.id, item.kind, isKidsGenre(item.genres), canStillAdvertise) },
            onToggleFavorite = { onToggleFavorite(item.id) }
        )

        if (related.isNotEmpty()) {
            Column(modifier = Modifier.padding(start = horizontalPadding, top = 10.dp)) {
                SectionTitle("Más títulos", modifier = Modifier.padding(end = horizontalPadding))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp), contentPadding = PaddingValues(end = horizontalPadding)) {
                    items(related, key = MediaItem::id) { recommendation ->
                        TvMediaArtwork(
                            item = recommendation,
                            modifier = Modifier.width(196.dp).height(110.dp),
                            featured = false,
                            onClick = { onOpenMedia(recommendation.id) }
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            val tmdbStatus = if (detailLoading && !hasTmdbMetadata) "Consultando TMDb…" else metadataNotice
            if (tmdbStatus != null) Text(tmdbStatus, color = TvMuted, style = MaterialTheme.typography.bodySmall)
            if (!hasPlayback) {
                Text(
                    if (item.kind == MediaKind.SERIES) "La serie no tiene una fuente general; puedes reproducir los episodios que dispongan de una."
                    else "Este título no tiene una fuente de reproducción disponible en el catálogo.",
                    color = TvMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (item.kind == MediaKind.SERIES) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("Episodios")
                    if (seasons.isEmpty()) {
                        Text("No hay temporadas disponibles en los datos cargados.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
                    } else {
                        TvFilterRow(
                            options = seasons.map { "Temporada $it" },
                            selected = "Temporada $selectedSeason",
                            onSelect = { label -> label.substringAfter("Temporada ").toIntOrNull()?.let { selectedSeason = it } },
                            modifier = Modifier.fillMaxWidth()
                        )
                        episodes.forEach { episode ->
                            val sourceId = episodeSourceId(item.id, selectedSeason, episode.number)
                            val legacySource = remember(sourceId, playlist) { LocalMediaSources.get(context, sourceId, playlist) }
                            TvEpisodeRow(
                                episode = episode,
                                hasSource = resolvePlaybackSource(playlist.source(sourceId), legacySource?.let { PlaylistSource(it.url, it.type) }) != null,
                                onPlay = { onPlay(sourceId, item.kind, isKidsGenre(item.genres), canStillAdvertise) }
                            )
                        }
                        Text(episodeNotice, color = TvMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle(if (hasTmdbMetadata) "Información de TMDb" else "Información del catálogo")
                DetailInfoRow("Año", item.year.takeIf { it > 0 }?.toString() ?: "—")
                DetailInfoRow("Reparto", item.castNames.take(5).joinToString(", ").ifBlank { "No disponible" })
                DetailInfoRow("Géneros", item.genres.joinToString(", ").ifBlank { "No disponible" })
                if (item.kind == MediaKind.SERIES) {
                    DetailInfoRow("Temporadas", item.seasonCount?.takeIf { it > 0 }?.toString() ?: "—")
                    DetailInfoRow("Episodios", item.episodeCount?.toString() ?: "—")
                }
            }
        }
    }
}

@Composable
private fun PhoneMediaPreviewHero(
    item: MediaItem,
    canPlay: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clipToBounds()
            .background(Brush.linearGradient(listOf(Color(item.colorA), Color(item.colorB), Color.Black)))
    ) {
        val artwork = item.backdropUrl ?: item.posterUrl
        if (!artwork.isNullOrBlank()) DetailBackdropImage(imageUrl = artwork, modifier = Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent, Color.Black.copy(alpha = 0.5f)))))
        Box(
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp).size(40.dp).clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f)).clickable(onClickLabel = "Volver", onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White, modifier = Modifier.size(22.dp))
        }
        if (canPlay) {
            Box(
                modifier = Modifier.align(Alignment.Center).size(64.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f))
                    .clickable(onClickLabel = "Reproducir", onClick = onPlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Reproducir", tint = Color.Black, modifier = Modifier.size(38.dp))
            }
        }
    }
}

@Composable
private fun PhoneMovieDetailScreen(
    item: MediaItem,
    related: List<MediaItem>,
    hasPlayback: Boolean,
    favorite: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenMedia: (String) -> Unit,
) {
    val context = LocalContext.current
    var synopsisExpanded by remember(item.id) { mutableStateOf(false) }
    val shareText = buildString {
        append(item.title)
        if (item.year > 0) append(" (${item.year})")
        if (item.synopsis.isNotBlank()) append("\n${item.synopsis}")
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF191A23)).verticalScroll(rememberScrollState())
    ) {
        PhoneMediaPreviewHero(item = item, canPlay = hasPlayback, onBack = onBack, onPlay = onPlay)

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.title,
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(10.dp))
                MovieDetailIconButton(Icons.Filled.Share, "Compartir película") {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir película"))
                }
                Spacer(Modifier.width(8.dp))
                MovieDetailIconButton(
                    if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    if (favorite) "Quitar de mi lista" else "Añadir a mi lista",
                    tint = if (favorite) Color(0xFFF04467) else Color.White,
                    onClick = onToggleFavorite,
                )
            }

            if (item.rating > 0.0) {
                Text(
                    "%.1f".format(Locale.US, item.rating),
                    modifier = Modifier.padding(top = 2.dp),
                    color = Color(0xFFFFA342),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            val metadata = listOfNotNull(
                item.countryNames.firstOrNull(),
                item.year.takeIf { it > 0 }?.toString(),
                item.genres.take(2).takeIf { it.isNotEmpty() }?.joinToString(" / "),
            ).joinToString("  |  ")
            if (metadata.isNotBlank()) {
                Text(
                    metadata,
                    modifier = Modifier.padding(top = 8.dp),
                    color = TvMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (item.synopsis.isNotBlank()) {
                Text(
                    item.synopsis,
                    modifier = Modifier.padding(top = 14.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = if (synopsisExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.synopsis.length > 150) {
                    Text(
                        if (synopsisExpanded) "Ver menos" else "Ver más",
                        modifier = Modifier.padding(top = 2.dp).clickable { synopsisExpanded = !synopsisExpanded },
                        color = Color(0xFFF04467),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (item.directorNames.isNotEmpty()) {
                Text(
                    "Director: ${item.directorNames.take(3).joinToString(" / ")}",
                    modifier = Modifier.padding(top = 18.dp),
                    color = TvMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (item.castNames.isNotEmpty()) {
                Text(
                    "Actores: ${item.castNames.take(5).joinToString(" / ")}",
                    modifier = Modifier.padding(top = 12.dp),
                    color = TvMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (related.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 28.dp)) {
                Text(
                    "También podría gustarte",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                ) {
                    items(related, key = MediaItem::id) { recommendation ->
                        PhoneMovieRecommendationCard(recommendation) { onOpenMedia(recommendation.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneSeriesDetailScreen(
    item: MediaItem,
    related: List<MediaItem>,
    seasons: List<Int>,
    selectedSeason: Int,
    episodes: List<MediaEpisode>,
    episodeNotice: String,
    canPlay: Boolean,
    favorite: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onSelectSeason: (Int) -> Unit,
    isEpisodePlayable: (MediaEpisode) -> Boolean,
    onPlayEpisode: (MediaEpisode) -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenMedia: (String) -> Unit,
) {
    val context = LocalContext.current
    var seasonMenuExpanded by remember(item.id) { mutableStateOf(false) }
    var synopsisExpanded by remember(item.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF191A23)).verticalScroll(rememberScrollState())
    ) {
        PhoneMediaPreviewHero(item = item, canPlay = canPlay, onBack = onBack, onPlay = onPlay)

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.title,
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.rating > 0.0) {
                    Text(
                        "%.1f".format(Locale.US, item.rating),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = Color(0xFFFFA342),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                MovieDetailIconButton(Icons.Filled.Share, "Compartir serie") {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "${item.title} (${item.year})\n${item.synopsis}")
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir serie"))
                }
                Spacer(Modifier.width(8.dp))
                MovieDetailIconButton(
                    if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    if (favorite) "Quitar de mi lista" else "Añadir a mi lista",
                    tint = if (favorite) Color(0xFFF04467) else Color.White,
                    onClick = onToggleFavorite,
                )
            }

            val metadata = listOfNotNull(
                item.countryNames.firstOrNull(),
                item.year.takeIf { it > 0 }?.toString(),
                item.genres.take(2).takeIf { it.isNotEmpty() }?.joinToString(" / "),
            ).joinToString("  |  ")
            if (metadata.isNotBlank()) {
                Text(
                    metadata,
                    modifier = Modifier.padding(top = 8.dp),
                    color = TvMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (seasons.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        OutlinedButton(onClick = { seasonMenuExpanded = true }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("Temporada $selectedSeason", maxLines = 1)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = seasonMenuExpanded, onDismissRequest = { seasonMenuExpanded = false }) {
                            seasons.forEach { season ->
                                DropdownMenuItem(
                                    text = { Text("Temporada $season") },
                                    onClick = {
                                        seasonMenuExpanded = false
                                        onSelectSeason(season)
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${episodes.size} Capítulos",
                        color = TvMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (episodes.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        episodes.forEachIndexed { index, episode ->
                            val playable = isEpisodePlayable(episode)
                            Box(
                                modifier = Modifier.size(width = 72.dp, height = 52.dp)
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(if (index == 0 && playable) Color(0xFFF10D49) else Color(0xFF242530))
                                    .border(1.dp, Color.White.copy(alpha = if (playable) 0.12f else 0.05f), RoundedCornerShape(17.dp))
                                    .clickable(
                                        enabled = playable,
                                        onClickLabel = "Reproducir episodio ${episode.number}",
                                        onClick = { onPlayEpisode(episode) },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (index == 0 && playable) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = "Reproducir episodio ${episode.number}", tint = Color.White, modifier = Modifier.size(30.dp))
                                } else {
                                    Text(
                                        episode.number.toString(),
                                        color = if (playable) Color.White else TvMuted,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(episodeNotice, modifier = Modifier.padding(top = 10.dp), color = TvMuted, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (item.synopsis.isNotBlank()) {
                Text(
                    item.synopsis,
                    modifier = Modifier.padding(top = 16.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = if (synopsisExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.synopsis.length > 150) {
                    Text(
                        if (synopsisExpanded) "Ver menos" else "Ver más",
                        modifier = Modifier.padding(top = 2.dp).clickable { synopsisExpanded = !synopsisExpanded },
                        color = Color(0xFFF04467),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (item.directorNames.isNotEmpty()) {
                Text(
                    "Director: ${item.directorNames.take(3).joinToString(" / ")}",
                    modifier = Modifier.padding(top = 18.dp),
                    color = TvMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (item.castNames.isNotEmpty()) {
                Text(
                    "Actores: ${item.castNames.take(5).joinToString(" / ")}",
                    modifier = Modifier.padding(top = 12.dp),
                    color = TvMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (related.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 28.dp)) {
                Text(
                    "También podría gustarte",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 10.dp)) {
                    items(related, key = MediaItem::id) { recommendation ->
                        PhoneMovieRecommendationCard(recommendation) { onOpenMedia(recommendation.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieDetailIconButton(
    icon: ImageVector,
    description: String,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(11.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(23.dp))
    }
}

@Composable
private fun PhoneMovieRecommendationCard(item: MediaItem, onClick: () -> Unit) {
    Column(modifier = Modifier.width(112.dp).clickable(onClickLabel = "Abrir ${item.title}", onClick = onClick)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp))) {
            PosterArtwork(item = item, modifier = Modifier.fillMaxSize(), showTitle = false)
            if (item.kind == MediaKind.SERIES && item.episodeCount != null && item.episodeCount > 0) {
                Text(
                    "${item.episodeCount} Capítulos",
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 5.dp, vertical = 4.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else if (item.rating > 0.0) {
                Text(
                    "%.1f".format(Locale.US, item.rating),
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                        .background(Color(0xFFE63857), RoundedCornerShape(3.dp)).padding(horizontal = 5.dp, vertical = 2.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            item.title,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 5.dp),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MediaDetailHero(
    item: MediaItem,
    isTv: Boolean,
    hasPlayback: Boolean,
    favorite: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val initialFocus = remember(item.id) { FocusRequester() }
    val heroHeight = if (isTv) (LocalConfiguration.current.screenHeightDp.dp * 0.72f).coerceIn(330.dp, 500.dp)
        else (LocalConfiguration.current.screenHeightDp.dp * 0.53f).coerceIn(360.dp, 480.dp)
    LaunchedEffect(item.id, hasPlayback, isTv) {
        if (isTv) {
            delay(180)
            runCatching { initialFocus.requestFocus() }
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = heroHeight).clipToBounds()
            .background(Brush.linearGradient(listOf(Color(item.colorA), Color(item.colorB), Color.Black)))
    ) {
        val artwork = item.backdropUrl ?: item.posterUrl
        if (!artwork.isNullOrBlank()) DetailBackdropImage(imageUrl = artwork, modifier = Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.96f), Color.Black.copy(alpha = 0.65f), Color.Black.copy(alpha = 0.08f)))))
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent, Color.Black))))

        Column(modifier = Modifier.fillMaxWidth().padding(start = if (isTv) 36.dp else 18.dp, end = if (isTv) 28.dp else 18.dp, top = 16.dp, bottom = 18.dp)) {
            TvActionButton("Volver", onBack, icon = Icons.Filled.ArrowBack)
            Spacer(Modifier.height(12.dp))
            Column(modifier = Modifier.fillMaxWidth(if (isTv) 0.72f else 1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(if (item.kind == MediaKind.SERIES) "SERIE" else "PELÍCULA", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text(
                    item.title.uppercase(), color = Color.White, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium,
                    fontSize = if (isTv) 34.sp else 29.sp, lineHeight = if (isTv) 36.sp else 32.sp, letterSpacing = 1.5.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (item.rating > 0.0) Text("★ ${"%.1f".format(item.rating)} / 10", color = Color(0xFF73E3C5), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(item.year.takeIf { it > 0 }?.toString() ?: "—", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                    if (item.duration != "—") Text(item.duration, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                }
                Text(item.synopsis, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (item.genres.isNotEmpty()) {
                    Text("Géneros: ${item.genres.joinToString(", ")}", color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasPlayback) {
                        TvDetailAction("Reproducir", Icons.Filled.PlayArrow, Modifier.focusRequester(initialFocus), primary = true, onClick = onPlay)
                    }
                    TvDetailAction(
                        if (favorite) "En mi lista" else "Mi lista",
                        if (favorite) Icons.Filled.Check else Icons.Filled.Add,
                        modifier = if (hasPlayback) Modifier else Modifier.focusRequester(initialFocus),
                        onClick = onToggleFavorite
                    )
                }
            }
        }
    }
}

@Composable
private fun TvDetailAction(label: String, icon: ImageVector, modifier: Modifier = Modifier, primary: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val light = focused || primary
    val shape = RoundedCornerShape(3.dp)
    Row(
        modifier = modifier.onFocusChanged { focused = it.isFocused }.clip(shape)
            .background(if (light) Color.White else Color.White.copy(alpha = 0.16f))
            .border(if (focused) 2.dp else 0.dp, if (focused) MaterialTheme.colorScheme.primary else Color.Transparent, shape)
            .clickable(onClick = onClick).padding(horizontal = 17.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (light) Color.Black else Color.White, modifier = Modifier.size(18.dp))
        Text(label, color = if (light) Color.Black else Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.width(90.dp), color = TvMuted, style = MaterialTheme.typography.bodySmall)
        Text(value, modifier = Modifier.weight(1f), color = Color.White, style = MaterialTheme.typography.bodySmall)
    }
}
