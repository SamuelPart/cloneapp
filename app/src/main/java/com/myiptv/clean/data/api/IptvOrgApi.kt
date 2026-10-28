package com.myiptv.clean.data.api

import com.myiptv.clean.data.model.IptvOrgChannel
import com.myiptv.clean.data.model.IptvOrgStream
import retrofit2.http.GET

interface IptvOrgApi {
    @GET("api/channels.json")
    suspend fun channels(): List<IptvOrgChannel>

    @GET("api/streams.json")
    suspend fun streams(): List<IptvOrgStream>
}
