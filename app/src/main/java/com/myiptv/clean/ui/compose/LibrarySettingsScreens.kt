package com.myiptv.clean.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myiptv.clean.data.repository.TmdbRepository

@Composable
fun DownloadsScreen(isTv: Boolean) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = if (isTv) 36.dp else 18.dp, vertical = 16.dp)) {
        PageHeading("Sin conexión", "Descargas", "Contenido guardado en este dispositivo.")
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Filled.Download,
                title = "No hay descargas",
                message = "La aplicación todavía no ofrece descargas sin conexión. Los enlaces de reproducción no se guardan como archivos."
            )
        }
    }
}

@Composable
fun HistoryScreen(isTv: Boolean) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = if (isTv) 36.dp else 18.dp, vertical = 16.dp)) {
        PageHeading("Actividad local", "Historial", "Reproducciones recientes en este dispositivo.")
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Filled.History,
                title = "Historial vacío",
                message = "La actividad de reproducción todavía no se guarda."
            )
        }
    }
}

@Composable
fun SettingsScreen(isTv: Boolean) {
    val horizontalPadding = if (isTv) 36.dp else 18.dp
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        PageHeading("Fuentes de información y reproducción", "Ajustes", "Las credenciales y fuentes de vídeo no se incluyen en la aplicación.")

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Fuentes", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            SettingsSourceRow(
                icon = Icons.Filled.Key,
                title = "Metadatos TMDb",
                subtitle = if (TmdbRepository.isConfigured) "Hay una API Key local configurada." else "Falta la API Key local. Añádela en local.properties o en TMDB_API_KEY."
            )
            androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            SettingsSourceRow(
                icon = Icons.Filled.LiveTv,
                title = "Canales de televisión",
                subtitle = "Catálogo de emisiones en directo de distintos países."
            )
            androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            SettingsSourceRow(
                icon = Icons.Filled.Movie,
                title = "Enlaces de películas y series",
                subtitle = "Añade un enlace directo autorizado o un embed iframe desde la ficha. Se conserva solo en este dispositivo."
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text("Privacidad y atribución", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("La clave TMDb se lee desde local.properties o la variable de entorno TMDB_API_KEY; no está incluida en Git.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text("Este producto utiliza la API de TMDb, pero no está avalado ni certificado por TMDb.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text("Tipografía Inter © The Inter Project Authors, distribuida bajo SIL Open Font License 1.1.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text("La disponibilidad de las emisiones de televisión puede variar según el país y las condiciones de cada señal.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(7.dp))
            Text("XUPER TV · versión 1.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, letterSpacing = 0.4.sp)
        }
    }
}

@Composable
private fun SettingsSourceRow(icon: ImageVector, title: String, subtitle: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), modifier = Modifier.size(38.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}
