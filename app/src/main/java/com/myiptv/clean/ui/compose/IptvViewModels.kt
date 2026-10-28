package com.myiptv.clean.ui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myiptv.clean.data.repository.ChannelCatalogRepository
import com.myiptv.clean.data.repository.TmdbRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TMDB_DOCUMENTARY_GENRE = 99

class HomeViewModel : ViewModel() {
    var featured by mutableStateOf<MediaItem?>(null)
        private set
    var trending by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    var recommended by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    var weeklyTrends by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    var documentaries by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    var liveChannels by mutableStateOf<List<LiveChannel>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf("Cargando fuentes…")
        private set

    private var mediaStatus = if (TmdbRepository.isConfigured) "Consultando catálogo TMDb…" else "TMDb requiere una clave API local."
    private var channelStatus = "Consultando canales de televisión…"
    private var tvShelvesLoaded = false
    private var tvShelvesJob: Job? = null
    private var documentaryMovies: List<MediaItem> = emptyList()
    private var documentarySeries: List<MediaItem> = emptyList()

    init {
        loadChannels()
        loadCatalog()
    }

    private fun loadChannels() {
        viewModelScope.launch {
            try {
                liveChannels = ChannelCatalogRepository.refresh()
                channelStatus = ChannelCatalogRepository.statusMessage
            } catch (_: Exception) {
                channelStatus = ChannelCatalogRepository.statusMessage
            }
            updateStatus()
        }
    }

    private fun loadCatalog() {
        if (!TmdbRepository.isConfigured) {
            updateStatus()
            return
        }
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
                featured = movies.firstOrNull() ?: series.firstOrNull()
                trending = movies.take(12)
                recommended = series.take(12)
                mediaStatus = if (all.isEmpty()) "TMDb no devolvió títulos." else "Metadatos e imágenes cargados desde TMDb."
            } catch (_: Exception) {
                mediaStatus = "No se pudo obtener el catálogo desde TMDb."
            } finally {
                isLoading = false
                updateStatus()
            }
        }
    }

    /** Optional shelves use TMDb data only and never block the main catalog or live channels. */
    fun loadTvShelves() {
        if (!TmdbRepository.isConfigured || tvShelvesLoaded || tvShelvesJob?.isActive == true) return
        tvShelvesJob = viewModelScope.launch {
            val result = loadTvShelfData(
                loadTrends = { TmdbRepository.trendingThisWeek() },
                loadDocumentaryMovies = { TmdbRepository.discoverMovies(TMDB_DOCUMENTARY_GENRE) },
                loadDocumentarySeries = { TmdbRepository.discoverSeries(TMDB_DOCUMENTARY_GENRE) }
            )
            result.trends?.let { weeklyTrends = it.distinctBy(MediaItem::id).take(12) }
            result.documentaryMovies?.let { documentaryMovies = it }
            result.documentarySeries?.let { documentarySeries = it }
            documentaries = homeGridCatalog(documentaryMovies, documentarySeries, MediaItem::id)
            // Keep earlier successful results and retry failed shelves when home is reopened.
            tvShelvesLoaded = result.allSucceeded
        }
    }

    private fun updateStatus() {
        statusMessage = "$mediaStatus · $channelStatus"
    }
}

class ChannelsViewModel : ViewModel() {
    var query by mutableStateOf("")
    var selectedCategory by mutableStateOf("Todos")
    var channels by mutableStateOf<List<LiveChannel>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf("")
        private set

    val categories: List<String>
        get() = listOf("Todos") + channels.map(LiveChannel::category).distinct().sorted()

    val filteredChannels: List<LiveChannel>
        get() = channels.filter { channel ->
            (selectedCategory == "Todos" || channel.category == selectedCategory) &&
                (query.isBlank() || channel.name.contains(query, ignoreCase = true) || channel.category.contains(query, ignoreCase = true))
        }

    init {
        load(force = false)
    }

    fun refresh() = load(force = true)

    private fun load(force: Boolean) {
        if (isLoading) return
        isLoading = true
        viewModelScope.launch {
            try {
                channels = ChannelCatalogRepository.refresh(force = force)
                errorMessage = ChannelCatalogRepository.statusMessage.takeIf { it.startsWith("No se pudieron actualizar") }.orEmpty()
            } catch (_: Exception) {
                channels = ChannelCatalogRepository.cachedChannels()
                errorMessage = ChannelCatalogRepository.statusMessage
            } finally {
                isLoading = false
            }
        }
    }
}

class MoviesViewModel : ViewModel() {
    private var movieCatalog by mutableStateOf<List<MediaItem>>(emptyList())
    private var seriesCatalog by mutableStateOf<List<MediaItem>>(emptyList())
    private var remoteSearchResults by mutableStateOf<List<MediaItem>?>(null)
    private val queryState = mutableStateOf("")
    private var searchJob: Job? = null

    var query: String
        get() = queryState.value
        set(value) {
            queryState.value = value
            searchJob?.cancel()
            remoteSearchResults = null
            if (value.trim().length < 2) return
            if (!TmdbRepository.isConfigured) {
                statusMessage = "Configura una clave TMDb local para buscar títulos."
                return
            }
            searchJob = viewModelScope.launch {
                delay(350)
                try {
                    remoteSearchResults = TmdbRepository.search(value)
                    statusMessage = if (remoteSearchResults.isNullOrEmpty()) "TMDb no encontró coincidencias."
                    else "Resultados cargados desde TMDb."
                } catch (_: Exception) {
                    statusMessage = "No se pudo completar la búsqueda en TMDb."
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
    var statusMessage by mutableStateOf(catalogMessage())
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
        if (!TmdbRepository.isConfigured) {
            statusMessage = "Configura una clave TMDb local para cargar películas y series."
            return
        }
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
                movieCatalog = movies
                seriesCatalog = series
                statusMessage = if (movies.isEmpty() && series.isEmpty()) "TMDb no devolvió títulos." else "Catálogo actualizado desde TMDb."
            } catch (_: Exception) {
                statusMessage = "No se pudo cargar el catálogo de TMDb."
            } finally {
                isLoading = false
            }
        }
    }
}

class FavoritesViewModel : ViewModel() {
    var savedIds by mutableStateOf(emptySet<String>())
        private set

    fun toggle(id: String) {
        savedIds = if (id in savedIds) savedIds - id else savedIds + id
    }
}

class SearchViewModel : ViewModel() {
    private val queryState = mutableStateOf("")
    private var searchJob: Job? = null
    private var remoteResults by mutableStateOf<List<MediaItem>?>(null)
    var featuredMedia by mutableStateOf(TmdbRepository.cachedCatalog())
        private set
    var channels by mutableStateOf<List<LiveChannel>>(ChannelCatalogRepository.cachedChannels())
        private set

    var query: String
        get() = queryState.value
        set(value) {
            queryState.value = value
            searchJob?.cancel()
            remoteResults = null
            if (value.trim().length < 2) return
            if (!TmdbRepository.isConfigured) {
                statusMessage = "Configura una clave TMDb local para buscar películas y series."
                return
            }
            searchJob = viewModelScope.launch {
                delay(350)
                isLoading = true
                try {
                    remoteResults = TmdbRepository.search(value)
                    statusMessage = if (remoteResults.isNullOrEmpty()) "TMDb no encontró coincidencias."
                    else "Resultados cargados desde TMDb."
                } catch (_: Exception) {
                    statusMessage = "No se pudo completar la búsqueda en TMDb."
                } finally {
                    isLoading = false
                }
            }
        }

    var isLoading by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf(catalogMessage())
        private set

    val mediaResults: List<MediaItem>
        get() = remoteResults.orEmpty()

    val channelResults: List<LiveChannel>
        get() = channels.filter {
            query.isBlank() || it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
        }

    init {
        viewModelScope.launch {
            try {
                channels = ChannelCatalogRepository.refresh()
            } catch (_: Exception) {
                channels = ChannelCatalogRepository.cachedChannels()
            }
        }
        if (TmdbRepository.isConfigured) {
            viewModelScope.launch {
                try {
                    featuredMedia = TmdbRepository.popularMovies() + TmdbRepository.popularSeries()
                    statusMessage = if (featuredMedia.isEmpty()) "TMDb no devolvió títulos." else "Metadatos disponibles desde TMDb."
                } catch (_: Exception) {
                    featuredMedia = TmdbRepository.cachedCatalog()
                    if (featuredMedia.isEmpty()) statusMessage = "No se pudo cargar el catálogo de TMDb."
                }
            }
        }
    }
}

private fun catalogMessage(): String = if (TmdbRepository.isConfigured) {
    "Catálogo conectado a TMDb."
} else {
    "TMDb sin configurar: agrega la clave local para cargar películas y series."
}
