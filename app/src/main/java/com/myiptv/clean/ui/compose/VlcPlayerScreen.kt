package com.myiptv.clean.ui.compose

import android.view.KeyEvent as AndroidKeyEvent
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.myiptv.clean.ui.player.PlaybackController
import com.myiptv.clean.ui.player.PlaybackEvent
import com.myiptv.clean.ui.player.PlaybackState
import com.myiptv.clean.ui.player.PlayerAudioTrack
import com.myiptv.clean.ui.player.VlcPlaybackEngine
import com.myiptv.clean.ui.player.formatPlaybackTime
import com.myiptv.clean.ui.player.isPlaybackCenter
import com.myiptv.clean.ui.player.playerControlSizes
import com.myiptv.clean.ui.player.shouldAutoHideControls
import com.myiptv.clean.ui.player.shouldToggleOnSurfaceTap
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random
import org.videolan.libvlc.util.VLCVideoLayout

private data class VlcBinding(val engine: VlcPlaybackEngine, val controller: PlaybackController)

@Composable
internal fun VlcPlayerScreen(
    sourceUrl: String,
    title: String,
    isLiveChannel: Boolean,
    userAgent: String?,
    referrer: String?,
    isTv: Boolean,
    fullscreen: PlayerFullscreenState,
    onBack: () -> Unit,
    tvPreview: Boolean = false,
    onPreviewActivate: () -> Unit = {},
    playbackKey: Any? = null,
    onPlaybackEvent: (PlaybackEvent) -> Unit = {},
    onChannelStep: ((Int) -> Unit)? = null,
    phoneLiveActions: PhoneLivePlayerActions? = null,
    shortClipActive: Boolean = true,
    shortClipPreload: Boolean = false,
    shortClipDurationMs: Long? = null,
    shortClipStartAtMs: Long? = null,
    onShortClipProgress: (Long) -> Unit = {},
    onShortClipWatched: () -> Unit = {},
    onShortClipFinished: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    var binding by remember(sourceUrl, userAgent, referrer, playbackKey) { mutableStateOf<VlcBinding?>(null) }
    var state by remember(sourceUrl, userAgent, referrer, playbackKey) { mutableStateOf(PlaybackState()) }
    var initializationError by remember(sourceUrl, userAgent, referrer, playbackKey) { mutableStateOf<String?>(null) }
    var generation by remember(sourceUrl, userAgent, referrer, playbackKey) { mutableIntStateOf(0) }
    var controlsVisible by remember(sourceUrl, playbackKey) { mutableStateOf(!tvPreview && phoneLiveActions == null) }
    var audioDialogOpen by remember(sourceUrl, playbackKey) { mutableStateOf(false) }
    var muteMessage by remember(sourceUrl, playbackKey) { mutableStateOf<String?>(null) }
    var interaction by remember(sourceUrl, playbackKey) { mutableIntStateOf(0) }
    var shortClipElapsedMs by remember(sourceUrl, playbackKey) { mutableLongStateOf(0L) }
    var shortClipActionVisible by remember(sourceUrl, playbackKey) { mutableStateOf(false) }
    var shortClipFinished by remember(sourceUrl, playbackKey) { mutableStateOf(false) }
    var shortClipWatched by remember(sourceUrl, playbackKey) { mutableStateOf(false) }
    var shortClipStartApplied by remember(sourceUrl, playbackKey) { mutableStateOf(false) }
    var previewFocused by remember { mutableStateOf(false) }
    val latestPlaybackState = rememberUpdatedState(state)
    val latestShortClipPreload = rememberUpdatedState(shortClipPreload)
    val latestPlaybackEventCallback = rememberUpdatedState(onPlaybackEvent)
    val latestShortClipProgress = rememberUpdatedState(onShortClipProgress)
    val latestShortClipWatched = rememberUpdatedState(onShortClipWatched)
    val latestShortClipFinished = rememberUpdatedState(onShortClipFinished)
    val previousPreview = remember { mutableStateOf(tvPreview) }
    val screenFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val useVideoLayoutBounds = usePhoneLiveVideoBounds(isTv, phoneLiveActions != null)

    fun showControls() {
        controlsVisible = true
        interaction++
    }
    fun togglePlayback() {
        showControls()
        if (shortClipDurationMs != null && shortClipActive) shortClipActionVisible = true
        if (initializationError != null) {
            initializationError = null
            state = PlaybackState()
            binding = null
            generation++
        } else binding?.controller?.togglePlayback()
    }
    fun togglePhoneMute() {
        val actions = phoneLiveActions ?: return
        val next = !actions.muted
        val current = binding
        // Queue a choice while the audio output is opening; apply it when Playing arrives.
        if (current == null || !state.isPlaying || state.isBuffering) {
            actions.onMutedChange(next)
            muteMessage = null
        } else if (runCatching { current.engine.setMuted(next) }.getOrDefault(false)) {
            actions.onMutedChange(next)
            muteMessage = null
        } else {
            muteMessage = "No se pudo cambiar el sonido de esta señal."
        }
        showControls()
    }

    DisposableEffect(sourceUrl, userAgent, referrer, lifecycleOwner, generation, playbackKey, useVideoLayoutBounds) {
        var engine: VlcPlaybackEngine? = null
        var controller: PlaybackController? = null
        var observer: LifecycleEventObserver? = null
        try {
            val newEngine = VlcPlaybackEngine(context, sourceUrl, userAgent, referrer, useVideoLayoutBounds)
            engine = newEngine
            // Keep the callback associated with this exact channel/attempt; stale VLC events must
            // never mark the next channel as failed after a rapid manual change.
            val newController = PlaybackController(
                newEngine,
                onPlaybackEvent = { event ->
                    if (latestShortClipPreload.value && event == PlaybackEvent.Playing) newEngine.setMutedForShortPreload(true)
                    latestPlaybackEventCallback.value(event)
                }
            ) { state = it }
            controller = newController
            binding = VlcBinding(newEngine, newController)
            initializationError = null
            val newObserver = LifecycleEventObserver { _, event ->
                when (event) {
                    // A phone-live stream must not keep playing beneath a rewarded ad activity,
                    // even when that activity only pauses (rather than stops) its host.
                    Lifecycle.Event.ON_RESUME -> if (useVideoLayoutBounds) newController.onForeground()
                    Lifecycle.Event.ON_PAUSE -> if (useVideoLayoutBounds) newController.onBackground()
                    Lifecycle.Event.ON_START -> if (!useVideoLayoutBounds) newController.onForeground()
                    Lifecycle.Event.ON_STOP -> newController.onBackground()
                    else -> Unit
                }
            }
            observer = newObserver
            lifecycleOwner.lifecycle.addObserver(newObserver)
            val foregroundState = if (useVideoLayoutBounds) Lifecycle.State.RESUMED else Lifecycle.State.STARTED
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(foregroundState)) newController.onForeground()
        } catch (error: Throwable) {
            if (error !is Exception && error !is LinkageError) throw error
            if (controller != null) controller.release() else engine?.release()
            binding = null
            initializationError = "No se pudo iniciar el reproductor en este dispositivo."
            state = PlaybackState(isPlaying = false, isBuffering = false, error = initializationError)
        }
        onDispose {
            observer?.let { lifecycleOwner.lifecycle.removeObserver(it) }
            controller?.release()
        }
    }

    PlaybackCoinTimer(
        active = shortClipDurationMs == null && binding != null && state.isPlaying && !state.isBuffering && !state.ended && state.error == null,
        isTv = isTv,
    )

    LaunchedEffect(binding, shortClipActive, shortClipPreload, shortClipDurationMs) {
        val current = binding ?: return@LaunchedEffect
        if (shortClipDurationMs != null) {
            when {
                shortClipActive -> {
                    current.controller.onForeground()
                    current.engine.setMutedForShortPreload(false)
                    current.controller.play()
                }
                shortClipPreload -> {
                    current.controller.onForeground()
                    current.engine.setMutedForShortPreload(true)
                }
                else -> current.controller.onBackground()
            }
        }
    }
    LaunchedEffect(binding, shortClipPreload, state.isPlaying, state.isBuffering) {
        val current = binding ?: return@LaunchedEffect
        if (shortClipDurationMs != null && shortClipPreload && state.isPlaying && !state.isBuffering) {
            // Audio output initialization can lag behind VLC's Playing event. Retry briefly and
            // pause only after mute succeeds, otherwise a zero startup volume could become sticky.
            var attempts = 0
            while (attempts < 10 && isActive && latestShortClipPreload.value) {
                if (current.engine.setMutedForShortPreload(true)) {
                    current.controller.pause()
                    break
                }
                attempts++
                delay(120L)
            }
        }
    }
    LaunchedEffect(binding, state.isSeekable, state.durationMs, shortClipDurationMs, shortClipStartAtMs) {
        val current = binding ?: return@LaunchedEffect
        val clipDuration = shortClipDurationMs ?: return@LaunchedEffect
        if (!shortClipStartApplied && state.isSeekable && state.durationMs > 0L) {
            val maxStart = (state.durationMs - clipDuration).coerceAtLeast(0L)
            val startAt = shortClipStartAtMs?.coerceIn(0L, maxStart)
                ?: if (maxStart > 0L) Random.Default.nextLong(maxStart + 1L) else 0L
            current.controller.seekTo(startAt)
            shortClipStartApplied = true
        }
    }
    LaunchedEffect(binding, shortClipDurationMs, shortClipActive) {
        val clipDuration = shortClipDurationMs ?: return@LaunchedEffect
        if (binding == null || !shortClipActive) return@LaunchedEffect
        while (isActive && !shortClipFinished) {
            delay(250L)
            val current = latestPlaybackState.value
            when {
                current.ended && shortClipActive -> {
                    shortClipFinished = true
                    latestShortClipFinished.value()
                }
                current.isPlaying && !current.isBuffering && current.error == null && shortClipActive -> {
                    shortClipElapsedMs = (shortClipElapsedMs + 250L).coerceAtMost(clipDuration)
                    latestShortClipProgress.value(shortClipElapsedMs)
                    if (!shortClipWatched && shortClipElapsedMs >= minOf(30_000L, clipDuration / 3L)) {
                        shortClipWatched = true
                        latestShortClipWatched.value()
                    }
                    if (shortClipElapsedMs >= clipDuration) {
                        shortClipFinished = true
                        latestShortClipFinished.value()
                    }
                }
                current.error != null && shortClipActive -> {
                    shortClipFinished = true
                    latestShortClipFinished.value()
                }
            }
        }
    }

    LaunchedEffect(binding, initializationError, shortClipDurationMs, shortClipActive) {
        if (binding == null && initializationError != null && shortClipDurationMs != null && shortClipActive) {
            delay(3_500L)
            if (!shortClipFinished) {
                shortClipFinished = true
                latestShortClipFinished.value()
            }
        }
    }

    LaunchedEffect(binding, phoneLiveActions?.muted, state.isPlaying, state.isBuffering) {
        val current = binding
        val actions = phoneLiveActions
        if (current != null && actions != null && state.isPlaying && !state.isBuffering) {
            if (runCatching { current.engine.setMuted(actions.muted) }.getOrDefault(false)) {
                muteMessage = null
            } else {
                muteMessage = "No se pudo cambiar el sonido de esta señal."
                showControls()
            }
        }
    }

    DisposableEffect(view, state.isPlaying) {
        val previous = view.keepScreenOn
        view.keepScreenOn = state.isPlaying
        onDispose { view.keepScreenOn = previous }
    }

    LaunchedEffect(binding) {
        val controller = binding?.controller ?: return@LaunchedEffect
        while (isActive) {
            delay(1_000)
            controller.refreshProgress()
        }
    }
    val canAutoHide = shouldAutoHideControls(isTv, controlsVisible,
        tvPlaybackAllowsHiding = state.isPlaying && !state.isBuffering,
        blocked = state.error != null || audioDialogOpen)
    // State/progress updates on mobile must not restart the inactivity timer.
    LaunchedEffect(canAutoHide, interaction) {
        if (canAutoHide) {
            delay(5_000)
            controlsVisible = false
        }
    }
    LaunchedEffect(shortClipActionVisible, interaction, shortClipDurationMs) {
        if (shortClipDurationMs != null && shortClipActionVisible) {
            delay(1_100L)
            shortClipActionVisible = false
        }
    }
    LaunchedEffect(state.error, state.ended) {
        if (state.error != null || state.ended) showControls()
    }
    LaunchedEffect(state.notice) {
        if (state.notice != null) {
            showControls()
            delay(3_000)
            binding?.controller?.clearNotice()
        }
    }
    LaunchedEffect(tvPreview) {
        val returningToPreview = tvPreview && !previousPreview.value
        previousPreview.value = tvPreview
        if (tvPreview) {
            controlsVisible = false
            audioDialogOpen = false
            if (returningToPreview) {
                withFrameNanos { }
                runCatching { screenFocus.requestFocus() }
            }
        } else if (isTv && isLiveChannel) {
            showControls()
        }
    }
    val phonePreview = phoneLiveActions != null && !fullscreen.active
    LaunchedEffect(phonePreview, phoneLiveActions != null) {
        if (phoneLiveActions != null) {
            if (phonePreview) {
                controlsVisible = state.error != null || state.ended
                audioDialogOpen = false
            } else {
                // Entering fullscreen is an interaction; the usual controls can then auto-hide.
                showControls()
            }
        }
    }
    LaunchedEffect(isTv, tvPreview, controlsVisible, audioDialogOpen, binding, shortClipDurationMs) {
        // The preview must not steal focus from the channel list on every channel change.
        if (isTv && !tvPreview && !audioDialogOpen && shortClipDurationMs == null) {
            withFrameNanos { }
            runCatching { if (controlsVisible) playFocus.requestFocus() else screenFocus.requestFocus() }
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val keyCode = event.nativeKeyEvent.keyCode
                val repeated = event.nativeKeyEvent.repeatCount > 0
                when (keyCode) {
                    AndroidKeyEvent.KEYCODE_CHANNEL_UP, AndroidKeyEvent.KEYCODE_CHANNEL_DOWN -> {
                        val step = onChannelStep ?: return@onPreviewKeyEvent false
                        if (!repeated) step(if (keyCode == AndroidKeyEvent.KEYCODE_CHANNEL_UP) 1 else -1)
                        true
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (!repeated) togglePlayback()
                        true
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_PLAY -> {
                        if (!repeated) {
                            showControls()
                            if (initializationError != null) togglePlayback() else binding?.controller?.play()
                        }
                        true
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        if (!repeated) { showControls(); binding?.controller?.pause() }
                        true
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD, AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> {
                        showControls()
                        binding?.controller?.seekBy(if (keyCode == AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD) 10_000L else -10_000L)
                        true
                    }
                    AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        if (tvPreview) {
                            if (!repeated) onPreviewActivate()
                            true
                        } else if (!controlsVisible) {
                            if (!repeated) togglePlayback()
                            true
                        } else {
                            showControls()
                            false // The focused control owns its click.
                        }
                    }
                    AndroidKeyEvent.KEYCODE_DPAD_UP, AndroidKeyEvent.KEYCODE_DPAD_DOWN,
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT, AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (tvPreview) false else {
                            val wasHidden = !controlsVisible
                            showControls()
                            wasHidden // First direction reveals controls; the next moves focus.
                        }
                    }
                    else -> false
                }
            }
            .onFocusChanged { previewFocused = it.isFocused }
            .border(if (tvPreview && previewFocused) 2.dp else 0.dp, if (tvPreview && previewFocused) TvAccent else Color.Transparent)
            .focusRequester(screenFocus)
            .focusable(enabled = isTv)
    ) {
        val compactHeight = maxHeight < 300.dp
        val sizes = playerControlSizes(maxWidth.value, isTv)
        binding?.let { current ->
            key(current) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        VLCVideoLayout(viewContext).apply {
                            isFocusable = false
                            isFocusableInTouchMode = false
                            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                            current.engine.videoLayout = this
                            current.controller.onSurfaceReady()
                        }
                    }
                )
            }
        }

        // This layer is behind the actual buttons, so touch targets and the timeline win hit testing.
        Box(
            modifier = Modifier.matchParentSize().pointerInput(binding, initializationError, isTv, controlsVisible, tvPreview, onPreviewActivate, shortClipDurationMs, shortClipActive) {
                detectTapGestures { point ->
                    if (tvPreview) onPreviewActivate() else if (shortClipDurationMs != null && shortClipActive) {
                        // Shorts use the same tap anywhere to toggle behavior as mobile clip feeds.
                        togglePlayback()
                    } else {
                        val center = isPlaybackCenter(point.x, point.y, size.width, size.height)
                        if (shouldToggleOnSurfaceTap(isTv, controlsVisible, center)) togglePlayback() else showControls()
                    }
                }
            }
        )

        if (shortClipDurationMs != null) {
            if (shortClipActive && (initializationError != null || state.error != null)) {
                Text(
                    "No se pudo reproducir este clip. Pasando al siguiente…",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = 0.65f)).padding(14.dp)
                )
            } else if (shortClipActive && state.isBuffering) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(30.dp), color = Color.White, strokeWidth = 2.dp)
            }
            if (shortClipActive && shortClipActionVisible) {
                val clipAction = when {
                    state.error != null || state.ended -> Icons.Filled.Replay
                    state.isPlaying -> Icons.Filled.Pause
                    else -> Icons.Filled.PlayArrow
                }
                val clipActionLabel = when {
                    state.error != null -> "Reintentar clip"
                    state.ended -> "Reproducir clip de nuevo"
                    state.isPlaying -> "Pausar clip"
                    else -> "Reproducir clip"
                }
                Box(
                    modifier = Modifier.align(Alignment.Center).size(76.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.58f))
                        .clickable(onClick = ::togglePlayback)
                        .semantics { contentDescription = clipActionLabel },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(clipAction, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        } else if (phonePreview && !controlsVisible && state.isBuffering && state.error == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(28.dp), color = Color.White, strokeWidth = 2.dp)
        }
        if (shortClipDurationMs != null) {
            // The feed supplies its own controls and navigation overlay.
        } else if (tvPreview) {
            Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    initializationError ?: "Pulsa OK para pantalla completa",
                    color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        } else if (controlsVisible && phonePreview && phoneLiveActions != null) {
            PhoneLivePlayerControls(
                state = state,
                actions = phoneLiveActions.copy(
                    onToggleFavorite = { showControls(); phoneLiveActions.onToggleFavorite() },
                    onShare = { showControls(); phoneLiveActions.onShare() },
                    onOpenDetail = { showControls(); phoneLiveActions.onOpenDetail() }
                ),
                canFullscreen = fullscreen.available,
                muteMessage = muteMessage,
                onTogglePlayback = ::togglePlayback,
                onToggleMute = ::togglePhoneMute,
                onFullscreen = {
                    showControls()
                    phoneLiveActions.onRequestFullscreen {
                        // The ordinary/free fullscreen path does not stop or recreate the player.
                        // Only accepting an ad suspends the exact current controller first.
                        val controller = binding?.controller
                        val resume: () -> Unit = {
                            if (controller != null && binding?.controller === controller &&
                                lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                            ) controller.onForeground()
                        }
                        try {
                            controller?.onBackground()
                        } catch (error: Throwable) {
                            runCatching { resume() }
                            throw error
                        }
                        resume
                    }
                },
                modifier = Modifier.matchParentSize()
            )
        } else if (controlsVisible) {
            VlcPlayerHeader(title, isLiveChannel, isTv, fullscreen, onBack, {
                showControls()
                fullscreen.set(!fullscreen.active)
            }, Modifier.align(Alignment.TopCenter).safeAreaWhenEnforced(
                isTv, fullscreen.active, WindowInsetsSides.Horizontal + WindowInsetsSides.Top
            ))
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(sizes.spacing.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.isSeekable) {
                    PlayerIconButton(Icons.Filled.Replay10, "Retroceder 10 segundos", sizes.side, onClick = {
                        showControls(); binding?.controller?.seekBy(-10_000L)
                    })
                }
                val actionIcon = when {
                    state.error != null || state.ended -> Icons.Filled.Replay
                    state.isPlaying -> Icons.Filled.Pause
                    else -> Icons.Filled.PlayArrow
                }
                val actionLabel = when {
                    state.error != null -> "Reintentar"
                    state.ended -> "Reproducir de nuevo"
                    state.isPlaying -> "Pausar"
                    else -> "Reproducir"
                }
                PlayerIconButton(actionIcon, actionLabel, sizes.center, modifier = Modifier.focusRequester(playFocus), primary = true, onClick = ::togglePlayback)
                if (state.isSeekable) {
                    PlayerIconButton(Icons.Filled.Forward10, "Avanzar 10 segundos", sizes.side, onClick = {
                        showControls(); binding?.controller?.seekBy(10_000L)
                    })
                }
            }
            VlcBottomControls(
                state = state,
                isLiveChannel = isLiveChannel,
                isTv = isTv,
                compactHeight = compactHeight,
                compactTimelineRow = compactHeight && maxWidth >= 400.dp,
                onInteraction = ::showControls,
                onSeek = { binding?.controller?.seekTo(it) },
                onSeekBy = { binding?.controller?.seekBy(it) },
                onAudio = {
                    showControls()
                    binding?.controller?.refreshAudioTracks()
                    audioDialogOpen = true
                },
                modifier = Modifier.align(Alignment.BottomCenter).safeAreaWhenEnforced(
                    isTv, fullscreen.active, WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
        }
    }

    if (audioDialogOpen) {
        AudioTracksDialog(
            tracks = state.audioTracks,
            selected = state.selectedAudioTrack,
            isTv = isTv,
            onSelect = { id ->
                binding?.controller?.selectAudioTrack(id)
                audioDialogOpen = false
                showControls()
            },
            onDismiss = { audioDialogOpen = false; showControls() },
            onChannelStep = onChannelStep
        )
    }
}

@Composable
private fun VlcPlayerHeader(
    title: String,
    isLiveChannel: Boolean,
    isTv: Boolean,
    fullscreen: PlayerFullscreenState,
    onBack: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.9f), Color.Transparent)))
            .padding(horizontal = if (isTv) 30.dp else 14.dp, vertical = if (isTv) 20.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlayerIconButton(Icons.Filled.ArrowBack, "Volver", if (isTv) 48f else 44f, onClick = onBack)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = Color.White, style = if (isTv) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (isLiveChannel) "Canal en directo" else "Reproducción SamPlay", color = TvMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        if (!isTv && fullscreen.available) {
            PlayerIconButton(
                if (fullscreen.active) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                if (fullscreen.active) "Salir de pantalla completa" else "Pantalla completa horizontal",
                44f, onClick = onToggleFullscreen
            )
        }
    }
}

@Composable
internal fun PlayerIconButton(
    icon: ImageVector,
    label: String,
    sizeDp: Float,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val light = focused || primary
    Box(
        modifier = modifier.size(sizeDp.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(CircleShape)
            .background(if (light) Color.White else Color.Black.copy(alpha = 0.48f))
            .border(if (focused) 3.dp else 1.dp, if (focused) TvAccent else Color.White.copy(alpha = 0.24f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = if (light) Color.Black else Color.White, modifier = Modifier.size(if (primary) 32.dp else 24.dp))
    }
}

@Composable
private fun VlcBottomControls(
    state: PlaybackState,
    isLiveChannel: Boolean,
    isTv: Boolean,
    compactHeight: Boolean,
    compactTimelineRow: Boolean,
    onInteraction: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.93f))))
            .padding(start = if (isTv) 32.dp else 16.dp, end = if (isTv) 32.dp else 16.dp, top = if (compactHeight) 8.dp else 28.dp, bottom = if (compactHeight) 8.dp else 18.dp)
    ) {
        val message = state.error ?: state.notice
        if (message != null) {
            Text(message, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 4.dp))
        }
        if (state.isSeekable && compactTimelineRow) {
            // On short landscape screens, keep the timeline and audio in one row so neither
            // overlaps the central playback button.
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) { PlayerTimeline(state, isTv, onInteraction, onSeek, onSeekBy) }
                Spacer(Modifier.width(12.dp))
                if (state.isBuffering && state.error == null) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TvAccent, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                PlayerTextButton(Icons.Filled.VolumeUp, "Audio", onAudio)
            }
        } else {
            if (state.isSeekable) PlayerTimeline(state, isTv, onInteraction, onSeek, onSeekBy)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (state.isBuffering && state.error == null) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TvAccent, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Conectando…", color = Color.White, style = MaterialTheme.typography.labelSmall)
                } else if (isLiveChannel) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(Color(0xFFD13F4C)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("EN DIRECTO", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("SamPlay", color = TvMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                PlayerTextButton(Icons.Filled.VolumeUp, "Audio", onAudio)
            }
        }
    }
}

@Composable
internal fun PlayerTimeline(
    state: PlaybackState,
    isTv: Boolean,
    onInteraction: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBy: (Long) -> Unit
) {
    var scrub by remember(state.durationMs) { mutableStateOf<Float?>(null) }
    var focused by remember { mutableStateOf(false) }
    val fraction = if (state.durationMs > 0L) (state.timeMs.toDouble() / state.durationMs).toFloat().coerceIn(0f, 1f) else 0f
    val shownTime = scrub?.let { (state.durationMs * it.toDouble()).toLong() } ?: state.timeMs
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(formatPlaybackTime(shownTime), color = Color.White, style = MaterialTheme.typography.labelSmall)
        Slider(
            value = scrub ?: fraction,
            onValueChange = { scrub = it; onInteraction() },
            onValueChangeFinished = {
                scrub?.let { onSeek((state.durationMs * it.toDouble()).toLong()) }
                scrub = null
                onInteraction()
            },
            modifier = Modifier.weight(1f)
                .semantics { contentDescription = "Progreso de reproducción" }
                .onFocusChanged { focused = it.isFocused }
                .border(if (focused) 1.dp else 0.dp, if (focused) TvAccent else Color.Transparent, RoundedCornerShape(8.dp))
                .onPreviewKeyEvent { event ->
                    val code = event.nativeKeyEvent.keyCode
                    if (isTv && event.type == KeyEventType.KeyDown && (code == AndroidKeyEvent.KEYCODE_DPAD_LEFT || code == AndroidKeyEvent.KEYCODE_DPAD_RIGHT)) {
                        scrub = null
                        onInteraction()
                        onSeekBy(if (code == AndroidKeyEvent.KEYCODE_DPAD_LEFT) -10_000L else 10_000L)
                        true
                    } else false
                },
            colors = SliderDefaults.colors(thumbColor = TvAccent, activeTrackColor = TvAccent, inactiveTrackColor = Color.White.copy(alpha = 0.28f))
        )
        Text(formatPlaybackTime(state.durationMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
internal fun PlayerTextButton(icon: ImageVector?, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = modifier.onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (focused) Color.White else Color.White.copy(alpha = 0.12f))
            .border(if (focused) 2.dp else 1.dp, if (focused) TvAccent else Color.White.copy(alpha = 0.18f), shape)
            .clickable(onClick = onClick)
            .heightIn(min = 44.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = if (focused) Color.Black else Color.White, modifier = Modifier.size(19.dp))
        Text(label, color = if (focused) Color.Black else Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AudioTracksDialog(
    tracks: List<PlayerAudioTrack>,
    selected: Int?,
    isTv: Boolean,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    onChannelStep: ((Int) -> Unit)?
) {
    val initialFocus = remember { FocusRequester() }
    // The first lazy item is guaranteed to be composed even if the selected track is far below.
    val focusId = tracks.firstOrNull()?.id
    LaunchedEffect(isTv, focusId) {
        if (isTv) {
            withFrameNanos { }
            runCatching { initialFocus.requestFocus() }
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                val code = event.nativeKeyEvent.keyCode
                if (onChannelStep != null && event.type == KeyEventType.KeyDown &&
                    (code == AndroidKeyEvent.KEYCODE_CHANNEL_UP || code == AndroidKeyEvent.KEYCODE_CHANNEL_DOWN)
                ) {
                    if (event.nativeKeyEvent.repeatCount == 0) {
                        onDismiss()
                        onChannelStep(if (code == AndroidKeyEvent.KEYCODE_CHANNEL_UP) 1 else -1)
                    }
                    true
                } else false
            }.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp).heightIn(max = (maxHeight - 16.dp).coerceAtLeast(120.dp)),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF17191E),
                contentColor = Color.White
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Pista de audio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (tracks.isEmpty()) {
                        Text("No hay pistas de audio disponibles para esta fuente.", color = TvMuted, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 260.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(tracks, key = { it.id }) { track ->
                                AudioTrackRow(
                                    track = track,
                                    selected = track.id == selected,
                                    modifier = if (track.id == focusId) Modifier.focusRequester(initialFocus) else Modifier,
                                    onClick = { onSelect(track.id) }
                                )
                            }
                        }
                    }
                    PlayerTextButton(null, "Cerrar", onDismiss, modifier = if (focusId == null) Modifier.focusRequester(initialFocus) else Modifier)
                }
            }
        }
    }
}

@Composable
private fun AudioTrackRow(track: PlayerAudioTrack, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember(track.id) { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (focused) Color.White else if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .border(if (focused) 2.dp else 0.dp, if (focused) TvAccent else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(track.name, modifier = Modifier.weight(1f), color = if (focused) Color.Black else Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (selected) Icon(Icons.Filled.Check, contentDescription = "Seleccionada", tint = if (focused) Color.Black else TvAccent, modifier = Modifier.size(20.dp))
    }
}
