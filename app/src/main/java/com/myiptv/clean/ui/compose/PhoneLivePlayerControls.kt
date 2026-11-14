package com.myiptv.clean.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myiptv.clean.ui.player.PlaybackState

/** Actions provided by the mobile live screen; no separate VLC instance is created for preview. */
internal data class PhoneLivePlayerActions(
    val favorite: Boolean,
    val muted: Boolean,
    val reportedQuality: String?,
    val onToggleFavorite: () -> Unit,
    val onMutedChange: (Boolean) -> Unit,
    val onShare: () -> Unit,
    val onOpenDetail: () -> Unit,
    val onRequestFullscreen: (pauseForAd: () -> (() -> Unit)) -> Unit,
)

@Composable
internal fun PhoneLivePlayerControls(
    state: PlaybackState,
    actions: PhoneLivePlayerActions,
    canFullscreen: Boolean,
    muteMessage: String?,
    onTogglePlayback: () -> Unit,
    onToggleMute: () -> Unit,
    onFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actionIcon = when {
        state.error != null || state.ended -> Icons.Filled.Replay
        state.isPlaying -> Icons.Filled.Pause
        else -> Icons.Filled.PlayArrow
    }
    val actionLabel = when {
        state.error != null -> "Reintentar canal"
        state.ended -> "Reproducir canal de nuevo"
        state.isPlaying -> "Pausar canal"
        else -> "Reproducir canal"
    }
    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.align(Alignment.TopEnd)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.62f), Color.Transparent)))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            PhonePlayerIcon(Icons.Filled.Share, "Compartir señal", actions.onShare)
            PhonePlayerIcon(Icons.Filled.Info, "Ficha del canal", actions.onOpenDetail)
            val quality = actions.reportedQuality
            Box(
                modifier = Modifier.padding(horizontal = 4.dp).clip(RoundedCornerShape(3.dp))
                    .background(Color.Black.copy(alpha = 0.42f))
                    .semantics { contentDescription = if (quality == null) "Señal en directo" else "Calidad máxima indicada por la fuente: $quality" }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(quality ?: "EN VIVO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            PhonePlayerIcon(
                if (actions.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                if (actions.favorite) "Quitar de favoritos" else "Añadir a favoritos",
                actions.onToggleFavorite,
                tint = if (actions.favorite) PhoneChannelAccent else Color.White
            )
        }
        PlayerIconButton(actionIcon, actionLabel, 52f, modifier = Modifier.align(Alignment.Center), primary = true, onClick = onTogglePlayback)
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PhonePlayerIcon(
                if (actions.muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                if (actions.muted) "Activar sonido" else "Silenciar canal",
                onToggleMute
            )
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                val message = muteMessage ?: state.error ?: state.notice
                if (message != null) {
                    Text(message, color = Color.White, style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                } else if (state.isBuffering) {
                    Text("Conectando…", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (canFullscreen) PhonePlayerIcon(Icons.Filled.Fullscreen, "Pantalla completa horizontal", onFullscreen)
            else Spacer(Modifier.width(44.dp))
        }
    }
}

@Composable
private fun PhonePlayerIcon(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = Color.White) {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(23.dp))
    }
}
