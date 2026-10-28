package com.myiptv.clean.data.model

import com.google.gson.annotations.SerializedName

data class IptvOrgChannel(
    val id: String? = null,
    val name: String? = null,
    val country: String? = null,
    val categories: List<String>? = null,
    @SerializedName("is_nsfw") val isNsfw: Boolean? = null,
    val closed: String? = null
)

data class IptvOrgStream(
    val channel: String? = null,
    val feed: String? = null,
    val title: String? = null,
    val url: String? = null,
    val referrer: String? = null,
    @SerializedName("user_agent") val userAgent: String? = null,
    val labels: List<String>? = null
)
