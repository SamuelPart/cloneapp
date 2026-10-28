package com.myiptv.clean.ui.compose

import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem as ExoMediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import com.myiptv.clean.data.repository.ChannelCatalogRepository
import com.myiptv.clean.data.repository.LocalMediaSources
import com.myiptv.clean.data.repository.TmdbRepository

@Composable
fun PlayerScreen(
    contentId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val episodeReference = remember(contentId) { parseEpisodeSourceId(contentId) }
    val mediaId = episodeReference?.mediaId ?: contentId
    val channel = remember(contentId) { ChannelCatalogRepository.cachedChannel(contentId) }
    val media = remember(mediaId) { TmdbRepository.cachedMedia(mediaId) }
    val savedSource = remember(contentId) { LocalMediaSources.get(context, contentId) }
    val sourceUrl = channel?.streamUrl ?: savedSource?.url ?: if (episodeReference == null) media?.streamUrl else null
    val sourceType = when {
        channel != null -> MediaPlaybackType.DIRECT
        savedSource != null -> savedSource.type
        else -> media?.playbackType ?: MediaPlaybackType.DIRECT
    }
    val title = when {
        channel != null -> channel.name
        media == null -> "Reproductor"
        episodeReference == null -> media.title
        else -> "${media.title} · T${episodeReference.seasonNumber} · E${episodeReference.episodeNumber}"
    }
    val isChannelReference = contentId.startsWith("tdt_") || contentId.startsWith("iptv_")

    BackHandler(onBack = onBack)
    if (sourceUrl.isNullOrBlank()) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF080B12)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(28.dp)) {
                Icon(if (isChannelReference) Icons.Filled.LiveTv else Icons.Filled.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(38.dp))
                Text("Fuente de reproducción no disponible", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(
                    if (isChannelReference) "No se encontró una señal activa para este canal."
                    else "Añade un enlace directo o un embed iframe en la ficha del título para reproducirlo.",
                    color = Color.White.copy(alpha = 0.74f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
                )
                PlayerBackButton(onBack)
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (sourceType == MediaPlaybackType.IFRAME) {
            val embedUrl = remember(sourceUrl) { iframeSourceUrl(sourceUrl) }
            if (embedUrl == null) {
                Column(modifier = Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("El embed iframe no contiene una URL HTTP/HTTPS válida.", color = Color.White, textAlign = TextAlign.Center)
                    PlayerBackButton(onBack)
                }
            } else {
                val webView = remember(embedUrl) {
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val scheme = request.url.scheme?.lowercase()
                                return scheme !in setOf("http", "https")
                            }
                        }
                        loadUrl(embedUrl)
                    }
                }
                DisposableEffect(webView) {
                    onDispose {
                        webView.stopLoading()
                        webView.loadUrl("about:blank")
                        webView.destroy()
                    }
                }
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { webView },
                    update = { if (it.url != embedUrl) it.loadUrl(embedUrl) }
                )
            }
        } else {
            val exoPlayer = remember(sourceUrl, channel?.userAgent, channel?.referrer) {
                createExoPlayer(context, sourceUrl.orEmpty(), channel?.userAgent, channel?.referrer)
            }
            var playbackError by remember(sourceUrl) { mutableStateOf("") }
            DisposableEffect(exoPlayer) {
                val listener = object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        playbackError = error.message ?: "No se pudo reproducir esta fuente."
                    }
                }
                exoPlayer.addListener(listener)
                onDispose {
                    exoPlayer.removeListener(listener)
                    exoPlayer.release()
                }
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        player = exoPlayer
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                update = { playerView -> if (playerView.player !== exoPlayer) playerView.player = exoPlayer }
            )
            if (playbackError.isNotBlank()) {
                Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(18.dp), color = Color(0xDD141722), shape = MaterialTheme.shapes.medium) {
                    Text(playbackError, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Surface(modifier = Modifier.align(Alignment.TopStart).padding(14.dp), color = Color(0xAA090C13), shape = MaterialTheme.shapes.large) {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(end = 16.dp)) {
                PlayerBackButton(onBack)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(if (channel != null) "Canal en directo" else if (sourceType == MediaPlaybackType.IFRAME) "Embed" else "Enlace directo", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun PlayerBackButton(onBack: () -> Unit) {
    Surface(
        modifier = Modifier.clip(MaterialTheme.shapes.large).clickable(onClick = onBack),
        color = Color(0xAA171B26), contentColor = Color.White, shape = MaterialTheme.shapes.large
    ) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", modifier = Modifier.size(21.dp))
        }
    }
}

private fun createExoPlayer(
    context: android.content.Context,
    streamUrl: String,
    userAgent: String?,
    referrer: String?
): ExoPlayer {
    val httpFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(userAgent?.takeIf(String::isNotBlank) ?: Util.getUserAgent(context, "CLEAN IPTV"))
    referrer?.takeIf(String::isNotBlank)?.let { value ->
        httpFactory.setDefaultRequestProperties(mapOf("Referer" to value))
    }
    val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .build()
        .apply {
            setMediaItem(ExoMediaItem.fromUri(Uri.parse(streamUrl)))
            prepare()
            playWhenReady = true
        }
}

private data class EpisodeSourceReference(
    val mediaId: String,
    val seasonNumber: Int,
    val episodeNumber: Int
)

private fun parseEpisodeSourceId(value: String): EpisodeSourceReference? {
    val match = Regex("""^(tmdb_(?:movie|tv)_\d+)_s(\d+)_e(\d+)$""").matchEntire(value) ?: return null
    return EpisodeSourceReference(
        mediaId = match.groupValues[1],
        seasonNumber = match.groupValues[2].toIntOrNull() ?: return null,
        episodeNumber = match.groupValues[3].toIntOrNull() ?: return null
    )
}

private fun iframeSourceUrl(value: String): String? {
    val candidate = if (value.contains("<iframe", ignoreCase = true)) extractIframeUrl(value) else value.trim()
    val uri = candidate?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return null
    return uri.toString().takeIf { uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank() }
}

private fun extractIframeUrl(markup: String): String? {
    val sourceStart = markup.indexOf("src", ignoreCase = true)
    if (sourceStart < 0) return null
    val equals = markup.indexOf('=', startIndex = sourceStart + 3)
    if (equals < 0) return null
    var start = equals + 1
    while (start < markup.length && markup[start].isWhitespace()) start++
    if (start >= markup.length) return null
    val quote = markup[start].takeIf { it == '"' || it == '\'' }
    if (quote != null) start++
    val end = if (quote != null) markup.indexOf(quote, start) else markup.indexOfAny(charArrayOf(' ', '\t', '\r', '\n', '>'), start)
    val safeEnd = if (end < 0) markup.length else end
    return markup.substring(start, safeEnd).takeIf(String::isNotBlank)
}
