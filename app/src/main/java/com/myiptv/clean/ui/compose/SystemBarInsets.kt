package com.myiptv.clean.ui.compose

import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/** The background can remain edge-to-edge, while interactive content avoids system bars/cutouts. */
@Composable
internal fun Modifier.safeAreaWhenEnforced(
    isTelevision: Boolean,
    immersive: Boolean,
    sides: WindowInsetsSides = WindowInsetsSides.Horizontal + WindowInsetsSides.Vertical
): Modifier {
    val context = LocalContext.current
    return if (needsSafeArea(Build.VERSION.SDK_INT, context.applicationInfo.targetSdkVersion, isTelevision, immersive)) {
        windowInsetsPadding(WindowInsets.safeDrawing.only(sides))
    } else this
}
