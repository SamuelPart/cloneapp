package com.myiptv.clean.data.model

data class Channel(
    val id: String,
    val name: String,
    val logo: String,
    val url: String,
    val category: String,
    val isFavorite: Boolean = false
)

data class Movie(
    val id: String,
    val title: String,
    val poster: String,
    val backdrop: String,
    val overview: String,
    val rating: Double,
    val releaseDate: String,
    val genres: List<String>,
    val videoUrl: String? = null,
    val duration: Int? = null
)

data class EpgProgram(
    val id: String,
    val channelId: String,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val description: String,
    val category: String
)

data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val itemCount: Int
)

data class Favorite(
    val id: String,
    val type: String, // "channel" or "movie"
    val title: String,
    val poster: String,
    val url: String,
    val addedAt: Long = System.currentTimeMillis()
)
