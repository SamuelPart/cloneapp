package com.myiptv.clean.update

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.myiptv.clean.R
import com.myiptv.clean.ui.compose.IptvActivity

internal const val EXTRA_OPEN_APP_UPDATES = "com.myiptv.clean.OPEN_APP_UPDATES"

internal object AppUpdateNotifications {
    private const val CHANNEL_ID = "samplay_app_updates"
    private const val PREFERENCES = "samplay_app_update_notifications"
    private const val LAST_NOTIFIED = "last_notified_version_code"
    private const val TAG = "samplay:app-update"
    private const val ID = 1

    @SuppressLint("MissingPermission") // The runtime permission and app/channel settings are checked below.
    fun showIfNew(context: Context, release: AppUpdateRelease) {
        val app = context.applicationContext
        try {
            val prefs = app.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            if (release.versionCode <= prefs.getLong(LAST_NOTIFIED, 0L)) return
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return
            val manager = NotificationManagerCompat.from(app)
            if (!manager.areNotificationsEnabled()) return
            if (Build.VERSION.SDK_INT >= 26) {
                val androidManager = app.getSystemService(NotificationManager::class.java)
                val channel = NotificationChannel(CHANNEL_ID, "Actualizaciones de SamPlay", NotificationManager.IMPORTANCE_DEFAULT)
                channel.description = "Avisos de nuevas versiones de la aplicación"
                androidManager.createNotificationChannel(channel)
                if (androidManager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE) return
            }
            val intent = Intent(app, IptvActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                data = Uri.parse("samplay://actualizacion/${release.versionCode}")
                putExtra(EXTRA_OPEN_APP_UPDATES, true)
            }
            val pending = PendingIntent.getActivity(app, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_catalog_notification)
                .setContentTitle("Nueva versión de SamPlay")
                .setContentText("La versión ${release.versionName} está disponible. Toca para revisarla.")
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pending)
                .build()
            manager.notify(TAG, ID, notification)
            prefs.edit().putLong(LAST_NOTIFIED, release.versionCode).apply()
        } catch (_: RuntimeException) {
            // Checking and the Settings screen still work if this device cannot show notifications.
        }
    }

    fun clear(context: Context) {
        try { NotificationManagerCompat.from(context).cancel(TAG, ID) } catch (_: RuntimeException) { }
    }
}
