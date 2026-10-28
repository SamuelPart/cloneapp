package com.myiptv.clean.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
    onBack: () -> Unit,
    onOpenMedia: (String) -> Unit,
    onPlay: (String) -> Unit,
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
        TvUnavailableDetail(
            Icons.Filled.Movie,
            if (loading) "Cargando ficha" else "Título no disponible",
            if (loading) "Consultando el catálogo…" else "No se encontró este título en el catálogo M3U ni en TMDb.",
            onBack
        )
        return
    }

    val context = LocalContext.current
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
    val horizontalPadding = if (isTv) 36.dp else 18.dp

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
        MediaDetailHero(
            item = item,
            isTv = isTv,
            hasPlayback = hasPlayback,
            favorite = isFavorite(item.id),
            onBack = onBack,
            onPlay = { onPlay(item.id) },
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
                                onPlay = { onPlay(sourceId) }
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
