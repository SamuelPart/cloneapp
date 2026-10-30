package com.myiptv.clean.ui.compose

import android.content.Context
import android.net.ConnectivityManager
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.myiptv.clean.ui.player.CHANNEL_CHECK_MIN_MS
import com.myiptv.clean.ui.player.ChannelHealthDecision
import com.myiptv.clean.ui.player.ChannelProbeResult
import com.myiptv.clean.ui.player.LiveChannelProbe
import com.myiptv.clean.ui.player.LivePlaybackEvidence
import com.myiptv.clean.ui.player.PlaybackEvent
import com.myiptv.clean.ui.player.adjacentChannelIndex
import com.myiptv.clean.ui.player.channelHealthDecision
import com.myiptv.clean.ui.player.nextUnfailedChannelIndex
import com.myiptv.clean.ui.player.shouldProbeLiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** A plain holder lets the app's common ancestor see CH keys even when the sidebar has focus. */
internal class TvChannelKeys {
    var onStep: ((Int) -> Unit)? = null
}

internal data class TvChannelRequest(val id: String, val sequence: Long)

private data class ChannelSourceKey(val id: String, val url: String, val userAgent: String?, val referrer: String?)

@Suppress("DEPRECATION")
private fun isDefinitelyOffline(context: Context): Boolean = try {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    if (manager == null) false else manager.activeNetworkInfo?.isConnected != true
} catch (_: SecurityException) {
    false // Unknown connectivity must never be mistaken for an offline device.
}

@Composable
internal fun TvLiveScreen(
    viewModel: ChannelsViewModel,
    fullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    channelKeys: TvChannelKeys,
    requestedChannel: TvChannelRequest?,
    onRequestHandled: () -> Unit,
    onOpenChannelDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val allChannels = viewModel.channels
    val channels = remember(allChannels, viewModel.query, viewModel.selectedCategory) {
        viewModel.filteredChannels.distinctBy(LiveChannel::id)
    }
    val listState = rememberLazyListState()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    var retried by remember { mutableStateOf(false) }
    var failedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var exhausted by remember { mutableStateOf(false) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    fun selectChannel(id: String, manual: Boolean) {
        if (manual) failedIds = emptySet()
        selectedId = id
        retried = false
        exhausted = false
        attempt++
    }

    fun stepChannel(direction: Int) {
        val choices = channels.ifEmpty { allChannels }
        val index = adjacentChannelIndex(choices.size, choices.indexOfFirst { it.id == selectedId }, direction)
        if (index != null) selectChannel(choices[index].id, manual = true)
    }

    SideEffect { channelKeys.onStep = ::stepChannel }
    DisposableEffect(channelKeys) {
        onDispose {
            channelKeys.onStep = null
            onFullscreenChange(false)
        }
    }
    BackHandler(enabled = fullscreen) { onFullscreenChange(false) }

    LaunchedEffect(allChannels, requestedChannel, viewModel.isLoading) {
        if (requestedChannel != null) {
            val requested = allChannels.firstOrNull { it.id == requestedChannel.id }
            if (requested != null) {
                // A channel opened from another section must remain visible despite old filters.
                viewModel.query = ""
                viewModel.selectedCategory = "Todos"
                selectChannel(requested.id, manual = true)
                onRequestHandled()
            } else if (!viewModel.isLoading) {
                onRequestHandled()
            }
        } else if (allChannels.none { it.id == selectedId }) {
            allChannels.firstOrNull()?.let { selectChannel(it.id, manual = true) }
        }
    }
    LaunchedEffect(selectedId, channels, fullscreen) {
        if (!fullscreen) {
            val index = channels.indexOfFirst { it.id == selectedId }
            if (index >= 0) listState.animateScrollToItem(index)
        }
    }

    val current = allChannels.firstOrNull { it.id == selectedId }
    val sourceKey = current?.let { ChannelSourceKey(it.id, it.streamUrl, it.userAgent, it.referrer) }
    val latestSourceKey = rememberUpdatedState(sourceKey)
    var evidence by remember(sourceKey, attempt) { mutableStateOf(LivePlaybackEvidence()) }
    var checkMessage by remember(sourceKey, attempt) { mutableStateOf("Comprobando señal…") }
    var startedAt by remember(sourceKey, attempt) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val currentAttempt = attempt
    val playerFullscreen = rememberPlayerFullscreen(isTv = true, contentId = "tv-live")

    LaunchedEffect(sourceKey) {
        if (sourceKey != null) {
            retried = false
            exhausted = false
            failedIds = failedIds - sourceKey.id
        }
    }
    LaunchedEffect(sourceKey, currentAttempt, lifecycleOwner, evidence.failed, evidence.pausedByUser, exhausted) {
        val channel = current ?: return@LaunchedEffect
        if (exhausted) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var waitedForNetwork = false
            while (isActive && selectedId == channel.id && latestSourceKey.value == sourceKey && attempt == currentAttempt) {
                if (isDefinitelyOffline(context)) {
                    waitedForNetwork = true
                    checkMessage = "Sin conexión. Esperando la red para comprobar esta señal…"
                    delay(5_000)
                    continue
                }
                if (waitedForNetwork && evidence.failed) {
                    // A disconnected device says nothing about whether the channel is healthy.
                    // Give the same channel another chance when a connection returns.
                    retried = true
                    attempt++
                    break
                }
                // VLC itself checks the link while it opens/buffers. Never create a second HTTP
                // session during that time: some providers would disconnect a working stream.
                // Once VLC reports a terminal failure, corroborate it with a bounded HTTP probe.
                val probe = if (shouldProbeLiveChannel(evidence)) {
                    LiveChannelProbe.check(channel.streamUrl, channel.userAgent, channel.referrer)
                } else ChannelProbeResult.INCONCLUSIVE
                val remaining = CHANNEL_CHECK_MIN_MS - (SystemClock.elapsedRealtime() - startedAt)
                if (remaining > 0) delay(remaining)
                if (selectedId != channel.id || latestSourceKey.value != sourceKey || attempt != currentAttempt) break
                if (isDefinitelyOffline(context)) {
                    waitedForNetwork = true
                    checkMessage = "Sin conexión. Esperando la red para comprobar esta señal…"
                    delay(5_000)
                    continue
                }
                when (channelHealthDecision(SystemClock.elapsedRealtime() - startedAt, evidence, probe, retried)) {
                    ChannelHealthDecision.HEALTHY -> checkMessage = if (evidence.hasStarted) "En directo" else "Conectando señal…"
                    ChannelHealthDecision.WAIT -> checkMessage = when {
                        evidence.pausedByUser -> "Reproducción pausada"
                        evidence.failed -> "Fallo no confirmado. Puedes reintentar o esperar."
                        else -> "La señal sigue cargando; comprobación no concluyente…"
                    }
                    ChannelHealthDecision.RETRY -> {
                        // A responding or inconclusive URL and a VLC failure are ambiguous: retry
                        // once before considering the channel unplayable.
                        retried = true
                        attempt++
                        break
                    }
                    ChannelHealthDecision.NEXT -> {
                        val failed = failedIds + channel.id
                        failedIds = failed
                        val choices = channels.ifEmpty { allChannels }
                        val index = nextUnfailedChannelIndex(choices.map(LiveChannel::id), channel.id, failed)
                        if (index == null) {
                            exhausted = true
                            checkMessage = "No se pudo reproducir ningún canal de esta lista. Selecciona uno para reintentar."
                        } else {
                            selectChannel(choices[index].id, manual = false)
                        }
                        break
                    }
                }
                delay(5_000)
            }
        }
    }

    Row(modifier = Modifier.fillMaxSize().padding(if (fullscreen) 0.dp else 12.dp)) {
        // This box and the VLC composable stay in the same composition position when fullscreen
        // changes. Only the surrounding layout changes, so the stream is not restarted.
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.Black), contentAlignment = Alignment.Center) {
            if (current == null) {
                Text(
                    if (viewModel.isLoading) "Cargando canales en directo…"
                    else viewModel.errorMessage.ifBlank { "No hay canales disponibles." },
                    color = TvMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp)
                )
            } else {
                VlcPlayerScreen(
                    sourceUrl = current.streamUrl,
                    title = current.name,
                    isLiveChannel = true,
                    userAgent = current.userAgent,
                    referrer = current.referrer,
                    isTv = true,
                    fullscreen = playerFullscreen,
                    onBack = { onFullscreenChange(false) },
                    tvPreview = !fullscreen,
                    onPreviewActivate = { onFullscreenChange(true) },
                    playbackKey = currentAttempt,
                    onPlaybackEvent = { event ->
                        if (selectedId == current.id && latestSourceKey.value == sourceKey && attempt == currentAttempt) {
                            if (event == PlaybackEvent.Opening && evidence.failed) {
                                // A manual VLC retry starts a fresh cycle, including after the
                                // automatic scan has exhausted all channels.
                                startedAt = SystemClock.elapsedRealtime()
                                checkMessage = "Comprobando señal…"
                                retried = false
                                exhausted = false
                                failedIds = emptySet()
                            }
                            evidence = evidence.withEvent(event)
                        }
                    },
                    onChannelStep = ::stepChannel
                )
                if (exhausted || (!evidence.hasStarted && !fullscreen)) {
                    Text(
                        if (exhausted) checkMessage else checkMessage.takeUnless { it == "En directo" }.orEmpty(),
                        modifier = Modifier.align(Alignment.TopStart)
                            .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(bottomEnd = 6.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (!fullscreen) {
            Spacer(Modifier.width(14.dp))
            TvLiveChannelList(
                channels = channels,
                selectedId = selectedId,
                viewModel = viewModel,
                showFilters = showFilters,
                onToggleFilters = { showFilters = !showFilters },
                onSelect = { selectChannel(it, manual = true) },
                onOpenChannelDetail = onOpenChannelDetail,
                listState = listState,
                modifier = Modifier.weight(0.39f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun TvLiveChannelList(
    channels: List<LiveChannel>,
    selectedId: String?,
    viewModel: ChannelsViewModel,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    onSelect: (String) -> Unit,
    onOpenChannelDetail: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("CANALES", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text("${channels.size} en la lista", color = TvMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            if (selectedId != null) {
                PlayerIconButton(Icons.Filled.Info, "Ficha del canal y favoritos", 40f, onClick = { onOpenChannelDetail(selectedId) })
                Spacer(Modifier.width(4.dp))
            }
            PlayerIconButton(Icons.Filled.Search, "Buscar o filtrar canales", 40f, onClick = onToggleFilters)
            Spacer(Modifier.width(4.dp))
            PlayerIconButton(Icons.Filled.Refresh, "Actualizar canales", 40f, onClick = viewModel::refresh)
        }
        if (viewModel.errorMessage.isNotBlank() && channels.isNotEmpty()) {
            Text(viewModel.errorMessage, color = TvMuted, style = MaterialTheme.typography.labelSmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 6.dp))
        }
        if (showFilters) {
            TvSearchBox(viewModel.query, { viewModel.query = it }, "Buscar canales")
            TvFilterRow(viewModel.categories, viewModel.selectedCategory, { viewModel.selectedCategory = it }, Modifier.fillMaxWidth().padding(vertical = 5.dp))
        }
        if (channels.isEmpty()) {
            Text(
                if (viewModel.isLoading) "Cargando canales…" else viewModel.errorMessage.ifBlank { "No hay canales para este filtro." },
                color = TvMuted, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(state = listState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                items(channels, key = LiveChannel::id) { channel ->
                    TvLiveChannelRow(channel, selected = channel.id == selectedId, onClick = { onSelect(channel.id) })
                }
            }
        }
    }
}

@Composable
private fun TvLiveChannelRow(channel: LiveChannel, selected: Boolean, onClick: () -> Unit) {
    var focused by remember(channel.id) { mutableStateOf(false) }
    val shape = RoundedCornerShape(7.dp)
    Row(
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.clip(shape)
            .background(if (focused) Color.White.copy(alpha = 0.18f) else if (selected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
            .border(if (focused) 2.dp else 0.dp, if (focused) TvAccent else Color.Transparent, shape)
            .clickable(onClick = onClick).heightIn(min = 58.dp).padding(horizontal = 5.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        ChannelLogo(channel, Modifier.size(48.dp))
        Column(Modifier.weight(1f)) {
            Text(channel.name, color = Color.White, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(channel.category, color = TvMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (selected) Icon(Icons.Filled.PlayArrow, contentDescription = "Reproduciendo", tint = TvAccent, modifier = Modifier.size(20.dp))
    }
}
