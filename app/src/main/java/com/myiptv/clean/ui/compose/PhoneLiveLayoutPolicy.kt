package com.myiptv.clean.ui.compose

/** A phone/tablet must not switch to TV live UI when its fullscreen player rotates. */
internal fun useTvContentLayout(isTvDevice: Boolean, isPlayer: Boolean, isChannelDestination: Boolean, widthDp: Float): Boolean =
    isTvDevice || (!isPlayer && !isChannelDestination && widthDp >= 840f)
