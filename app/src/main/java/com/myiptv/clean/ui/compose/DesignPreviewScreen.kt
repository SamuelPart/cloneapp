package com.myiptv.clean.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** A deliberately separate, opt-in preview; none of these titles enter the real repositories. */
@Composable
internal fun DesignPreviewScreen(onClose: () -> Unit, onOpenTitle: (String, Float?) -> Unit) {
    var slide by rememberSaveable { mutableStateOf(0) }
    var heroFocused by remember { mutableStateOf(false) }
    val carousel = DesignCatalog.carousel
    val index = slide.coerceAtLeast(0) % carousel.size
    val grid = DesignCatalog.gridRows(index)

    LaunchedEffect(heroFocused) {
        if (!heroFocused && carousel.size > 1) {
            while (true) {
                delay(5_000)
                slide = nextCarouselIndex(slide, carousel.size)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        val compact = maxWidth < 600.dp
        val heroHeight = if (compact) (maxWidth * 0.54f).coerceIn(145.dp, 230.dp) else (maxWidth * 0.30f).coerceIn(150.dp, 310.dp)
        val tileHeight = if (compact) (maxWidth * 0.28f).coerceIn(100.dp, 155.dp) else (maxWidth * 0.145f).coerceIn(82.dp, 150.dp)
        val columns = if (compact) 2 else 4
        val rows = if (compact) grid.drop(1).flatMap { it.chunked(2) } else grid.drop(1)

        Column(modifier = Modifier.fillMaxSize()) {
            DesignNotice(onClose)
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item(key = "featured") {
                    if (compact) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            DesignTitleCard(
                                item = grid[0][0],
                                modifier = Modifier.fillMaxWidth().height(heroHeight),
                                featured = true,
                                activeSlide = index,
                                slideCount = carousel.size,
                                onFocus = { heroFocused = it },
                                onOpenTitle = onOpenTitle
                            )
                            DesignTitleCard(
                                item = grid[0][1],
                                modifier = Modifier.fillMaxWidth().height(heroHeight),
                                featured = true,
                                onOpenTitle = onOpenTitle
                            )
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth().height(heroHeight), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            DesignTitleCard(
                                item = grid[0][0],
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                featured = true,
                                activeSlide = index,
                                slideCount = carousel.size,
                                onFocus = { heroFocused = it },
                                onOpenTitle = onOpenTitle
                            )
                            DesignTitleCard(
                                item = grid[0][1],
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                featured = true,
                                onOpenTitle = onOpenTitle
                            )
                        }
                    }
                }
                itemsIndexed(rows, key = { rowIndex, _ -> "grid-$rowIndex" }) { _, row ->
                    Row(modifier = Modifier.fillMaxWidth().height(tileHeight), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        row.forEach { item ->
                            key(item.id) {
                                DesignTitleCard(
                                    item = item,
                                    modifier = Modifier.weight(1f).fillMaxSize(),
                                    featured = false,
                                    onOpenTitle = onOpenTitle
                                )
                            }
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                itemsIndexed(DesignCatalog.rows, key = { _, row -> "section-${row.title}" }) { _, row ->
                    Column(modifier = Modifier.padding(top = 15.dp)) {
                        Text(row.title, modifier = Modifier.padding(bottom = 8.dp), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            itemsIndexed(row.items, key = { itemIndex, entry -> "${entry.title.id}-$itemIndex" }) { _, entry ->
                                DesignTitleCard(
                                    item = entry.title,
                                    progress = entry.progress,
                                    modifier = Modifier.width(if (compact) 175.dp else 195.dp).height(if (compact) 100.dp else 110.dp),
                                    featured = false,
                                    onOpenTitle = onOpenTitle
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesignNotice(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            .background(Color(0xFF16242A), RoundedCornerShape(6.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("VISTA DE DISEÑO", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp)
            Text("Información ficticia: no hay vídeos ni progreso real.", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
        DesignAction("Salir", onClick = onClose)
    }
}

@Composable
private fun DesignTitleCard(
    item: DesignTitle,
    modifier: Modifier,
    featured: Boolean,
    progress: Float? = null,
    activeSlide: Int = 0,
    slideCount: Int = 0,
    onFocus: (Boolean) -> Unit = {},
    onOpenTitle: (String, Float?) -> Unit
) {
    TvArtworkCard(
        title = item.title,
        subtitle = "${item.kind} · ${item.year}",
        imageUrl = item.imageUrl(if (featured) 1100 else 700),
        modifier = modifier,
        featured = featured,
        activeSlide = activeSlide,
        slideCount = slideCount,
        progress = progress,
        onFocus = onFocus,
        onClick = { onOpenTitle(item.id, progress) }
    )
}

@Composable
internal fun DesignDetailScreen(
    id: String,
    isTv: Boolean,
    progress: Float?,
    onBack: () -> Unit,
    onOpenTitle: (String) -> Unit
) {
    val item = DesignCatalog.titles[id]
    if (item == null) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            DesignAction("Volver", onClick = onBack)
            EmptyState(Icons.Filled.Movie, "Ejemplo no disponible", "Este título no está en el catálogo de diseño.")
        }
        return
    }
    val focus = remember(item.id) { FocusRequester() }
    LaunchedEffect(item.id) {
        delay(180)
        runCatching { focus.requestFocus() }
    }
    val heroHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.72f).coerceIn(330.dp, 500.dp)
    val related = DesignCatalog.titles.values.filter { it.id != item.id }.take(5)
    val sidePadding = if (isTv) 36.dp else 18.dp

    LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(bottom = 28.dp)) {
        item(key = "detail") {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = heroHeight).clipToBounds()
                    .background(Brush.linearGradient(listOf(Color(0xFF15303E), Color(0xFF101014), Color.Black)))
            ) {
                DetailBackdropImage(imageUrl = item.imageUrl(), modifier = Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.96f), Color.Black.copy(alpha = 0.65f), Color.Black.copy(alpha = 0.08f)))))
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent, Color.Black))))
                Column(modifier = Modifier.fillMaxWidth().padding(start = sidePadding, end = sidePadding, top = 16.dp, bottom = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DesignAction("‹  Volver", modifier = Modifier.focusRequester(focus), onClick = onBack)
                        Text("VISTA DE DISEÑO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(15.dp))
                    Column(modifier = Modifier.fillMaxWidth(if (isTv) 0.72f else 1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.kind.uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Text(
                            item.title.uppercase(),
                            color = Color.White,
                            fontFamily = FontFamily.Serif,
                            fontSize = if (isTv) 34.sp else 28.sp,
                            lineHeight = if (isTv) 36.sp else 31.sp,
                            letterSpacing = 1.5.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${item.match}% para ti (ejemplo)", color = Color(0xFF73E3C5), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text(item.year.toString(), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                            Text(item.ageRating, modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(3.dp)).padding(horizontal = 5.dp), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                            Text(item.duration, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                        }
                        Text(item.synopsis, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        Text("Géneros: ${item.genres.joinToString(", ")}", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                        if (progress != null) DesignProgress(progress)
                        Text(
                            "Contenido de muestra: no hay vídeo ni tráiler para reproducir.",
                            modifier = Modifier.padding(top = 4.dp),
                            color = Color.White.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        if (related.isNotEmpty()) {
            item(key = "related") {
                Column(modifier = Modifier.fillMaxWidth().padding(start = sidePadding, top = 16.dp)) {
                    Text("Más títulos de ejemplo", modifier = Modifier.padding(bottom = 8.dp), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp), contentPadding = PaddingValues(end = sidePadding)) {
                        items(related, key = DesignTitle::id) { title ->
                            DesignTitleCard(
                                item = title,
                                modifier = Modifier.width(195.dp).height(110.dp),
                                featured = false,
                                onOpenTitle = { id, _ -> onOpenTitle(id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesignProgress(progress: Float) {
    val amount = progress.coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Progreso de ejemplo: ${(amount * 100).roundToInt()} %", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
        Box(
            Modifier.width(200.dp).height(4.dp).background(Color.White.copy(alpha = 0.3f), CircleShape)
                .semantics { contentDescription = "Progreso de ejemplo: ${(amount * 100).roundToInt()} %" }
        ) {
            Box(Modifier.fillMaxWidth(amount).height(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        }
    }
}

@Composable
private fun DesignAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Text(
        text = label,
        modifier = modifier.onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(50))
            .background(if (focused) Color.White else Color.White.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        color = if (focused) Color.Black else Color.White,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1
    )
}
