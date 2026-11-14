package com.myiptv.clean.ui.compose

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

// The CSS `ease` curve: start gently, then settle into place.
private val DetailFadeEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

/** Draws over the caller's fallback background, revealing only after a successful image load. */
@Composable
internal fun DetailBackdropImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    // Reset both the load state and animation when navigating to a different image.
    key(imageUrl) {
        var loaded by remember { mutableStateOf(false) }
        val progress by animateFloatAsState(
            targetValue = if (loaded) 1f else 0f,
            animationSpec = tween(durationMillis = 600, easing = DetailFadeEasing),
            label = "detailBackdropFade"
        )
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = modifier.graphicsLayer {
                alpha = progress
                translationY = 12.dp.toPx() * (1f - progress)
            },
            contentScale = ContentScale.Crop,
            onLoading = { loaded = false },
            onSuccess = { loaded = true },
            onError = { loaded = false }
        )
    }
}
