package com.myiptv.clean.ui.compose

import androidx.compose.runtime.Composable
import com.myiptv.clean.data.repository.MediaPlaylist

@Composable
fun DownloadsScreen(isTv: Boolean) = TvLibraryScreen(downloads = true, isTv = isTv)

@Composable
fun HistoryScreen(isTv: Boolean) = TvLibraryScreen(downloads = false, isTv = isTv)

@Composable
fun SettingsScreen(
    isTv: Boolean,
    playlist: MediaPlaylist,
    onOpenCoins: () -> Unit,
    onOpenProfile: () -> Unit = {},
    openUpdateRequest: Long? = null,
    onUpdateRequestHandled: (Long) -> Unit = {}
) {
    if (isTv) TvSettingsScreen(playlist, isTv = true, onOpenCoins = onOpenCoins, onOpenProfile = onOpenProfile)
    else PhoneSettingsScreen(playlist, onOpenCoins, onOpenProfile, openUpdateRequest, onUpdateRequestHandled)
}
