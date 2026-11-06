package com.myiptv.clean.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.myiptv.clean.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest

internal data class AppUpdateUiState(
    val checking: Boolean = false,
    val downloading: Boolean = false,
    val release: AppUpdateRelease? = null,
    val downloadingRelease: AppUpdateRelease? = null,
    val downloadedBytes: Long = 0L,
    val verifiedApk: File? = null,
    val message: String = "Aún no se han consultado versiones de SamPlay.",
)

internal enum class InstallResult { STARTED, NEED_PERMISSION, NOT_READY, INVALID, NO_INSTALLER }

/** Checks a public GitHub release; it never installs or launches an APK without the user's action. */
internal object AppUpdateManager {
    private val checkLock = Mutex()
    private val downloadLock = Mutex()
    private val mutableState = MutableStateFlow(AppUpdateUiState())
    val state = mutableState.asStateFlow()
    private val stateLock = Any()

    private fun updateState(transform: (AppUpdateUiState) -> AppUpdateUiState) {
        synchronized(stateLock) { mutableState.value = transform(mutableState.value) }
    }

    suspend fun check(context: Context) = withContext(Dispatchers.IO) {
        checkLock.withLock {
            updateState { it.copy(checking = true) }
            try {
                val result = GitHubReleaseClient().check(BuildConfig.VERSION_CODE.toLong())
                currentCoroutineContext().ensureActive()
                when (result) {
                    is AppReleaseCheck.Available -> {
                        val release = result.release
                        updateState { old ->
                            old.copy(
                                release = release,
                                verifiedApk = old.verifiedApk.takeIf { old.release == release },
                                message = "Versión ${release.versionName} disponible en GitHub.",
                            )
                        }
                        AppUpdateNotifications.showIfNew(context, release)
                    }
                    AppReleaseCheck.UpToDate -> {
                        updateState { it.copy(release = null, verifiedApk = null, message = "La versión publicada no es más reciente que la instalada.") }
                        AppUpdateNotifications.clear(context)
                    }
                    AppReleaseCheck.NotPublished -> {
                        updateState { it.copy(release = null, verifiedApk = null, message = "No se encontró una versión de SamPlay publicada en GitHub.") }
                        AppUpdateNotifications.clear(context)
                    }
                    AppReleaseCheck.NoCompatibleApk -> {
                        updateState { it.copy(release = null, verifiedApk = null, message = "La última versión publicada no incluye un APK de SamPlay verificable.") }
                        AppUpdateNotifications.clear(context)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A network error never invents an available release or discards an already verified APK.
                updateState { it.copy(message = "No se pudo consultar GitHub. Comprueba la conexión y vuelve a intentarlo.") }
            } finally {
                updateState { it.copy(checking = false) }
            }
        }
    }

    suspend fun download(context: Context) = withContext(Dispatchers.IO) {
        downloadLock.withLock {
            val snapshot = mutableState.value
            val release = snapshot.release ?: return@withLock
            // Multiple quick button presses must not download the same verified APK again.
            if (snapshot.verifiedApk != null) return@withLock
            val app = context.applicationContext
            val directory = File(app.cacheDir, "samplay-app-updates")
            val destination = File(directory, "samplay-update.apk")
            updateState { it.copy(downloading = true, downloadingRelease = release, verifiedApk = null, downloadedBytes = 0L, message = "Descargando SamPlay ${release.versionName}…") }
            try {
                val coroutine = currentCoroutineContext()
                var lastPublished = 0L
                GitHubApkDownloader().download(
                    release, destination,
                    checkCancellation = { coroutine.ensureActive() },
                    onProgress = { bytes ->
                        if (bytes == release.sizeBytes || bytes - lastPublished >= 1024 * 1024) {
                            lastPublished = bytes
                            updateState { it.copy(downloadedBytes = bytes) }
                        }
                    },
                )
                coroutine.ensureActive()
                if (!InstalledApkVerifier.matches(app, destination, release)) {
                    destination.delete()
                    throw IOException("El paquete, la versión o la firma no coinciden")
                }
                updateState { old ->
                    if (old.release != release) old.copy(message = "La versión disponible cambió. Vuelve a descargarla.")
                    else old.copy(verifiedApk = destination, message = "Descarga verificada. Ya puedes instalar la actualización.")
                }
                if (mutableState.value.release != release || mutableState.value.verifiedApk != destination) destination.delete()
            } catch (cancelled: CancellationException) {
                destination.delete()
                updateState { it.copy(verifiedApk = null, message = "Descarga cancelada.") }
                throw cancelled
            } catch (_: Exception) {
                destination.delete()
                updateState { it.copy(verifiedApk = null, message = "No se pudo verificar la descarga. Comprueba la conexión y que el APK tenga la misma firma de SamPlay.") }
            } finally {
                updateState { it.copy(downloading = false, downloadingRelease = null) }
            }
        }
    }

    suspend fun install(context: Context): InstallResult {
        val snapshot = state.value
        val release = snapshot.release ?: return InstallResult.NOT_READY
        val file = snapshot.verifiedApk ?: return InstallResult.NOT_READY
        val valid = withContext(Dispatchers.IO) {
            try {
                file.isFile && file.length() == release.sizeBytes && fileSha256(file) == release.sha256 &&
                    InstalledApkVerifier.matches(context.applicationContext, file, release)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
        }
        if (!valid || state.value.release != release || state.value.verifiedApk != file) {
            updateState {
                if (it.release == release && it.verifiedApk == file) it.copy(verifiedApk = null, message = "El APK ya no es válido. Descárgalo otra vez.")
                else it
            }
            return InstallResult.INVALID
        }
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) return InstallResult.NEED_PERMISSION
        return withContext(Dispatchers.Main) {
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.appupdates", file)
                val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                InstallResult.STARTED
            } catch (_: ActivityNotFoundException) {
                InstallResult.NO_INSTALLER
            } catch (_: RuntimeException) {
                InstallResult.NO_INSTALLER
            }
        }
    }

    fun openInstallPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 26) return true
        return try {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (_: RuntimeException) {
            false
        }
    }

    private suspend fun fileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count == -1) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().toHex()
    }
}
