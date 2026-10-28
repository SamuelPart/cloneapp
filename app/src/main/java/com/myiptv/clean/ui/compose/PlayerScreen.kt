package com.myiptv.clean.ui.compose

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myiptv.clean.data.repository.ChannelCatalogRepository
import com.myiptv.clean.data.repository.LocalMediaSources
import com.myiptv.clean.data.repository.MediaPlaylist
import com.myiptv.clean.data.repository.PlaylistSource
import com.myiptv.clean.data.repository.TmdbRepository
import com.myiptv.clean.data.repository.parseEpisodeSourceId
import com.myiptv.clean.data.repository.resolvePlaybackSource

@Composable
fun PlayerScreen(
    contentId: String,
    isTv: Boolean,
    playlist: MediaPlaylist,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val episodeReference = remember(contentId) { parseEpisodeSourceId(contentId) }
    val mediaId = episodeReference?.mediaId ?: contentId
    val channel = remember(contentId) { ChannelCatalogRepository.cachedChannel(contentId) }
    val media = remember(mediaId, playlist) { TmdbRepository.cachedMedia(mediaId) ?: playlist.item(mediaId) }
    val savedSource = remember(contentId) { LocalMediaSources.get(context, contentId) }
    val mediaSource = resolvePlaybackSource(
        catalog = playlist.source(contentId),
        legacy = savedSource?.let { PlaylistSource(it.url, it.type) },
        fallback = if (episodeReference == null) media?.streamUrl?.let { PlaylistSource(it, media.playbackType) } else null
    )
    val sourceUrl = channel?.streamUrl ?: mediaSource?.url
    val sourceType = if (channel != null) MediaPlaybackType.DIRECT else mediaSource?.type ?: MediaPlaybackType.DIRECT
    val title = when {
        channel != null -> channel.name
        media == null -> "Reproductor"
        episodeReference == null -> media.title
        else -> "${media.title} · T${episodeReference.seasonNumber} · E${episodeReference.episodeNumber}"
    }
    val isChannelReference = contentId.startsWith("tdt_") || contentId.startsWith("iptv_")

    BackHandler(onBack = onBack)
    if (sourceUrl.isNullOrBlank()) {
        Box(modifier = Modifier.fillMaxSize().background(if (isTv) Color.Black else Color(0xFF080B12)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(28.dp)) {
                Icon(if (isChannelReference) Icons.Filled.LiveTv else Icons.Filled.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(38.dp))
                Text("Fuente de reproducción no disponible", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(
                    if (isChannelReference) "No se encontró una señal activa para este canal."
                    else "Este título o episodio no tiene una fuente disponible en el catálogo M3U.",
                    color = Color.White.copy(alpha = 0.74f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
                )
                PlayerBackButton(onBack, isTv)
            }
        }
        return
    }

    if (sourceType == MediaPlaybackType.DIRECT) {
        VlcPlayerScreen(
            sourceUrl = sourceUrl,
            title = title,
            isLiveChannel = channel != null,
            userAgent = channel?.userAgent,
            referrer = channel?.referrer,
            isTv = isTv,
            onBack = onBack
        )
    } else {
        EmbedPlayerScreen(sourceUrl, title, isTv, onBack)
    }
}

/** Web pages use a local bot when their player exposes a controllable HTML video. */
@Composable
private fun EmbedPlayerScreen(sourceUrl: String, title: String, isTv: Boolean, onBack: () -> Unit) {
    val embedUrl = remember(sourceUrl) { iframeSourceUrl(sourceUrl) }
    if (embedUrl == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("El embed iframe no contiene una URL HTTP/HTTPS válida.", color = Color.White, textAlign = TextAlign.Center)
                PlayerBackButton(onBack, isTv)
            }
        }
    } else {
        WebPlayerScreen(embedUrl, title, isTv, onBack)
    }
}

@Composable
private fun PlayerBackButton(onBack: () -> Unit, isTv: Boolean) {
    if (isTv) {
        TvActionButton("Volver", onBack, icon = Icons.Filled.ArrowBack)
        return
    }
    Surface(
        modifier = Modifier.clip(MaterialTheme.shapes.large).clickable(onClick = onBack),
        color = Color(0xAA171B26), contentColor = Color.White, shape = MaterialTheme.shapes.large
    ) {
        Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", modifier = Modifier.size(21.dp))
        }
    }
}

private fun iframeSourceUrl(value: String): String? {
    val candidate = if (value.contains("<iframe", ignoreCase = true)) extractIframeUrl(value) else value.trim()
    val uri = candidate?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return null
    return uri.toString().takeIf { uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank() }
}

private fun extractIframeUrl(markup: String): String? {
    val source = Regex("""(?i)(?:^|[\s<])src\s*=\s*(?:"([^"]+)"|'([^']+)'|([^\s>]+))""")
        .find(markup)?.groupValues?.drop(1)?.firstOrNull(String::isNotBlank)
    return source?.replace("&amp;", "&", ignoreCase = true)
}
