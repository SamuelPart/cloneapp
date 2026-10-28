package com.myiptv.clean.data.model

import com.google.gson.annotations.SerializedName

data class TmdbPageResponse(
    val results: List<TmdbMediaResponse>? = null,
    @SerializedName("total_pages") val totalPages: Int? = null
)

data class TmdbGenreListResponse(
    val genres: List<TmdbGenreResponse>? = null
)

data class TmdbGenreResponse(
    val id: Int = 0,
    val name: String? = null
)

data class TmdbMediaResponse(
    val id: Long = 0L,
    @SerializedName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    @SerializedName("original_title") val originalTitle: String? = null,
    @SerializedName("original_name") val originalName: String? = null,
    val overview: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("vote_average") val voteAverage: Double? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    @SerializedName("genre_ids") val genreIds: List<Int>? = null,
    val genres: List<TmdbGenreResponse>? = null,
    val runtime: Int? = null,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>? = null,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int? = null,
    val credits: TmdbCreditsResponse? = null
)

data class TmdbCreditsResponse(
    val cast: List<TmdbCastResponse>? = null
)

data class TmdbCastResponse(
    val name: String? = null,
    @SerializedName("profile_path") val profilePath: String? = null
)

data class TmdbSeasonResponse(
    val episodes: List<TmdbEpisodeResponse>? = null
)

data class TmdbEpisodeResponse(
    val id: Long = 0L,
    val name: String? = null,
    val overview: String? = null,
    @SerializedName("episode_number") val episodeNumber: Int = 0,
    @SerializedName("still_path") val stillPath: String? = null,
    val runtime: Int? = null
)
