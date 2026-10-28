package com.myiptv.clean.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
    val item = DemoCatalog.media.find { it.id == mediaId }
    if (item == null) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            DetailBackButton(onBack)
            EmptyState(Icons.Filled.Movie, "Título no disponible", "No encontramos este elemento en la muestra.")
        }
        return
    }
    val isTv = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 840
    val favorite = isFavorite(item.id)
    var favoriteFocused by remember { mutableStateOf(false) }
    var selectedSeason by remember { mutableStateOf(1) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = if (isTv) 36.dp else 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            DetailBackButton(onBack)
            Text("${if (item.kind == MediaKind.SERIES) "Series" else "Películas"}  /  ${item.title}", modifier = Modifier.padding(start = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                Text(if (item.kind == MediaKind.SERIES) "SERIE ORIGINAL" else "PELÍCULA DESTACADA", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                Text(item.title, color = MaterialTheme.colorScheme.onBackground, style = if (isTv) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${item.year}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..2).forEach { season ->
                            androidx.compose.material3.FilterChip(
                                selected = selectedSeason == season,
                                onClick = { selectedSeason = season },
                                label = { Text("Temporada $season") }
                            )
                        }
                    }
                    val episodes = if (selectedSeason == 1) {
                        listOf(
                            SeriesEpisode("01", "La señal", "42 min", "Un hallazgo inesperado pone en marcha una investigación."),
                            SeriesEpisode("02", "Luces lejanas", "39 min", "El equipo sigue una pista hasta el otro lado de la ciudad."),
                            SeriesEpisode("03", "Punto ciego", "44 min", "Una vieja amistad cambia el rumbo del caso.")
                        )
                    } else {
                        listOf(
                            SeriesEpisode("01", "Regreso", "46 min", "La ciudad ha cambiado y las respuestas también."),
                            SeriesEpisode("02", "Frecuencia", "41 min", "Una transmisión revela un nuevo patrón."),
                            SeriesEpisode("03", "La última luz", "48 min", "Todo converge en una noche decisiva.")
                        )
                    }
                    episodes.forEach { episode ->
                        EpisodeRow(episode, onClick = { onPlay(item.id) })
                    }
                    Text("Episodios y sinopsis de demostración.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Detalles")
                    DetailInfoRow("Audio", "Español · configuración de ejemplo")
                    DetailInfoRow("Calidad", item.quality)
                    DetailInfoRow("Géneros", item.genres.joinToString(", "))
                }
            }
            Column {
                SectionTitle("También te puede gustar")
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.foundation.lazy.items(DemoCatalog.media.filter { it.id != item.id }.take(5), key = MediaItem::id) { recommendation ->
                        MediaPosterCard(recommendation, modifier = Modifier.width(if (isTv) 190.dp else 148.dp), onClick = { onOpenMedia(recommendation.id) })
                    }
                }
            }
            Text("Ficha descriptiva de demostración · No hay contenido multimedia conectado.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private data class SeriesEpisode(val number: String, val title: String, val duration: String, val synopsis: String)

@Composable
private fun EpisodeRow(episode: SeriesEpisode, onClick: () -> Unit) {
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
            Icon(Icons.Filled.PlayArrow, contentDescription = "Reproducir episodio ${episode.number}", tint = Color.White, modifier = Modifier.size(27.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("EPISODIO ${episode.number}  ·  ${episode.duration}", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
