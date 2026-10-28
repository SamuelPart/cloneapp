package com.myiptv.clean.ui.compose

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
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
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.R as ExoUiR
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import com.myiptv.clean.R
import com.myiptv.clean.data.repository.ChannelCatalogRepository
import com.myiptv.clean.data.repository.LocalMediaSources
import com.myiptv.clean.data.repository.MediaPlaylist
import com.myiptv.clean.data.repository.PlaylistSource
import com.myiptv.clean.data.repository.TmdbRepository
import com.myiptv.clean.data.repository.parseEpisodeSourceId
import com.myiptv.clean.data.repository.resolvePlaybackSource
import com.myiptv.clean.ui.player.phonePlayerButtonWidth
import kotlinx.coroutines.delay

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
    var controllerVisible by remember(contentId) { mutableStateOf(true) }
    var iframeHeaderVisible by remember(contentId) { mutableStateOf(true) }
    val controllerListener = remember(contentId) {
        PlayerControlView.VisibilityListener { visibility -> controllerVisible = visibility == View.VISIBLE }
    }
    LaunchedEffect(contentId, sourceType, isTv) {
        if (isTv && sourceType == MediaPlaybackType.IFRAME) {
            iframeHeaderVisible = true
            delay(5_000)
            iframeHeaderVisible = false
        }
    }

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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (sourceType == MediaPlaybackType.IFRAME) {
            val embedUrl = remember(sourceUrl) { iframeSourceUrl(sourceUrl) }
            if (embedUrl == null) {
                Column(modifier = Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("El embed iframe no contiene una URL HTTP/HTTPS válida.", color = Color.White, textAlign = TextAlign.Center)
                    PlayerBackButton(onBack, isTv)
                }
            } else {
                val webView = remember(embedUrl) {
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        webChromeClient = WebChromeClient()
                        if (isTv) {
                            isFocusable = true
                            isFocusableInTouchMode = true
                            post { requestFocus() }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val scheme = request.url.scheme?.lowercase()
                                return scheme !in setOf("http", "https")
                            }

                            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                                val scheme = Uri.parse(url).scheme?.lowercase()
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
                    update = { /* Redirects and provider navigation belong to the embed. */ }
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
                    val playerView = LayoutInflater.from(viewContext).inflate(R.layout.tv_player_view, null, false) as PlayerView
                    playerView.apply {
                        setControllerVisibilityListener(controllerListener)
                        isFocusable = isTv
                        if (!isTv) adaptPhonePlayerControls(this)
                        player = exoPlayer
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        controllerVisible = isControllerVisible
                        post {
                            if (isTv) requestFocus()
                            showController()
                        }
                    }
                },
                update = { playerView ->
                    playerView.setControllerVisibilityListener(controllerListener)
                    if (playerView.player !== exoPlayer) playerView.player = exoPlayer
                }
            )
            if (playbackError.isNotBlank()) {
                Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(18.dp), color = Color(0xDD141722), shape = MaterialTheme.shapes.medium) {
                    Text(playbackError, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        val subtitle = if (channel != null) "Canal en directo" else if (sourceType == MediaPlaybackType.IFRAME) "Embed" else "Enlace directo"
        if (isTv) {
            val showHeader = if (sourceType == MediaPlaybackType.IFRAME) iframeHeaderVisible else controllerVisible
            if (showHeader) {
                TvPlayerHeader(title, subtitle, onBack, Modifier.align(Alignment.TopStart))
            }
        } else if (sourceType == MediaPlaybackType.IFRAME || controllerVisible) {
            Surface(modifier = Modifier.align(Alignment.TopStart).padding(14.dp), color = Color(0xAA090C13), shape = MaterialTheme.shapes.large) {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(end = 16.dp)) {
                    PlayerBackButton(onBack, false)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(title, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(subtitle, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun TvPlayerHeader(title: String, subtitle: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.9f), Color.Transparent)))
            .padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TvActionButton("Volver", onBack, icon = Icons.Filled.ArrowBack)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, color = TvMuted, style = MaterialTheme.typography.labelSmall)
        }
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
        androidx.compose.foundation.layout.Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", modifier = Modifier.size(21.dp))
        }
    }
}

/** Keep the TV controller design while fitting all its buttons on narrow phones. */
private fun adaptPhonePlayerControls(playerView: PlayerView) {
    val density = playerView.resources.displayMetrics.density
    fun pixels(dp: Float): Int = (dp * density + 0.5f).toInt()
    val panel = playerView.findViewById<View>(R.id.samplay_player_controls)
    panel?.setPadding(pixels(12f), panel.paddingTop, pixels(12f), panel.paddingBottom)
    val side = phonePlayerButtonWidth(playerView.resources.configuration.screenWidthDp.toFloat())
    val regularButtons = intArrayOf(ExoUiR.id.exo_prev, ExoUiR.id.exo_rew, ExoUiR.id.exo_ffwd, ExoUiR.id.exo_next)
    val centralButtons = intArrayOf(ExoUiR.id.exo_play, ExoUiR.id.exo_pause)
    fun resize(id: Int, dp: Float) {
        val button = playerView.findViewById<View>(id) ?: return
        val params = button.layoutParams ?: return
        params.width = pixels(dp)
        params.height = pixels(dp)
        button.layoutParams = params
    }
    regularButtons.forEach { resize(it, side) }
    centralButtons.forEach { resize(it, (side + 8f).coerceAtMost(56f)) }
}

private fun createExoPlayer(
    context: android.content.Context,
    streamUrl: String,
    userAgent: String?,
    referrer: String?
): ExoPlayer {
    val httpFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(userAgent?.takeIf(String::isNotBlank) ?: Util.getUserAgent(context, "SamPlay"))
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
