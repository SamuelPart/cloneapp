package com.myiptv.clean.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myiptv.clean.data.repository.TmdbRepository

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
    val isKids = collection == MediaCollection.KIDS
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = if (isTv) 36.dp else 18.dp, vertical = 16.dp)) {
        PageHeading(
            eyebrow = if (isKids) "Para compartir en familia" else "Animación japonesa",
            title = if (isKids) "Kids" else "Anime",
            description = if (isKids) {
                "Películas familiares y series infantiles según TMDb. Esta sección no sustituye la supervisión parental."
            } else {
                "Series de animación de origen japonés según TMDb."
            }
        )
        Text(
            text = viewModel.statusMessage,
            modifier = Modifier.padding(bottom = 10.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
        if (viewModel.items.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Filled.Movie,
                    title = if (viewModel.isLoading) "Cargando catálogo" else "No hay títulos disponibles",
                    message = viewModel.statusMessage,
                    action = if (!viewModel.isLoading && TmdbRepository.isConfigured) "Reintentar" else null,
                    onAction = if (!viewModel.isLoading && TmdbRepository.isConfigured) ({ viewModel.load(collection, force = true) }) else null
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (isTv) 178.dp else 145.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isTv) 15.dp else 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(viewModel.items, key = MediaItem::id) { item ->
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
