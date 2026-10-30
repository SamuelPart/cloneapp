package com.myiptv.clean.notifications

import com.myiptv.clean.data.repository.CatalogAddition

internal const val EXTRA_CATALOG_MEDIA_ID = "com.myiptv.clean.extra.CATALOG_MEDIA_ID"

/** The sequence distinguishes two taps on the same title while the activity is already open. */
internal data class CatalogNotificationTarget(val mediaId: String, val sequence: Long)

/** Shared by cold starts and onNewIntent; consuming an old request cannot erase a newer tap. */
internal class CatalogNotificationRequests {
    private var sequence = 0L
    var current: CatalogNotificationTarget? = null
        private set

    fun accept(mediaId: String?): CatalogNotificationTarget? {
        val id = mediaId?.trim()?.takeIf(String::isNotEmpty) ?: return null
        sequence++
        return CatalogNotificationTarget(id, sequence).also { current = it }
    }

    fun consume(handledSequence: Long): Boolean {
        if (current?.sequence != handledSequence) return false
        current = null
        return true
    }
}

/** Different title/season/episode batches have different Android notification and intent identities. */
internal fun catalogNotificationIdentity(addition: CatalogAddition): String =
    "${addition.mediaId.length}:${addition.mediaId}|${addition.season ?: "title"}|" +
        addition.episodes.distinct().sorted().joinToString(",")
