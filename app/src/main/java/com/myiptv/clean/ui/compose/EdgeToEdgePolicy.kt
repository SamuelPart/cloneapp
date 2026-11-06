package com.myiptv.clean.ui.compose

/** Preserve the old layout unless Android enforces edge-to-edge for this app. */
internal fun needsSafeArea(deviceApi: Int, targetApi: Int, isTelevision: Boolean, immersive: Boolean): Boolean =
    deviceApi >= 35 && targetApi >= 35 && !isTelevision && !immersive
