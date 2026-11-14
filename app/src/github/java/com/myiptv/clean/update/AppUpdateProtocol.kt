package com.myiptv.clean.update

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

internal const val GITHUB_LATEST_RELEASE = "https://api.github.com/repos/SamuelPart/cloneapp/releases/latest"
internal const val MAX_RELEASE_METADATA_BYTES = 1024 * 1024
internal const val MAX_UPDATE_APK_BYTES = 512L * 1024 * 1024
private const val RELEASE_DOWNLOAD_ROOT = "https://github.com/SamuelPart/cloneapp/releases/download"
private val RELEASE_TAG = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,79}")
private val APK_NAME = Regex("SamPlay-([1-9][0-9]{0,9})-([A-Za-z0-9][A-Za-z0-9._-]{0,39})\\.apk")
private val SHA256 = Regex("sha256:([0-9a-fA-F]{64})")

internal data class AppUpdateRelease(
    val versionCode: Long,
    val versionName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val sha256: String,
)

internal sealed interface AppReleaseCheck {
    data class Available(val release: AppUpdateRelease) : AppReleaseCheck
    data object UpToDate : AppReleaseCheck
    data object NotPublished : AppReleaseCheck
    data object NoCompatibleApk : AppReleaseCheck
}

private fun JsonObject.string(name: String): String? = get(name)?.let { element ->
    if (element.isJsonPrimitive && element.asJsonPrimitive.isString) element.asString else null
}

private fun JsonObject.boolean(name: String): Boolean? = get(name)?.let { element ->
    if (element.isJsonPrimitive && element.asJsonPrimitive.isBoolean) element.asBoolean else null
}

private fun JsonObject.positiveLong(name: String): Long? = get(name)?.let { element ->
    if (!element.isJsonPrimitive || !element.asJsonPrimitive.isNumber) return@let null
    val text = element.asString
    if (!text.matches(Regex("[1-9][0-9]{0,11}"))) return@let null
    text.toLongOrNull()
}

/** Only a stable GitHub release with one explicitly named, hashed universal APK is usable. */
internal fun parseLatestAppRelease(json: String, installedVersionCode: Long): AppReleaseCheck {
    val root = try {
        @Suppress("DEPRECATION")
        JsonParser().parse(json).takeIf(JsonElement::isJsonObject)?.asJsonObject
    } catch (_: RuntimeException) {
        null
    } ?: throw IOException("La respuesta de versiones no es válida")
    if (root.boolean("draft") != false || root.boolean("prerelease") != false) return AppReleaseCheck.NoCompatibleApk
    val tag = root.string("tag_name")?.takeIf { RELEASE_TAG.matches(it) } ?: return AppReleaseCheck.NoCompatibleApk
    val assets = root.get("assets")?.takeIf(JsonElement::isJsonArray)?.asJsonArray ?: return AppReleaseCheck.NoCompatibleApk
    val namedAssets = assets.mapNotNull { element ->
        val asset = element.takeIf(JsonElement::isJsonObject)?.asJsonObject ?: return@mapNotNull null
        val name = asset.string("name") ?: return@mapNotNull null
        val match = APK_NAME.matchEntire(name) ?: return@mapNotNull null
        Triple(asset, name, match)
    }
    if (namedAssets.size != 1) return AppReleaseCheck.NoCompatibleApk
    val (asset, name, match) = namedAssets.single()
    val code = match.groupValues[1].toLongOrNull()?.takeIf { it in 1..2_100_000_000L }
        ?: return AppReleaseCheck.NoCompatibleApk
    if (asset.string("state") != "uploaded") return AppReleaseCheck.NoCompatibleApk
    val expectedUrl = "$RELEASE_DOWNLOAD_ROOT/$tag/$name"
    if (asset.string("browser_download_url") != expectedUrl) return AppReleaseCheck.NoCompatibleApk
    val size = asset.positiveLong("size")?.takeIf { it <= MAX_UPDATE_APK_BYTES } ?: return AppReleaseCheck.NoCompatibleApk
    val digest = asset.string("digest")?.let { SHA256.matchEntire(it) }?.groupValues?.get(1)?.lowercase(Locale.ROOT)
        ?: return AppReleaseCheck.NoCompatibleApk
    val release = AppUpdateRelease(code, match.groupValues[2], expectedUrl, size, digest)
    return if (code > installedVersionCode) AppReleaseCheck.Available(release) else AppReleaseCheck.UpToDate
}

/** Reads only the public release metadata; it never uses a GitHub credential. */
internal class GitHubReleaseClient(
    private val openConnection: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
) {
    fun check(installedVersionCode: Long): AppReleaseCheck {
        val connection = openConnection(URL(GITHUB_LATEST_RELEASE))
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("User-Agent", "SamPlay-Updater/1.0")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.setRequestProperty("Cache-Control", "no-cache")
            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_NOT_FOUND) return AppReleaseCheck.NotPublished
            if (status != HttpURLConnection.HTTP_OK) throw IOException("No se pudo consultar GitHub (HTTP $status)")
            val declared = connection.getHeaderField("Content-Length")?.toLongOrNull()
            if (declared != null && (declared < 0 || declared > MAX_RELEASE_METADATA_BYTES)) throw IOException("Respuesta demasiado grande")
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8_192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    total += count
                    if (total > MAX_RELEASE_METADATA_BYTES) throw IOException("Respuesta demasiado grande")
                    output.write(buffer, 0, count)
                }
                if (declared != null && declared != total.toLong()) throw IOException("Respuesta incompleta")
                output.toByteArray()
            }
            val json = try {
                Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()
            } catch (error: Exception) {
                throw IOException("La respuesta no es UTF-8 válido", error)
            }
            return parseLatestAppRelease(json, installedVersionCode)
        } finally {
            connection.disconnect()
        }
    }
}

/** Streams a single APK from GitHub, verifies its exact size and SHA-256, and publishes it atomically. */
internal class GitHubApkDownloader(
    private val openConnection: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
) {
    fun download(
        release: AppUpdateRelease,
        destination: File,
        checkCancellation: () -> Unit = {},
        onProgress: (Long) -> Unit = {},
    ) {
        val urlParts = release.downloadUrl.removePrefix("$RELEASE_DOWNLOAD_ROOT/").split('/')
        val expectedName = "SamPlay-${release.versionCode}-${release.versionName}.apk"
        if (release.sizeBytes !in 1..MAX_UPDATE_APK_BYTES || !release.sha256.matches(Regex("[0-9a-f]{64}")) ||
            !release.downloadUrl.startsWith("$RELEASE_DOWNLOAD_ROOT/") || urlParts.size != 2 ||
            !RELEASE_TAG.matches(urlParts[0]) || urlParts[1] != expectedName || !APK_NAME.matches(expectedName)
        ) throw IOException("Datos de la descarga no válidos")
        val directory = destination.parentFile ?: throw IOException("No hay carpeta para la descarga")
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("No se pudo crear la carpeta de actualización")
        val temporary = File(directory, "${destination.name}.part")
        temporary.delete()
        var success = false
        try {
            var url = URL(release.downloadUrl)
            repeat(MAX_REDIRECTS + 1) { redirectCount ->
                checkAllowedDownloadUrl(url, redirectCount == 0, release.downloadUrl)
                checkCancellation()
                val connection = openConnection(url)
                try {
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 25_000
                    connection.setRequestProperty("User-Agent", "SamPlay-Updater/1.0")
                    connection.setRequestProperty("Accept-Encoding", "identity")
                    val response = connection.responseCode
                    if (response in REDIRECT_CODES) {
                        if (redirectCount == MAX_REDIRECTS) throw IOException("Demasiadas redirecciones")
                        val location = connection.getHeaderField("Location") ?: throw IOException("Redirección sin destino")
                        url = URL(url, location)
                        return@repeat
                    }
                    if (response != HttpURLConnection.HTTP_OK) throw IOException("Descarga no disponible (HTTP $response)")
                    val declared = connection.getHeaderField("Content-Length")?.toLongOrNull()
                    if (declared != null && declared != release.sizeBytes) throw IOException("El tamaño del APK no coincide")
                    val digest = MessageDigest.getInstance("SHA-256")
                    var total = 0L
                    connection.inputStream.use { input ->
                        FileOutputStream(temporary).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                checkCancellation()
                                val count = input.read(buffer)
                                if (count == -1) break
                                total += count
                                if (total > release.sizeBytes || total > MAX_UPDATE_APK_BYTES) throw IOException("APK demasiado grande")
                                digest.update(buffer, 0, count)
                                output.write(buffer, 0, count)
                                onProgress(total)
                            }
                            output.fd.sync()
                        }
                    }
                    if (total != release.sizeBytes || digest.digest().toHex() != release.sha256) {
                        throw IOException("El APK descargado está incompleto o no coincide con su SHA-256")
                    }
                    checkCancellation()
                    if (destination.exists() && !destination.delete()) throw IOException("No se pudo reemplazar la descarga anterior")
                    if (!temporary.renameTo(destination)) throw IOException("No se pudo guardar el APK verificado")
                    success = true
                    return
                } finally {
                    connection.disconnect()
                }
            }
            throw IOException("No se pudo descargar el APK")
        } finally {
            if (!success) temporary.delete()
        }
    }

    private fun checkAllowedDownloadUrl(url: URL, initial: Boolean, expected: String) {
        if (url.protocol != "https" || url.userInfo != null || url.ref != null || (url.port != -1 && url.port != 443) ||
            url.host.lowercase(Locale.ROOT) !in ALLOWED_HOSTS || (initial && url.toExternalForm() != expected)
        ) throw IOException("Dirección de descarga no permitida")
    }

    companion object {
        private const val MAX_REDIRECTS = 5
        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
        private val ALLOWED_HOSTS = setOf("github.com", "objects.githubusercontent.com", "release-assets.githubusercontent.com", "github-releases.githubusercontent.com")
    }
}

internal fun ByteArray.toHex(): String = joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
