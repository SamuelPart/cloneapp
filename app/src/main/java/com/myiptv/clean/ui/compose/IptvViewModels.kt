package com.myiptv.clean.ui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myiptv.clean.data.repository.TmdbRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    var featured by mutableStateOf(DemoCatalog.media.first())
        private set
    var trending by mutableStateOf(DemoCatalog.media.sortedByDescending(MediaItem::rating).take(6))
        private set
    var recommended by mutableStateOf(DemoCatalog.media.drop(2))
        private set
    val liveChannels = DemoCatalog.channels
    var isLoading by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf(configurationMessage())
        private set

    init {
        loadCatalog()
    }

    private fun loadCatalog() {
        if (!TmdbRepository.isConfigured) return
        isLoading = true
        viewModelScope.launch {
            try {
                coroutineScope {
                    val movieGenresRequest = async { TmdbRepository.genreNames(MediaKind.MOVIE) }
                    val seriesGenresRequest = async { TmdbRepository.genreNames(MediaKind.SERIES) }
                    movieGenresRequest.await()
                    seriesGenresRequest.await()
                }
                val (movies, series) = coroutineScope {
                    val moviesRequest = async { TmdbRepository.popularMovies() }
                    val seriesRequest = async { TmdbRepository.popularSeries() }
                    moviesRequest.await() to seriesRequest.await()
                }
                val all = movies + series
                featured = movies.firstOrNull() ?: series.firstOrNull() ?: featured
                trending = all.sortedByDescending(MediaItem::rating).take(12).ifEmpty { trending }
                recommended = series.ifEmpty { recommended }
                statusMessage = "Catálogo, fichas e imágenes servidos por TMDb. La reproducción y los canales siguen siendo de demostración."
            } catch (_: Exception) {
                statusMessage = "No se pudo conectar con TMDb; se muestra el catálogo de demostración."
            } finally {
                isLoading = false
            }
        }
    }
}

class ChannelsViewModel : ViewModel() {
    var query by mutableStateOf("")
    var selectedCategory by mutableStateOf("Todos")

    val filteredChannels: List<LiveChannel>
        get() = DemoCatalog.channels.filter { channel ->
            (selectedCategory == "Todos" || channel.category == selectedCategory) &&
                channel.name.contains(query, ignoreCase = true)
        }
}

class MoviesViewModel : ViewModel() {
    private var movieCatalog by mutableStateOf(DemoCatalog.media.filter { it.kind == MediaKind.MOVIE })
    private var seriesCatalog by mutableStateOf(DemoCatalog.media.filter { it.kind == MediaKind.SERIES })
    private var remoteSearchResults by mutableStateOf<List<MediaItem>?>(null)
    private val queryState = mutableStateOf("")
    private var searchJob: Job? = null

    var query: String
        get() = queryState.value
        set(value) {
            queryState.value = value
            searchJob?.cancel()
            remoteSearchResults = null
            if (value.trim().length < 2 || !TmdbRepository.isConfigured) return
            searchJob = viewModelScope.launch {
                delay(350)
                try {
                    remoteSearchResults = TmdbRepository.search(value)
                    statusMessage = "Resultados de búsqueda obtenidos de TMDb."
                } catch (_: Exception) {
                    statusMessage = "No se pudo buscar en TMDb; se filtra el catálogo disponible."
                }
            }
        }

    var selectedGenre by mutableStateOf("Todos")
    var selectedTab by mutableStateOf("Populares")
    var movieGenres by mutableStateOf(listOf("Todos"))
        private set
    var seriesGenres by mutableStateOf(listOf("Todos"))
        private set
    var isLoading by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf(configurationMessage())
        private set

    init {
        loadCatalog()
    }

    fun genresFor(kind: MediaKind): List<String> =
        if (kind == MediaKind.MOVIE) movieGenres else seriesGenres

    fun itemsFor(kind: MediaKind): List<MediaItem> {
        val source = remoteSearchResults ?: if (kind == MediaKind.MOVIE) movieCatalog else seriesCatalog
        val filtered = source.filter { item ->
            item.kind == kind &&
                (selectedGenre == "Todos" || item.genres.any { it.equals(selectedGenre, ignoreCase = true) }) &&
                (query.isBlank() || item.title.contains(query, ignoreCase = true) || item.genres.any { it.contains(query, ignoreCase = true) })
        }
        return when (selectedTab) {
            "Mejor valoradas" -> filtered.sortedByDescending(MediaItem::rating)
            "Recientes" -> filtered.sortedByDescending(MediaItem::year)
            else -> filtered
        }
    }

    private fun loadCatalog() {
        if (!TmdbRepository.isConfigured) return
        isLoading = true
        viewModelScope.launch {
            try {
                val (movieGenreNames, seriesGenreNames) = coroutineScope {
                    val moviesRequest = async { TmdbRepository.genreNames(MediaKind.MOVIE) }
                    val seriesRequest = async { TmdbRepository.genreNames(MediaKind.SERIES) }
                    moviesRequest.await() to seriesRequest.await()
                }
                movieGenres = movieGenreNames
                seriesGenres = seriesGenreNames
                val (movies, series) = coroutineScope {
                    val moviesRequest = async { TmdbRepository.popularMovies() }
                    val seriesRequest = async { TmdbRepository.popularSeries() }
                    moviesRequest.await() to seriesRequest.await()
                }
                movieCatalog = movies.ifEmpty { movieCatalog }
                seriesCatalog = series.ifEmpty { seriesCatalog }
                statusMessage = "Catálogo y géneros obtenidos de TMDb."
            } catch (_: Exception) {
                statusMessage = "No se pudo conectar con TMDb; se muestra el catálogo de demostración."
            } finally {
                isLoading = false
            }
        }
    }
}

class FavoritesViewModel : ViewModel() {
    var savedIds by mutableStateOf(setOf("m-azul", "ch-cultura"))
        private set

    fun toggle(id: String) {
        savedIds = if (id in savedIds) savedIds - id else savedIds + id
    }
}

class SettingsViewModel : ViewModel() {
    var autoPlay by mutableStateOf(true)
    var skipIntro by mutableStateOf(false)
    var pushNotifications by mutableStateOf(true)
    var newEpisodes by mutableStateOf(true)
    var selectedQuality by mutableStateOf("Auto")
}

class SearchViewModel : ViewModel() {
    private val queryState = mutableStateOf("")
    private var searchJob: Job? = null
    private var remoteResults by mutableStateOf<List<MediaItem>?>(null)

    var query: String
        get() = queryState.value
        set(value) {
            queryState.value = value
            searchJob?.cancel()
            remoteResults = null
            if (value.trim().length < 2 || !TmdbRepository.isConfigured) return
            searchJob = viewModelScope.launch {
                delay(350)
                isLoading = true
                try {
                    remoteResults = TmdbRepository.search(value)
                    statusMessage = "Resultados de películas y series obtenidos de TMDb."
                } catch (_: Exception) {
                    statusMessage = "No se pudo buscar en TMDb; se muestran coincidencias locales."
                } finally {
                    isLoading = false
                }
            }
        }

    var isLoading by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf(configurationMessage())
        private set

    val mediaResults: List<MediaItem>
        get() = remoteResults ?: DemoCatalog.media.filter {
            it.title.contains(query, ignoreCase = true) || it.genres.any { genre -> genre.contains(query, true) }
        }

    val channelResults: List<LiveChannel>
        get() = DemoCatalog.channels.filter {
            it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
        }
}

class DownloadsViewModel : ViewModel() {
    val sampleItems = DemoCatalog.media.take(2)
}

class HistoryViewModel : ViewModel() {
    val recentlyWatched = DemoCatalog.media.take(4)
    var historyCleared by mutableStateOf(false)
        private set

    fun clear() {
        historyCleared = true
    }
}

class PlayerViewModel : ViewModel() {
    var selectedQuality by mutableStateOf("Auto")
    var subtitlesEnabled by mutableStateOf(false)
    var audioLanguage by mutableStateOf("Español")
}

private fun configurationMessage(): String = if (TmdbRepository.isConfigured) {
    "Conectando con TMDb…"
} else {
    "Configura tu clave TMDb local para cargar fichas y pósteres reales."
}
