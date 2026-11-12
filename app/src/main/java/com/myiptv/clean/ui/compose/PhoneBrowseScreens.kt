package com.myiptv.clean.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.myiptv.clean.data.repository.MediaPlaylist
import com.myiptv.clean.data.repository.TmdbRepository

/** Keep genre and ordering controls, but surface them through a compact filter dialog on phones. */
@Composable
internal fun PhoneBrowseMediaScreen(
    kind: MediaKind,
    viewModel: MoviesViewModel,
    onOpenMedia: (String) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    playlist: MediaPlaylist
) {
    val title = if (kind == MediaKind.MOVIE) "Películas Populares" else "Series Populares"
    val genres = viewModel.genresFor(kind, playlist.items)
    val sortOptions = listOf("Populares", "Mejor valoradas", "Recientes")
    var filterDialogOpen by remember(kind) { mutableStateOf(false) }
    val titles = viewModel.itemsFor(kind, playlist.items)
    val message = if (viewModel.query.isNotBlank() || viewModel.selectedGenre != "Todos") {
        "No se encontraron títulos para este filtro."
    } else "${playlist.statusMessage} ${viewModel.statusMessage}"
    PhonePosterGrid(
        items = titles,
        favorites = favorites,
        onOpenMedia = onOpenMedia,
        onToggleFavorite = onToggleFavorite,
        header = {
            Column {
                TvPageHeader(
                    title,
                    if (kind == MediaKind.MOVIE) "Películas del catálogo M3U y de TMDb." else "Series del catálogo M3U y de TMDb.",
                    action = {
                        IconButton(onClick = { filterDialogOpen = true }) {
                            Icon(
                                Icons.Filled.Tune,
                                contentDescription = "Abrir filtros",
                                tint = if (viewModel.selectedGenre != "Todos" || viewModel.selectedTab != "Populares") PhoneChannelAccent else Color.White,
                            )
                        }
                    },
                )
                TvStatus("${titles.size} títulos · ${playlist.statusMessage} · ${viewModel.statusMessage}")
            }
        },
        emptyContent = {
            if (viewModel.isLoading || !playlist.loaded) {
                SamplayLoadingPane(Modifier.fillMaxWidth(), "Cargando $title…")
            } else {
                TvEmptyState(
                    Icons.Filled.Movie,
                    "No hay títulos",
                    message,
                    Modifier.fillMaxWidth().heightIn(min = 180.dp)
                )
            }
        }
    )
    if (filterDialogOpen) {
        AlertDialog(
            onDismissRequest = { filterDialogOpen = false },
            title = { Text("Filtros del catálogo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Género")
                    TvFilterRow(
                        options = genres,
                        selected = viewModel.selectedGenre,
                        onSelect = { viewModel.selectedGenre = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Ordenar por")
                    TvFilterRow(
                        options = sortOptions,
                        selected = viewModel.selectedTab,
                        onSelect = { viewModel.selectedTab = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { filterDialogOpen = false }) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.selectedGenre = "Todos"
                    viewModel.selectedTab = "Populares"
                    viewModel.query = ""
                }) { Text("Limpiar") }
            },
        )
    }
}

/** The Kids callback remains the one supplied by navigation, which marks details as child content. */
@Composable
internal fun PhoneCollectionScreen(
    collection: MediaCollection,
    viewModel: MediaCollectionViewModel,
    onOpenMedia: (String) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit
) {
    val isKids = collection == MediaCollection.KIDS
    PhoneCollectionHomeScreen(
        collection = collection,
        items = viewModel.items,
        loading = viewModel.isLoading,
        status = viewModel.statusMessage.ifBlank {
            if (isKids) "Películas familiares y series infantiles de TMDb." else "Series de animación de origen japonés de TMDb."
        },
        favorites = favorites,
        onToggleFavorite = onToggleFavorite,
        onOpenMedia = onOpenMedia,
        onRetry = if (TmdbRepository.isConfigured) ({ viewModel.load(collection, force = true) }) else null,
    )
}

@Composable
internal fun PhonePosterGrid(
    items: List<MediaItem>,
    favorites: Set<String>,
    onOpenMedia: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    header: @Composable () -> Unit,
    emptyContent: @Composable () -> Unit
) {
    val unique = remember(items) { items.distinctBy(MediaItem::id) }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = phonePosterColumns(maxWidth.value)
        // Filters and posters share one scrolling surface, including in short landscape windows.
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "phone-catalog-header", span = { GridItemSpan(columns) }) { header() }
            if (unique.isEmpty()) {
                item(key = "phone-catalog-empty", span = { GridItemSpan(columns) }) { emptyContent() }
            } else {
                items(unique, key = { "phone-media-${it.id}" }) { item ->
                    PhonePosterCard(
                        item = item,
                        isFavorite = item.id in favorites,
                        onOpen = { onOpenMedia(item.id) },
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
