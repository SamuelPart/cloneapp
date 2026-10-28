package com.myiptv.clean.data.api

import com.myiptv.clean.config.ConfigManager
import com.myiptv.clean.data.model.Channel
import com.myiptv.clean.data.model.EpgProgram
import com.myiptv.clean.data.model.Movie
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

class ApiService(private val configManager: ConfigManager) {

    private val retrofit: Retrofit by lazy {
        val baseUrl = configManager.loadConfig().portalMain.ensureTrailingSlash()
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val channelApi: ChannelApi by lazy { retrofit.create(ChannelApi::class.java) }
    val movieApi: MovieApi by lazy { retrofit.create(MovieApi::class.java) }
    val epgApi: EpgApi by lazy { retrofit.create(EpgApi::class.java) }

    private fun String.ensureTrailingSlash(): String =
        if (endsWith('/')) this else "$this/"
}

interface ChannelApi {
    @GET("channels")
    suspend fun getChannels(): Response<List<Channel>>

    @GET("channels/{id}")
    suspend fun getChannel(@Path("id") id: String): Response<Channel>

    @GET("channels/category/{category}")
    suspend fun getChannelsByCategory(
        @Path("category") category: String
    ): Response<List<Channel>>
}

interface MovieApi {
    @GET("movies")
    suspend fun getMovies(): Response<List<Movie>>

    @GET("movies/{id}")
    suspend fun getMovie(@Path("id") id: String): Response<Movie>

    @GET("movies/category/{category}")
    suspend fun getMoviesByCategory(
        @Path("category") category: String
    ): Response<List<Movie>>

    @GET("search")
    suspend fun search(@Query("q") query: String): Response<List<Movie>>
}

interface EpgApi {
    @GET("epg/channel/{channelId}")
    suspend fun getEpg(
        @Path("channelId") channelId: String
    ): Response<List<EpgProgram>>

    @GET("epg/channel/{channelId}/day/{day}")
    suspend fun getEpgByDay(
        @Path("channelId") channelId: String,
        @Path("day") day: String
    ): Response<List<EpgProgram>>
}
