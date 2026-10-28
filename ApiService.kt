package com.myiptv.clean.data.api

import com.myiptv.clean.config.ConfigManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

class ApiService(private val configManager: ConfigManager) {
    
    private val retrofit: Retrofit by lazy {
        val config = configManager.loadConfig()
        Retrofit.Builder()
            .baseUrl(config.portalMain)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    val channelApi: ChannelApi by lazy {
        retrofit.create(ChannelApi::class.java)
    }
    
    val movieApi: MovieApi by lazy {
        retrofit.create(MovieApi::class.java)
    }
    
    val epgApi: EpgApi by lazy {
        retrofit.create(EpgApi::class.java)
    }
}

interface ChannelApi {
    @GET("channels")
    suspend fun getChannels(): retrofit2.Response<List<Channel>>
    
    @GET("channels/{id}")
    suspend fun getChannel(@Path("id") id: String): retrofit2.Response<Channel>
    
    @GET("channels/category/{category}")
    suspend fun getChannelsByCategory(@Path("category") category: String): retrofit2.Response<List<Channel>>
}

interface MovieApi {
    @GET("movies")
    suspend fun getMovies(): retrofit2.Response<List<Movie>>
    
    @GET("movies/{id}")
    suspend fun getMovie(@Path("id") id: String): retrofit2.Response<Movie>
    
    @GET("movies/category/{category}")
    suspend fun getMoviesByCategory(@Path("category") category: String): retrofit2.Response<List<Movie>>
    
    @GET("search")
    suspend fun search(@Query("q") query: String): retrofit2.Response<List<Movie>>
}

interface EpgApi {
    @GET("epg/channel/{channelId}")
    suspend fun getEpg(@Path("channelId") channelId: String): retrofit2.Response<List<EpgProgram>>
    
    @GET("epg/channel/{channelId}/day/{day}")
    suspend fun getEpgByDay(@Path("channelId") channelId: String, @Path("day") day: String): retrofit2.Response<List<EpgProgram>>
}
