package com.myiptv.clean.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MediaCollectionScreen(
    collection: MediaCollection,
    isTv: Boolean,
    onOpenMedia: (String) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    viewModel: MediaCollectionViewModel = viewModel()
) {
    LaunchedEffect(collection) { viewModel.load(collection) }
    TvCollectionScreen(collection, viewModel, onOpenMedia, favorites, onToggleFavorite, isTv)
}
