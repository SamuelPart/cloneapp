package com.myiptv.clean.update

import android.content.Context

// Shared navigation can still handle a pending intent from an older installation. No APK
// downloader, installer, update notification, or background release check is included here.
internal const val EXTRA_OPEN_APP_UPDATES = "com.myiptv.clean.OPEN_APP_UPDATES"

internal object AppUpdateSyncScheduler {
    const val ENABLED = false
    const val CHECK_INTERVAL_MS = 12L * 60 * 60 * 1_000
    fun schedule(@Suppress("UNUSED_PARAMETER") context: Context) = Unit
}

internal object AppUpdateManager {
    suspend fun check(@Suppress("UNUSED_PARAMETER") context: Context) = Unit
}
