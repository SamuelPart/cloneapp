package com.myiptv.clean.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.myiptv.clean.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

internal data class GuestProfile(
    val clientId: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String?,
    val coverUrl: String?,
    val coverAnimated: Boolean
)

internal class GuestProfileRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val clientId = preferences.getString(CLIENT_ID, null) ?: UUID.randomUUID().toString().also {
        preferences.edit().putString(CLIENT_ID, it).apply()
    }
    private val ownerToken = preferences.getString(OWNER_TOKEN, null) ?: UUID.randomUUID().toString().also {
        preferences.edit().putString(OWNER_TOKEN, it).apply()
    }

    suspend fun myProfile(): GuestProfile? = withContext(Dispatchers.IO) {
        val username = preferences.getString(USERNAME, null) ?: return@withContext null
        runCatching { find(username) }.getOrNull()
    }

    suspend fun find(username: String): GuestProfile = withContext(Dispatchers.IO) {
        val cleanUsername = normalizeUsername(username)
        parseProfile(request("GET", "?username=${queryValue(cleanUsername)}"))
    }

    suspend fun publishComment(clipId: String, text: String): Unit = withContext(Dispatchers.IO) {
        require(preferences.getString(USERNAME, null) != null) { "Guarda tu perfil para comentar con tu nombre y foto." }
        val cleanText = text.trim().take(MAX_COMMENT_LENGTH)
        require(cleanText.isNotEmpty()) { "El comentario no puede estar vacío." }
        request(
            "POST", "",
            JSONObject()
                .put("action", "comment")
                .put("client_id", clientId)
                .put("owner_token", ownerToken)
                .put("clip_id", clipId)
                .put("body", cleanText)
                .toString()
        )
        Unit
    }

    suspend fun save(
        displayName: String,
        username: String,
        avatarUri: Uri?,
        coverUri: Uri?,
        removeAvatar: Boolean,
        removeCover: Boolean,
        coverAnimated: Boolean
    ): GuestProfile = withContext(Dispatchers.IO) {
        val cleanName = displayName.trim()
        val cleanUsername = normalizeUsername(username)
        require(cleanName.length in 1..40) { "El nombre debe tener entre 1 y 40 caracteres." }
        val body = JSONObject()
            .put("client_id", clientId)
            .put("owner_token", ownerToken)
            .put("display_name", cleanName)
            .put("username", cleanUsername)
            .put("cover_animated", coverAnimated)
            .put("remove_avatar", removeAvatar)
            .put("remove_cover", removeCover)
        avatarUri?.let { appendMedia(body, "avatar", it) }
        coverUri?.let { appendMedia(body, "cover", it) }
        val profile = parseProfile(request("POST", "", body.toString(), retryOn503 = true))
        preferences.edit().putString(USERNAME, profile.username).apply()
        profile
    }

    private fun appendMedia(body: JSONObject, field: String, uri: Uri) {
        val mime = appContext.contentResolver.getType(uri)?.lowercase().orEmpty()
        val allowed = if (field == "avatar") setOf("image/jpeg", "image/png", "image/webp")
            else setOf("image/jpeg", "image/png", "image/webp", "image/gif")
        require(mime in allowed) {
            if (field == "avatar") "El avatar debe ser una foto JPG, PNG o WebP estática."
            else "La portada debe ser una foto JPG, PNG, WebP o GIF."
        }
        val bytes = appContext.contentResolver.openInputStream(uri)?.use { stream ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                total += count
                require(total <= MAX_IMAGE_BYTES) { "La imagen supera el máximo de 3 MB." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: throw IOException("No se pudo leer la imagen seleccionada.")
        body.put("${field}_mime", mime)
            .put("${field}_base64", Base64.encodeToString(bytes, Base64.NO_WRAP))
    }

    private fun request(method: String, suffix: String, body: String? = null, retryOn503: Boolean = method == "GET"): String {
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val key = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        if (baseUrl.isBlank() || key.isBlank()) throw IOException("Supabase no está configurado.")
        val retryLimit = if (retryOn503) MAX_503_RETRIES else 0
        for (attempt in 0..retryLimit) {
            val connection = (URL("$baseUrl/functions/v1/guest-profiles$suffix").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = REQUEST_TIMEOUT_MS
                readTimeout = REQUEST_TIMEOUT_MS
                setRequestProperty("apikey", key)
                setRequestProperty("Authorization", "Bearer $key")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (body != null) doOutput = true
            }
            try {
                if (body != null) connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (status == 503 && retryOn503 && attempt < retryLimit) {
                    Thread.sleep(500L * (attempt + 1))
                    continue
                }
                if (status !in 200..299) {
                    val detail = runCatching { JSONObject(responseBody).optString("error") }.getOrDefault("")
                    throw IOException(detail.ifBlank { "No se pudo cargar el perfil ($status)." })
                }
                return responseBody
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("El servicio de perfiles no está disponible. Inténtalo de nuevo en unos segundos.")
    }

    private fun parseProfile(json: String) = JSONObject(json).let {
        GuestProfile(
            clientId = it.getString("client_id"),
            displayName = it.getString("display_name"),
            username = it.getString("username"),
            avatarUrl = it.optString("avatar_url").takeIf { url -> url.isNotBlank() },
            coverUrl = it.optString("cover_url").takeIf { url -> url.isNotBlank() },
            coverAnimated = it.optBoolean("cover_animated")
        )
    }

    private fun normalizeUsername(value: String): String {
        val username = value.trim().removePrefix("@").lowercase()
        require(username.matches(Regex("^[a-z0-9_.]{3,20}$"))) {
            "El @usuario debe tener de 3 a 20 caracteres: letras, números, _ o ."
        }
        return username
    }

    private fun queryValue(value: String) = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val PREFERENCES = "samplay_guest_profile"
        const val CLIENT_ID = "client_id"
        const val OWNER_TOKEN = "owner_token"
        const val USERNAME = "username"
        const val MAX_IMAGE_BYTES = 3 * 1024 * 1024
        const val MAX_COMMENT_LENGTH = 500
        const val MAX_503_RETRIES = 2
        const val REQUEST_TIMEOUT_MS = 30_000
    }
}
