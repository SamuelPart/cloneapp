package com.myiptv.clean.ui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myiptv.clean.data.repository.ChannelCatalogRepository
import com.myiptv.clean.data.repository.TmdbRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Separate phone state keeps Android TV's search screen and behavior unchanged. */
internal interface PhoneSearchSources {
    val tmdbConfigured: Boolean
    fun cachedChannels(): List<LiveChannel>
    suspend fun refreshChannels(): List<LiveChannel>
    suspend fun popularMovies(): List<MediaItem>
    suspend fun popularSeries(): List<MediaItem>
    suspend fun search(query: String): List<MediaItem>
}

private object SamplayPhoneSearchSources : PhoneSearchSources {
    override val tmdbConfigured: Boolean get() = TmdbRepository.isConfigured
    override fun cachedChannels(): List<LiveChannel> = ChannelCatalogRepository.cachedChannels()
    override suspend fun refreshChannels(): List<LiveChannel> = ChannelCatalogRepository.refresh()
    override suspend fun popularMovies(): List<MediaItem> = TmdbRepository.popularMovies()
    override suspend fun popularSeries(): List<MediaItem> = TmdbRepository.popularSeries()
    override suspend fun search(query: String): List<MediaItem> = TmdbRepository.search(query)
}

internal class PhoneSearchViewModel(
    private val sources: PhoneSearchSources = SamplayPhoneSearchSources,
    private val debounceMs: Long = 350L
) : ViewModel() {
    var query by mutableStateOf("")
        private set
    var remoteResults by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    var searchPending by mutableStateOf(false)
        private set
    var searchError by mutableStateOf<String?>(null)
        private set
    var channels by mutableStateOf(sources.cachedChannels())
        private set
    var channelsLoading by mutableStateOf(false)
        private set
    var popularMedia by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    var popularLoading by mutableStateOf(false)
        private set
    var popularError by mutableStateOf<String?>(null)
        private set

    val canRefreshPopular: Boolean get() = sources.tmdbConfigured

    private var searchJob: Job? = null
    private var popularJob: Job? = null
    private var searchVersion = 0L

    init {
        loadChannels()
        refreshPopular()
    }

    fun updateQuery(value: String) {
        if (value == query) return
        query = value
        searchVersion++
        val requestVersion = searchVersion
        searchJob?.cancel()
        remoteResults = emptyList()
        searchError = null
        searchPending = false
        val term = value.trim()
        // The local M3U and channels can still be searched offline or with a single letter.
        if (term.length < 2 || !sources.tmdbConfigured) return
        searchPending = true
        searchJob = viewModelScope.launch {
            try {
                delay(debounceMs)
                val found = sources.search(term)
                if (requestVersion == searchVersion) remoteResults = found
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (requestVersion == searchVersion) searchError = "No se pudo consultar TMDb."
            } finally {
                if (requestVersion == searchVersion) searchPending = false
            }
        }
    }

    /** Retry real TMDb lists without hiding the M3U fallback if the service is unavailable. */
    fun refreshPopular() {
        if (!sources.tmdbConfigured || popularJob?.isActive == true) return
        popularLoading = true
        popularError = null
        popularJob = viewModelScope.launch {
            try {
                val (movies, series) = coroutineScope {
                    val moviesRequest = async { sources.popularMovies() }
                    val seriesRequest = async { sources.popularSeries() }
                    moviesRequest.await() to seriesRequest.await()
                }
                popularMedia = phonePopularTitles(movies, series)
                if (popularMedia.isEmpty()) popularError = "TMDb no devolvió títulos populares."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Retain the last confirmed popular list if a retry fails.
                popularError = "No se pudieron actualizar los títulos populares de TMDb."
            } finally {
                popularLoading = false
            }
        }
    }

    private fun loadChannels() {
        channelsLoading = true
        viewModelScope.launch {
            try {
                channels = sources.refreshChannels()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                channels = sources.cachedChannels()
            } finally {
                channelsLoading = false
            }
        }
    }
}
