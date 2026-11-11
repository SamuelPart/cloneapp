package com.myiptv.clean.ui.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myiptv.clean.R

/** Indefinite branded loading animation for work that waits on network or device performance. */
@Composable
internal fun SamplayLoading(
    modifier: Modifier = Modifier,
    message: String = "Cargando…",
    lionSize: Dp = 132.dp,
) {
    val motion = rememberInfiniteTransition()
    val rotation by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1_250, easing = LinearEasing), RepeatMode.Restart),
    )
    val jump by motion.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(tween(430, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    )

    Box(
        modifier = modifier.semantics { contentDescription = "SamPlay, $message" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(
                modifier = Modifier.size(lionSize).graphicsLayer { translationY = jump },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.samplay_lion_animation_base),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation },
                )
                Image(
                    painter = painterResource(R.drawable.samplay_lion_eyes_06),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation },
                )
            }
            TvBrandLogo(Modifier.width(120.dp))
            if (message.isNotBlank()) {
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
internal fun SamplayLoadingPane(
    modifier: Modifier = Modifier,
    message: String = "Cargando…",
) {
    SamplayLoading(modifier.fillMaxWidth().heightIn(min = 220.dp), message = message)
}

@Composable
internal fun SamplayLoadingScreen(message: String = "Cargando…", onBack: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        SamplayLoading(Modifier.fillMaxSize(), message = message, lionSize = 150.dp)
        onBack?.let { back ->
            Box(
                Modifier.align(Alignment.TopStart).size(52.dp).clickable(onClickLabel = "Volver", onClick = back),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}
