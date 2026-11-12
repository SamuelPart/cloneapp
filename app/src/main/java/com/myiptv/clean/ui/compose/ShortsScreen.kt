package com.myiptv.clean.ui.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.myiptv.clean.data.repository.LocalShortsPreferenceStore
import com.myiptv.clean.data.repository.MediaPlaylist
import com.myiptv.clean.data.repository.ShortClip
import com.myiptv.clean.data.repository.SHORTS_BATCH_SIZE
import com.myiptv.clean.data.repository.ShortsPreferenceStore
import com.myiptv.clean.data.repository.recommendShortClips
import com.myiptv.clean.data.repository.shortsCandidates
import com.myiptv.clean.data.repository.shortsTasteLabels
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ShortsScreen(
    playlist: MediaPlaylist,
    isTv: Boolean,
    onBack: () -> Unit,
    onOpenMedia: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findShortsActivity() }
    DisposableEffect(activity, isTv) {
        val window = activity?.window
        if (window == null || isTv) return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val insets = ViewCompat.getRootWindowInsets(window.decorView)
        val statusWasVisible = insets?.isVisible(WindowInsetsCompat.Type.statusBars()) ?: true
        val navigationWasVisible = insets?.isVisible(WindowInsetsCompat.Type.navigationBars()) ?: true
        val previousBehavior = controller.systemBarsBehavior
        controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller.systemBarsBehavior = previousBehavior
            if (statusWasVisible) controller.show(WindowInsetsCompat.Type.statusBars()) else controller.hide(WindowInsetsCompat.Type.statusBars())
            if (navigationWasVisible) controller.show(WindowInsetsCompat.Type.navigationBars()) else controller.hide(WindowInsetsCompat.Type.navigationBars())
        }
    }
    val preferences: ShortsPreferenceStore = remember(context) { LocalShortsPreferenceStore(context) }
    var genreCounts by remember(preferences) { mutableStateOf(preferences.genreCounts()) }
    val candidates = remember(playlist.items, playlist.sources) { shortsCandidates(playlist) }
    val clips = remember { mutableStateListOf<ShortClip>() }
    val pagerState = rememberPagerState(pageCount = { clips.size })
    val scope = rememberCoroutineScope()

    fun appendRecommendedBatch() {
        val batch = recommendShortClips(candidates, genreCounts, batchSize = SHORTS_BATCH_SIZE)
        val previousId = clips.lastOrNull()?.candidate?.playbackId
        val ordered = if (batch.size > 1 && batch.firstOrNull()?.candidate?.playbackId == previousId) {
            batch.drop(1) + batch.first()
        } else batch
        clips.addAll(ordered)
    }

    LaunchedEffect(candidates) {
        clips.clear()
        if (candidates.isNotEmpty()) clips.addAll(recommendShortClips(candidates, genreCounts, batchSize = SHORTS_BATCH_SIZE))
        if (clips.isNotEmpty()) pagerState.scrollToPage(0)
    }
    LaunchedEffect(pagerState.currentPage, clips.size, candidates.size) {
        if (clips.isNotEmpty() && candidates.isNotEmpty() && clips.size - pagerState.currentPage <= 3) appendRecommendedBatch()
    }

    BackHandler(onBack = onBack)
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            clips.isNotEmpty() -> VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { page -> "$page-${clips[page].candidate.playbackId}" }
            ) { page ->
                val clip = clips[page]
                val candidate = clip.candidate
                var elapsedMs by remember(page, candidate.playbackId, clip.durationMs) { mutableLongStateOf(0L) }
                var savedPreference by remember(page, candidate.playbackId) { mutableStateOf(false) }
                val fullscreen = remember(page, candidate.playbackId) { PlayerFullscreenState(null) }

                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    if (pagerState.currentPage == page) VlcPlayerScreen(
                        sourceUrl = candidate.source.url,
                        title = candidate.media.title,
                        isLiveChannel = false,
                        userAgent = null,
                        referrer = null,
                        isTv = isTv,
                        fullscreen = fullscreen,
                        onBack = onBack,
                        playbackKey = "short-$page-${candidate.playbackId}",
                        shortClipActive = pagerState.currentPage == page,
                        shortClipDurationMs = clip.durationMs,
                        shortClipStartAtMs = clip.startAtMs,
                        onShortClipProgress = { elapsedMs = it },
                        onShortClipWatched = {
                            if (!savedPreference) {
                                savedPreference = true
                                preferences.recordWatch(shortsTasteLabels(candidate.media))
                                genreCounts = preferences.genreCounts()
                            }
                        },
                        onShortClipFinished = {
                            if (pagerState.currentPage == page) {
                                scope.launch {
                                    if (page + 1 < clips.size) pagerState.animateScrollToPage(page + 1)
                                }
                            }
                        },
                    )

                    LinearProgressIndicator(
                        progress = (elapsedMs.toFloat() / clip.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f),
                        modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp)
                            .clip(CircleShape),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )

                    Row(
                        modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircleAction(onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White, modifier = Modifier.size(21.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Text("SHORTS", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    }

                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))))
                            .padding(start = 20.dp, end = 18.dp, top = 42.dp, bottom = 24.dp)
                    ) {
                        Text(
                            candidate.media.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        val metadata = listOfNotNull(
                            candidate.media.year.takeIf { it > 0 }?.toString(),
                            candidate.media.kind.shortLabel(),
                            candidate.media.genres.takeIf { it.isNotEmpty() }?.take(3)?.joinToString(" · ")
                        ).joinToString("  •  ")
                        if (metadata.isNotBlank()) {
                            Text(metadata, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall,
                                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onOpenMedia(candidate.media.id) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E5CF7))
                            ) {
                                Icon(Icons.Filled.Movie, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Ver completo", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            CircleAction({ if (page + 1 < clips.size) scope.launch { pagerState.animateScrollToPage(page + 1) } }) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Siguiente video", tint = Color.White, modifier = Modifier.size(27.dp))
                            }
                        }
                    }
                }
            }
            !playlist.loaded -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            else -> ShortsEmptyState(onBack)
        }
    }
}

@Composable
private fun CircleAction(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(46.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.48f),
        contentColor = Color.White
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun ShortsEmptyState(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Movie, contentDescription = null, tint = Color.White.copy(alpha = 0.72f), modifier = Modifier.size(46.dp))
        Text("Aún no hay clips disponibles", color = Color.White, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
        Text("Shorts usa enlaces directos del catálogo para crear fragmentos de 30 a 60 segundos. Los enlaces iframe no se incluyen.",
            color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp))
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) { Text("Volver al catálogo") }
    }
}

private tailrec fun Context.findShortsActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext === this) null else baseContext.findShortsActivity()
    else -> null
}

private fun MediaKind.shortLabel(): String = when (this) {
    MediaKind.MOVIE -> "Película"
    MediaKind.SERIES -> "Serie"
}
