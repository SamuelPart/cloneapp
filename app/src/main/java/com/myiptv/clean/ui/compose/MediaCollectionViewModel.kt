package com.myiptv.clean.ui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myiptv.clean.data.repository.TmdbRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

enum class MediaCollection { KIDS, ANIME }

class MediaCollectionViewModel : ViewModel() {
    var items by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf("Preparando el catálogo…")
        private set

    private var activeCollection: MediaCollection? = null
    private var loadJob: Job? = null
    private var requestVersion = 0

    fun load(collection: MediaCollection, force: Boolean = false) {
        if (!force && activeCollection == collection) return
        loadJob?.cancel()
        activeCollection = collection
        val version = ++requestVersion
        items = emptyList()
        if (!TmdbRepository.isConfigured) {
            isLoading = false
            statusMessage = "Configura una clave TMDb local para cargar esta sección."
            return
        }

        isLoading = true
        statusMessage = "Consultando TMDb…"
        loadJob = viewModelScope.launch {
            try {
                val result = when (collection) {
                    MediaCollection.KIDS -> coroutineScope {
                        val movies = async {
                            TmdbRepository.genreNames(MediaKind.MOVIE)
                            TmdbRepository.discoverMovies(10751) // Family
                        }
                        val series = async {
                            TmdbRepository.genreNames(MediaKind.SERIES)
                            TmdbRepository.discoverSeries(10762) // Kids
                        }
                        interleaveCatalogs(movies.await(), series.await())
                    }
                    MediaCollection.ANIME -> {
                        TmdbRepository.genreNames(MediaKind.SERIES)
                        TmdbRepository.discoverSeries(16, originCountry = "JP") // Animation, Japan
                    }
                }
                if (version == requestVersion) {
                    items = result.distinctBy(MediaItem::id)
                    statusMessage = if (items.isEmpty()) "TMDb no devolvió títulos para esta sección." else "Catálogo cargado desde TMDb."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (version == requestVersion) statusMessage = "No se pudo cargar la sección desde TMDb."
            } finally {
                if (version == requestVersion) isLoading = false
            }
        }
    }
}
