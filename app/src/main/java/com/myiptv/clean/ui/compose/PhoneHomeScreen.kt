package com.myiptv.clean.ui.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import com.myiptv.clean.CleanIPTVApp
import com.myiptv.clean.ads.AdsRules
import com.myiptv.clean.ads.SamplayNativeAd
import com.myiptv.clean.data.repository.MediaPlaylist
import com.myiptv.clean.data.repository.TmdbRepository
import com.myiptv.clean.data.repository.mergeMediaCatalog
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal enum class PhoneHomeCategory { RECOMMENDED, MOVIES, SERIES }
internal enum class PhoneHomeShelf { WEEKLY_TRENDS, DOCUMENTARIES }

/** Phone-only home, backed by the same M3U, TMDb, favorites and channels as the TV home. */
@Composable
internal fun PhoneHomeScreen(
    viewModel: HomeViewModel,
    playlist: MediaPlaylist,
    onOpenChannels: () -> Unit,
    onOpenMovies: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenMedia: (String) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    category: PhoneHomeCategory = PhoneHomeCategory.RECOMMENDED,
    onOpenWeeklyTrends: () -> Unit = {},
    onOpenDocumentaries: () -> Unit = {},
) {
    val context = LocalContext.current
    val available = remember(playlist.items, viewModel.trending, viewModel.recommended) {
        mergeMediaCatalog(playlist.items, viewModel.trending + viewModel.recommended)
    }
    val allMovies = remember(available) { available.filter { it.kind == MediaKind.MOVIE } }
    val allSeries = remember(available) { available.filter { it.kind == MediaKind.SERIES } }
    val movies = if (category == PhoneHomeCategory.SERIES) emptyList() else allMovies
    val series = if (category == PhoneHomeCategory.MOVIES) emptyList() else allSeries
    val preferred = when (category) {
        PhoneHomeCategory.MOVIES -> allMovies.firstOrNull()
        PhoneHomeCategory.SERIES -> allSeries.firstOrNull()
        PhoneHomeCategory.RECOMMENDED -> if (playlist.items.isNotEmpty()) available.firstOrNull() else viewModel.featured
    }
    val featured = remember(preferred, movies, series, category) {
        if (category == PhoneHomeCategory.RECOMMENDED) featuredCatalog(preferred, movies, series, MediaItem::id)
        else if (category == PhoneHomeCategory.MOVIES) featuredCatalog(preferred, movies, emptyList(), MediaItem::id)
        else featuredCatalog(preferred, emptyList(), series, MediaItem::id)
    }
    val isInCategory: (MediaItem) -> Boolean = { item ->
        when (category) {
            PhoneHomeCategory.RECOMMENDED -> true
            PhoneHomeCategory.MOVIES -> item.kind == MediaKind.MOVIE
            PhoneHomeCategory.SERIES -> item.kind == MediaKind.SERIES
        }
    }
    val trends = remember(viewModel.weeklyTrends, category) { viewModel.weeklyTrends.filter(isInCategory) }
    val documentaries = remember(viewModel.documentaries, category) { viewModel.documentaries.filter(isInCategory) }

    LaunchedEffect(viewModel) { viewModel.loadTvShelves() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "featured") {
            if (featured.isEmpty()) {
                PhoneEmptyFeatured(
                    loading = !playlist.loaded || viewModel.isLoading,
                    status = if (playlist.items.isEmpty()) playlist.statusMessage else viewModel.statusMessage,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            } else {
                PhoneFeaturedCarousel(featured, onOpenMedia, Modifier.padding(horizontal = 14.dp))
            }
        }
        if (movies.isNotEmpty()) {
            item(key = "movies") {
                PhoneMediaShelf(
                    if (category == PhoneHomeCategory.RECOMMENDED) "Películas Populares" else "Películas destacadas",
                    movies, favorites, onToggleFavorite, onOpenMedia, onOpenMovies
                )
            }
        }
        if (category == PhoneHomeCategory.RECOMMENDED && (movies.isNotEmpty() || series.isNotEmpty())) {
            item(key = "samplay-native-ad") {
                if (AdsRules.canPlaceNativeOnHome(context.isTelevisionDevice(), isTvLayout = false)) {
                    Box(Modifier.padding(horizontal = 14.dp)) {
                        SamplayNativeAd((context.applicationContext as CleanIPTVApp).ads)
                    }
                }
            }
        }
        if (series.isNotEmpty()) {
            item(key = "series") {
                PhoneMediaShelf(
                    if (category == PhoneHomeCategory.RECOMMENDED) "Series Populares" else "Series destacadas",
                    series, favorites, onToggleFavorite, onOpenMedia, onOpenSeries
                )
            }
        }
        if (trends.isNotEmpty()) {
            item(key = "weekly-trends") {
                PhoneMediaShelf("Tendencias esta semana", trends, favorites, onToggleFavorite, onOpenMedia, onOpenWeeklyTrends)
            }
        }
        if (documentaries.isNotEmpty()) {
            item(key = "documentaries") {
                PhoneMediaShelf("Documentales", documentaries, favorites, onToggleFavorite, onOpenMedia, onOpenDocumentaries)
            }
        }
        if (category == PhoneHomeCategory.RECOMMENDED) {
            item(key = "live-channels") {
                Column {
                    PhoneShelfHeading("TV en directo", onOpenChannels)
                    if (viewModel.liveChannels.isEmpty()) {
                        Text(
                            viewModel.statusMessage.substringAfter(" · ", "Cargando fuentes de TV…"),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(viewModel.liveChannels.take(20), key = LiveChannel::id) { channel ->
                                TvChannelArtwork(
                                    channel = channel,
                                    modifier = Modifier.width(165.dp).height(98.dp).clip(RoundedCornerShape(7.dp)),
                                    onClick = { onOpenChannel(channel.id) }
                                )
                            }
                        }
                    }
                }
            }
            item(key = "continue") {
                Column(Modifier.padding(horizontal = 14.dp)) {
                    Text("Continuar viendo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "El progreso de reproducción todavía no se guarda en este dispositivo.",
                        modifier = Modifier.padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhoneFeaturedCarousel(items: List<MediaItem>, onOpenMedia: (String) -> Unit, modifier: Modifier = Modifier) {
    val pager = rememberPagerState(pageCount = { items.size })
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val heroInteraction = remember { MutableInteractionSource() }
    val isHeroPressed by heroInteraction.collectIsPressedAsState()
    val currentHeroPressed by rememberUpdatedState(isHeroPressed)
    LaunchedEffect(pager, items.map(MediaItem::id), lifecycleOwner) {
        if (items.size > 1) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    delay(5_000)
                    if (!pager.isScrollInProgress && !currentHeroPressed) {
                        // A user swipe may cancel this animation; keep the timer alive afterwards.
                        launch { pager.animateScrollToPage(nextCarouselIndex(pager.currentPage, items.size)) }
                    }
                }
            }
        }
    }

    Box(modifier.fillMaxWidth().aspectRatio(1.85f).clip(RoundedCornerShape(8.dp))) {
        HorizontalPager(state = pager, key = { items[it].id }, modifier = Modifier.fillMaxSize()) { page ->
            val item = items[page]
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Brush.linearGradient(listOf(Color(item.colorA), Color(item.colorB))))
                    .clickable(
                        interactionSource = heroInteraction,
                        indication = LocalIndication.current,
                        onClickLabel = "Abrir ${item.title}"
                    ) { onOpenMedia(item.id) }
            ) {
                val artwork = item.backdropUrl?.takeIf(String::isNotBlank) ?: item.posterUrl?.takeIf(String::isNotBlank)
                if (artwork != null) {
                    AsyncImage(model = artwork, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.86f)))))
                Column(Modifier.align(Alignment.BottomStart).padding(start = 13.dp, end = 13.dp, bottom = 27.dp)) {
                    Text(item.title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(if (item.kind == MediaKind.MOVIE) "Película" else "Serie", item.year.takeIf { it > 0 }?.toString())
                            .filterNotNull().joinToString(" · "),
                        color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp, maxLines = 1
                    )
                }
            }
        }
        if (items.size > 1) {
            Row(modifier = Modifier.align(Alignment.BottomCenter), verticalAlignment = Alignment.CenterVertically) {
                items.forEachIndexed { index, item ->
                    Box(
                        modifier = Modifier.size(width = 22.dp, height = 25.dp)
                            .semantics { contentDescription = "Destacado ${index + 1}: ${item.title}" }
                            .clickable { scope.launch { pager.animateScrollToPage(index) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.width(if (pager.currentPage == index) 13.dp else 6.dp).height(6.dp)
                                .background(if (pager.currentPage == index) Color.White else Color.White.copy(alpha = 0.45f), CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneEmptyFeatured(loading: Boolean, status: String, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().aspectRatio(1.85f).clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF17191F)),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            SamplayLoading(Modifier.fillMaxSize(), "Cargando recomendaciones…", lionSize = 112.dp)
        } else {
            Column(
                modifier = Modifier.fillMaxSize().clickable(onClick = onOpenSettings).padding(16.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Movie, contentDescription = null, tint = PhoneChannelAccent, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(8.dp))
                Text("Explora nuevas historias", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Text(
                    status.ifBlank { if (TmdbRepository.isConfigured) "Catálogo no disponible" else "Revisa tus fuentes en Ajustes" },
                    color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun PhoneCollectionHomeScreen(
    collection: MediaCollection,
    items: List<MediaItem>,
    loading: Boolean,
    status: String,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onOpenMedia: (String) -> Unit,
    onRetry: (() -> Unit)? = null,
) {
    val isKids = collection == MediaCollection.KIDS
    val title = if (isKids) "Kids" else "Anime"
    val featured = remember(items) { items.take(4) }
    val movies = remember(items) { items.filter { it.kind == MediaKind.MOVIE } }
    val series = remember(items) { items.filter { it.kind == MediaKind.SERIES } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(key = "collection-featured") {
            if (featured.isNotEmpty()) {
                PhoneFeaturedCarousel(featured, onOpenMedia, Modifier.padding(horizontal = 14.dp))
            } else if (loading) {
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp).aspectRatio(1.85f)
                        .clip(RoundedCornerShape(8.dp)).background(Color(0xFF17191F)),
                ) {
                    SamplayLoading(Modifier.fillMaxSize(), "Cargando $title…", lionSize = 112.dp)
                }
            } else {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(status, modifier = Modifier.padding(top = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (onRetry != null) {
                        Text(
                            "Reintentar",
                            modifier = Modifier.padding(top = 10.dp).clickable(onClick = onRetry),
                            color = PhoneChannelAccent,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        if (movies.isNotEmpty()) {
            item(key = "collection-movies") {
                PhoneMediaShelf(if (isKids) "Películas para niños" else "Películas de anime", movies, favorites, onToggleFavorite, onOpenMedia)
            }
        }
        if (series.isNotEmpty()) {
            item(key = "collection-series") {
                PhoneMediaShelf(if (isKids) "Series para niños" else "Series de anime", series, favorites, onToggleFavorite, onOpenMedia)
            }
        }
        if (items.isNotEmpty()) {
            item(key = "collection-all") {
                PhoneMediaShelf(if (isKids) "Más contenido infantil" else "Más anime", items, favorites, onToggleFavorite, onOpenMedia)
            }
        }
    }
}

@Composable
internal fun PhoneCuratedCatalogScreen(
    title: String,
    items: List<MediaItem>,
    loading: Boolean,
    status: String,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onOpenMedia: (String) -> Unit,
    onRetry: (() -> Unit)? = null,
) {
    PhonePosterGrid(
        items = items,
        favorites = favorites,
        onOpenMedia = onOpenMedia,
        onToggleFavorite = onToggleFavorite,
        header = { TvPageHeader(title) },
        emptyContent = {
            if (loading) {
                SamplayLoadingPane(Modifier.fillMaxWidth(), "Cargando $title…")
            } else {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (onRetry != null) {
                        Text("Reintentar", modifier = Modifier.padding(top = 10.dp).clickable(onClick = onRetry), color = PhoneChannelAccent)
                    }
                }
            }
        },
    )
}

@Composable
internal fun PhoneHomeShelfCatalogScreen(
    shelf: PhoneHomeShelf,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onOpenMedia: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    LaunchedEffect(viewModel) { viewModel.loadTvShelves() }
    val title = if (shelf == PhoneHomeShelf.WEEKLY_TRENDS) "Tendencias esta semana" else "Documentales"
    val items = if (shelf == PhoneHomeShelf.WEEKLY_TRENDS) viewModel.weeklyTrends else viewModel.documentaries
    PhoneCuratedCatalogScreen(
        title = title,
        items = items,
        loading = viewModel.isLoadingShelves,
        status = viewModel.shelvesStatus,
        favorites = favorites,
        onToggleFavorite = onToggleFavorite,
        onOpenMedia = onOpenMedia,
        onRetry = if (TmdbRepository.isConfigured) viewModel::loadTvShelves else null,
    )
}

@Composable
private fun PhoneMediaShelf(
    title: String,
    titles: List<MediaItem>,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onOpenMedia: (String) -> Unit,
    onOpenAll: (() -> Unit)? = null
) {
    val unique = remember(titles) { titles.distinctBy(MediaItem::id) }
    Column {
        PhoneShelfHeading(title, onOpenAll)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val posterWidth = (maxWidth * 0.31f).coerceIn(88.dp, 160.dp)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(unique, key = MediaItem::id) { item ->
                    PhonePosterCard(
                        item = item,
                        isFavorite = item.id in favorites,
                        onOpen = { onOpenMedia(item.id) },
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        modifier = Modifier.width(posterWidth)
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneShelfHeading(title: String, onOpenAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f).then(if (onOpenAll != null) Modifier.clickable(onClick = { onOpenAll.invoke() }) else Modifier),
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onOpenAll != null) {
            Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClickLabel = "Ver todo: $title", onClick = onOpenAll), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = "Ver todo: $title", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** Shared portrait poster for the phone home and phone catalog grids. */
@Composable
internal fun PhonePosterCard(
    item: MediaItem,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.clickable(onClickLabel = "Abrir ${item.title}", onClick = onOpen)) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(7.dp))) {
            PosterArtwork(item, modifier = Modifier.fillMaxSize(), showTitle = false)
            val action = if (isFavorite) "Quitar ${item.title} de mi lista" else "Agregar ${item.title} a mi lista"
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(36.dp).clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.58f)).clickable(onClickLabel = action, onClick = onToggleFavorite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = action,
                    tint = if (isFavorite) PhoneChannelAccent else Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        Text(
            item.title,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
