package com.myiptv.clean.ui.compose

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

internal fun Context.isTelevisionDevice(uiMode: Int = resources.configuration.uiMode): Boolean {
    val features = packageManager
    return usesTvDeviceLayout(
        isTelevisionMode = (uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION,
        hasLeanback = features.hasSystemFeature(PackageManager.FEATURE_LEANBACK),
        hasTelevisionFeature = features.hasSystemFeature(PackageManager.FEATURE_TELEVISION),
        hasTouchscreen = features.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
    )
}
