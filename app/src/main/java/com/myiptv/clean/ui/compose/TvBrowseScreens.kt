package com.myiptv.clean.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myiptv.clean.BuildConfig
import com.myiptv.clean.data.repository.ChannelCatalogRepository
import com.myiptv.clean.data.repository.MediaPlaylist
import com.myiptv.clean.data.repository.mergeMediaCatalog
import com.myiptv.clean.data.repository.TmdbRepository

@Composable
internal fun TvChannelsScreen(viewModel: ChannelsViewModel, onOpenChannel: (String) -> Unit, isTv: Boolean) {
    val channels = viewModel.filteredChannels
    Column(modifier = Modifier.fillMaxSize().padding(start = if (isTv) 0.dp else 16.dp, end = if (isTv) 24.dp else 16.dp)) {
        TvPageHeader(
            title = "TV en directo",
            subtitle = "Emisiones en directo de distintos países.",
            action = { TvActionButton("Actualizar", viewModel::refresh, icon = Icons.Filled.Refresh) }
        )
        TvCatalogControls(
            query = viewModel.query,
            onQueryChange = { viewModel.query = it },
            placeholder = "Buscar canales",
            filters = viewModel.categories,
            selectedFilter = viewModel.selectedCategory,
            onSelectFilter = { viewModel.selectedCategory = it }
        )
        TvStatus("${channels.size} canales · ${viewModel.errorMessage.ifBlank { ChannelCatalogRepository.statusMessage }}")
        if (channels.isEmpty()) {
            when {
                viewModel.isLoading -> TvEmptyState(Icons.Filled.LiveTv, "Cargando canales", "Consultando emisiones en directo.", Modifier.weight(1f))
                viewModel.errorMessage.isNotBlank() -> TvEmptyState(Icons.Filled.LiveTv, "No se pudo cargar la lista", viewModel.errorMessage, Modifier.weight(1f), "Reintentar", viewModel::refresh)
                else -> TvEmptyState(Icons.Filled.Search, "No hay canales", "No se encontraron canales para este filtro.", Modifier.weight(1f))
            }
        } else {
            TvChannelGrid(channels, onOpenChannel, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun TvBrowseMediaScreen(
    kind: MediaKind,
    viewModel: MoviesViewModel,
    onOpenMedia: (String) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    playlist: MediaPlaylist,
    isTv: Boolean
) {
    val title = if (kind == MediaKind.MOVIE) "Películas" else "Series"
    val items = viewModel.itemsFor(kind, playlist.items)
    Column(modifier = Modifier.fillMaxSize().padding(start = if (isTv) 0.dp else 16.dp, end = if (isTv) 24.dp else 16.dp)) {
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
        TvStatus("${items.size} títulos · ${playlist.statusMessage} · ${viewModel.statusMessage}")
        if (items.isEmpty()) {
            val message = if (viewModel.query.isNotBlank() || viewModel.selectedGenre != "Todos") {
                "No se encontraron títulos para este filtro."
            } else "${playlist.statusMessage} ${viewModel.statusMessage}"
            TvEmptyState(Icons.Filled.Movie, if (viewModel.isLoading || !playlist.loaded) "Cargando catálogo" else "No hay títulos", message, Modifier.weight(1f))
        } else {
            TvMediaGrid(items, onOpenMedia, Modifier.weight(1f), favorites, onToggleFavorite)
        }
    }
}

@Composable
internal fun TvCollectionScreen(
    collection: MediaCollection,
    viewModel: MediaCollectionViewModel,
    onOpenMedia: (String) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    isTv: Boolean
) {
    val isKids = collection == MediaCollection.KIDS
    Column(modifier = Modifier.fillMaxSize().padding(start = if (isTv) 0.dp else 16.dp, end = if (isTv) 24.dp else 16.dp)) {
        TvPageHeader(
            title = if (isKids) "Kids" else "Anime",
            subtitle = if (isKids) "Películas familiares y series infantiles según TMDb. Esta sección no sustituye la supervisión parental."
            else "Series de animación de origen japonés según TMDb."
        )
        TvStatus(viewModel.statusMessage)
        if (viewModel.items.isEmpty()) {
            TvEmptyState(
                Icons.Filled.Movie,
                if (viewModel.isLoading) "Cargando catálogo" else "No hay títulos disponibles",
                viewModel.statusMessage,
                Modifier.weight(1f),
                action = if (!viewModel.isLoading && TmdbRepository.isConfigured) "Reintentar" else null,
                onAction = if (!viewModel.isLoading && TmdbRepository.isConfigured) ({ viewModel.load(collection, force = true) }) else null
            )
        } else {
            TvMediaGrid(viewModel.items, onOpenMedia, Modifier.weight(1f), favorites, onToggleFavorite)
        }
    }
}

@Composable
internal fun TvFavoritesScreen(
    savedIds: Set<String>,
    onToggle: (String) -> Unit,
    onOpenMedia: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onExplore: () -> Unit,
    playlist: MediaPlaylist,
    isTv: Boolean
) {
    val media = mergeMediaCatalog(playlist.items, TmdbRepository.cachedCatalog()).filter { it.id in savedIds }
    val channels = ChannelCatalogRepository.cachedChannels().filter { it.id in savedIds }
    val loadedIds = media.map(MediaItem::id).toSet() + channels.map(LiveChannel::id)
    val unavailableCount = (savedIds - loadedIds).size
    Column(modifier = Modifier.fillMaxSize().padding(start = if (isTv) 0.dp else 16.dp, end = if (isTv) 24.dp else 16.dp)) {
        TvPageHeader("Mi lista", "Películas, series y canales guardados.")
        if (media.isEmpty() && channels.isEmpty()) {
            val title = if (savedIds.isEmpty()) "Tu lista está vacía" else "No hay elementos disponibles"
            val message = if (savedIds.isEmpty()) "Guarda películas, series y canales para encontrarlos rápido."
            else "Los elementos guardados no están en los catálogos cargados."
            TvEmptyState(Icons.Filled.Favorite, title, message, Modifier.weight(1f), "Explorar catálogo", onExplore)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (media.isNotEmpty()) {
                    item(key = "media") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TvSectionLabel("Películas y series")
                            TvMediaRail(media, onOpenMedia, savedIds, onToggle)
                        }
                    }
                }
                if (channels.isNotEmpty()) {
                    item(key = "channels") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TvSectionLabel("Canales")
                            TvChannelRail(channels, onOpenChannel, savedIds, onToggle)
                        }
                    }
                }
                if (unavailableCount > 0) {
                    item(key = "unavailable") { TvStatus("$unavailableCount elementos guardados no están en los catálogos cargados.") }
                }
            }
        }
    }
}

@Composable
internal fun TvSearchScreen(
    viewModel: SearchViewModel,
    onOpenMedia: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    playlist: MediaPlaylist,
    isTv: Boolean
) {
    val isSearching = viewModel.query.isNotBlank()
    val localMedia = playlist.items.filter { item ->
        !isSearching || item.title.contains(viewModel.query, ignoreCase = true) ||
            item.genres.any { it.contains(viewModel.query, ignoreCase = true) }
    }
    val remoteMedia = if (isSearching) viewModel.mediaResults else viewModel.featuredMedia.sortedByDescending(MediaItem::rating).take(5)
    val media = mergeMediaCatalog(localMedia, remoteMedia)
    val channels = if (isSearching) viewModel.channelResults else viewModel.channels
    Column(modifier = Modifier.fillMaxSize().padding(start = if (isTv) 0.dp else 16.dp, end = if (isTv) 24.dp else 16.dp)) {
        TvPageHeader("Buscar", "Películas, series y canales en un solo lugar.")
        TvSearchBox(viewModel.query, { viewModel.query = it }, "¿Qué te gustaría ver?")
        TvStatus(viewModel.statusMessage)
        if (media.isEmpty() && channels.isEmpty()) {
            TvEmptyState(
                Icons.Filled.Search,
                when {
                    viewModel.isLoading -> "Buscando"
                    isSearching -> "No encontramos resultados"
                    else -> "No hay contenido disponible"
                },
                if (isSearching && !viewModel.isLoading) "Prueba otro título, género o canal." else viewModel.statusMessage,
                Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
            ) {
                if (channels.isNotEmpty()) {
                    item(key = "channels") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TvSectionLabel("Canales de TV")
                            TvChannelRail(channels, onOpenChannel)
                        }
                    }
                }
                if (media.isNotEmpty()) {
                    item(key = "media") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TvSectionLabel("Películas y series")
                            TvMediaRail(media, onOpenMedia)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TvLibraryScreen(downloads: Boolean, isTv: Boolean) {
    Column(modifier = Modifier.fillMaxSize().padding(start = if (isTv) 0.dp else 16.dp, end = if (isTv) 24.dp else 16.dp)) {
        TvPageHeader(if (downloads) "Descargas" else "Historial", if (downloads) "Contenido sin conexión." else "Actividad de reproducción local.")
        TvEmptyState(
            icon = if (downloads) Icons.Filled.Download else Icons.Filled.History,
            title = if (downloads) "No hay descargas" else "Historial vacío",
            message = if (downloads) "La aplicación todavía no ofrece descargas sin conexión. Los enlaces de reproducción no se guardan como archivos."
            else "La actividad de reproducción todavía no se guarda.",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun TvSettingsScreen(playlist: MediaPlaylist, isTv: Boolean) {
    Column(modifier = Modifier.fillMaxSize().padding(start = if (isTv) 0.dp else 16.dp, end = if (isTv) 24.dp else 16.dp)) {
        TvPageHeader("Ajustes", "Fuentes de información, reproducción y privacidad.")
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TvInfoPanel {
                TvSectionLabel("Fuentes")
                TvSourceRow(Icons.Filled.Key, "Metadatos TMDb", if (TmdbRepository.isConfigured) "Hay una API Key local configurada." else "Falta la API Key local. Añádela en local.properties o en TMDB_API_KEY.")
                TvSourceRow(Icons.Filled.LiveTv, "Canales de televisión", "Catálogo de emisiones en directo de distintos países.")
                TvSourceRow(Icons.Filled.Movie, "Catálogo M3U", "${playlist.statusMessage} Las fuentes se incluyen desde samplay_catalog.m3u en el proyecto.")
            }
            TvInfoPanel {
                TvSectionLabel("Privacidad y atribución")
                Text("La clave TMDb se lee desde local.properties o la variable de entorno TMDB_API_KEY; no está incluida en Git.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
                Text("Este producto utiliza la API de TMDb, pero no está avalado ni certificado por TMDb.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
                Text("Tipografía Inter © The Inter Project Authors, distribuida bajo SIL Open Font License 1.1.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
                Text("La disponibilidad de las emisiones de televisión puede variar según el país y las condiciones de cada señal.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
            }
            TvStatus("SamPlay · versión ${BuildConfig.VERSION_NAME}")
        }
    }
}

@Composable
private fun TvSectionLabel(text: String) {
    Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun TvSourceRow(icon: ImageVector, title: String, subtitle: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = TvAccent, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TvMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
