package com.myiptv.clean.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.myiptv.clean.ads.AdSection
import com.myiptv.clean.ads.CoinRules

internal data class PlaybackRewardOffer(val id: String, val section: AdSection, val isChannel: Boolean)

/** Watching and spending are optional; dismissing the dialog always starts the content. */
@Composable
internal fun PlaybackRewardOfferDialog(
    offer: PlaybackRewardOffer,
    balance: Int,
    onWatch: () -> Unit,
    onSpend: () -> Unit,
    onContinue: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onContinue,
        title = { Text("Gana 10 monedas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Si quieres, completa un anuncio y recibirás 10 monedas para usar en SamPlay.")
                Text("También puedes reproducir gratis sin verlo: no necesitas gastar monedas.")
                Text(
                    if (offer.isChannel) "Canjear 10 monedas evita esta invitación en este canal durante una hora. Saldo: $balance."
                    else "Canjear 10 monedas evita esta invitación para esta reproducción. Saldo: $balance."
                )
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onWatch) { Text("Ver anuncio y ganar 10") }
                if (balance >= CoinRules.PLAYBACK_PRICE) {
                    TextButton(onClick = onSpend) { Text("Canjear 10 monedas") }
                }
            }
        },
        dismissButton = { TextButton(onClick = onContinue) { Text("Reproducir gratis") } },
    )
}
