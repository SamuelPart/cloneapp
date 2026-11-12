package com.myiptv.clean.data.repository

import android.content.Context
import com.myiptv.clean.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

internal data class ShortsComment(
    val id: String,
    val body: String,
    val createdAt: String,
    val displayName: String? = null,
    val username: String? = null,
    val avatarUrl: String? = null
)

internal data class ShortsInteractionState(
    val liked: Boolean = false,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val comments: List<ShortsComment> = emptyList()
)

/** Supabase-backed public Shorts actions; the client id is anonymous and scoped to this install. */
internal class ShortsInteractionsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val clientId = preferences.getString(CLIENT_ID, null) ?: UUID.randomUUID().toString().also {
        preferences.edit().putString(CLIENT_ID, it).apply()
    }
    private val clientToken = preferences.getString(CLIENT_TOKEN, null) ?: UUID.randomUUID().toString().also {
        preferences.edit().putString(CLIENT_TOKEN, it).apply()
    }

    suspend fun load(clipId: String): ShortsInteractionState = coroutineScope {
        val likes = async(Dispatchers.IO) { countRows("shorts_reactions", clipId) }
        val liked = async(Dispatchers.IO) { hasReaction(clipId) }
        val comments = async(Dispatchers.IO) { loadComments(clipId) }
        val shares = async(Dispatchers.IO) { countRows("shorts_shares", clipId) }
        val commentsResult = comments.await()
        ShortsInteractionState(
            liked = liked.await(),
            likeCount = likes.await(),
            commentCount = commentsResult.second,
            shareCount = shares.await(),
            comments = commentsResult.first
        )
    }

    suspend fun setLiked(clipId: String, liked: Boolean): Unit = withContext(Dispatchers.IO) {
        requestFunction(
            JSONObject()
                .put("action", "reaction")
                .put("client_id", clientId)
                .put("client_token", clientToken)
                .put("clip_id", clipId)
                .put("liked", liked)
        )
        Unit
    }

    suspend fun addComment(clipId: String, text: String): Unit = withContext(Dispatchers.IO) {
        val cleanText = text.trim().take(MAX_COMMENT_LENGTH)
        require(cleanText.isNotEmpty()) { "El comentario no puede estar vacío." }
        requestFunction(
            JSONObject()
                .put("action", "anonymous_comment")
                .put("client_id", clientId)
                .put("client_token", clientToken)
                .put("clip_id", clipId)
                .put("body", cleanText)
        )
        Unit
    }

    suspend fun recordShare(clipId: String): Unit = withContext(Dispatchers.IO) {
        requestFunction(
            JSONObject()
                .put("action", "share")
                .put("client_id", clientId)
                .put("client_token", clientToken)
                .put("clip_id", clipId)
        )
        Unit
    }

    private fun countRows(table: String, clipId: String): Int {
        val response = request(
            method = "GET",
            path = "$table?select=clip_id&clip_id=eq.${queryValue(clipId)}",
            prefer = "count=exact",
            range = "0-0"
        )
        val total = response.contentRange?.substringAfterLast('/')?.toIntOrNull()
        return total ?: JSONArray(response.body).length()
    }

    private fun hasReaction(clipId: String): Boolean {
        val response = request(
            method = "GET",
            path = "shorts_reactions?select=client_id&clip_id=eq.${queryValue(clipId)}&client_id=eq.${queryValue(clientId)}",
            range = "0-0"
        )
        return JSONArray(response.body).length() > 0
    }

    private fun loadComments(clipId: String): Pair<List<ShortsComment>, Int> {
        val response = request(
            method = "GET",
            path = "shorts_comments?select=id,body,created_at,profile:guest_profiles!shorts_comments_profile_client_id_fkey(client_id,display_name,username,avatar_url)&clip_id=eq.${queryValue(clipId)}&order=created_at.desc",
            prefer = "count=exact",
            range = "0-49"
        )
        val rows = JSONArray(response.body)
        val comments = (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            val profile = row.optJSONObject("profile")
            ShortsComment(
                id = row.optString("id"),
                body = row.optString("body"),
                createdAt = row.optString("created_at"),
                displayName = profile?.optString("display_name")?.takeIf { it.isNotBlank() },
                username = profile?.optString("username")?.takeIf { it.isNotBlank() },
                avatarUrl = profile?.optString("avatar_url")?.takeIf { it.isNotBlank() }
            )
        }
        val total = response.contentRange?.substringAfterLast('/')?.toIntOrNull() ?: comments.size
        return comments to total
    }

    private fun requestFunction(body: JSONObject) {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val key = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        if (baseUrl.isBlank() || key.isBlank()) throw IOException("Supabase no está configurado.")
        val connection = (URL("$baseUrl/functions/v1/guest-profiles").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = REQUEST_TIMEOUT_MS
            readTimeout = REQUEST_TIMEOUT_MS
            setRequestProperty("apikey", key)
            setRequestProperty("Authorization", "Bearer $key")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            doOutput = true
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw IOException("Supabase rechazó la acción ($status): ${responseBody.take(MAX_ERROR_LENGTH)}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun request(
        method: String,
        path: String,
        body: String? = null,
        prefer: String? = null,
        range: String? = null
    ): HttpResponse {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val key = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        if (baseUrl.isBlank() || key.isBlank()) throw IOException("Supabase no está configurado.")
        val connection = (URL("$baseUrl/rest/v1/$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = REQUEST_TIMEOUT_MS
            readTimeout = REQUEST_TIMEOUT_MS
            setRequestProperty("apikey", key)
            setRequestProperty("Authorization", "Bearer $key")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            prefer?.let { setRequestProperty("Prefer", it) }
            range?.let {
                setRequestProperty("Range-Unit", "items")
                setRequestProperty("Range", it)
            }
            if (body != null) doOutput = true
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw IOException("Supabase rechazó la acción ($status): ${responseBody.take(MAX_ERROR_LENGTH)}")
            }
            return HttpResponse(responseBody, connection.getHeaderField("Content-Range"))
        } finally {
            connection.disconnect()
        }
    }

    private fun queryValue(value: String): String = URLEncoder.encode(value, "UTF-8")

    private data class HttpResponse(val body: String, val contentRange: String?)

    private companion object {
        const val PREFERENCES = "samplay_shorts_social"
        const val CLIENT_ID = "anonymous_client_id"
        const val CLIENT_TOKEN = "anonymous_client_token"
        const val MAX_COMMENT_LENGTH = 500
        const val MAX_ERROR_LENGTH = 300
        const val REQUEST_TIMEOUT_MS = 12_000
    }
}
