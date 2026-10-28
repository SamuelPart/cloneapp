package com.myiptv.clean.ui.compose

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.myiptv.clean.data.repository.MediaPlaylistRepository
import com.myiptv.clean.notifications.CatalogNotifications
import kotlinx.coroutines.launch

private const val PREFERENCES = "samplay_catalog_notifications"
private const val ASKED_PERMISSION = "asked_notification_permission"

/** Android 13+ needs a runtime permission; older devices use their existing app setting. */
@Composable
internal fun RequestCatalogNotificationsOnce() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scope.launch { MediaPlaylistRepository.publishPending(context) }
    }
    LaunchedEffect(context) {
        val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission(context) && !prefs.getBoolean(ASKED_PERMISSION, false)) {
            prefs.edit().putBoolean(ASKED_PERMISSION, true).apply()
            request.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            MediaPlaylistRepository.publishPending(context)
        }
    }
}

@Composable
internal fun CatalogNotificationSettings() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(CatalogNotifications.isEnabled(context)) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        enabled = CatalogNotifications.isEnabled(context)
        if (granted) scope.launch { MediaPlaylistRepository.publishPending(context) }
    }
    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = CatalogNotifications.isEnabled(context)
                if (enabled) scope.launch { MediaPlaylistRepository.publishPending(context) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (enabled) "Avisos activados: SamPlay informa de nuevas películas, series y episodios con su sonido."
            else "Los avisos están desactivados. Las novedades quedan pendientes hasta que los permitas.",
            color = TvMuted, style = MaterialTheme.typography.bodySmall
        )
        if (!enabled) {
            TvActionButton("Activar notificaciones", onClick = {
                val activity = context.findActivity()
                val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                val canAsk = Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission(context) && activity != null &&
                    (!prefs.getBoolean(ASKED_PERMISSION, false) ||
                        ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS))
                if (canAsk) {
                    prefs.edit().putBoolean(ASKED_PERMISSION, true).apply()
                    request.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else openNotificationSettings(context)
            })
        }
    }
}

private fun hasNotificationPermission(context: Context): Boolean = Build.VERSION.SDK_INT < 33 ||
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= 26) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            // Some TV builds do not expose notification settings.
        }
    }
}
