package com.myiptv.clean.ui.compose

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myiptv.clean.data.repository.MediaPlaylist

/** TV and phone use the same visual language, with each layout adapting to its available width. */
@Composable
fun HomeScreen(
    isTv: Boolean,
    playlist: MediaPlaylist,
    onOpenChannels: () -> Unit,
    onOpenMovies: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenMedia: (String) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    TvHomeScreen(
        viewModel = viewModel,
        isTv = isTv,
        playlist = playlist,
        onOpenChannels = onOpenChannels,
        onOpenMovies = onOpenMovies,
        onOpenSeries = onOpenSeries,
        onOpenSettings = onOpenSettings,
        onOpenChannel = onOpenChannel,
        onOpenMedia = onOpenMedia,
        favorites = favorites,
        onToggleFavorite = onToggleFavorite
    )
}

@Composable
fun ChannelsScreen(isTv: Boolean, onOpenChannel: (String) -> Unit, viewModel: ChannelsViewModel = viewModel()) {
    TvChannelsScreen(viewModel, onOpenChannel, isTv)
}

@Composable
fun BrowseMediaScreen(
    isTv: Boolean,
    kind: MediaKind,
    playlist: MediaPlaylist,
    onOpenMedia: (String) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    viewModel: MoviesViewModel = viewModel()
) {
    TvBrowseMediaScreen(kind, viewModel, onOpenMedia, favorites, onToggleFavorite, playlist, isTv)
}

@Composable
fun FavoritesScreen(
    isTv: Boolean,
    playlist: MediaPlaylist,
    savedIds: Set<String>,
    onToggle: (String) -> Unit,
    onOpenMedia: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onExplore: () -> Unit
) {
    TvFavoritesScreen(savedIds, onToggle, onOpenMedia, onOpenChannel, onExplore, playlist, isTv)
}

@Composable
fun SearchScreen(
    isTv: Boolean,
    playlist: MediaPlaylist,
    onOpenMedia: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    TvSearchScreen(viewModel, onOpenMedia, onOpenChannel, playlist, isTv)
}
