package com.myiptv.clean.ui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    val featured = DemoCatalog.media.take(5)
    val trending = DemoCatalog.media.sortedByDescending(MediaItem::rating).take(6)
    val recommended = DemoCatalog.media.drop(2)
    val liveChannels = DemoCatalog.channels
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
    var query by mutableStateOf("")
    var selectedGenre by mutableStateOf("Todos")
    var selectedTab by mutableStateOf("Populares")

    val filteredMovies: List<MediaItem>
        get() {
            val filtered = DemoCatalog.media.filter { item ->
                (selectedGenre == "Todos" || item.genres.any { it.equals(selectedGenre, true) }) &&
                    item.title.contains(query, ignoreCase = true)
            }
            return when (selectedTab) {
                "Mejor valoradas" -> filtered.sortedByDescending(MediaItem::rating)
                "Recientes" -> filtered.sortedByDescending(MediaItem::year)
                else -> filtered
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
    var query by mutableStateOf("")

    val mediaResults: List<MediaItem>
        get() = DemoCatalog.media.filter { it.title.contains(query, ignoreCase = true) || it.genres.any { genre -> genre.contains(query, true) } }

    val channelResults: List<LiveChannel>
        get() = DemoCatalog.channels.filter { it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) }
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
