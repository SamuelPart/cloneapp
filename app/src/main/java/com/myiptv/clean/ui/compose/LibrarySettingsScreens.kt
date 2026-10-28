package com.myiptv.clean.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myiptv.clean.data.repository.TmdbRepository

@Composable
fun DownloadsScreen(
    isTv: Boolean,
    onOpenMedia: (String) -> Unit,
    viewModel: DownloadsViewModel = viewModel()
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = if (isTv) 36.dp else 18.dp, vertical = 16.dp)) {
        PageHeading("Sin conexión", "Descargas", "Administra el contenido guardado en este dispositivo.")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 18.dp)) {
            LibraryStatCard("Muestras", viewModel.sampleItems.size.toString(), Icons.Filled.Download, Modifier.weight(1f))
            LibraryStatCard("En cola", "0", Icons.Filled.Wifi, Modifier.weight(1f))
            LibraryStatCard("Espacio usado", "—", Icons.Filled.Settings, Modifier.weight(1f))
        }
        if (viewModel.sampleItems.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Filled.Download,
                    title = "Aún no hay descargas",
                    message = "La descarga sin conexión requiere una fuente compatible. Cuando esté configurada, tus títulos aparecerán aquí.",
                    action = "Explorar títulos",
                    onAction = { onOpenMedia(DemoCatalog.media.first().id) }
                )
            }
        } else {
            Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                SectionTitle("Previsualización de la lista")
                viewModel.sampleItems.forEachIndexed { index, item ->
                    DownloadPreviewCard(
                        item = item,
                        status = if (index == 0) "Muestra de descarga" else "Elemento de demostración",
                        progress = if (index == 0) 0.72f else 1f,
                        onClick = { onOpenMedia(item.id) }
                    )
                }
                Text("Estos elementos ilustran el diseño. No son archivos descargados.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LibraryStatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(17.dp)).background(MaterialTheme.colorScheme.surface).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DownloadPreviewCard(item: MediaItem, status: String, progress: Float, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.border(if (focused) 2.dp else 1.dp, if (focused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape).clip(shape).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(modifier = Modifier.width(78.dp).height(106.dp)) { PosterArtwork(item, Modifier.fillMaxSize()) }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(status, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            androidx.compose.material3.LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text("Interfaz de muestra · No ocupa almacenamiento", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
        Icon(Icons.Filled.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HistoryScreen(
    isTv: Boolean,
    onOpenMedia: (String) -> Unit,
    onOpenPlayer: (String) -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = if (isTv) 36.dp else 18.dp, vertical = 16.dp)) {
        PageHeading(
            eyebrow = "Sigue donde lo dejaste",
            title = "Historial",
            description = "Tu actividad reciente en este dispositivo.",
            trailing = {
                OutlinedButton(onClick = viewModel::clear, enabled = !viewModel.historyCleared) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (viewModel.historyCleared) "Borrado" else "Borrar")
                }
            }
        )
        if (viewModel.historyCleared) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Filled.History, "Historial borrado", "Cuando uses el catálogo, tus reproducciones recientes aparecerán aquí.")
            }
        } else {
            Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("VISTO RECIENTEMENTE · DATOS DE EJEMPLO", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                viewModel.recentlyWatched.forEachIndexed { index, item ->
                    HistoryItemCard(
                        item = item,
                        progress = listOf(0.64f, 0.29f, 0.81f, 0.44f)[index % 4],
                        subtitle = listOf("Ayer · 21:14", "Ayer · 18:32", "Lun · 20:05", "Dom · 16:47")[index % 4],
                        onContinue = { onOpenPlayer(item.id) },
                        onDetails = { onOpenMedia(item.id) }
                    )
                }
                Text("El historial se muestra con contenido ficticio para previsualizar la interfaz.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun HistoryItemCard(item: MediaItem, progress: Float, subtitle: String, onContinue: () -> Unit, onDetails: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    var continueFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .border(if (focused) 2.dp else 1.dp, if (focused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(17.dp))
            .clip(RoundedCornerShape(17.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onDetails)
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(modifier = Modifier.width(78.dp).height(105.dp)) { PosterArtwork(item, Modifier.fillMaxSize()) }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(item.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${(progress * 100).toInt()}% visto", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            androidx.compose.material3.LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape), color = MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
        }
        Surface(modifier = Modifier.size(42.dp).onFocusChanged { continueFocused = it.isFocused }.border(if (continueFocused) 2.dp else 1.dp, if (continueFocused) MaterialTheme.colorScheme.secondary else Color.Transparent, CircleShape).clip(CircleShape).clickable(onClick = onContinue), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.17f)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Continuar ${item.title}", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenDownloads: () -> Unit,
    onOpenHistory: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = if (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 840) 36.dp else 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PageHeading("Personaliza tu experiencia", "Ajustes", "Controla reproducción, notificaciones y preferencias.")
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(21.dp)).background(
                Brush.linearGradient(listOf(Color(0xFF34265D), MaterialTheme.colorScheme.surface, Color(0xFF18373A)))
            ).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(modifier = Modifier.size(58.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Perfil local", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Sin cuenta conectada", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)) {
                Text("DEMO", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }

        SettingsGroup("Reproducción") {
            PreferenceRow(Icons.Filled.PlayArrow, "Reproducción automática", "Continuar con el siguiente episodio") {
                Switch(checked = viewModel.autoPlay, onCheckedChange = { viewModel.autoPlay = it })
            }
            androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            PreferenceRow(Icons.Filled.PlayArrow, "Omitir introducciones", "Saltar la intro cuando esté disponible") {
                Switch(checked = viewModel.skipIntro, onCheckedChange = { viewModel.skipIntro = it })
            }
            androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Calidad de reproducción", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text("Preferencia para una futura fuente multimedia", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                FilterChips(listOf("Auto", "HD", "4K"), viewModel.selectedQuality, { viewModel.selectedQuality = it })
            }
        }

        SettingsGroup("Notificaciones") {
            PreferenceRow(Icons.Filled.Notifications, "Notificaciones", "Novedades y avisos de la aplicación") {
                Switch(checked = viewModel.pushNotifications, onCheckedChange = { viewModel.pushNotifications = it })
            }
            androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            PreferenceRow(Icons.Filled.Notifications, "Nuevos episodios", "Avisarme cuando haya episodios nuevos") {
                Switch(checked = viewModel.newEpisodes, onCheckedChange = { viewModel.newEpisodes = it })
            }
        }

        SettingsGroup("Tu actividad") {
            SettingsLink(Icons.Filled.Download, "Descargas", "Gestionar contenido sin conexión", onOpenDownloads)
            androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            SettingsLink(Icons.Filled.History, "Historial", "Ver o borrar actividad reciente", onOpenHistory)
        }

        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Acerca de CLEAN IPTV", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("Versión 1.0.0 · Interfaz de demostración", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text(
                if (TmdbRepository.isConfigured) "Metadatos, fichas e imágenes: TMDb."
                else "TMDb está desconectado: configura la clave local para cargar metadatos e imágenes.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
            Text("Este producto utiliza la API de TMDb, pero no está avalado ni certificado por TMDb.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text("Canales, EPG y reproducción no están conectados a una fuente IPTV real.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, modifier = Modifier.padding(top = 10.dp, bottom = 3.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun SettingsLink(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.border(if (focused) 2.dp else 1.dp, if (focused) MaterialTheme.colorScheme.secondary else Color.Transparent, RoundedCornerShape(13.dp)).clip(RoundedCornerShape(13.dp)).background(if (focused) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.headlineSmall)
    }
}
