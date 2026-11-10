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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myiptv.clean.data.repository.MediaPlaylist
import com.myiptv.clean.data.repository.TmdbRepository

/** Keep the existing real search, genre and sorting filters; only the phone presentation changes. */
@Composable
internal fun PhoneBrowseMediaScreen(
    kind: MediaKind,
    viewModel: MoviesViewModel,
    onOpenMedia: (String) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    playlist: MediaPlaylist
) {
    val title = if (kind == MediaKind.MOVIE) "Películas" else "Series"
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
                TvPageHeader(title, if (kind == MediaKind.MOVIE) "Películas del catálogo M3U y de TMDb." else "Series del catálogo M3U y de TMDb.")
                TvCatalogControls(
                    query = viewModel.query,
                    onQueryChange = { viewModel.query = it },
                    placeholder = "Buscar $title",
                    filters = viewModel.genresFor(kind, playlist.items),
                    selectedFilter = viewModel.selectedGenre,
                    onSelectFilter = { viewModel.selectedGenre = it }
                )
                TvFilterRow(
                    options = listOf("Populares", "Mejor valoradas", "Recientes"),
                    selected = viewModel.selectedTab,
                    onSelect = { viewModel.selectedTab = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
                TvStatus("${titles.size} títulos · ${playlist.statusMessage} · ${viewModel.statusMessage}")
            }
        },
        emptyContent = {
            TvEmptyState(
                Icons.Filled.Movie,
                if (viewModel.isLoading || !playlist.loaded) "Cargando catálogo" else "No hay títulos",
                message,
                Modifier.fillMaxWidth().heightIn(min = 180.dp)
            )
        }
    )
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
    PhonePosterGrid(
        items = viewModel.items,
        favorites = favorites,
        onOpenMedia = onOpenMedia,
        onToggleFavorite = onToggleFavorite,
        header = {
            Column {
                TvPageHeader(
                    title = if (isKids) "Kids" else "Anime",
                    subtitle = if (isKids) "Películas familiares y series infantiles según TMDb. Esta sección no sustituye la supervisión parental."
                    else "Series de animación de origen japonés según TMDb."
                )
                TvStatus(viewModel.statusMessage)
            }
        },
        emptyContent = {
            TvEmptyState(
                Icons.Filled.Movie,
                if (viewModel.isLoading) "Cargando catálogo" else "No hay títulos disponibles",
                viewModel.statusMessage,
                Modifier.fillMaxWidth().heightIn(min = 180.dp),
                action = if (!viewModel.isLoading && TmdbRepository.isConfigured) "Reintentar" else null,
                onAction = if (!viewModel.isLoading && TmdbRepository.isConfigured) ({ viewModel.load(collection, force = true) }) else null
            )
        }
    )
}

@Composable
private fun PhonePosterGrid(
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
