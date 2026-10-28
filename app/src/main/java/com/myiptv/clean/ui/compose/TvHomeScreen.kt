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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.myiptv.clean.data.repository.TmdbRepository
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

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
    val mediaRows = remember(viewModel.trending, viewModel.recommended) {
        homeGridCatalog(viewModel.trending, viewModel.recommended, MediaItem::id).chunked(4)
    }
    val channelRows = remember(viewModel.liveChannels) { viewModel.liveChannels.take(8).chunked(4) }
    var slide by rememberSaveable { mutableStateOf(0) }
    var primaryHeroFocused by remember { mutableStateOf(false) }
    var secondaryHeroFocused by remember { mutableStateOf(false) }
    val index = if (featured.isEmpty()) 0 else slide.coerceAtLeast(0) % featured.size

    // Do not replace the title while the viewer is focused on it with the remote.
    LaunchedEffect(featured.map(MediaItem::id), primaryHeroFocused, secondaryHeroFocused) {
        if (!primaryHeroFocused && !secondaryHeroFocused && featured.size > 1) {
            while (true) {
                delay(5_000)
                slide = nextCarouselIndex(slide, featured.size)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(end = 24.dp)) {
        val heroHeight = (maxWidth * 0.30f).coerceIn(150.dp, 310.dp)
        val tileHeight = (maxWidth * 0.145f).coerceIn(82.dp, 150.dp)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item(key = "featured") {
                Row(modifier = Modifier.fillMaxWidth().height(heroHeight), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    val first = featured.getOrNull(index)
                    if (first != null) {
                        TvMediaArtwork(
                            item = first,
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            featured = true,
                            activeSlide = index,
                            slideCount = featured.size,
                            onFocus = { primaryHeroFocused = it },
                            onClick = { onOpenMedia(first.id) }
                        )
                    } else {
                        TvCatalogPlaceholder(
                            loading = viewModel.isLoading,
                            configured = TmdbRepository.isConfigured,
                            status = viewModel.statusMessage.substringBefore(" · "),
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            onClick = onOpenSettings
                        )
                    }
                    val second = if (featured.size > 1) featured[(index + 1) % featured.size] else null
                    if (second != null) {
                        TvMediaArtwork(
                            item = second,
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            featured = true,
                            onFocus = { secondaryHeroFocused = it },
                            onClick = { onOpenMedia(second.id) }
                        )
                    } else {
                        TvLiveSpotlight(
                            channelCount = viewModel.liveChannels.size,
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            onClick = onOpenChannels
                        )
                    }
                }
            }

            if (mediaRows.isNotEmpty()) {
                item(key = "media-heading") {
                    TvShelfHeading("PELÍCULAS Y SERIES", "Ver películas", onOpenMovies, "Ver series", onOpenSeries)
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
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            item(key = "channels-heading") {
                TvShelfHeading("TV EN DIRECTO", "Ver canales", onOpenChannels)
            }
            if (channelRows.isEmpty()) {
                item(key = "channels-empty") {
                    Text(
                        text = viewModel.statusMessage.substringAfter(" · ", "Cargando fuentes de TV…"),
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                itemsIndexed(channelRows, key = { _, row -> "channels-${row.first().id}" }) { _, row ->
                    Row(modifier = Modifier.fillMaxWidth().height(tileHeight), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        row.forEach { channel ->
                            key(channel.id) {
                                TvChannelArtwork(
                                    channel = channel,
                                    modifier = Modifier.weight(1f).fillMaxSize(),
                                    onClick = { onOpenChannel(channel.id) }
                                )
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
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
        imageUrl = item.backdropUrl ?: item.posterUrl,
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

/** Shared artwork treatment for real titles and the explicitly labelled design preview. */
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
    progress: Float? = null,
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
            if (progress != null) {
                val amount = progress.coerceIn(0f, 1f)
                Box(
                    Modifier.fillMaxWidth().padding(top = 5.dp).height(3.dp)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                        .semantics { contentDescription = "Progreso de ejemplo: ${(amount * 100).roundToInt()} %" }
                ) {
                    Box(Modifier.fillMaxWidth(amount).height(3.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                }
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
                    configured -> "Catálogo no disponible"
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
private fun TvChannelArtwork(channel: LiveChannel, modifier: Modifier, onClick: () -> Unit) {
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
    action: String,
    onAction: () -> Unit,
    secondAction: String? = null,
    onSecondAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, modifier = Modifier.weight(1f), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, maxLines = 1)
        TvShelfAction(action, onAction)
        if (secondAction != null && onSecondAction != null) TvShelfAction(secondAction, onSecondAction)
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
