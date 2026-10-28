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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myiptv.clean.data.repository.TmdbRepository

@Composable
fun ChannelDetailScreen(
    channelId: String,
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    isFavorite: (String) -> Boolean,
    onToggleFavorite: (String) -> Unit
) {
    val channel = DemoCatalog.channels.find { it.id == channelId }
    if (channel == null) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            DetailBackButton(onBack)
            EmptyState(Icons.Filled.LiveTv, "Canal no disponible", "Este canal no está en el catálogo de demostración.")
        }
        return
    }
    val favorite = isFavorite(channel.id)
    var favoriteFocused by remember { mutableStateOf(false) }
    val isTv = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 840

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = if (isTv) 36.dp else 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            DetailBackButton(onBack)
            Text("Guía del canal", modifier = Modifier.padding(start = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTv) 36.dp else 18.dp)
                .height(if (isTv) 330.dp else 280.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(Brush.linearGradient(listOf(Color(channel.accent).copy(alpha = 0.75f), Color(0xFF101725), Color(0xFF131326))))
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.15f), Color.Transparent, Color.Black.copy(alpha = 0.42f)))))
            Column(
                modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(if (isTv) 0.64f else 1f).padding(horizontal = if (isTv) 40.dp else 22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChannelLogo(channel, Modifier.size(if (isTv) 70.dp else 60.dp))
                    Column {
                        LiveBadge()
                        Spacer(Modifier.height(8.dp))
                        Text(channel.category.uppercase(), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                    }
                }
                Text(channel.name, color = Color.White, style = if (isTv) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                Text(channel.currentProgram, color = Color.White.copy(alpha = 0.88f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text("Programación de ejemplo · ${channel.startTime} — ${channel.endTime}", color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 5.dp)) {
                    PrimaryButton("Ver canal", Icons.Filled.PlayArrow, onClick = { onPlay(channel.id) })
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .onFocusChanged { favoriteFocused = it.isFocused }
                            .border(if (favoriteFocused) 2.dp else 1.dp, if (favoriteFocused) MaterialTheme.colorScheme.secondary else Color.Transparent, CircleShape)
                            .clip(CircleShape)
                            .clickable { onToggleFavorite(channel.id) },
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
            Column {
                SectionTitle("Ahora y próximamente")
                channel.schedule.forEachIndexed { index, programme ->
                    EpgProgramCard(programme, isCurrent = index == 0, modifier = Modifier.padding(bottom = 9.dp))
                }
                Text("La guía EPG es ilustrativa y no está conectada a una programación real.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Más canales")
                DemoCatalog.channels.filter { it.id != channel.id }.take(3).forEach { nextChannel ->
                    ChannelCard(nextChannel, modifier = Modifier.fillMaxWidth()) { onPlay(nextChannel.id) }
                }
            }
        }
    }
}

@Composable
private fun EpgProgramCard(programme: ProgramItem, isCurrent: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(54.dp)) {
            Text(programme.start, color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(programme.end, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
        Box(Modifier.width(3.dp).height(44.dp).clip(CircleShape).background(if (isCurrent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(programme.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (isCurrent) LiveBadge()
            }
            Text(programme.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun MediaDetailScreen(
    mediaId: String,
    onBack: () -> Unit,
    onOpenMedia: (String) -> Unit,
    onPlay: (String) -> Unit,
    isFavorite: (String) -> Boolean,
    onToggleFavorite: (String) -> Unit
) {
    val initialItem = remember(mediaId) {
        TmdbRepository.cachedMedia(mediaId) ?: DemoCatalog.media.find { it.id == mediaId }
    }
    val isTmdbItem = mediaId.startsWith("tmdb_") && TmdbRepository.isConfigured
    var detailLoading by remember(mediaId) { mutableStateOf(initialItem == null && isTmdbItem) }
    val loadedItem by produceState<MediaItem?>(initialValue = initialItem, key1 = mediaId) {
        if (isTmdbItem) {
            try {
                value = TmdbRepository.details(mediaId) ?: value
            } catch (_: Exception) {
                // Preserve the cached list item if the details request is temporarily unavailable.
            } finally {
                detailLoading = false
            }
        }
    }
    if (loadedItem == null) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            DetailBackButton(onBack)
            EmptyState(
                Icons.Filled.Movie,
                if (detailLoading) "Cargando ficha" else "Título no disponible",
                if (detailLoading) "Consultando la información del título…" else "No pudimos cargar este título. Revisa tu conexión con TMDb."
            )
        }
        return
    }
    val item = loadedItem ?: return
    val isTv = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 840
    val favorite = isFavorite(item.id)
    var favoriteFocused by remember { mutableStateOf(false) }
    var selectedSeason by remember(item.id) { mutableStateOf(1) }
    val isRemoteSeries = item.kind == MediaKind.SERIES && item.tmdbId != null && item.tmdbMediaType == "tv"
    val demoEpisodes = remember(item.id, selectedSeason) { sampleEpisodesForSeason(selectedSeason) }
    var episodeNotice by remember(item.id, selectedSeason) {
        mutableStateOf(if (isRemoteSeries) "Cargando episodios…" else "Episodios de demostración.")
    }
    val episodes by produceState<List<MediaEpisode>>(
        initialValue = if (isRemoteSeries) emptyList() else demoEpisodes,
        key1 = item.id,
        key2 = selectedSeason
    ) {
        if (isRemoteSeries && TmdbRepository.isConfigured) {
            try {
                value = TmdbRepository.seasonEpisodes(item.tmdbId!!, selectedSeason)
                episodeNotice = if (value.isEmpty()) "TMDb no devolvió episodios para esta temporada." else "Episodios y sinopsis obtenidos de TMDb."
            } catch (_: Exception) {
                value = emptyList()
                episodeNotice = "No se pudieron cargar los episodios desde TMDb."
            }
        } else {
            value = demoEpisodes
            episodeNotice = "Episodios de demostración."
        }
    }
    val seasons = (1..(item.seasonCount ?: 2).coerceIn(1, 20)).toList()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = if (isTv) 36.dp else 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            DetailBackButton(onBack)
            Text("${if (item.kind == MediaKind.SERIES) "Series" else "Películas"}  /  ${item.title}", modifier = Modifier.padding(start = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTv) 36.dp else 18.dp)
                .height(if (isTv) 260.dp else 165.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(Color(item.colorA), Color(item.colorB), Color(0xFF121827))))
        ) {
            if (!item.backdropUrl.isNullOrBlank()) {
                coil.compose.AsyncImage(
                    model = item.backdropUrl,
                    contentDescription = "Fondo de ${item.title}",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
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
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC56E), modifier = Modifier.size(15.dp))
                    Text(item.rating.toString(), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Text(item.genres.joinToString("  ·  "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Text(item.synopsis, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, maxLines = if (isTv) 5 else 4, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(top = 4.dp)) {
                    PrimaryButton(if (isTv) "Reproducir" else "Ver", Icons.Filled.PlayArrow, onClick = { onPlay(item.id) })
                    Surface(
                        modifier = Modifier.size(48.dp).onFocusChanged { favoriteFocused = it.isFocused }.border(if (favoriteFocused) 2.dp else 1.dp, if (favoriteFocused) MaterialTheme.colorScheme.secondary else Color.Transparent, CircleShape).clip(CircleShape).clickable { onToggleFavorite(item.id) },
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = if (favorite) "Quitar de mi lista" else "Agregar a mi lista", tint = if (favorite) Color(0xFFFF7893) else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = if (isTv) 36.dp else 18.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (item.kind == MediaKind.SERIES) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("Episodios")
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        seasons.forEach { season ->
                            androidx.compose.material3.FilterChip(
                                selected = selectedSeason == season,
                                onClick = { selectedSeason = season },
                                label = { Text("Temporada $season") }
                            )
                        }
                    }
                    if (episodes.isEmpty()) {
                        Text(episodeNotice, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    } else {
                        episodes.forEach { episode ->
                            EpisodeRow(episode, onClick = { onPlay(item.id) })
                        }
                        Text(episodeNotice, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle("Información")
                DetailInfoRow("Año", item.year.takeIf { it > 0 }?.toString() ?: "—")
                DetailInfoRow("Reparto", item.castNames.take(5).joinToString(", ").ifBlank { "No disponible" })
                DetailInfoRow("Géneros", item.genres.joinToString(", ").ifBlank { "No disponible" })
            }
            Column {
                SectionTitle("También te puede gustar")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items((TmdbRepository.cachedCatalog() + DemoCatalog.media).distinctBy(MediaItem::id).filter { it.id != item.id }.take(5), key = MediaItem::id) { recommendation ->
                        MediaPosterCard(recommendation, modifier = Modifier.width(if (isTv) 190.dp else 148.dp), onClick = { onOpenMedia(recommendation.id) })
                    }
                }
            }
            Text(
                if (item.tmdbId != null) "Metadatos e imágenes proporcionados por TMDb. La reproducción no está conectada a un stream real."
                else "Ficha de muestra · No hay contenido multimedia conectado.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun sampleEpisodesForSeason(season: Int): List<MediaEpisode> = if (season == 1) {
    listOf(
        MediaEpisode(1, "La señal", "42 min", "Un hallazgo inesperado pone en marcha una investigación."),
        MediaEpisode(2, "Luces lejanas", "39 min", "El equipo sigue una pista hasta el otro lado de la ciudad."),
        MediaEpisode(3, "Punto ciego", "44 min", "Una vieja amistad cambia el rumbo del caso.")
    )
} else {
    listOf(
        MediaEpisode(1, "Regreso", "46 min", "La ciudad ha cambiado y las respuestas también."),
        MediaEpisode(2, "Frecuencia", "41 min", "Una transmisión revela un nuevo patrón."),
        MediaEpisode(3, "La última luz", "48 min", "Todo converge en una noche decisiva.")
    )
}

@Composable
private fun EpisodeRow(episode: MediaEpisode, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.border(if (focused) 2.dp else 1.dp, if (focused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape).clip(shape).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(
            modifier = Modifier.width(104.dp).aspectRatio(1.45f).clip(RoundedCornerShape(12.dp)).background(
                Brush.linearGradient(listOf(Color(0xFF44338B), Color(0xFF176A78), Color(0xFF161D31)))
            ),
            contentAlignment = Alignment.Center
        ) {
            if (!episode.stillUrl.isNullOrBlank()) {
                coil.compose.AsyncImage(
                    model = episode.stillUrl,
                    contentDescription = "Imagen del episodio ${episode.number}",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Icon(Icons.Filled.PlayArrow, contentDescription = "Reproducir episodio ${episode.number}", tint = Color.White, modifier = Modifier.size(27.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("EPISODIO ${episode.number.toString().padStart(2, '0')}  ·  ${episode.duration}", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(episode.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(episode.synopsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(label, modifier = Modifier.width(76.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DetailBackButton(onBack: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.size(42.dp).onFocusChanged { focused = it.isFocused }.border(if (focused) 2.dp else 1.dp, if (focused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline, CircleShape).clip(CircleShape).clickable(onClick = onBack),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}
