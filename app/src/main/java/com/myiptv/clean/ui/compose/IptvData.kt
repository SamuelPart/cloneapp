package com.myiptv.clean.ui.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.ui.graphics.vector.ImageVector

enum class MediaKind { MOVIE, SERIES }
enum class MediaPlaybackType { DIRECT, IFRAME }

data class MediaItem(
    val id: String,
    val title: String,
    val kind: MediaKind,
    val year: Int,
    val rating: Double,
    val genres: List<String>,
    val synopsis: String,
    val duration: String,
    val colorA: Long,
    val colorB: Long,
    val streamUrl: String? = null,
    val playbackType: MediaPlaybackType = MediaPlaybackType.DIRECT,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val tmdbId: Long? = null,
    val tmdbMediaType: String? = null,
    val castNames: List<String> = emptyList(),
    val seasonCount: Int? = null,
    val episodeCount: Int? = null
)

data class MediaEpisode(
    val number: Int,
    val title: String,
    val duration: String,
    val synopsis: String,
    val stillUrl: String? = null
)

data class LiveChannel(
    val id: String,
    val name: String,
    val category: String,
    val logoLetters: String,
    val streamUrl: String,
    val accent: Long,
    val logoUrl: String? = null,
    val epgId: String? = null,
    val sourceName: String = "TDTChannels",
    val userAgent: String? = null,
    val referrer: String? = null
)

data class ContentCategory(
    val title: String,
    val icon: ImageVector,
    val accent: Long
)

object ContentCategories {
    val items = listOf(
        ContentCategory("Películas", Icons.Filled.Movie, 0xFF8E5CF7L),
        ContentCategory("Series", Icons.Filled.Theaters, 0xFF34B6A5L),
        ContentCategory("Televisión", Icons.Filled.LiveTv, 0xFFE45773L)
    )
}
