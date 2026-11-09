package com.myiptv.clean.ui.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.myiptv.clean.CleanIPTVApp
import com.myiptv.clean.ads.AdAudience
import com.myiptv.clean.ads.AdsController
import com.myiptv.clean.ads.AdsRules
import java.util.Calendar

/** Neutral, optional age screen. Skipping or being under 18 never blocks app content. */
@Composable
internal fun AdAudienceDialog(ads: AdsController, canPrompt: Boolean) {
    val context = LocalContext.current
    val activity = context.adActivity()
    var dismissed by remember { mutableStateOf(false) }
    var day by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    if (!canPrompt || dismissed || ads.audience != AdAudience.UNKNOWN || activity == null) return

    AlertDialog(
        onDismissRequest = { dismissed = true },
        title = { Text("Fecha de nacimiento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Introduce tu fecha de nacimiento para adaptar la experiencia. No guardamos la fecha.")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = day, onValueChange = { day = it.filter(Char::isDigit).take(2); invalid = false },
                        label = { Text("Día") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = month, onValueChange = { month = it.filter(Char::isDigit).take(2); invalid = false },
                        label = { Text("Mes") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = year, onValueChange = { year = it.filter(Char::isDigit).take(4); invalid = false },
                        label = { Text("Año") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.35f),
                    )
                }
                if (invalid) Text("Introduce una fecha válida.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = AdsRules.audienceForBirthDate(
                    day.toIntOrNull() ?: 0, month.toIntOrNull() ?: 0,
                    year.toIntOrNull() ?: 0, Calendar.getInstance(),
                )
                if (value == null) invalid = true else {
                    // The date is not written to preferences or sent to advertising services.
                    ads.saveAudience(value, activity)
                    dismissed = true
                    day = ""; month = ""; year = ""
                }
            }) { Text("Continuar") }
        },
        dismissButton = { TextButton(onClick = { dismissed = true }) { Text("Ahora no") } },
    )
}

/** Privacy entry point is visible whenever UMP requires it, even when no ads are available. */
@Composable
internal fun AdPrivacyAndRewardsSettings(isTv: Boolean) {
    val context = LocalContext.current
    val activity = context.adActivity() ?: return
    if (isTv || context.isTelevisionDevice()) return
    val ads = (context.applicationContext as CleanIPTVApp).ads
    var message by remember { mutableStateOf("") }
    var rewardDialog by remember { mutableStateOf<RewardFormat?>(null) }

    if (ads.privacyOptionsRequired && ads.audience == AdAudience.ADULT) {
        TvInfoPanel {
            Text("Privacidad de anuncios", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            Text("Puedes revisar tus opciones de privacidad para los anuncios.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
            TvActionButton("Opciones de privacidad", onClick = {
                ads.showPrivacyOptions(activity) { success ->
                    message = if (success) "Opciones actualizadas." else "No se pudieron abrir las opciones de privacidad."
                }
            })
            if (message.isNotBlank()) Text(message, color = TvMuted, style = MaterialTheme.typography.bodySmall)
        }
    }

    if (ads.audience == AdAudience.ADULT && (ads.canShowAds || ads.isAdFree || ads.fullscreenShowing)) {
        LaunchedEffect(ads.canShowAds, ads.fullscreenShowing) {
            if (ads.canShowAds && !ads.fullscreenShowing) ads.loadRewards()
        }
        TvInfoPanel {
            Text("Descanso de anuncios", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            if (ads.isAdFree) {
                Text("Tu recompensa está activa: 30 minutos sin anuncios de SamPlay desde que la recibiste.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
            } else {
                Text(
                    "Opcional: completa uno de estos anuncios para obtener 30 minutos sin anuncios de SamPlay. No incluye anuncios de sitios externos.",
                    color = TvMuted, style = MaterialTheme.typography.bodySmall,
                )
                if (ads.rewardedAvailable) {
                    TvActionButton("Ver anuncio recompensado", onClick = { rewardDialog = RewardFormat.STANDARD })
                }
                if (ads.rewardedInterstitialAvailable) {
                    TvActionButton("Ver anuncio recompensado alternativo", onClick = { rewardDialog = RewardFormat.INTERSTITIAL })
                }
                if (!ads.rewardedAvailable && !ads.rewardedInterstitialAvailable) {
                    Text(if (ads.rewardsLoading) "Cargando opciones…" else "No hay anuncios disponibles en este momento.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
                    if (!ads.rewardsLoading && !ads.fullscreenShowing) {
                        TvActionButton("Reintentar", onClick = { ads.loadRewards() })
                    }
                }
                if (message.isNotBlank()) Text(message, color = TvMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    rewardDialog?.let { format ->
        AlertDialog(
            onDismissRequest = { rewardDialog = null },
            title = { Text("30 minutos sin anuncios de SamPlay") },
            text = {
                Text("Si completas este anuncio, recibirás 30 minutos sin anuncios de SamPlay. Puedes omitirlo y seguir usando la app normalmente. Los anuncios de sitios externos no están incluidos.")
            },
            confirmButton = {
                TextButton(onClick = {
                    rewardDialog = null
                    if (!ads.showRewarded(activity, interstitial = format == RewardFormat.INTERSTITIAL)) {
                        message = "El anuncio ya no está disponible. Puedes seguir usando SamPlay."
                        ads.loadRewards()
                    }
                }) { Text("Ver anuncio") }
            },
            // Required for rewarded interstitials, and harmless for standard rewarded ads.
            dismissButton = { TextButton(onClick = { rewardDialog = null }) { Text("No, gracias") } },
        )
    }
}

private enum class RewardFormat { STANDARD, INTERSTITIAL }

private tailrec fun Context.adActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.adActivity()
    else -> null
}
