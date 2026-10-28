package com.myiptv.clean.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    isTv: Boolean,
    onOpenChannels: () -> Unit,
    onOpenMovies: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenMedia: (String) -> Unit,
    onOpenPlayer: (String) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val horizontalPadding = if (isTv) 36.dp else 18.dp
    val featured = viewModel.featured.first()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(if (isTv) 28.dp else 23.dp)
    ) {
        FeaturedHero(
            item = featured,
            isTv = isTv,
            onPlay = { onOpenPlayer(featured.id) },
            onMore = { onOpenMedia(featured.id) },
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = if (isTv) 8.dp else 4.dp)
        )

        Column(modifier = Modifier.padding(horizontal = horizontalPadding)) {
            SectionTitle("Explora por categoría")
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DemoCatalog.categories.forEach { category ->
                    CategoryTile(
                        category = category,
                        onClick = {
                            when (category.title) {
                                "Películas" -> onOpenMovies()
                                "Series" -> onOpenSeries()
                                else -> onOpenChannels()
                            }
                        }
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(start = horizontalPadding)) {
            SectionTitle("Ahora en vivo", "Ver todos", onOpenChannels, modifier = Modifier.padding(end = horizontalPadding))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(end = horizontalPadding)) {
                items(viewModel.liveChannels.take(5), key = LiveChannel::id) { channel ->
                    CompactChannelCard(channel = channel, modifier = Modifier.width(if (isTv) 330.dp else 286.dp)) {
                        onOpenChannel(channel.id)
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(start = horizontalPadding)) {
            SectionTitle("Tendencias para ti", "Ver películas", onOpenMovies, modifier = Modifier.padding(end = horizontalPadding))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(end = horizontalPadding)) {
                items(viewModel.trending, key = MediaItem::id) { item ->
                    MediaPosterCard(
                        item = item,
                        modifier = Modifier.width(if (isTv) 190.dp else 148.dp),
                        isFavorite = item.id in favorites,
                        onClick = { onOpenMedia(item.id) },
                        onFavoriteClick = { onToggleFavorite(item.id) }
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(start = horizontalPadding)) {
            SectionTitle("Series que podrías disfrutar", "Explorar series", onOpenSeries, modifier = Modifier.padding(end = horizontalPadding))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(end = horizontalPadding)) {
                items(viewModel.recommended.filter { it.kind == MediaKind.SERIES }, key = MediaItem::id) { item ->
                    MediaPosterCard(
                        item = item,
                        modifier = Modifier.width(if (isTv) 190.dp else 148.dp),
                        isFavorite = item.id in favorites,
                        onClick = { onOpenMedia(item.id) },
                        onFavoriteClick = { onToggleFavorite(item.id) }
                    )
                }
            }
        }

        Text(
            "Catálogo y programación de demostración · Conecta una fuente IPTV para ver contenido real.",
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun FeaturedHero(
    item: MediaItem,
    isTv: Boolean,
    onPlay: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isTv) 330.dp else 400.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(item.colorA), Color(item.colorB), Color(0xFF141327))))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.1f), size.maxDimension * 0.47f, androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.2f))
            drawCircle(Color(item.colorA).copy(alpha = 0.6f), size.maxDimension * 0.34f, androidx.compose.ui.geometry.Offset(size.width * 0.96f, size.height * 0.78f))
            drawCircle(Color.White.copy(alpha = 0.18f), size.minDimension * 0.22f, androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.2f), style = Stroke(width = 2.dp.toPx()))
            for (index in 0..9) {
                drawLine(
                    color = Color.White.copy(alpha = 0.055f),
                    start = androidx.compose.ui.geometry.Offset(size.width * (0.44f + index * 0.07f), 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width * (0.12f + index * 0.07f), size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0.58f),
                    0.62f to Color.Black.copy(alpha = 0.1f),
                    1f to Color.Transparent
                )
            )
        )
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = if (isTv) 36.dp else 23.dp, vertical = if (isTv) 30.dp else 26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD38B), modifier = Modifier.size(17.dp))
                    Text("DESTACADO DE LA SEMANA", color = Color(0xFFFFD38B), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                }
                Text(
                    item.title,
                    color = Color.White,
                    style = if (isTv) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${item.year}   ·   ${item.genres.take(2).joinToString(" / ")}   ·   ★ ${item.rating}   ·   ${item.quality}",
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.synopsis,
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (isTv) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(if (isTv) 0.72f else 1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                    PrimaryButton("Ver ahora", Icons.Filled.Movie, onPlay)
                    TertiaryButton("Detalles", onClick = onMore)
                }
            }
            if (isTv) {
                Spacer(Modifier.width(28.dp))
                Box(modifier = Modifier.width(190.dp).aspectRatio(0.72f)) {
                    PosterArtwork(item, Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun ChannelsScreen(
    isTv: Boolean,
    onOpenChannel: (String) -> Unit,
    viewModel: ChannelsViewModel = viewModel()
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = if (isTv) 36.dp else 18.dp, vertical = 16.dp)) {
        PageHeading(
            eyebrow = "Señal en directo",
            title = "Canales",
            description = "Encuentra algo que ver ahora.",
            trailing = { LiveIndicator() }
        )
        SearchBox(
            value = viewModel.query,
            onValueChange = { viewModel.query = it },
            placeholder = "Buscar canales",
            modifier = Modifier.padding(bottom = 12.dp)
        )
        FilterChips(
            options = DemoCatalog.channelCategories,
            selected = viewModel.selectedCategory,
            onSelect = { viewModel.selectedCategory = it },
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${viewModel.filteredChannels.size} canales", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            Text("Guía de programación · Ejemplo", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
        if (viewModel.filteredChannels.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Filled.Search, "No encontramos canales", "Prueba con otro nombre o categoría.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (isTv) 340.dp else 300.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.filteredChannels, key = LiveChannel::id) { channel ->
                    ChannelCard(channel = channel, modifier = Modifier.fillMaxWidth()) { onOpenChannel(channel.id) }
                }
            }
        }
    }
}

@Composable
private fun LiveIndicator() {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFF5271).copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFFF6B82)))
        Text("EN VIVO", color = Color(0xFFFF8797), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BrowseMediaScreen(
    isTv: Boolean,
    kind: MediaKind,
    onOpenMedia: (String) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    viewModel: MoviesViewModel = viewModel()
) {
    val screenTitle = if (kind == MediaKind.MOVIE) "Películas" else "Series"
    val items = viewModel.filteredMovies.filter { it.kind == kind }
    val genres = if (kind == MediaKind.MOVIE) DemoCatalog.movieGenres else listOf("Todos", "Aventura", "Drama", "Misterio", "Suspenso", "Familia")
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = if (isTv) 36.dp else 18.dp, vertical = 16.dp)) {
        PageHeading(
            eyebrow = if (kind == MediaKind.MOVIE) "Historias para cada momento" else "Nuevos episodios",
            title = screenTitle,
            description = if (kind == MediaKind.MOVIE) "Descubre tu próxima película favorita." else "Sigue tus historias y encuentra nuevas series."
        )
        SearchBox(
            value = viewModel.query,
            onValueChange = { viewModel.query = it },
            placeholder = "Buscar $screenTitle",
            modifier = Modifier.padding(bottom = 11.dp)
        )
        FilterChips(options = genres, selected = viewModel.selectedGenre, onSelect = { viewModel.selectedGenre = it }, modifier = Modifier.padding(bottom = 10.dp))
        FilterChips(
            options = listOf("Populares", "Mejor valoradas", "Recientes"),
            selected = viewModel.selectedTab,
            onSelect = { viewModel.selectedTab = it },
            modifier = Modifier.padding(bottom = 7.dp)
        )
        Text("${items.size} títulos · Catálogo de demostración", modifier = Modifier.padding(vertical = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        if (items.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Filled.Movie, "Sin resultados", "Cambia el filtro o busca otro título.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (isTv) 178.dp else 145.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isTv) 15.dp else 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(items, key = MediaItem::id) { item ->
                    MediaPosterCard(
                        item = item,
                        modifier = Modifier.fillMaxWidth(),
                        isFavorite = item.id in favorites,
                        onClick = { onOpenMedia(item.id) },
                        onFavoriteClick = { onToggleFavorite(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    isTv: Boolean,
    savedIds: Set<String>,
    onToggle: (String) -> Unit,
    onOpenMedia: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onExplore: () -> Unit
) {
    val media = DemoCatalog.media.filter { it.id in savedIds }
    val channels = DemoCatalog.channels.filter { it.id in savedIds }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = if (isTv) 36.dp else 18.dp, vertical = 16.dp)) {
        PageHeading("Tu colección", "Mi lista", "Todo lo que guardaste, en un solo lugar.")
        if (media.isEmpty() && channels.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Filled.Favorite, "Tu lista está vacía", "Guarda películas, series y canales para encontrarlos rápido.", "Explorar catálogo", onExplore)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(21.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
            ) {
                if (media.isNotEmpty()) {
                    item {
                        Column {
                            SectionTitle("Películas y series")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(media, key = MediaItem::id) { item ->
                                    MediaPosterCard(
                                        item = item,
                                        modifier = Modifier.width(if (isTv) 190.dp else 150.dp),
                                        isFavorite = true,
                                        onClick = { onOpenMedia(item.id) },
                                        onFavoriteClick = { onToggle(item.id) }
                                    )
                                }
                            }
                        }
                    }
                }
                if (channels.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionTitle("Canales")
                            channels.forEach { channel ->
                                ChannelCard(channel, modifier = Modifier.fillMaxWidth()) { onOpenChannel(channel.id) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    isTv: Boolean,
    onOpenMedia: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val recent = DemoCatalog.recentSearches
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = if (isTv) 36.dp else 18.dp, vertical = 16.dp)) {
        PageHeading("Encuentra tu próximo favorito", "Buscar", "Películas, series y canales en un solo lugar.")
        SearchBox(
            value = viewModel.query,
            onValueChange = { viewModel.query = it },
            placeholder = "¿Qué te gustaría ver?"
        )
        if (viewModel.query.isBlank()) {
            Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Column {
                    SectionTitle("Búsquedas recientes")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        recent.forEach { term ->
                            AssistChip(
                                onClick = { viewModel.query = term },
                                label = { Text(term) },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.onSurface)
                            )
                        }
                    }
                }
                Column {
                    SectionTitle("Tendencias")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(DemoCatalog.media.sortedByDescending(MediaItem::rating).take(5), key = MediaItem::id) { item ->
                            MediaPosterCard(item, modifier = Modifier.width(if (isTv) 190.dp else 148.dp), onClick = { onOpenMedia(item.id) })
                        }
                    }
                }
                Column {
                    SectionTitle("Canales en directo")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(DemoCatalog.channels.take(4), key = LiveChannel::id) { channel ->
                            CompactChannelCard(channel, modifier = Modifier.width(if (isTv) 320.dp else 280.dp)) { onOpenChannel(channel.id) }
                        }
                    }
                }
                Text("Los resultados mostrados son datos de ejemplo.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        } else {
            val mediaResults = viewModel.mediaResults
            val channelResults = viewModel.channelResults
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 22.dp)
            ) {
                item {
                    Text("Resultados de demostración", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                }
                if (channelResults.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionTitle("Canales")
                            channelResults.forEach { channel ->
                                ChannelCard(channel, modifier = Modifier.fillMaxWidth()) { onOpenChannel(channel.id) }
                            }
                        }
                    }
                }
                if (mediaResults.isNotEmpty()) {
                    item {
                        Column {
                            SectionTitle("Películas y series")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(mediaResults, key = MediaItem::id) { item ->
                                    MediaPosterCard(item, modifier = Modifier.width(if (isTv) 190.dp else 148.dp), onClick = { onOpenMedia(item.id) })
                                }
                            }
                        }
                    }
                }
                if (mediaResults.isEmpty() && channelResults.isEmpty()) {
                    item { EmptyState(Icons.Filled.Search, "No encontramos resultados", "Prueba otro título, género o canal.") }
                }
            }
        }
    }
}
