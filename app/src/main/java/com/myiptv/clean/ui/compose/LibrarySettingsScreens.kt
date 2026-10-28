package com.myiptv.clean.ui.compose

import androidx.compose.runtime.Composable
import com.myiptv.clean.data.repository.MediaPlaylist

@Composable
fun DownloadsScreen(isTv: Boolean) = TvLibraryScreen(downloads = true, isTv = isTv)

@Composable
fun HistoryScreen(isTv: Boolean) = TvLibraryScreen(downloads = false, isTv = isTv)

@Composable
fun SettingsScreen(isTv: Boolean, playlist: MediaPlaylist) = TvSettingsScreen(playlist, isTv)
