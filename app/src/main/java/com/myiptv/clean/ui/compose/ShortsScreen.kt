package com.myiptv.clean.ui.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import coil.compose.AsyncImage
import com.myiptv.clean.data.repository.GuestProfile
import com.myiptv.clean.data.repository.GuestProfileRepository
import com.myiptv.clean.data.repository.GuestStory
import com.myiptv.clean.data.repository.LocalShortsPreferenceStore
import com.myiptv.clean.data.repository.MediaPlaylist
import com.myiptv.clean.data.repository.ShortClip
import com.myiptv.clean.data.repository.ShortsComment
import com.myiptv.clean.data.repository.ShortsInteractionState
import com.myiptv.clean.data.repository.ShortsInteractionsRepository
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
    initialClipId: String? = null,
    onInitialClipHandled: () -> Unit = {},
    onBack: () -> Unit,
    onOpenMedia: (String) -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {}
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
    val interactions = remember(context) { ShortsInteractionsRepository(context) }
    val guestProfiles = remember(context) { GuestProfileRepository(context) }
    var ownerProfile by remember { mutableStateOf<GuestProfile?>(null) }
    var stories by remember { mutableStateOf(emptyList<GuestStory>()) }
    var selectedStoryGroup by remember { mutableStateOf<List<GuestStory>?>(null) }
    var storyComposerUri by remember { mutableStateOf<Uri?>(null) }
    var storyCaption by remember { mutableStateOf("") }
    var storyMessage by remember { mutableStateOf<String?>(null) }
    var storyPublishing by remember { mutableStateOf(false) }
    val storyPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { storyComposerUri = uri; storyCaption = ""; storyMessage = null }
    }
    var genreCounts by remember(preferences) { mutableStateOf(preferences.genreCounts()) }
    val candidates = remember(playlist.items, playlist.sources) { shortsCandidates(playlist) }
    val clips = remember { mutableStateListOf<ShortClip>() }
    val pagerState = rememberPagerState(pageCount = { clips.size })
    var activeSourceIdentity by remember { mutableStateOf(emptyList<Triple<String, String, MediaPlaybackType>>()) }
    val scope = rememberCoroutineScope()
    val storyGroups = remember(stories) {
        stories.groupBy { it.profileClientId }.values
            .map { group -> group.sortedBy(GuestStory::createdAt) }
            .sortedByDescending { group -> group.lastOrNull()?.createdAt.orEmpty() }
    }
    LaunchedEffect(guestProfiles) {
        ownerProfile = guestProfiles.myProfile()
        stories = runCatching { guestProfiles.loadStories() }.getOrDefault(emptyList())
    }
    fun refreshStories() {
        scope.launch { stories = runCatching { guestProfiles.loadStories() }.getOrDefault(stories) }
    }

    fun appendRecommendedBatch() {
        val batch = recommendShortClips(candidates, genreCounts, batchSize = SHORTS_BATCH_SIZE)
        val previousId = clips.lastOrNull()?.candidate?.playbackId
        val ordered = if (batch.size > 1 && batch.firstOrNull()?.candidate?.playbackId == previousId) {
            batch.drop(1) + batch.first()
        } else batch
        clips.addAll(ordered)
    }

    LaunchedEffect(candidates, initialClipId) {
        val identity = candidates.map { Triple(it.playbackId, it.source.url, it.source.type) }
        if (identity != activeSourceIdentity || initialClipId != null) {
            activeSourceIdentity = identity
            clips.clear()
            if (candidates.isNotEmpty()) {
                val recommended = recommendShortClips(candidates, genreCounts, batchSize = SHORTS_BATCH_SIZE)
                val sharedClip = candidates.firstOrNull { it.media.id == initialClipId }
                val firstBatch = if (sharedClip == null) recommended else
                    listOf(sharedClip) + recommended.filterNot { it.playbackId == sharedClip.playbackId }
                clips.addAll(firstBatch.take(SHORTS_BATCH_SIZE))
                pagerState.scrollToPage(0)
                if (sharedClip != null) onInitialClipHandled()
            }
        } else {
            // TMDb can enrich titles/genres after the local catalog loads. Refresh metadata in-place
            // so the pager and the currently warming player do not disappear or restart.
            val latestByPlaybackId = candidates.associateBy { it.playbackId }
            clips.indices.forEach { index ->
                val previous = clips[index]
                val latest = latestByPlaybackId[previous.candidate.playbackId]
                if (latest != null && previous.candidate.media != latest.media) {
                    clips[index] = previous.copy(candidate = latest)
                }
            }
        }
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
            ) { page: Int ->
                val clip = clips[page]
                val candidate = clip.candidate
                val isCurrentPage = pagerState.currentPage == page
                val isNextPage = page == pagerState.currentPage + 1
                var elapsedMs by remember(page, candidate.playbackId, clip.durationMs) { mutableLongStateOf(0L) }
                var savedPreference by remember(page, candidate.playbackId) { mutableStateOf(false) }
                var interactionState by remember(page, candidate.playbackId, clip.startAtMs, clip.durationMs) {
                    mutableStateOf(ShortsInteractionState())
                }
                var interactionMessage by remember(page, candidate.playbackId, clip.startAtMs, clip.durationMs) { mutableStateOf<String?>(null) }
                var commentsOpen by remember(page, candidate.playbackId, clip.startAtMs, clip.durationMs) { mutableStateOf(false) }
                var commentSending by remember(page, candidate.playbackId, clip.startAtMs, clip.durationMs) { mutableStateOf(false) }
                var commentDraft by remember(page, candidate.playbackId, clip.startAtMs, clip.durationMs) { mutableStateOf("") }
                val interactionClipId = remember(candidate.playbackId, clip.startAtMs, clip.durationMs) {
                    "${candidate.playbackId}|${clip.startAtMs ?: -1L}|${clip.durationMs}"
                }
                val fullscreen = remember(page, candidate.playbackId) { PlayerFullscreenState(null) }

                LaunchedEffect(interactions, interactionClipId, isCurrentPage) {
                    if (!isCurrentPage) return@LaunchedEffect
                    runCatching { interactions.load(interactionClipId) }
                        .onSuccess { interactionState = it; interactionMessage = null }
                        .onFailure { interactionMessage = "No se pudieron cargar las acciones. Revisa tu conexión." }
                }

                fun refreshInteractions() {
                    scope.launch {
                        runCatching { interactions.load(interactionClipId) }
                            .onSuccess { interactionState = it; interactionMessage = null }
                            .onFailure { interactionMessage = "No se pudo actualizar. Inténtalo de nuevo." }
                    }
                }
                fun toggleReaction() {
                    val previous = interactionState
                    val nextLiked = !previous.liked
                    interactionState = previous.copy(
                        liked = nextLiked,
                        likeCount = (previous.likeCount + (if (nextLiked) 1 else -1)).coerceAtLeast(0)
                    )
                    scope.launch {
                        runCatching { interactions.setLiked(interactionClipId, nextLiked) }
                            .onSuccess { refreshInteractions() }
                            .onFailure {
                                interactionState = previous
                                interactionMessage = "No se pudo guardar la reacción. Inténtalo de nuevo."
                            }
                    }
                }
                fun shareShort() {
                    try {
                        val cardUri = createShortPromoCard(context, candidate.media.title, candidate.media.id.toString())
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, cardUri)
                            putExtra(Intent.EXTRA_TEXT, "${candidate.media.title} · Descúbrelo en SamPlay")
                            clipData = android.content.ClipData.newUri(context.contentResolver, "SamPlay Short", cardUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooser = Intent.createChooser(shareIntent, "Compartir Short de SamPlay")
                        if (activity != null) activity.startActivity(chooser)
                        else context.startActivity(chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        scope.launch {
                            runCatching { interactions.recordShare(interactionClipId) }
                                .onSuccess { refreshInteractions() }
                                .onFailure { interactionMessage = "No se pudo registrar el compartido." }
                        }
                    } catch (_: Exception) {
                        interactionMessage = "No se pudo generar o compartir la tarjeta promocional."
                    }
                }
                fun submitComment() {
                    val text = commentDraft.trim()
                    if (text.isEmpty() || commentSending) return
                    commentSending = true
                    scope.launch {
                        runCatching {
                            if (ownerProfile != null) guestProfiles.publishComment(interactionClipId, text)
                            else interactions.addComment(interactionClipId, text)
                        }
                            .onSuccess {
                                commentDraft = ""
                                refreshInteractions()
                            }
                            .onFailure { interactionMessage = "No se pudo publicar el comentario. Inténtalo de nuevo." }
                        commentSending = false
                    }
                }

                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    if (isCurrentPage || isNextPage) VlcPlayerScreen(
                        sourceUrl = candidate.source.url,
                        title = candidate.media.title,
                        isLiveChannel = false,
                        userAgent = null,
                        referrer = null,
                        isTv = isTv,
                        fullscreen = fullscreen,
                        onBack = onBack,
                        playbackKey = "short-$page-${candidate.playbackId}",
                        shortClipActive = isCurrentPage,
                        shortClipPreload = isNextPage,
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

                    if (isCurrentPage) {
                        Column(
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ShortsInteractionAction(
                                icon = if (interactionState.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                label = if (interactionState.liked) "Quitar reacción" else "Me gusta",
                                count = interactionState.likeCount,
                                tint = if (interactionState.liked) Color(0xFFFF4F78) else Color.White,
                                onClick = ::toggleReaction
                            )
                            ShortsInteractionAction(
                                icon = Icons.Filled.ChatBubbleOutline,
                                label = "Comentarios",
                                count = interactionState.commentCount,
                                onClick = { commentsOpen = true }
                            )
                            ShortsInteractionAction(
                                icon = Icons.Filled.Share,
                                label = "Compartir",
                                count = interactionState.shareCount,
                                onClick = ::shareShort
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (elapsedMs.toFloat() / clip.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f) },
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White, modifier = Modifier.size(21.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Text("SHORTS", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    }

                    if (isCurrentPage) {
                        StoriesTray(
                            ownProfile = ownerProfile,
                            groups = storyGroups,
                            onAddStory = { if (ownerProfile == null) onOpenProfile() else storyPicker.launch("image/*") },
                            onOpenStory = { selectedStoryGroup = it }
                        )
                    }

                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))))
                            .padding(start = 20.dp, end = 82.dp, top = 42.dp, bottom = 24.dp)
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
                        interactionMessage?.let { message ->
                            Text(message, color = Color(0xFFFFC1C1), style = MaterialTheme.typography.labelSmall,
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
                if (isCurrentPage && commentsOpen) {
                    ShortsCommentsDialog(
                        comments = interactionState.comments,
                        totalCount = interactionState.commentCount,
                        draft = commentDraft,
                        sending = commentSending,
                        message = interactionMessage,
                        onDraftChange = { commentDraft = it.take(500) },
                        onSend = ::submitComment,
                        onOpenUserProfile = onOpenUserProfile,
                        onDismiss = { commentsOpen = false }
                    )
                }
            }
            !playlist.loaded -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            else -> ShortsEmptyState(onBack)
        }
    }
    storyComposerUri?.let { uri ->
        StoryComposerDialog(
            imageUri = uri,
            caption = storyCaption,
            message = storyMessage,
            saving = storyPublishing,
            onCaptionChange = { storyCaption = it.take(240) },
            onDismiss = { if (!storyPublishing) storyComposerUri = null },
            onPublish = {
                if (!storyPublishing) {
                    storyPublishing = true
                    storyMessage = null
                    scope.launch {
                        runCatching { guestProfiles.publishStory(uri, storyCaption) }
                            .onSuccess { storyComposerUri = null; refreshStories() }
                            .onFailure { storyMessage = it.message ?: "No se pudo publicar la historia." }
                        storyPublishing = false
                    }
                }
            }
        )
    }
    selectedStoryGroup?.let { group ->
        StoryViewerDialog(
            stories = group,
            onOpenProfile = { username -> selectedStoryGroup = null; onOpenUserProfile(username) },
            onDismiss = { selectedStoryGroup = null }
        )
    }
}

@Composable
private fun BoxScope.StoriesTray(
    ownProfile: GuestProfile?,
    groups: List<List<GuestStory>>,
    onAddStory: () -> Unit,
    onOpenStory: (List<GuestStory>) -> Unit
) {
    val ownStories = groups.firstOrNull { it.firstOrNull()?.profileClientId == ownProfile?.clientId }.orEmpty()
    val otherStories = groups.filterNot { it.firstOrNull()?.profileClientId == ownProfile?.clientId }
    LazyRow(
        modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(top = 78.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
    ) {
        item(key = "own-story") {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                if (ownStories.isNotEmpty()) onOpenStory(ownStories) else onAddStory()
            }) {
                StoryAvatar(ownProfile?.avatarUrl, ownProfile?.displayName ?: "Tu historia", isOwn = ownStories.isEmpty())
                Text(if (ownStories.isEmpty()) "Tu historia" else "Mi historia", color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
        if (ownStories.isNotEmpty()) {
            item(key = "add-story") {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onAddStory)) {
                    StoryAvatar(ownProfile?.avatarUrl, "Añadir historia", isOwn = true)
                    Text("Añadir", color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
        items(otherStories, key = { it.first().profileClientId }) { group ->
            val author = group.first()
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onOpenStory(group) }) {
                StoryAvatar(author.avatarUrl, author.displayName, isOwn = false)
                Text("@${author.username}", color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun StoryAvatar(imageUrl: String?, name: String, isOwn: Boolean) {
    Box(
        modifier = Modifier.size(62.dp).clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFFFF426F), Color(0xFFFFB347))))
            .padding(2.dp).clip(CircleShape).background(Color(0xFF171920)),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(imageUrl, contentDescription = "Foto de $name", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.84f), modifier = Modifier.size(30.dp))
        }
        if (isOwn) {
            Surface(shape = CircleShape, color = Color(0xFFFF426F), modifier = Modifier.align(Alignment.BottomEnd).size(21.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, contentDescription = "Publicar historia", tint = Color.White, modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

@Composable
private fun StoryComposerDialog(
    imageUri: Uri,
    caption: String,
    message: String?,
    saving: Boolean,
    onCaptionChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onPublish: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp).padding(18.dp), shape = RoundedCornerShape(20.dp), color = Color(0xFF171920), contentColor = Color.White) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Nueva historia", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Vista previa de la historia",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                OutlinedTextField(
                    value = caption,
                    onValueChange = onCaptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Añade un comentario (opcional)") },
                    supportingText = { Text("${caption.length}/240 · se eliminará en 24 horas") },
                    maxLines = 3
                )
                message?.let { Text(it, color = Color(0xFFFFA0A0), style = MaterialTheme.typography.bodySmall) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, enabled = !saving, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(onClick = onPublish, enabled = !saving, modifier = Modifier.weight(1f)) {
                        if (saving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (saving) "Publicando…" else "Publicar")
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryViewerDialog(
    stories: List<GuestStory>,
    onOpenProfile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var index by remember(stories) { mutableIntStateOf(0) }
    val story = stories.getOrNull(index) ?: return
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp).aspectRatio(0.58f).padding(12.dp), shape = RoundedCornerShape(20.dp), color = Color(0xFF171920), contentColor = Color.White) {
            Box {
                AsyncImage(
                    model = story.mediaUrl,
                    contentDescription = "Historia de @${story.username}${if (story.mediaType == "image/gif") ", GIF animado" else ""}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.72f)))).padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StoryAvatar(story.avatarUrl, story.displayName, isOwn = false)
                        Column(modifier = Modifier.weight(1f).padding(start = 10.dp).clickable { onOpenProfile(story.username) }) {
                            Text(story.displayName, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("@${story.username}", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Cerrar historia", tint = Color.White) }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (story.caption.isNotBlank()) Text(story.caption, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { if (index + 1 < stories.size) index++ else onDismiss() }) {
                                Text(if (index + 1 < stories.size) "Siguiente" else "Cerrar", color = Color.White)
                            }
                        }
                    }
                }
            }
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
private fun ShortsInteractionAction(
    icon: ImageVector,
    label: String,
    count: Int,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(50.dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.48f),
            contentColor = tint
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(28.dp))
            }
        }
        Text(shortInteractionCount(count), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ShortsCommentsDialog(
    comments: List<ShortsComment>,
    totalCount: Int,
    draft: String,
    sending: Boolean,
    message: String?,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp).heightIn(max = 560.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF17191E),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Comentarios · ${shortInteractionCount(totalCount)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (message != null) {
                        Text(message, color = Color(0xFFFFC1C1), style = MaterialTheme.typography.bodySmall)
                    }
                    if (comments.isEmpty()) {
                        Text("Aún no hay comentarios. ¡Sé el primero!", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f, fill = false).heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(count = comments.size) { index ->
                                val comment = comments[index]
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable(enabled = comment.username != null) {
                                        comment.username?.let(onOpenUserProfile)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.08f)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                                        Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFF30313A)), contentAlignment = Alignment.Center) {
                                            if (!comment.avatarUrl.isNullOrBlank()) {
                                                AsyncImage(comment.avatarUrl, contentDescription = "Foto de ${comment.displayName ?: "usuario"}", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                            } else {
                                                Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.82f), modifier = Modifier.size(22.dp))
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(comment.displayName ?: "Invitado", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                            comment.username?.let { Text("@$it", color = Color(0xFFFF8BA0), style = MaterialTheme.typography.labelSmall) }
                                            Text(comment.body, color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = onDraftChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Escribe un comentario") },
                        supportingText = { Text("${draft.length}/500") },
                        maxLines = 3
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cerrar") }
                        Button(onClick = onSend, enabled = draft.isNotBlank() && !sending, modifier = Modifier.weight(1f)) {
                            if (sending) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(if (sending) "Enviando" else "Publicar")
                        }
                    }
                }
            }
        }
    }
}

private fun shortInteractionCount(count: Int): String = when {
    count < 1_000 -> count.coerceAtLeast(0).toString()
    count < 10_000 -> "${count / 1_000}.${(count % 1_000) / 100} mil"
    count < 1_000_000 -> "${count / 1_000} mil"
    else -> "${count / 1_000_000} M"
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
        Text("Shorts usa enlaces directos del catálogo para crear fragmentos de 1:30 a 2:30 minutos. Los enlaces iframe no se incluyen.",
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
