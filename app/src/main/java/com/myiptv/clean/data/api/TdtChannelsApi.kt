package com.myiptv.clean.data.api

import okhttp3.ResponseBody
import retrofit2.http.GET

interface TdtChannelsApi {
    @GET("lists/tv.m3u8")
    suspend fun televisionPlaylist(): ResponseBody
}
