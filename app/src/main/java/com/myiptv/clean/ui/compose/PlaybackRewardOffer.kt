package com.myiptv.clean.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    var showHelp by remember(offer) { mutableStateOf(false) }
    if (insufficient) {
        InsufficientCoinsDialog(balance = balance, price = CoinRules.PLAYBACK_PRICE, onContinue = onContinue) {
            insufficient = false
        }
        return
    }

    if (showHelp) {
        val coinHelp = if (offer.isChannel) {
            "Usar monedas evita nuevas invitaciones en este canal por una hora."
        } else {
            "Usar monedas omite el anuncio de esta reproducción."
        }
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Ayuda") },
            text = { Text("Ver anuncio: mira un anuncio y continúa. $coinHelp") },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Entendido") } },
        )
    }

    Dialog(onDismissRequest = onContinue) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Elige cómo continuar",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = { showHelp = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = "Ayuda sobre las opciones",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Button(onClick = onWatch, modifier = Modifier.fillMaxWidth()) { Text("Ver anuncio") }
                OutlinedButton(
                    onClick = { if (onSpend() == CoinPurchaseStatus.INSUFFICIENT) insufficient = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Usar monedas") }
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
