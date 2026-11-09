package com.myiptv.clean.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.myiptv.clean.ads.AdSection
import com.myiptv.clean.ads.CoinPurchaseStatus
import com.myiptv.clean.ads.CoinRules

internal data class PlaybackRewardOffer(val id: String, val section: AdSection, val isChannel: Boolean)

/** Watching and spending are optional; dismissing the dialog continues without a charge. */
@Composable
internal fun PlaybackRewardOfferDialog(
    offer: PlaybackRewardOffer,
    balance: Int,
    onWatch: () -> Unit,
    onSpend: () -> CoinPurchaseStatus?,
    onContinue: () -> Unit,
) {
    var insufficient by remember(offer) { mutableStateOf(false) }
    if (insufficient) {
        InsufficientCoinsDialog(balance = balance, price = CoinRules.PLAYBACK_PRICE, onContinue = onContinue) {
            insufficient = false
        }
        return
    }

    Dialog(onDismissRequest = onContinue) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            // Scroll the whole invitation on short/landscape screens and at large font scales.
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SamplayCoinIcon(Modifier.size(38.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Elige cómo continuar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text("El anuncio de SamPlay es opcional: complétalo y gana ${CoinRules.REWARDED_COINS} monedas.")
                Text(
                    if (offer.isChannel) "Con ${CoinRules.PLAYBACK_PRICE} monedas evitas nuevas invitaciones en este canal durante una hora."
                    else "Usar ${CoinRules.PLAYBACK_PRICE} monedas omite este anuncio en esta reproducción."
                )
                Text("Saldo: $balance monedas. También puedes continuar gratis sin ver este anuncio ni gastar monedas.")
                Button(onClick = onWatch, modifier = Modifier.fillMaxWidth()) { Text("Ver anuncio") }
                OutlinedButton(
                    onClick = { if (onSpend() == CoinPurchaseStatus.INSUFFICIENT) insufficient = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Usar monedas") }
                TextButton(onClick = onContinue, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Continuar gratis")
                }
            }
        }
    }
}

@Composable
internal fun InsufficientCoinsDialog(balance: Int, price: Int, onContinue: (() -> Unit)? = null, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monedas insuficientes") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SamplayCoinIcon(Modifier.size(42.dp))
                Spacer(Modifier.width(12.dp))
                Text("Necesitas $price monedas y tienes $balance. No se ha cobrado nada.")
            }
        },
        confirmButton = {
            if (onContinue != null) TextButton(onClick = onContinue) { Text("Continuar gratis") }
            else TextButton(onClick = onDismiss) { Text("Entendido") }
        },
        dismissButton = {
            if (onContinue != null) TextButton(onClick = onDismiss) { Text("Volver") }
        },
    )
}
