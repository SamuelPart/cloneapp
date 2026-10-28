package com.myiptv.clean.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myiptv.clean.data.repository.ChannelCatalogRepository
import com.myiptv.clean.data.repository.ConfiguredMediaSource
import com.myiptv.clean.data.repository.LocalMediaSources
import com.myiptv.clean.data.repository.TmdbRepository
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
        if (isTv) {
            TvUnavailableDetail(Icons.Filled.LiveTv, "Canal no disponible", "Actualiza las fuentes de canales y vuelve a intentarlo.", onBack)
        } else {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                DetailBackButton(onBack)
                EmptyState(Icons.Filled.LiveTv, "Canal no disponible", "Actualiza las fuentes de canales y vuelve a intentarlo.")
            }
        }
        return
    }

    val favorite = isFavorite(channel.id)
    if (isTv) {
        TvChannelDetail(channel, favorite, onBack, onPlay = { onPlay(channel.id) }, onToggleFavorite = { onToggleFavorite(channel.id) })
        return
    }
    var favoriteFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = if (isTv) 36.dp else 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DetailBackButton(onBack)
            Text("TV en directo  /  ${channel.name}", modifier = Modifier.padding(start = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTv) 36.dp else 18.dp)
                .height(if (isTv) 330.dp else 280.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(Brush.linearGradient(listOf(Color(channel.accent).copy(alpha = 0.78f), Color(0xFF101725), Color(0xFF131326))))
        ) {
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent, Color.Black.copy(alpha = 0.42f)))))
            Column(
                modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(if (isTv) 0.68f else 1f).padding(horizontal = if (isTv) 40.dp else 22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChannelLogo(channel, Modifier.size(if (isTv) 70.dp else 60.dp))
                    Column {
                        LiveBadge()
                        Spacer(Modifier.height(8.dp))
                        Text(channel.category.uppercase(), color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                    }
                }
                Text(channel.name, color = Color.White, style = if (isTv) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Emisión de televisión en directo", color = Color.White.copy(alpha = 0.86f), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 5.dp)) {
                    PrimaryButton("Ver canal", Icons.Filled.PlayArrow, onClick = { onPlay(channel.id) })
                    Surface(
                        modifier = Modifier.size(48.dp).onFocusChanged { favoriteFocused = it.isFocused }
                            .border(if (favoriteFocused) 2.dp else 1.dp, if (favoriteFocused) MaterialTheme.colorScheme.secondary else Color.Transparent, CircleShape)
                            .clip(CircleShape).clickable { onToggleFavorite(channel.id) },
                        color = Color.White.copy(alpha = 0.12f),
                        contentColor = Color.White
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (favorite) "Quitar de mi lista" else "Agregar a mi lista",
                                tint = if (favorite) Color(0xFFFF7893) else Color.White
                            )
                        }
                    }
                }
            }
            if (isTv) {
                ChannelLogo(channel, Modifier.align(Alignment.CenterEnd).padding(end = 84.dp).size(170.dp).clip(RoundedCornerShape(30.dp)))
            }
        }

        Column(modifier = Modifier.padding(horizontal = if (isTv) 36.dp else 18.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle("Información de la señal")
                DetailInfoRow("Categoría", channel.category)
                channel.epgId?.let { DetailInfoRow("ID EPG", it) }
            }
            Text(
                "La disponibilidad de cada emisión depende de su operador y puede variar según la región.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun MediaDetailScreen(
    mediaId: String,
    isTv: Boolean,
    onBack: () -> Unit,
    onOpenMedia: (String) -> Unit,
    onPlay: (String) -> Unit,
    isFavorite: (String) -> Boolean,
    onToggleFavorite: (String) -> Unit
) {
    val initialItem = remember(mediaId) { TmdbRepository.cachedMedia(mediaId) }
    val isTmdbItem = mediaId.startsWith("tmdb_") && TmdbRepository.isConfigured
    var detailLoading by remember(mediaId) { mutableStateOf(initialItem == null && isTmdbItem) }
    val loadedItem by produceState<MediaItem?>(initialValue = initialItem, key1 = mediaId) {
        if (isTmdbItem) {
            try {
                value = TmdbRepository.details(mediaId) ?: value
            } catch (_: Exception) {
                // Keep any metadata already returned by the catalog/search request.
            } finally {
                detailLoading = false
            }
        }
    }
    if (loadedItem == null) {
        val title = if (detailLoading) "Cargando ficha" else "Título no disponible"
        val message = if (detailLoading) "Consultando los datos reales de TMDb…" else "No hay una ficha TMDb disponible para este título."
        if (isTv) {
            TvUnavailableDetail(Icons.Filled.Movie, title, message, onBack)
        } else {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                DetailBackButton(onBack)
                EmptyState(Icons.Filled.Movie, title, message)
            }
        }
        return
    }

    val item = loadedItem ?: return
    val context = LocalContext.current
    val favorite = isFavorite(item.id)
    var favoriteFocused by remember { mutableStateOf(false) }
    var selectedSeason by remember(item.id) { mutableStateOf(1) }
    var savedSource by remember(item.id) { mutableStateOf(LocalMediaSources.get(context, item.id)) }
    var showSourceDialog by remember(item.id) { mutableStateOf(false) }
    val isRemoteSeries = item.kind == MediaKind.SERIES && item.tmdbId != null && item.tmdbMediaType == "tv"
    val seasonCount = item.seasonCount?.coerceAtLeast(0) ?: 0
    var episodeNotice by remember(item.id, selectedSeason, seasonCount) { mutableStateOf("Cargando episodios desde TMDb…") }
    val episodes by produceState<List<MediaEpisode>>(
        initialValue = emptyList(),
        key1 = item.id,
        key2 = "$selectedSeason:$seasonCount:$isRemoteSeries"
    ) {
        if (isRemoteSeries && TmdbRepository.isConfigured && seasonCount > 0) {
            try {
                value = TmdbRepository.seasonEpisodes(item.tmdbId!!, selectedSeason)
                episodeNotice = if (value.isEmpty()) "TMDb no devolvió episodios para esta temporada." else "Episodios cargados desde TMDb."
            } catch (_: Exception) {
                value = emptyList()
                episodeNotice = "No se pudieron cargar los episodios desde TMDb."
            }
        } else {
            value = emptyList()
            episodeNotice = when {
                !TmdbRepository.isConfigured -> "Configura TMDb para consultar los episodios."
                seasonCount == 0 -> "TMDb no informó temporadas para esta ficha."
                else -> "Los episodios no están disponibles desde TMDb."
            }
        }
    }
    val seasons = if (seasonCount > 0) (1..seasonCount).toList() else emptyList()
    val playbackUrl = savedSource?.url ?: item.streamUrl
    val related = TmdbRepository.cachedCatalog().filter { it.id != item.id }.sortedByDescending(MediaItem::rating).take(8)

    Column(modifier = Modifier.fillMaxSize().background(if (isTv) Color.Black else Color.Transparent).verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
        if (isTv) {
            TvMediaDetailHero(
                item = item,
                hasPlayback = !playbackUrl.isNullOrBlank(),
                favorite = favorite,
                onBack = onBack,
                onPlay = { onPlay(item.id) },
                onConfigure = { showSourceDialog = true },
                onToggleFavorite = { onToggleFavorite(item.id) }
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = if (isTv) 36.dp else 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                DetailBackButton(onBack)
                Text("${if (item.kind == MediaKind.SERIES) "Series" else "Películas"}  /  ${item.title}", modifier = Modifier.padding(start = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = if (isTv) 36.dp else 18.dp)
                    .height(if (isTv) 260.dp else 165.dp).clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Color(item.colorA), Color(item.colorB), Color(0xFF121827))))
            ) {
                if (!item.backdropUrl.isNullOrBlank()) {
                    DetailBackdropImage(
                        imageUrl = item.backdropUrl,
                        contentDescription = "Fondo de ${item.title}",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.08f), Color(0xFF10131F).copy(alpha = 0.92f)))))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = if (isTv) 36.dp else 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isTv) 30.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(if (isTv) 240.dp else 100.dp).aspectRatio(0.72f)) {
                    PosterArtwork(item, Modifier.fillMaxSize())
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (isTv) 12.dp else 8.dp)) {
                    Text(if (item.kind == MediaKind.SERIES) "SERIE" else "PELÍCULA", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                    Text(item.title, color = MaterialTheme.colorScheme.onBackground, style = if (isTv) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.year.takeIf { it > 0 }?.toString() ?: "—", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.duration, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.rating > 0.0) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC56E), modifier = Modifier.size(15.dp))
                            Text("%.1f".format(item.rating), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Sin valoración", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(item.genres.joinToString("  ·  "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    Text(item.synopsis, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, maxLines = if (isTv) 5 else 4, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(top = 4.dp)) {
                        if (!playbackUrl.isNullOrBlank()) {
                            PrimaryButton("Reproducir", Icons.Filled.PlayArrow, onClick = { onPlay(item.id) })
                            TertiaryButton("Cambiar enlace", onClick = { showSourceDialog = true })
                        } else {
                            TertiaryButton("Añadir enlace", onClick = { showSourceDialog = true })
                        }
                        Surface(
                            modifier = Modifier.size(48.dp).onFocusChanged { favoriteFocused = it.isFocused }
                                .border(if (favoriteFocused) 2.dp else 1.dp, if (favoriteFocused) MaterialTheme.colorScheme.secondary else Color.Transparent, CircleShape)
                                .clip(CircleShape).clickable { onToggleFavorite(item.id) },
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = if (favorite) "Quitar de mi lista" else "Agregar a mi lista", tint = if (favorite) Color(0xFFFF7893) else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }

        if (isTv && related.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(start = 36.dp, top = 12.dp, bottom = 8.dp)) {
                SectionTitle("Más títulos", modifier = Modifier.padding(end = 36.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 36.dp)
                ) {
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

        Column(modifier = Modifier.padding(horizontal = if (isTv) 36.dp else 18.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (playbackUrl.isNullOrBlank()) {
                Text(
                    if (item.kind == MediaKind.SERIES) "Añade una fuente de reproducción para la serie o un enlace independiente junto a cada episodio."
                    else "Añade un enlace de vídeo directo o un embed iframe para reproducir este título.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (item.kind == MediaKind.SERIES) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("Episodios")
                    if (seasons.isEmpty()) {
                        Text(episodeNotice, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    } else {
                        if (isTv) {
                            TvFilterRow(
                                options = seasons.map { "Temporada $it" },
                                selected = "Temporada $selectedSeason",
                                onSelect = { label -> label.substringAfter("Temporada ").toIntOrNull()?.let { selectedSeason = it } },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                seasons.forEach { season ->
                                    FilterChip(
                                        selected = selectedSeason == season,
                                        onClick = { selectedSeason = season },
                                        label = { Text("Temporada $season") }
                                    )
                                }
                            }
                        }
                        if (episodes.isEmpty()) {
                            Text(episodeNotice, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        } else {
                            episodes.forEach { episode ->
                                val episodeSourceId = "${item.id}_s${selectedSeason}_e${episode.number}"
                                EpisodeRow(episode, episodeSourceId, onPlay, isTv)
                            }
                            Text(episodeNotice, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle("Información de TMDb")
                DetailInfoRow("Año", item.year.takeIf { it > 0 }?.toString() ?: "—")
                DetailInfoRow("Reparto", item.castNames.take(5).joinToString(", ").ifBlank { "No disponible" })
                DetailInfoRow("Géneros", item.genres.joinToString(", ").ifBlank { "No disponible" })
                if (item.kind == MediaKind.SERIES) {
                    DetailInfoRow("Temporadas", seasonCount.takeIf { it > 0 }?.toString() ?: "—")
                    DetailInfoRow("Episodios", item.episodeCount?.toString() ?: "—")
                }
            }
            if (!isTv && related.isNotEmpty()) {
                Column {
                    SectionTitle("Más títulos")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(related, key = MediaItem::id) { recommendation ->
                            MediaPosterCard(recommendation, modifier = Modifier.width(if (isTv) 190.dp else 148.dp), onClick = { onOpenMedia(recommendation.id) })
                        }
                    }
                }
            }
        }
    }

    if (showSourceDialog) {
        MediaSourceDialog(
            initialSource = savedSource,
            isTv = isTv,
            onDismiss = { showSourceDialog = false },
            onSave = { url, type ->
                if (LocalMediaSources.save(context, item.id, url, type)) {
                    savedSource = LocalMediaSources.get(context, item.id)
                    true
                } else false
            },
            onRemove = if (savedSource != null) {
                {
                    LocalMediaSources.remove(context, item.id)
                    savedSource = null
                }
            } else null
        )
    }
}

@Composable
private fun TvMediaDetailHero(
    item: MediaItem,
    hasPlayback: Boolean,
    favorite: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onConfigure: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val initialFocus = remember(item.id) { FocusRequester() }
    val heroHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.72f).coerceIn(330.dp, 500.dp)
    LaunchedEffect(item.id) {
        delay(180)
        runCatching { initialFocus.requestFocus() }
    }

    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = heroHeight).clipToBounds()
            .background(Brush.linearGradient(listOf(Color(item.colorA), Color(item.colorB), Color.Black)))
    ) {
        val artwork = item.backdropUrl ?: item.posterUrl
        if (!artwork.isNullOrBlank()) {
            DetailBackdropImage(
                imageUrl = artwork,
                modifier = Modifier.matchParentSize()
            )
        }
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.96f), Color.Black.copy(alpha = 0.65f), Color.Black.copy(alpha = 0.08f)))))
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent, Color.Black))))

        Column(modifier = Modifier.fillMaxWidth().padding(start = 36.dp, end = 28.dp, top = 16.dp, bottom = 18.dp)) {
            TvActionButton("Volver", onBack, icon = Icons.Filled.ArrowBack)
            Spacer(Modifier.height(12.dp))
            Column(modifier = Modifier.fillMaxWidth(0.72f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    if (item.kind == MediaKind.SERIES) "SERIE" else "PELÍCULA",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    item.title.uppercase(),
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 34.sp,
                    lineHeight = 36.sp,
                    letterSpacing = 1.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TvDetailAction(
                        label = if (hasPlayback) "Reproducir" else "Añadir enlace",
                        icon = if (hasPlayback) Icons.Filled.PlayArrow else Icons.Filled.Add,
                        primary = true,
                        modifier = Modifier.focusRequester(initialFocus),
                        onClick = if (hasPlayback) onPlay else onConfigure
                    )
                    if (hasPlayback) TvDetailAction("Cambiar enlace", Icons.Filled.Add, onClick = onConfigure)
                    TvDetailAction(if (favorite) "En mi lista" else "Mi lista", if (favorite) Icons.Filled.Check else Icons.Filled.Add, onClick = onToggleFavorite)
                }
            }
        }
    }
}

@Composable
private fun TvDetailAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val light = focused || primary
    val shape = RoundedCornerShape(3.dp)
    Row(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (light) Color.White else Color.White.copy(alpha = 0.16f))
            .border(if (focused) 2.dp else 0.dp, if (focused) MaterialTheme.colorScheme.primary else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 17.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (light) Color.Black else Color.White, modifier = Modifier.size(18.dp))
        Text(label, color = if (light) Color.Black else Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun MediaSourceDialog(
    initialSource: ConfiguredMediaSource?,
    isTv: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, MediaPlaybackType) -> Boolean,
    onRemove: (() -> Unit)?
) {
    var url by remember(initialSource?.url) { mutableStateOf(initialSource?.url.orEmpty()) }
    var type by remember(initialSource?.type) { mutableStateOf(initialSource?.type ?: MediaPlaybackType.DIRECT) }
    var error by remember { mutableStateOf("") }
    val urlFocus = remember { FocusRequester() }
    LaunchedEffect(isTv) {
        if (isTv) {
            delay(180)
            runCatching { urlFocus.requestFocus() }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isTv) TvSurface else AlertDialogDefaults.containerColor,
        shape = if (isTv) RoundedCornerShape(4.dp) else AlertDialogDefaults.shape,
        title = { Text("Enlace de reproducción") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Añade una fuente pública y autorizada. Se guarda solo en este dispositivo.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth().then(if (isTv) Modifier.focusRequester(urlFocus) else Modifier),
                    label = { Text(if (type == MediaPlaybackType.IFRAME) "URL o código iframe" else "URL directa de vídeo") },
                    minLines = 2,
                    maxLines = 4
                )
                if (isTv) {
                    TvFilterRow(
                        options = listOf("Directo", "Embed iframe"),
                        selected = if (type == MediaPlaybackType.DIRECT) "Directo" else "Embed iframe",
                        onSelect = { type = if (it == "Directo") MediaPlaybackType.DIRECT else MediaPlaybackType.IFRAME },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = type == MediaPlaybackType.DIRECT, onClick = { type = MediaPlaybackType.DIRECT }, label = { Text("Directo") })
                        FilterChip(selected = type == MediaPlaybackType.IFRAME, onClick = { type = MediaPlaybackType.IFRAME }, label = { Text("Embed iframe") })
                    }
                }
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            val save = { if (onSave(url, type)) onDismiss() else { error = "Usa un enlace HTTP/HTTPS válido para esta fuente." } }
            if (isTv) TvActionButton("Guardar", save, primary = true)
            else TextButton(onClick = save) { Text("Guardar") }
        },
        dismissButton = {
            Row(horizontalArrangement = if (isTv) Arrangement.spacedBy(6.dp) else Arrangement.Start) {
                if (onRemove != null) {
                    val remove = { onRemove(); onDismiss() }
                    if (isTv) TvActionButton("Quitar enlace", remove)
                    else TextButton(onClick = remove) { Text("Quitar enlace") }
                }
                if (isTv) TvActionButton("Cancelar", onDismiss)
                else TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

@Composable
private fun EpisodeRow(
    episode: MediaEpisode,
    sourceId: String,
    onPlay: (String) -> Unit,
    isTv: Boolean
) {
    val context = LocalContext.current
    var configuredSource by remember(sourceId) { mutableStateOf(LocalMediaSources.get(context, sourceId)) }
    var showSourceDialog by remember(sourceId) { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    if (isTv) {
        TvEpisodeRow(episode, configuredSource != null, onPlay = { onPlay(sourceId) }, onConfigure = { showSourceDialog = true })
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
                .border(if (focused) 2.dp else 1.dp, if (focused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape)
                .clip(shape).background(MaterialTheme.colorScheme.surface).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Box(
                modifier = Modifier.width(104.dp).aspectRatio(1.45f).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF25334B), Color(0xFF161D31)))),
                contentAlignment = Alignment.Center
            ) {
                if (!episode.stillUrl.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = episode.stillUrl,
                        contentDescription = "Imagen del episodio ${episode.number}",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(27.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("EPISODIO ${episode.number.toString().padStart(2, '0')} · ${episode.duration}", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(episode.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(episode.synopsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (configuredSource == null) {
                TertiaryButton("Añadir enlace", onClick = { showSourceDialog = true })
            } else {
                PrimaryButton("Reproducir", Icons.Filled.PlayArrow, onClick = { onPlay(sourceId) })
            }
        }
    }

    if (showSourceDialog) {
        MediaSourceDialog(
            initialSource = configuredSource,
            isTv = isTv,
            onDismiss = { showSourceDialog = false },
            onSave = { url, type ->
                if (LocalMediaSources.save(context, sourceId, url, type)) {
                    configuredSource = LocalMediaSources.get(context, sourceId)
                    true
                } else false
            },
            onRemove = if (configuredSource != null) {
                {
                    LocalMediaSources.remove(context, sourceId)
                    configuredSource = null
                }
            } else null
        )
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(label, modifier = Modifier.width(90.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(value, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DetailBackButton(onBack: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.size(42.dp).onFocusChanged { focused = it.isFocused }
            .border(if (focused) 2.dp else 1.dp, if (focused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline, CircleShape)
            .clip(CircleShape).clickable(onClick = onBack),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}
