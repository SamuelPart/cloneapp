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
import androidx.compose.material.icons.outlined.Lightbulb
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

/** Playback requires a rewarded ad or coins; dismissing the offer cancels playback. */
@Composable
internal fun PlaybackRewardOfferDialog(
    offer: PlaybackRewardOffer,
    balance: Int,
    onWatch: () -> Unit,
    onSpend: () -> CoinPurchaseStatus?,
    onCancel: () -> Unit,
) {
    var insufficient by remember(offer) { mutableStateOf(false) }
    var showHelp by remember(offer) { mutableStateOf(false) }
    if (insufficient) {
        InsufficientCoinsDialog(
            balance = balance,
            price = CoinRules.PLAYBACK_PRICE,
            onWatch = { insufficient = false; onWatch() },
            onBack = onCancel,
            onDismiss = onCancel,
        )
        return
    }

    if (showHelp) {
        val coinHelp = if (offer.isChannel) {
            "Usar ${CoinRules.PLAYBACK_PRICE} monedas evita nuevas invitaciones en este canal por una hora."
        } else {
            "Usar ${CoinRules.PLAYBACK_PRICE} monedas omite el anuncio de esta reproducción."
        }
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Ayuda") },
            text = { Text("Ver anuncio: mira un anuncio y continúa. $coinHelp") },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Entendido") } },
        )
    }

    Dialog(onDismissRequest = onCancel) {
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
internal fun InsufficientCoinsDialog(
    balance: Int,
    price: Int,
    onWatch: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monedas insuficientes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SamplayCoinIcon(Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Necesitas $price monedas y tienes $balance. No se hizo ningún cobro.")
                }
                if (onWatch != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Consejo: gana ${CoinRules.REWARDED_COINS} monedas viendo un anuncio.")
                    }
                }
            }
        },
        confirmButton = {
            if (onWatch != null) Button(onClick = onWatch) { Text("Ver anuncio") }
            else TextButton(onClick = onDismiss) { Text("Entendido") }
        },
        dismissButton = {
            if (onBack != null) TextButton(onClick = onBack) { Text("Volver") }
        },
    )
}
