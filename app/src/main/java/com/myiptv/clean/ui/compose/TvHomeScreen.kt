package com.myiptv.clean.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.myiptv.clean.data.repository.TmdbRepository
import kotlinx.coroutines.delay

@Composable
fun TvHomeScreen(
    viewModel: HomeViewModel,
    onOpenChannels: () -> Unit,
    onOpenMovies: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenMedia: (String) -> Unit
) {
    val featured = remember(viewModel.featured, viewModel.trending, viewModel.recommended) {
        featuredCatalog(viewModel.featured, viewModel.trending, viewModel.recommended, MediaItem::id)
    }
    val gridTitles = remember(viewModel.trending, viewModel.recommended) {
        homeGridCatalog(viewModel.trending, viewModel.recommended, MediaItem::id)
    }
    var slide by rememberSaveable { mutableStateOf(0) }
    var primaryHeroFocused by remember { mutableStateOf(false) }
    var secondaryHeroFocused by remember { mutableStateOf(false) }
    val index = if (featured.isEmpty()) 0 else slide.coerceAtLeast(0) % featured.size
    val first = featured.getOrNull(index)
    val second = if (featured.size > 1) featured[(index + 1) % featured.size] else null

    LaunchedEffect(viewModel) { viewModel.loadTvShelves() }

    // Do not replace either featured title while it is focused with the remote.
    LaunchedEffect(featured.map(MediaItem::id), primaryHeroFocused, secondaryHeroFocused) {
        if (!primaryHeroFocused && !secondaryHeroFocused && featured.size > 1) {
            while (true) {
                delay(5_000)
                slide = nextCarouselIndex(slide, featured.size)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(end = 24.dp)) {
        val compact = maxWidth < 600.dp
        val sideBySideHeroes = maxWidth >= 360.dp
        val columns = tvCatalogColumnCount(maxWidth.value)
        val naturalHeroHeight = if (sideBySideHeroes) (maxWidth * 0.30f).coerceIn(110.dp, 310.dp) else (maxWidth * 0.54f).coerceIn(120.dp, 230.dp)
        val heroHeight = naturalHeroHeight.coerceAtMost((maxHeight * 0.55f).coerceAtLeast(100.dp))
        val tileHeight = ((maxWidth - 5.dp * (columns - 1)) / columns / 1.8f).coerceIn(82.dp, 150.dp)
        val mediaRows = remember(gridTitles, columns) { gridTitles.chunked(columns) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item(key = "featured") {
                if (!sideBySideHeroes) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        TvPrimaryFeatured(
                            item = first,
                            modifier = Modifier.fillMaxWidth().height(heroHeight),
                            index = index,
                            count = featured.size,
                            loading = viewModel.isLoading,
                            status = viewModel.statusMessage.substringBefore(" · "),
                            onFocus = { primaryHeroFocused = it },
                            onOpenMedia = onOpenMedia,
                            onOpenSettings = onOpenSettings
                        )
                        TvSecondaryFeatured(
                            item = second,
                            modifier = Modifier.fillMaxWidth().height(heroHeight),
                            channelCount = viewModel.liveChannels.size,
                            onFocus = { secondaryHeroFocused = it },
                            onOpenMedia = onOpenMedia,
                            onOpenChannels = onOpenChannels
                        )
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth().height(heroHeight), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        TvPrimaryFeatured(
                            item = first,
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            index = index,
                            count = featured.size,
                            loading = viewModel.isLoading,
                            status = viewModel.statusMessage.substringBefore(" · "),
                            onFocus = { primaryHeroFocused = it },
                            onOpenMedia = onOpenMedia,
                            onOpenSettings = onOpenSettings
                        )
                        TvSecondaryFeatured(
                            item = second,
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            channelCount = viewModel.liveChannels.size,
                            onFocus = { secondaryHeroFocused = it },
                            onOpenMedia = onOpenMedia,
                            onOpenChannels = onOpenChannels
                        )
                    }
                }
            }

            itemsIndexed(mediaRows, key = { _, row -> "media-${row.first().id}" }) { _, row ->
                Row(modifier = Modifier.fillMaxWidth().height(tileHeight), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    row.forEach { item ->
                        key(item.id) {
                            TvMediaArtwork(
                                item = item,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                featured = false,
                                onClick = { onOpenMedia(item.id) }
                            )
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            item(key = "continue") {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 15.dp)) {
                    TvShelfHeading("Continuar viendo")
                    Text(
                        "El progreso de reproducción todavía no se guarda en este dispositivo.",
                        modifier = Modifier.padding(bottom = 5.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (viewModel.weeklyTrends.isNotEmpty()) {
                item(key = "weekly-trends") {
                    TvMediaShelf("Tendencias esta semana", viewModel.weeklyTrends, compact, onOpenMedia)
                }
            }
            if (viewModel.trending.isNotEmpty()) {
                item(key = "popular-movies") {
                    TvMediaShelf("Películas populares", viewModel.trending, compact, onOpenMedia, "Ver películas", onOpenMovies)
                }
            }
            if (viewModel.recommended.isNotEmpty()) {
                item(key = "popular-series") {
                    TvMediaShelf("Series populares", viewModel.recommended, compact, onOpenMedia, "Ver series", onOpenSeries)
                }
            }
            if (viewModel.documentaries.isNotEmpty()) {
                item(key = "documentaries") {
                    TvMediaShelf("Documentales", viewModel.documentaries, compact, onOpenMedia)
                }
            }
            item(key = "live-channels") {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 15.dp)) {
                    TvShelfHeading("TV en directo", "Ver canales", onOpenChannels)
                    if (viewModel.liveChannels.isEmpty()) {
                        Text(
                            text = viewModel.statusMessage.substringAfter(" · ", "Cargando fuentes de TV…"),
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            items(viewModel.liveChannels.take(20), key = LiveChannel::id) { channel ->
                                TvChannelArtwork(
                                    channel = channel,
                                    modifier = Modifier.width(if (compact) 175.dp else 195.dp).height(if (compact) 100.dp else 110.dp),
                                    onClick = { onOpenChannel(channel.id) }
                                )
                            }
                        }
                    }
                }
            }
            item(key = "status") {
                Text(
                    text = viewModel.statusMessage,
                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun TvPrimaryFeatured(
    item: MediaItem?,
    modifier: Modifier,
    index: Int,
    count: Int,
    loading: Boolean,
    status: String,
    onFocus: (Boolean) -> Unit,
    onOpenMedia: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    if (item != null) {
        TvMediaArtwork(item, modifier, featured = true, activeSlide = index, slideCount = count, onFocus = onFocus) {
            onOpenMedia(item.id)
        }
    } else {
        TvCatalogPlaceholder(loading, TmdbRepository.isConfigured, status, modifier, onOpenSettings)
    }
}

@Composable
private fun TvSecondaryFeatured(
    item: MediaItem?,
    modifier: Modifier,
    channelCount: Int,
    onFocus: (Boolean) -> Unit,
    onOpenMedia: (String) -> Unit,
    onOpenChannels: () -> Unit
) {
    if (item != null) {
        TvMediaArtwork(item, modifier, featured = true, onFocus = onFocus) { onOpenMedia(item.id) }
    } else {
        TvLiveSpotlight(channelCount, modifier, onOpenChannels)
    }
}

@Composable
private fun TvMediaShelf(
    title: String,
    titles: List<MediaItem>,
    compact: Boolean,
    onOpenMedia: (String) -> Unit,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 15.dp)) {
        TvShelfHeading(title, action, onAction)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            items(titles.distinctBy(MediaItem::id), key = MediaItem::id) { item ->
                TvMediaArtwork(
                    item = item,
                    modifier = Modifier.width(if (compact) 175.dp else 195.dp).height(if (compact) 100.dp else 110.dp),
                    featured = false,
                    onClick = { onOpenMedia(item.id) }
                )
            }
        }
    }
}

@Composable
private fun TvCard(
    modifier: Modifier,
    onClick: () -> Unit,
    onFocus: (Boolean) -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .onFocusChanged {
                focused = it.isFocused
                onFocus(it.isFocused)
            }
            .background(Color(0xFF18181B))
            .clickable(onClick = onClick)
    ) {
        content()
        if (focused) {
            Box(Modifier.matchParentSize().border(3.dp, Color.White))
        }
    }
}

@Composable
internal fun TvMediaArtwork(
    item: MediaItem,
    modifier: Modifier,
    featured: Boolean,
    activeSlide: Int = 0,
    slideCount: Int = 0,
    onFocus: (Boolean) -> Unit = {},
    onClick: () -> Unit
) {
    TvArtworkCard(
        title = item.title,
        subtitle = "${if (item.kind == MediaKind.MOVIE) "Película" else "Serie"} · ${item.year.takeIf { it > 0 } ?: "—"}",
        imageUrl = item.backdropUrl?.takeIf(String::isNotBlank) ?: item.posterUrl,
        colorA = item.colorA,
        colorB = item.colorB,
        modifier = modifier,
        featured = featured,
        activeSlide = activeSlide,
        slideCount = slideCount,
        onFocus = onFocus,
        onClick = onClick
    )
}

/** Artwork treatment used by the real TV catalog and related titles. */
@Composable
internal fun TvArtworkCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    modifier: Modifier,
    featured: Boolean,
    colorA: Long = 0xFF15303EL,
    colorB: Long = 0xFF101014L,
    activeSlide: Int = 0,
    slideCount: Int = 0,
    onFocus: (Boolean) -> Unit = {},
    onClick: () -> Unit
) {
    TvCard(modifier = modifier, onClick = onClick, onFocus = onFocus) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(colorA), Color(colorB)))))
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(model = imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent, Color.Black.copy(alpha = 0.9f)))
            )
        )
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 13.dp, vertical = if (featured) 16.dp else 9.dp),
            horizontalAlignment = if (featured) Alignment.CenterHorizontally else Alignment.Start
        ) {
            Text(
                title.uppercase(),
                color = Color.White,
                fontFamily = if (featured) FontFamily.Serif else null,
                fontWeight = if (featured) FontWeight.Medium else FontWeight.ExtraBold,
                fontSize = if (featured) 25.sp else 13.sp,
                letterSpacing = if (featured) 2.sp else 0.sp,
                textAlign = if (featured) TextAlign.Center else TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = if (featured) 28.sp else 15.sp
            )
            if (!featured) {
                Text(subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (slideCount > 1) {
                Row(modifier = Modifier.padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(slideCount) { dot ->
                        Box(
                            Modifier
                                .width(if (dot == activeSlide) 18.dp else 5.dp)
                                .height(5.dp)
                                .background(if (dot == activeSlide) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.45f), CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvCatalogPlaceholder(
    loading: Boolean,
    configured: Boolean,
    status: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    TvCard(modifier = modifier, onClick = onClick) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF102936), Color(0xFF090B0F), Color.Black))))
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("PELÍCULAS Y SERIES", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(
                when {
                    loading -> "Cargando catálogo"
                    configured -> "Catálogo principal no disponible"
                    else -> "Explora nuevas historias"
                },
                color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold, maxLines = 2
            )
            Text(
                if (loading) "Consultando TMDb…" else status,
                color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text("VER AJUSTES  ›", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TvLiveSpotlight(channelCount: Int, modifier: Modifier, onClick: () -> Unit) {
    TvCard(modifier = modifier, onClick = onClick) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF163338), Color(0xFF101722), Color.Black))))
        Icon(Icons.Filled.LiveTv, contentDescription = null, tint = Color.White.copy(alpha = 0.08f), modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp).size(110.dp))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("EN DIRECTO", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("TV EN VIVO", color = Color.White, fontFamily = FontFamily.Serif, fontSize = 25.sp, letterSpacing = 2.sp)
            Text(if (channelCount > 0) "$channelCount canales en el catálogo" else "Explora los canales de televisión", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}

@Composable
internal fun TvChannelArtwork(channel: LiveChannel, modifier: Modifier, onClick: () -> Unit) {
    TvCard(modifier = modifier, onClick = onClick) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(channel.accent).copy(alpha = 0.55f), Color(0xFF101013), Color.Black))))
        ChannelLogo(channel, Modifier.align(Alignment.Center).size(48.dp))
        Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(9.dp)) {
            Text(channel.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("EN DIRECTO · ${channel.category}", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TvShelfHeading(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, modifier = Modifier.weight(1f), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (action != null && onAction != null) TvShelfAction(action, onAction)
    }
}

@Composable
private fun TvShelfAction(label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Text(
        text = "$label  ›",
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .background(if (focused) Color.White else Color.Transparent, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        color = if (focused) Color.Black else Color.White.copy(alpha = 0.7f),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1
    )
}
