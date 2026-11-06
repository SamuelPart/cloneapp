package com.myiptv.clean.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.myiptv.clean.BuildConfig
import com.myiptv.clean.update.AppUpdateManager
import com.myiptv.clean.update.InstallResult
import kotlinx.coroutines.launch

/** The system installer always asks the user; SamPlay never installs in the background. */
@Composable
internal fun AppUpdateSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val update by AppUpdateManager.state.collectAsState()
    var installMessage by remember { mutableStateOf<String?>(null) }
    var installing by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Versión instalada: ${BuildConfig.VERSION_NAME}", color = TvMuted, style = MaterialTheme.typography.bodySmall)
        Text(update.message, color = TvMuted, style = MaterialTheme.typography.bodySmall)
        val release = update.release
        if (update.downloading) {
            val activeDownload = update.downloadingRelease
            if (activeDownload != null) {
                val percent = (update.downloadedBytes * 100L / activeDownload.sizeBytes).coerceIn(0L, 100L)
                Text("Descargando ${activeDownload.versionName}: $percent %", color = TvMuted, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            if (update.checking) {
                Text("Buscando versiones…", color = TvMuted, style = MaterialTheme.typography.bodySmall)
            } else {
                TvActionButton("Buscar actualización", onClick = {
                    installMessage = null
                    scope.launch { AppUpdateManager.check(context.applicationContext) }
                })
            }
            if (release != null && update.verifiedApk == null) {
                TvActionButton("Descargar versión ${release.versionName}", onClick = {
                    installMessage = null
                    scope.launch { AppUpdateManager.download(context.applicationContext) }
                })
            } else if (release != null && update.verifiedApk != null) {
                if (installing) {
                    Text("Comprobando el APK…", color = TvMuted, style = MaterialTheme.typography.bodySmall)
                } else TvActionButton("Instalar actualización", onClick = {
                    installing = true
                    scope.launch {
                        try {
                            installMessage = when (AppUpdateManager.install(context)) {
                                InstallResult.STARTED -> "Confirma la instalación en Android."
                                InstallResult.NEED_PERMISSION -> {
                                    val opened = AppUpdateManager.openInstallPermission(context)
                                    if (opened) "Permite instalar aplicaciones para SamPlay, regresa y pulsa Instalar actualización."
                                    else "Permite instalar aplicaciones desconocidas para SamPlay en los ajustes de Android y vuelve a intentarlo."
                                }
                                InstallResult.NOT_READY -> "Primero descarga la versión disponible."
                                InstallResult.INVALID -> "No se pudo verificar el APK; vuelve a descargarlo."
                                InstallResult.NO_INSTALLER -> "Este dispositivo no tiene un instalador de APK disponible."
                            }
                        } finally {
                            installing = false
                        }
                    }
                }, primary = true)
            }
        }
        installMessage?.let { Text(it, color = TvMuted, style = MaterialTheme.typography.bodySmall) }
        Text("Android pedirá confirmación. La actualización se instala sobre SamPlay si usa la misma firma.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
    }
}
