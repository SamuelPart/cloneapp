package com.myiptv.clean.ui.compose

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myiptv.clean.data.repository.MediaPlaylist

/** Keep Android TV's existing home; phones use their own touch-oriented catalog layout. */
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
    category: PhoneHomeCategory = PhoneHomeCategory.RECOMMENDED,
    onOpenWeeklyTrends: () -> Unit = {},
    onOpenDocumentaries: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    if (isTv) {
        TvHomeScreen(
            viewModel = viewModel,
            isTv = true,
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
    } else {
        PhoneHomeScreen(
            viewModel = viewModel,
            playlist = playlist,
            onOpenChannels = onOpenChannels,
            onOpenMovies = onOpenMovies,
            onOpenSeries = onOpenSeries,
            onOpenSettings = onOpenSettings,
            onOpenChannel = onOpenChannel,
            onOpenMedia = onOpenMedia,
            favorites = favorites,
            onToggleFavorite = onToggleFavorite,
            category = category,
            onOpenWeeklyTrends = onOpenWeeklyTrends,
            onOpenDocumentaries = onOpenDocumentaries,
        )
    }
}

@Composable
internal fun PhoneCategoryHomeScreen(
    category: PhoneHomeCategory,
    playlist: MediaPlaylist,
    onOpenChannels: () -> Unit,
    onOpenMoviesCatalog: () -> Unit,
    onOpenSeriesCatalog: () -> Unit,
    onOpenWeeklyTrends: () -> Unit,
    onOpenDocumentaries: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenMedia: (String) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    PhoneHomeScreen(
        viewModel = viewModel,
        playlist = playlist,
        onOpenChannels = onOpenChannels,
        onOpenMovies = onOpenMoviesCatalog,
        onOpenSeries = onOpenSeriesCatalog,
        onOpenSettings = onOpenSettings,
        onOpenChannel = onOpenChannel,
        onOpenMedia = onOpenMedia,
        favorites = favorites,
        onToggleFavorite = onToggleFavorite,
        category = category,
        onOpenWeeklyTrends = onOpenWeeklyTrends,
        onOpenDocumentaries = onOpenDocumentaries,
    )
}

@Composable
internal fun ChannelsScreen(
    isTv: Boolean,
    onOpenChannel: (String) -> Unit,
    fullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    channelKeys: TvChannelKeys,
    requestedChannel: TvChannelRequest?,
    onRequestHandled: () -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    phoneSearchOpen: Boolean,
    onClosePhoneSearch: () -> Unit,
    onRequestPhoneFullscreen: (PhoneLiveFullscreenRequest) -> Unit,
    viewModel: ChannelsViewModel = viewModel()
) {
    if (isTv) TvLiveScreen(viewModel, fullscreen, onFullscreenChange, channelKeys, requestedChannel, onRequestHandled, onOpenChannel)
    else PhoneLiveScreen(viewModel, favorites, onToggleFavorite, onOpenChannel, phoneSearchOpen, onClosePhoneSearch, onFullscreenChange, onRequestPhoneFullscreen)
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
    if (isTv) TvBrowseMediaScreen(kind, viewModel, onOpenMedia, favorites, onToggleFavorite, playlist, isTv = true)
    else PhoneBrowseMediaScreen(kind, viewModel, onOpenMedia, favorites, onToggleFavorite, playlist)
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
    onBack: () -> Unit,
    onOpenMedia: (String) -> Unit,
    onOpenChannel: (String) -> Unit
) {
    if (isTv) {
        TvSearchScreen(viewModel(), onOpenMedia, onOpenChannel, playlist, isTv = true)
    } else {
        PhoneSearchScreen(playlist, onBack, onOpenMedia, onOpenChannel)
    }
}
