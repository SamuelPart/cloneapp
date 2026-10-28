package com.myiptv.clean.ui.compose

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.myiptv.clean.ui.player.EmbedBotState
import com.myiptv.clean.ui.player.EmbeddedWebPlayer
import com.myiptv.clean.ui.player.PlaybackState
import com.myiptv.clean.ui.player.isPlaybackCenter
import com.myiptv.clean.ui.player.playerControlSizes
import com.myiptv.clean.ui.player.shouldAutoHideControls
import com.myiptv.clean.ui.player.shouldToggleOnSurfaceTap
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Uses SamPlay controls only when the bot can actually reach an HTML video. */
@Composable
internal fun WebPlayerScreen(sourceUrl: String, title: String, isTv: Boolean, fullscreen: PlayerFullscreenState, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val rootView = LocalView.current
    var state by remember(sourceUrl) { mutableStateOf(EmbedBotState()) }
    var loading by remember(sourceUrl) { mutableStateOf(true) }
    var error by remember(sourceUrl) { mutableStateOf<String?>(null) }
    var notice by remember(sourceUrl) { mutableStateOf<String?>(null) }
    var blockedHost by remember(sourceUrl) { mutableStateOf<String?>(null) }
    var providerFullscreen by remember(sourceUrl) { mutableStateOf(false) }
    var providerOwnsFullscreen by remember(sourceUrl) { mutableStateOf(false) }
    var inForeground by remember(sourceUrl) { mutableStateOf(false) }
    var manualMode by remember(sourceUrl) { mutableStateOf(true) }
    var manualRequested by remember(sourceUrl) { mutableStateOf(false) }
    var fallbackHint by remember(sourceUrl) { mutableStateOf(false) }
    var controlsVisible by remember(sourceUrl) { mutableStateOf(true) }
    var interaction by remember(sourceUrl) { mutableIntStateOf(0) }
    val screenFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val errorFocus = remember { FocusRequester() }
    val script = remember(context) {
        runCatching { context.assets.open("samplay_embed_bot.js").bufferedReader(Charsets.UTF_8).use { it.readText() } }.getOrDefault("")
    }
    val player = remember(sourceUrl, script, lifecycleOwner) {
        try {
            EmbeddedWebPlayer(
                context = context,
                sourceUrl = sourceUrl,
                script = script,
                onState = { state = it },
                onLoading = { loading = it },
                onError = { error = it },
                onNotice = { notice = it },
                onBlockedRedirect = { blockedHost = it },
                onFullscreen = { providerFullscreen = it }
            )
        } catch (_: Exception) {
            null
        } catch (_: LinkageError) {
            null
        }
    }
    if (player == null) {
        LaunchedEffect(isTv) {
            if (isTv) {
                withFrameNanos { }
                runCatching { errorFocus.requestFocus() }
            }
        }
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("No se pudo iniciar el reproductor web en este dispositivo.", color = Color.White, textAlign = TextAlign.Center)
                PlayerTextButton(null, "Volver", onBack, Modifier.focusRequester(errorFocus))
            }
        }
        return
    }

    fun showControls() {
        controlsVisible = true
        interaction++
    }
    fun togglePlayback() {
        showControls()
        if (state.isPlaying) player.bot.pause() else player.bot.play()
    }
    fun seekBy(delta: Long) {
        if (!state.isSeekable) return
        val target = (state.timeMs.toDouble() + delta).coerceIn(0.0, state.durationMs.toDouble()).toLong()
        player.bot.seekTo(target)
        showControls()
    }
    fun useProviderControls() {
        manualRequested = true
        manualMode = true
        player.bot.setMasked(false)
        showControls()
    }
    fun useSamPlayControls() {
        if (!state.controllable) return
        manualRequested = false
        manualMode = false
        player.bot.setMasked(true)
        showControls()
    }

    val providerMode = manualMode || providerFullscreen
    BackHandler(enabled = providerFullscreen) { player.hideFullscreen() }
    // A web page's native fullscreen view borrows the same immersive landscape mode; it only
    // restores orientation on exit if the user had not already enabled SamPlay fullscreen.
    LaunchedEffect(providerFullscreen, fullscreen.active, isTv) {
        if (!isTv) {
            if (providerFullscreen && !fullscreen.active) {
                providerOwnsFullscreen = true
                fullscreen.set(true)
                showControls()
            } else if (!providerFullscreen && providerOwnsFullscreen) {
                providerOwnsFullscreen = false
                fullscreen.set(false)
            }
        }
    }
    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> { inForeground = true; player.onForeground() }
                Lifecycle.Event.ON_STOP -> { inForeground = false; player.onBackground() }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            inForeground = true
            player.onForeground()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            inForeground = false
            player.release()
        }
    }
    DisposableEffect(rootView, state.isPlaying, state.controllable, inForeground) {
        val previous = rootView.keepScreenOn
        rootView.keepScreenOn = inForeground && (state.isPlaying || !state.controllable)
        onDispose { rootView.keepScreenOn = previous }
    }
    LaunchedEffect(player, inForeground) {
        while (isActive && inForeground) {
            player.bot.tick()
            delay(1_000)
        }
    }
    LaunchedEffect(state.controllable, state.needsGesture, manualRequested) {
        manualMode = !state.controllable || state.needsGesture || manualRequested
    }
    LaunchedEffect(player, providerMode, state.controllable) {
        player.bot.setMasked(!providerMode && state.controllable)
    }
    LaunchedEffect(loading, state.controllable, error) {
        fallbackHint = false
        if (!loading && !state.controllable && error == null) {
            delay(7_000)
            fallbackHint = true
            delay(6_000)
            fallbackHint = false
        }
    }
    val canAutoHide = shouldAutoHideControls(isTv, controlsVisible,
        tvPlaybackAllowsHiding = state.isPlaying && !providerMode,
        blocked = !isTv && error != null)
    LaunchedEffect(canAutoHide, interaction) {
        if (canAutoHide) {
            delay(5_000)
            controlsVisible = false
        }
    }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(3_000)
            notice = null
        }
    }
    LaunchedEffect(error) { if (error != null) showControls() }
    LaunchedEffect(isTv, providerMode, controlsVisible, state.controllable, error, blockedHost) {
        if (isTv) {
            withFrameNanos { }
            when {
                error != null -> runCatching { errorFocus.requestFocus() }
                providerMode -> player.webView.requestFocus()
                else -> runCatching { if (controlsVisible && state.controllable) playFocus.requestFocus() else screenFocus.requestFocus() }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color.Black)
            // Observe without consuming: an inaccessible provider still receives the real touch
            // it may require for Play, seeking or its own fullscreen controls.
            .pointerInput(isTv, providerMode) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (!isTv && providerMode) showControls()
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val code = event.nativeKeyEvent.keyCode
                val repeated = event.nativeKeyEvent.repeatCount > 0
                when (code) {
                    AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> if (state.controllable) {
                        if (!repeated) togglePlayback(); true
                    } else false
                    AndroidKeyEvent.KEYCODE_MEDIA_PLAY -> if (state.controllable) {
                        if (!repeated) { showControls(); player.bot.play() }; true
                    } else false
                    AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> if (state.controllable) {
                        if (!repeated) { showControls(); player.bot.pause() }; true
                    } else false
                    AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD, AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> if (state.isSeekable) {
                        seekBy(if (code == AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD) 10_000L else -10_000L); true
                    } else false
                    AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        if (!providerMode && state.controllable && !controlsVisible) {
                            if (!repeated) togglePlayback(); true
                        } else {
                            if (!providerMode) showControls()
                            false
                        }
                    }
                    AndroidKeyEvent.KEYCODE_DPAD_UP, AndroidKeyEvent.KEYCODE_DPAD_DOWN,
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT, AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (providerMode) false else {
                            val hidden = !controlsVisible
                            showControls()
                            hidden
                        }
                    }
                    else -> false
                }
            }
            .focusRequester(screenFocus)
            .focusable(enabled = isTv && !providerMode)
    ) {
        val compactHeight = maxHeight < 300.dp
        val sizes = playerControlSizes(maxWidth.value, isTv)
        key(player) {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { player.container })
        }

        if (!providerMode && state.controllable) {
            Box(
                modifier = Modifier.matchParentSize().pointerInput(player, state.controllable, isTv, controlsVisible) {
                    detectTapGestures { point ->
                        val center = isPlaybackCenter(point.x, point.y, size.width, size.height)
                        if (shouldToggleOnSurfaceTap(isTv, controlsVisible, center)) togglePlayback() else showControls()
                    }
                }
            )
        }

        if ((providerMode && isTv) || controlsVisible) {
            WebPlayerHeader(
                title = title,
                isTv = isTv,
                manualMode = providerMode,
                controllable = state.controllable && !state.needsGesture && !providerFullscreen,
                fullscreen = fullscreen,
                onBack = onBack,
                onUseProvider = ::useProviderControls,
                onUseSamPlay = ::useSamPlayControls,
                onToggleFullscreen = {
                    showControls()
                    if (fullscreen.active) {
                        providerOwnsFullscreen = false
                        if (providerFullscreen) player.hideFullscreen()
                        fullscreen.set(false)
                    } else fullscreen.set(true)
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        if (!providerMode && controlsVisible && state.controllable) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(sizes.spacing.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.isSeekable) {
                    PlayerIconButton(Icons.Filled.Replay10, "Retroceder 10 segundos", sizes.side, onClick = { seekBy(-10_000L) })
                }
                val icon = if (state.ended) Icons.Filled.Replay else if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow
                val label = if (state.ended) "Reproducir de nuevo" else if (state.isPlaying) "Pausar" else "Reproducir"
                PlayerIconButton(icon, label, sizes.center, Modifier.focusRequester(playFocus), primary = true, onClick = ::togglePlayback)
                if (state.isSeekable) {
                    PlayerIconButton(Icons.Filled.Forward10, "Avanzar 10 segundos", sizes.side, onClick = { seekBy(10_000L) })
                }
            }
            if (state.isSeekable) {
                val timeline = PlaybackState(isPlaying = state.isPlaying, isBuffering = false, isSeekable = true,
                    timeMs = state.timeMs, durationMs = state.durationMs, ended = state.ended)
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.93f))))
                        .padding(start = if (isTv) 32.dp else 16.dp, end = if (isTv) 32.dp else 16.dp,
                            top = if (compactHeight) 8.dp else 24.dp, bottom = if (compactHeight) 8.dp else 18.dp)
                ) {
                    PlayerTimeline(timeline, isTv, ::showControls, { player.bot.seekTo(it) }, ::seekBy)
                }
            }
        }

        if (loading && error == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).size(26.dp), color = TvAccent, strokeWidth = 3.dp)
        }
        val message = error ?: notice ?: when {
            state.needsGesture -> "El proveedor necesita un toque: pulsa Play en su reproductor."
            fallbackHint -> "El bot no puede controlar este vídeo. Usa los controles de la página."
            else -> null
        }
        if (message != null) {
            Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 18.dp),
                color = Color(0xEE17191E), contentColor = Color.White, shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                    if (error != null && blockedHost != null) {
                        val host = blockedHost.orEmpty().let { if (it.length > 100) "…" + it.takeLast(99) else it }
                        Text("Destino: $host. Permítelo solo si reconoces el proveedor.",
                            textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        PlayerTextButton(null, "Mantener bloqueado", { player.dismissPendingRedirect() }, Modifier.focusRequester(errorFocus))
                        PlayerTextButton(null, "Permitir este destino", { player.approvePendingRedirect() })
                    }
                    if (error != null) {
                        PlayerTextButton(null, "Reintentar", { player.reload() },
                            if (blockedHost == null) Modifier.focusRequester(errorFocus) else Modifier)
                    }
                }
            }
        }
    }
}

@Composable
private fun WebPlayerHeader(
    title: String,
    isTv: Boolean,
    manualMode: Boolean,
    controllable: Boolean,
    fullscreen: PlayerFullscreenState,
    onBack: () -> Unit,
    onUseProvider: () -> Unit,
    onUseSamPlay: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.92f), Color.Transparent)))
            .padding(horizontal = if (isTv) 30.dp else 12.dp, vertical = if (isTv) 16.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PlayerIconButton(Icons.Filled.ArrowBack, "Volver", if (isTv) 48f else 44f, onClick = onBack)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, style = if (isTv) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (manualMode) "Controles del proveedor" else "Reproductor web · Bot SamPlay",
                color = TvMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!manualMode) PlayerTextButton(null, "Controles web", onUseProvider)
        else if (controllable) PlayerTextButton(null, "SamPlay", onUseSamPlay)
        if (!isTv && fullscreen.available) {
            PlayerIconButton(
                if (fullscreen.active) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                if (fullscreen.active) "Salir de pantalla completa" else "Pantalla completa horizontal",
                44f, onClick = onToggleFullscreen
            )
        }
    }
}
