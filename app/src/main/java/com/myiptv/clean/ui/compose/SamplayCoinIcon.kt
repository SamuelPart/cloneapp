package com.myiptv.clean.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.myiptv.clean.R

/** Silver SamPlay coin with a blue lion paw. The balance is announced by adjacent text. */
@Composable
internal fun SamplayCoinIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.samplay_coin_paw),
        contentDescription = null,
        modifier = modifier.aspectRatio(1f),
        contentScale = ContentScale.Fit,
    )
}
