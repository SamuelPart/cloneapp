package com.myiptv.clean.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PlayerScreen(
    contentId: String,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val channel = DemoCatalog.channels.find { it.id == contentId }
    val media = DemoCatalog.media.find { it.id == contentId }
    val title = channel?.name ?: media?.title ?: "Reproductor"
    val subtitle = channel?.currentProgram ?: if (media?.kind == MediaKind.SERIES) "Episodio de muestra · Temporada 1" else "Reproducción de muestra"
    var isPlaying by remember { mutableStateOf(false) }
    var centerFocused by remember { mutableStateOf(false) }
    var bottomPlayFocused by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0.38f) }
    var controlsVisible by remember { mutableStateOf(true) }
    val initialFocus = remember { FocusRequester() }

    BackHandler(onBack = onBack)
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color(0xFF05070B)).clickable { controlsVisible = !controlsVisible }
    ) {
        val isTv = maxWidth >= 840.dp || androidx.compose.ui.platform.LocalContext.current.packageManager.hasSystemFeature("android.software.leanback")
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Brush.radialGradient(listOf(Color(0xFF20274D), Color(0xFF080B12)), center = Offset(size.width * 0.5f, size.height * 0.45f), radius = size.maxDimension * 0.72f))
            drawCircle(Color(0xFF8F72FF).copy(alpha = 0.11f), radius = size.minDimension * 0.3f, center = Offset(size.width * 0.68f, size.height * 0.37f))
            drawCircle(Color.White.copy(alpha = 0.13f), radius = size.minDimension * 0.17f, center = Offset(size.width * 0.68f, size.height * 0.37f), style = Stroke(width = 1.4.dp.toPx()))
        }
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent, Color.Black.copy(alpha = 0.72f)))))

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = if (isTv) 44.dp else 18.dp, vertical = if (isTv) 28.dp else 18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            if (controlsVisible) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp), modifier = Modifier.fillMaxWidth()) {
                    PlayerIconButton(Icons.Filled.ArrowBack, "Volver", onBack)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(subtitle, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (channel != null) {
                        LiveBadge()
                    } else {
                        Text("DEMO", modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    PlayerIconButton(Icons.Filled.Settings, "Ajustes de reproducción") {
                        viewModel.selectedQuality = when (viewModel.selectedQuality) {
                            "Auto" -> "HD"
                            "HD" -> "4K"
                            else -> "Auto"
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(1.dp))
            }

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(
                    modifier = Modifier.size(if (isTv) 100.dp else 82.dp).onFocusChanged { centerFocused = it.isFocused }.clip(CircleShape).focusRequester(initialFocus).clickable { isPlaying = !isPlaying },
                    color = Color.White.copy(alpha = 0.12f),
                    contentColor = Color.White,
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(if (centerFocused) 3.dp else 1.dp, if (centerFocused) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.45f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (isPlaying) "Pausar vista previa" else "Reproducir vista previa", tint = Color.White, modifier = Modifier.size(if (isTv) 54.dp else 44.dp))
                    }
                }
                Text("Sin fuente de vídeo conectada", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Esta pantalla es una previsualización de la interfaz. Configura una URL HLS o DASH para reproducir contenido real.",
                    modifier = Modifier.fillMaxWidth(if (isTv) 0.6f else 0.92f),
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text("CONTROLES EN MODO DEMOSTRACIÓN", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            if (controlsVisible) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (channel != null) "${channel.startTime}  ·  En directo" else "00:${(progress * 82).toInt().toString().padStart(2, '0')}  /  ${media?.duration ?: "--:--"}", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("${viewModel.selectedQuality} · ${viewModel.audioLanguage}", color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelSmall)
                            PlayerTextAction(if (viewModel.subtitlesEnabled) "CC activado" else "CC desactivado") { viewModel.subtitlesEnabled = !viewModel.subtitlesEnabled }
                        }
                    }
                    Slider(value = progress, onValueChange = { progress = it }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(modifier = Modifier.size(48.dp).onFocusChanged { bottomPlayFocused = it.isFocused }.border(if (bottomPlayFocused) 2.dp else 1.dp, if (bottomPlayFocused) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.3f), CircleShape).clip(CircleShape).clickable { isPlaying = !isPlaying }, color = Color.White.copy(alpha = 0.16f)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (isPlaying) "Pausar" else "Reproducir", tint = Color.White)
                            }
                        }
                        PlayerTextAction("−10", style = MaterialTheme.typography.labelLarge) { progress = (progress - 0.05f).coerceAtLeast(0f) }
                        PlayerTextAction("+10", style = MaterialTheme.typography.labelLarge) { progress = (progress + 0.05f).coerceAtMost(1f) }
                        Spacer(Modifier.weight(1f))
                        if (isTv) {
                            PlayerTextAction("Idioma") { viewModel.audioLanguage = if (viewModel.audioLanguage == "Español") "Original" else "Español" }
                            PlayerTextAction("Subtítulos") { viewModel.subtitlesEnabled = !viewModel.subtitlesEnabled }
                        } else {
                            PlayerTextAction("${viewModel.audioLanguage} · CC") { viewModel.subtitlesEnabled = !viewModel.subtitlesEnabled }
                        }
                    }
                }
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(isTv) {
        if (isTv) {
            kotlinx.coroutines.delay(150)
            runCatching { initialFocus.requestFocus() }
        }
    }
}

@Composable
private fun PlayerTextAction(
    label: String,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.labelMedium,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .border(if (focused) 2.dp else 1.dp, if (focused) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.22f), shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (focused) MaterialTheme.colorScheme.secondary else Color.White, style = style)
    }
}

@Composable
private fun PlayerIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.size(46.dp).onFocusChanged { focused = it.isFocused }.clip(CircleShape)
            .background(if (focused) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
            .border(if (focused) 2.dp else 1.dp, if (focused) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.3f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}
