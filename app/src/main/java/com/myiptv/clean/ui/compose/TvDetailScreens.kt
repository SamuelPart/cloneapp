package com.myiptv.clean.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
internal fun TvUnavailableDetail(icon: ImageVector, title: String, message: String, onBack: () -> Unit) {
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(180)
        runCatching { initialFocus.requestFocus() }
    }
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(28.dp)) {
        TvActionButton("Volver", onBack, modifier = Modifier.focusRequester(initialFocus), icon = Icons.Filled.ArrowBack)
        TvEmptyState(icon, title, message, Modifier.weight(1f))
    }
}

@Composable
internal fun TvChannelDetail(
    channel: LiveChannel,
    favorite: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val initialFocus = remember(channel.id) { FocusRequester() }
    val heroHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.72f).coerceIn(300.dp, 480.dp)
    LaunchedEffect(channel.id) {
        delay(180)
        runCatching { initialFocus.requestFocus() }
    }
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(heroHeight)
                .background(Brush.horizontalGradient(listOf(Color.Black, Color(channel.accent).copy(alpha = 0.3f), Color.Black)))
        ) {
            ChannelLogo(channel, Modifier.align(Alignment.CenterEnd).padding(end = 75.dp).size(150.dp))
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Black.copy(alpha = 0.28f), Color.Transparent))))
            Column(
                modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.72f).padding(start = 36.dp, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TvActionButton("Volver", onBack, icon = Icons.Filled.ArrowBack)
                Text("TV EN DIRECTO · ${channel.category.uppercase()}", color = TvAccent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(channel.name, color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Emisión de televisión en directo", color = TvMuted, style = MaterialTheme.typography.bodyMedium)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvActionButton("Ver canal", onPlay, modifier = Modifier.focusRequester(initialFocus), icon = Icons.Filled.PlayArrow, primary = true)
                    TvActionButton(if (favorite) "En mi lista" else "Mi lista", onToggleFavorite, icon = if (favorite) Icons.Filled.Check else Icons.Filled.Add)
                }
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TvInfoPanel {
                Text("Información de la señal", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Categoría: ${channel.category}", color = TvMuted, style = MaterialTheme.typography.bodySmall)
                channel.epgId?.let { Text("ID EPG: $it", color = TvMuted, style = MaterialTheme.typography.bodySmall) }
                Text("La disponibilidad de cada emisión depende de su operador y puede variar según la región.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun TvEpisodeRow(
    episode: MediaEpisode,
    hasSource: Boolean,
    onPlay: () -> Unit,
    onConfigure: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 450.dp
        Row(
            modifier = Modifier.fillMaxWidth().background(TvSurface).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(if (compact) 90.dp else 150.dp).height(if (compact) 64.dp else 90.dp).background(Color(0xFF242429)), contentAlignment = Alignment.Center) {
                if (!episode.stillUrl.isNullOrBlank()) {
                    AsyncImage(model = episode.stillUrl, contentDescription = "Imagen del episodio ${episode.number}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(30.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                val duration = episode.duration.takeUnless { it.isBlank() || it == "—" }
                Text("EPISODIO ${episode.number.toString().padStart(2, '0')}" + (duration?.let { " · $it" } ?: ""), color = TvAccent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(episode.title, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(episode.synopsis, color = TvMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (hasSource) {
                        TvActionButton("Reproducir", onPlay, icon = Icons.Filled.PlayArrow, primary = true)
                        TvActionButton("Cambiar enlace", onConfigure, icon = Icons.Filled.Add)
                    } else {
                        TvActionButton("Añadir enlace", onConfigure, icon = Icons.Filled.Add)
                    }
                }
            }
        }
    }
}
