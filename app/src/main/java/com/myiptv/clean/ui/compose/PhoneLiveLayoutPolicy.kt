package com.myiptv.clean.ui.compose

/** A phone/tablet must not switch to TV live UI when its fullscreen player rotates. */
internal fun useTvContentLayout(isTvDevice: Boolean, isPlayer: Boolean, isChannelDestination: Boolean, widthDp: Float): Boolean =
    isTvDevice || (!isPlayer && !isChannelDestination && widthDp >= 840f)

/** Keep the LibVLC bounds adjustment limited to the phone live player. */
internal fun usePhoneLiveVideoBounds(isTv: Boolean, hasPhoneLiveActions: Boolean): Boolean =
    !isTv && hasPhoneLiveActions

/**
 * Prefer a full-width 16:9 preview in portrait. Reserve space for the channel tabs and filters,
 * plus the search field when open; in a very short window the video can shrink to zero rather
 * than covering those controls. Landscape retains the previous smaller preview when it fits.
 */
internal fun phoneLivePreviewHeightDp(widthDp: Float, heightDp: Float, portrait: Boolean, searchOpen: Boolean): Float {
    if (widthDp <= 0f || heightDp <= 0f) return 0f
    val naturalHeight = widthDp * 9f / 16f
    val reserved = if (searchOpen) 182f else 120f
    val availableForVideo = (heightDp - reserved).coerceAtLeast(0f)
    val preferredHeight = if (portrait) naturalHeight
    else (heightDp * 0.42f).coerceAtLeast(minOf(88f, heightDp))
    return minOf(naturalHeight, preferredHeight, availableForVideo)
}
