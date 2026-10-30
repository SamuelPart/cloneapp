package com.myiptv.clean.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.myiptv.clean.R
import com.myiptv.clean.data.repository.CatalogAddition
import com.myiptv.clean.data.repository.catalogAdditionLabel
import com.myiptv.clean.ui.compose.IptvActivity

internal object CatalogNotifications {
    private const val CHANNEL_ID = "samplay_new_catalog_v1"

    fun isEnabled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return false
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL_ID)
                if (channel?.importance == NotificationManager.IMPORTANCE_NONE) return false
            }
            true
        } catch (_: RuntimeException) {
            false
        }
    }

    /** False leaves pending additions in the atomic cache until the user allows notifications. */
    @SuppressLint("MissingPermission") // isEnabled checks both the runtime permission and the app setting.
    fun show(context: Context, additions: List<CatalogAddition>): Boolean {
        if (additions.isEmpty()) return true
        if (!isEnabled(context)) return false
        return try {
            val appContext = context.applicationContext
            val manager = NotificationManagerCompat.from(appContext)
            // Use a resource-name URI so an Android notification channel survives resource-ID changes.
            val soundName = appContext.resources.getResourceEntryName(R.raw.samplay_notification)
            val sound = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${appContext.packageName}/raw/$soundName")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, "Novedades de SamPlay", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Películas, series y episodios nuevos del catálogo"
                    setSound(sound, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                }
                appContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            }
            additions.forEach { addition ->
                val identity = catalogNotificationIdentity(addition)
                val label = catalogAdditionLabel(addition)
                val intent = Intent(appContext, IptvActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    // Extras alone do not distinguish PendingIntents; the unique data URI does.
                    data = Uri.Builder().scheme("samplay").authority("catalogo").appendPath(identity).build()
                    putExtra(EXTRA_CATALOG_MEDIA_ID, addition.mediaId)
                }
                val pendingIntent = PendingIntent.getActivity(
                    appContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_catalog_notification)
                    .setContentTitle("Novedad en SamPlay")
                    .setContentText(label)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(label))
                    .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setSound(sound)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(pendingIntent)
                    .build()
                // The full tag avoids hash collisions and replaces an already-posted identical
                // addition if the process dies before clearing the pending atomic cache.
                manager.notify("catalog:$identity", 0, notification)
            }
            true
        } catch (_: RuntimeException) {
            false
        }
    }
}
