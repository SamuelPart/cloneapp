package com.myiptv.clean.ui.compose

import android.content.Intent
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import com.myiptv.clean.data.repository.ChannelLogoKey
import com.myiptv.clean.data.repository.IptvOrgRepository
import com.myiptv.clean.data.repository.channelLogoUrl
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull

internal val PhoneChannelAccent = Color(0xFF416DFF)
private val PhoneListSurface = Color.White.copy(alpha = 0.065f)
private val PhoneDivider = Color.White.copy(alpha = 0.08f)
private data class PhoneChannelSource(val id: String, val url: String, val userAgent: String?, val referrer: String?)

/** Mobile-only live layout: the player stays in the same composition slot when expanded. */
@Composable
internal fun PhoneLiveScreen(
    viewModel: ChannelsViewModel,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onOpenChannelDetail: (String) -> Unit,
    searchOpen: Boolean,
    onCloseSearch: () -> Unit,
    onFullscreenChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val allChannels = viewModel.channels
    val catalog = remember(allChannels) { PhoneChannelCatalog(allChannels) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var selectedFilter by rememberSaveable { mutableStateOf(ALL_PHONE_CHANNELS) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var muted by rememberSaveable { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var retried by remember { mutableStateOf(false) }
    var failedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var exhausted by remember { mutableStateOf(false) }
    var logoRetry by remember { mutableIntStateOf(0) }
    var logos by remember { mutableStateOf<Map<ChannelLogoKey, String>>(emptyMap()) }
    val playerFullscreen = rememberPlayerFullscreen(isTv = false, contentId = "phone-live")
    val visibleRows = remember(catalog, viewModel.query, selectedFilter, favoritesOnly, favorites) {
        catalog.filter(viewModel.query, selectedFilter, favoritesOnly, favorites)
    }

    fun selectChannel(id: String, manual: Boolean) {
        if (manual) failedIds = emptySet()
        selectedId = id
        retried = false
        exhausted = false
        attempt++
    }

    fun stepChannel(direction: Int) {
        val choices = visibleRows.map(PhoneChannelRow::channel).ifEmpty { allChannels }
        val index = adjacentChannelIndex(choices.size, choices.indexOfFirst { it.id == selectedId }, direction)
        if (index != null) selectChannel(choices[index].id, manual = true)
    }

    BackHandler(enabled = playerFullscreen.active) { playerFullscreen.set(false) }
    BackHandler(enabled = searchOpen && !playerFullscreen.active) { onCloseSearch() }
    LaunchedEffect(playerFullscreen.active) { onFullscreenChange(playerFullscreen.active) }
    DisposableEffect(Unit) { onDispose { onFullscreenChange(false) } }
    LaunchedEffect(searchOpen) { if (!searchOpen) viewModel.query = "" }
    LaunchedEffect(catalog.filters, viewModel.isLoading) {
        if (!viewModel.isLoading && catalog.filters.none { it.key == selectedFilter }) selectedFilter = ALL_PHONE_CHANNELS
    }
    LaunchedEffect(catalog.rows, viewModel.isLoading) {
        if (catalog.rows.none { it.channel.id == selectedId }) {
            val first = catalog.rows.firstOrNull()?.channel
            if (first != null) selectChannel(first.id, manual = true)
            else if (!viewModel.isLoading) selectedId = null
        }
    }
    // IPTV-org's logos are optional and fetched independently after the channels have loaded.
    // A timeout, an unsupported format or a failed URL leaves the initials in place.
    LaunchedEffect(allChannels, logoRetry, lifecycleOwner) {
        if (allChannels.any { it.id.startsWith("iptv_") && !it.epgId.isNullOrBlank() }) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                logos = withTimeoutOrNull(20_000L) {
                    try {
                        IptvOrgRepository.channelLogos()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        emptyMap()
                    }
                }.orEmpty()
            }
        }
    }

    val current = allChannels.firstOrNull { it.id == selectedId }
    val source = current?.let { PhoneChannelSource(it.id, it.streamUrl, it.userAgent, it.referrer) }
    val latestSource = rememberUpdatedState(source)
    val currentAttempt = attempt
    var evidence by remember(source, attempt) { mutableStateOf(LivePlaybackEvidence()) }
    var checkMessage by remember(source, attempt) { mutableStateOf("Comprobando señal…") }
    var startedAt by remember(source, attempt) { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(source) {
        if (source != null) {
            retried = false
            exhausted = false
            failedIds = failedIds - source.id
        }
    }
    LaunchedEffect(source, currentAttempt, lifecycleOwner, evidence.failed, evidence.pausedByUser, exhausted, visibleRows) {
        val channel = current ?: return@LaunchedEffect
        if (exhausted) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var waitedForNetwork = false
            while (isActive && selectedId == channel.id && latestSource.value == source && attempt == currentAttempt) {
                if (isDefinitelyOffline(context)) {
                    waitedForNetwork = true
                    checkMessage = "Sin conexión. Esperando la red…"
                    delay(5_000L)
                    continue
                }
                if (waitedForNetwork && evidence.failed) {
                    retried = true
                    attempt++
                    break
                }
                // Never open a second connection while VLC is still loading/playing. A timeout
                // alone is not evidence that a slow, valid stream has failed.
                val probe = if (shouldProbeLiveChannel(evidence)) {
                    LiveChannelProbe.check(channel.streamUrl, channel.userAgent, channel.referrer)
                } else ChannelProbeResult.INCONCLUSIVE
                val remaining = CHANNEL_CHECK_MIN_MS - (SystemClock.elapsedRealtime() - startedAt)
                if (remaining > 0L) delay(remaining)
                if (selectedId != channel.id || latestSource.value != source || attempt != currentAttempt) break
                if (isDefinitelyOffline(context)) {
                    waitedForNetwork = true
                    checkMessage = "Sin conexión. Esperando la red…"
                    delay(5_000L)
                    continue
                }
                when (channelHealthDecision(SystemClock.elapsedRealtime() - startedAt, evidence, probe, retried)) {
                    ChannelHealthDecision.HEALTHY -> checkMessage = if (evidence.hasStarted) "En directo" else "Conectando señal…"
                    ChannelHealthDecision.WAIT -> checkMessage = when {
                        evidence.pausedByUser -> "Reproducción pausada"
                        evidence.failed -> "Fallo no confirmado. Puedes reintentar o esperar."
                        else -> "La señal sigue cargando…"
                    }
                    ChannelHealthDecision.RETRY -> {
                        retried = true
                        attempt++
                        break
                    }
                    ChannelHealthDecision.NEXT -> {
                        val failed = failedIds + channel.id
                        failedIds = failed
                        val choices = visibleRows.map(PhoneChannelRow::channel).ifEmpty { allChannels }
                        val index = nextUnfailedChannelIndex(choices.map(LiveChannel::id), channel.id, failed)
                        if (index == null) {
                            exhausted = true
                            checkMessage = "No se pudo reproducir esta lista. Selecciona un canal para reintentar."
                        } else {
                            selectChannel(choices[index].id, manual = false)
                        }
                        break
                    }
                }
                delay(5_000L)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // On short landscape windows reserve room for the list instead of making the preview huge.
        val previewHeight = (maxWidth * (9f / 16f)).coerceAtMost((maxHeight * 0.42f).coerceAtLeast(88.dp))
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .then(if (playerFullscreen.active) Modifier.weight(1f) else Modifier.height(previewHeight))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (current == null) {
                    Text(
                        if (viewModel.isLoading) "Cargando canales en directo…"
                        else viewModel.errorMessage.ifBlank { "No hay canales disponibles." },
                        color = TvMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp)
                    )
                } else {
                    VlcPlayerScreen(
                        sourceUrl = current.streamUrl,
                        title = current.name,
                        isLiveChannel = true,
                        userAgent = current.userAgent,
                        referrer = current.referrer,
                        isTv = false,
                        fullscreen = playerFullscreen,
                        onBack = { playerFullscreen.set(false) },
                        playbackKey = currentAttempt,
                        onPlaybackEvent = { event ->
                            if (selectedId == current.id && latestSource.value == source && attempt == currentAttempt) {
                                if (event == PlaybackEvent.Opening && evidence.failed) {
                                    startedAt = SystemClock.elapsedRealtime()
                                    checkMessage = "Comprobando señal…"
                                    retried = false
                                    exhausted = false
                                    failedIds = emptySet()
                                }
                                evidence = evidence.withEvent(event)
                            }
                        },
                        onChannelStep = ::stepChannel,
                        phoneLiveActions = PhoneLivePlayerActions(
                            favorite = current.id in favorites,
                            muted = muted,
                            reportedQuality = phoneReportedQuality(current.quality),
                            onToggleFavorite = { onToggleFavorite(current.id) },
                            onMutedChange = { muted = it },
                            onOpenDetail = { onOpenChannelDetail(current.id) },
                            onShare = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, current.name)
                                    putExtra(Intent.EXTRA_TEXT, "${current.name}\n${current.streamUrl}")
                                }
                                runCatching { context.startActivity(Intent.createChooser(intent, "Compartir señal")) }
                                    .onFailure { Toast.makeText(context, "No hay una aplicación para compartir.", Toast.LENGTH_SHORT).show() }
                            }
                        )
                    )
                    if (!playerFullscreen.active && (exhausted || !evidence.hasStarted)) {
                        Text(
                            checkMessage,
                            modifier = Modifier.align(Alignment.TopStart)
                                .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(bottomEnd = 6.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (!playerFullscreen.active) {
                PhoneLiveTabs(favoritesOnly, onSelect = { favoritesOnly = it })
                if (searchOpen) {
                    Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        TvSearchBox(viewModel.query, { viewModel.query = it }, "Buscar canales o país", Modifier.weight(1f))
                        PhoneSmallAction(Icons.Filled.Close, "Cerrar búsqueda", onCloseSearch)
                    }
                }
                PhoneLiveFilters(
                    options = catalog.filters,
                    selected = selectedFilter,
                    onSelect = { selectedFilter = it },
                    loading = viewModel.isLoading,
                    onRefresh = { logoRetry++; viewModel.refresh() }
                )
                if (viewModel.errorMessage.isNotBlank() && allChannels.isNotEmpty()) {
                    Text(viewModel.errorMessage, color = TvMuted, style = MaterialTheme.typography.labelSmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
                }
                if (visibleRows.isEmpty()) {
                    PhoneLiveEmpty(
                        loading = viewModel.isLoading && allChannels.isEmpty(),
                        hasError = allChannels.isEmpty() && viewModel.errorMessage.isNotBlank(),
                        error = viewModel.errorMessage,
                        favoritesOnly = favoritesOnly,
                        hasFilters = viewModel.query.isNotBlank() || selectedFilter != ALL_PHONE_CHANNELS,
                        onRefresh = { logoRetry++; viewModel.refresh() },
                        onClearFilters = { viewModel.query = ""; selectedFilter = ALL_PHONE_CHANNELS },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        itemsIndexed(visibleRows, key = { _, row -> row.channel.id }) { index, row ->
                            PhoneLiveChannelRow(
                                row = row,
                                number = phoneChannelNumber(index),
                                logoUrl = channelLogoUrl(row.channel, logos),
                                selected = row.channel.id == selectedId,
                                onSelect = {
                                    if (row.channel.id != selectedId || evidence.failed || exhausted) {
                                        selectChannel(row.channel.id, manual = true)
                                    }
                                },
                                onOpenDetail = { onOpenChannelDetail(row.channel.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneLiveTabs(favoritesOnly: Boolean, onSelect: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(46.dp).background(PhoneListSurface)) {
        listOf(false to "Canales", true to "Favoritos").forEach { (favorites, label) ->
            val selected = favorites == favoritesOnly
            Box(
                modifier = Modifier.weight(1f).height(46.dp).semantics { this.selected = selected }
                    .clickable { onSelect(favorites) },
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (selected) PhoneChannelAccent else Color.White, fontSize = 17.sp, maxLines = 1)
                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp)
                    .background(if (selected) PhoneChannelAccent else PhoneDivider))
            }
        }
    }
}

@Composable
private fun PhoneLiveFilters(options: List<PhoneChannelFilter>, selected: String, onSelect: (String) -> Unit, loading: Boolean, onRefresh: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(51.dp), verticalAlignment = Alignment.CenterVertically) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(options, key = PhoneChannelFilter::key) { option ->
                Box(
                    modifier = Modifier.height(44.dp).semantics { this.selected = option.key == selected }
                        .clickable { onSelect(option.key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(option.label, color = Color.White, fontSize = 13.sp, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(if (option.key == selected) PhoneChannelAccent else Color.Transparent)
                            .padding(horizontal = 10.dp, vertical = 7.dp))
                }
            }
        }
        if (loading) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(19.dp), color = PhoneChannelAccent, strokeWidth = 2.dp)
            }
        } else PhoneSmallAction(Icons.Filled.Refresh, "Actualizar canales", onRefresh)
    }
}

@Composable
private fun PhoneSmallAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, tint = Color.White.copy(alpha = 0.72f), modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun PhoneLiveChannelRow(row: PhoneChannelRow, number: String, logoUrl: String?, selected: Boolean, onSelect: () -> Unit, onOpenDetail: () -> Unit) {
    val textColor = if (selected) PhoneChannelAccent else Color.White
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().background(PhoneListSurface)
                .semantics { this.selected = selected }.clickable(onClick = onSelect)
                .height(62.dp).padding(start = 13.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PhoneChannelLogo(row.channel, logoUrl, Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(number, color = textColor, fontSize = 14.sp, modifier = Modifier.width(39.dp), maxLines = 1)
                    Text(row.channel.name, color = textColor, fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(row.subtitle, color = if (selected) PhoneChannelAccent else Color.White.copy(alpha = 0.72f),
                    fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onOpenDetail), contentAlignment = Alignment.Center) {
                Box(Modifier.size(23.dp).border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ArrowForward, contentDescription = "Ficha de ${row.channel.name}",
                        tint = Color.White.copy(alpha = 0.72f), modifier = Modifier.size(15.dp))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(PhoneDivider))
    }
}

@Composable
private fun PhoneChannelLogo(channel: LiveChannel, logoUrl: String?, modifier: Modifier = Modifier) {
    var loaded by remember(logoUrl) { mutableStateOf(false) }
    Box(modifier = modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
        if (!loaded) {
            Text(channel.logoLetters, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(3.dp),
                contentScale = ContentScale.Fit,
                onSuccess = { loaded = true },
                onError = { loaded = false }
            )
        }
    }
}

@Composable
private fun PhoneLiveEmpty(
    loading: Boolean,
    hasError: Boolean,
    error: String,
    favoritesOnly: Boolean,
    hasFilters: Boolean,
    onRefresh: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                when { favoritesOnly -> Icons.Filled.FavoriteBorder; hasFilters -> Icons.Filled.Search; else -> Icons.Filled.LiveTv },
                contentDescription = null, tint = TvMuted, modifier = Modifier.size(30.dp)
            )
            val title = when {
                loading -> "Cargando canales"
                hasError -> "No se pudo cargar la lista"
                hasFilters -> "No hay canales para este filtro"
                favoritesOnly -> "Aún no tienes canales favoritos"
                else -> "No hay canales disponibles"
            }
            val message = when {
                loading -> "Consultando las fuentes de televisión."
                hasError -> error
                hasFilters -> "Prueba otro país, categoría o búsqueda."
                favoritesOnly -> "Toca el corazón del reproductor para guardar un canal."
                else -> "Actualiza la lista para volver a intentarlo."
            }
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(message, color = TvMuted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
            when {
                hasError || (!loading && !favoritesOnly && !hasFilters) -> TvActionButton("Actualizar", onRefresh, icon = Icons.Filled.Refresh)
                hasFilters -> TvActionButton("Mostrar todos", onClearFilters)
            }
        }
    }
}
