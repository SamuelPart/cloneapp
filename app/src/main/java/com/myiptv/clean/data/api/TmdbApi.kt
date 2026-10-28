package com.myiptv.clean.data.api

import com.myiptv.clean.data.model.TmdbGenreListResponse
import com.myiptv.clean.data.model.TmdbMediaResponse
import com.myiptv.clean.data.model.TmdbPageResponse
import com.myiptv.clean.data.model.TmdbSeasonResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    @GET("movie/popular")
    suspend fun popularMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String,
        @Query("page") page: Int
    ): TmdbPageResponse

    @GET("tv/popular")
    suspend fun popularSeries(
        @Query("api_key") apiKey: String,
        @Query("language") language: String,
        @Query("page") page: Int
    ): TmdbPageResponse

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String,
        @Query("with_genres") genreId: Int,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1
    ): TmdbPageResponse

    @GET("discover/tv")
    suspend fun discoverSeries(
        @Query("api_key") apiKey: String,
        @Query("language") language: String,
        @Query("with_genres") genreId: Int,
        @Query("with_origin_country") originCountry: String? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1
    ): TmdbPageResponse

    @GET("genre/movie/list")
    suspend fun movieGenres(
        @Query("api_key") apiKey: String,
        @Query("language") language: String
    ): TmdbGenreListResponse

    @GET("genre/tv/list")
    suspend fun seriesGenres(
        @Query("api_key") apiKey: String,
        @Query("language") language: String
    ): TmdbGenreListResponse

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("language") language: String,
        @Query("query") query: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1
    ): TmdbPageResponse

    @GET("movie/{movie_id}")
    suspend fun movieDetails(
        @Path("movie_id") movieId: Long,
        @Query("api_key") apiKey: String,
        @Query("language") language: String,
        @Query("append_to_response") appendToResponse: String = "credits"
    ): TmdbMediaResponse

    @GET("tv/{series_id}")
    suspend fun seriesDetails(
        @Path("series_id") seriesId: Long,
        @Query("api_key") apiKey: String,
        @Query("language") language: String,
        @Query("append_to_response") appendToResponse: String = "credits"
    ): TmdbMediaResponse

    @GET("tv/{series_id}/season/{season_number}")
    suspend fun seasonDetails(
        @Path("series_id") seriesId: Long,
        @Path("season_number") seasonNumber: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String
    ): TmdbSeasonResponse
}
