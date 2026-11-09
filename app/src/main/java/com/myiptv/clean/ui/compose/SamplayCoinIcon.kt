package com.myiptv.clean.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.myiptv.clean.R

/** A gold-edged SamPlay coin using the app's actual glass-lion mascot. */
@Composable
internal fun SamplayCoinIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFFFF4C7), Color(0xFFEFB94C), Color(0xFF9B580B), Color(0xFFFFE598))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(0.84f).clip(CircleShape).background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null, // The adjacent text announces the balance.
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.45f; scaleY = 1.45f },
            )
        }
    }
}
