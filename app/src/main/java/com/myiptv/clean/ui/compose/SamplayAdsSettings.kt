package com.myiptv.clean.ui.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.myiptv.clean.CleanIPTVApp
import com.myiptv.clean.ads.AdAudience
import com.myiptv.clean.ads.AdsController
import com.myiptv.clean.ads.AdsRules
import com.myiptv.clean.ads.AdSection
import com.myiptv.clean.ads.CoinPurchaseStatus
import com.myiptv.clean.ads.CoinRules
import java.text.DateFormat
import java.util.Date
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

/** Keep advertising privacy in the main settings page, independent of the coin wallet. */
@Composable
internal fun AdPrivacySettings() {
    val context = LocalContext.current
    val activity = context.adActivity() ?: return
    if (context.isTelevisionDevice()) return
    val ads = (context.applicationContext as CleanIPTVApp).ads
    var message by remember { mutableStateOf("") }
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
}

/** Local balance, voluntary rewards and passes, shown only on the coins subpage. */
@Composable
internal fun SamplayCoinSettings() {
    val context = LocalContext.current
    val activity = context.adActivity() ?: return
    if (context.isTelevisionDevice()) return // A wide phone layout is not an Android TV device.
    val app = context.applicationContext as CleanIPTVApp
    val ads = app.ads
    val coins = app.coins
    val wallet = coins.state
    var message by remember { mutableStateOf("") }
    var rewardDialog by remember { mutableStateOf<RewardFormat?>(null) }
    var passDialog by remember { mutableStateOf<PassChoice?>(null) }
    var insufficientPrice by remember { mutableStateOf<Int?>(null) }

    TvInfoPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SamplayCoinIcon(Modifier.size(44.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Saldo SamPlay", color = TvMuted, style = MaterialTheme.typography.bodySmall)
                Text("${wallet.balance} monedas", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            }
        }
        Text("+2 al abrir cada día · +10 por cada 5 días consecutivos · +3 por un favorito nuevo al día.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
        val today = CoinRules.localDay(System.currentTimeMillis())
        val watched = if (wallet.watchDay == today) wallet.watchedMs else 0L
        Text("+5 por 20 minutos de reproducción real al día. Hoy: ${watched / 60_000} de 20 min.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
        Text("+10 por cada anuncio recompensado completado. Las monedas son locales, no transferibles y se pierden al desinstalar o borrar los datos.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
    }

    if (ads.audience == AdAudience.ADULT) {
        LaunchedEffect(ads.canShowAds, ads.fullscreenShowing) {
            if (ads.canShowAds && !ads.fullscreenShowing) ads.loadRewards()
        }
        TvInfoPanel {
            Text("Ganar monedas con anuncios", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            Text("Opcional: completa un anuncio para obtener 10 monedas. Puedes seguir usando la app sin verlo.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
            if (ads.isAdFree) {
                Text("Tienes un pase sin anuncios activo.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
            } else if (!ads.canShowAds) {
                Text("Los anuncios no están disponibles; puedes seguir usando SamPlay.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
            } else {
                if (ads.rewardedAvailable && ads.hasFreshReward(false)) {
                    TvActionButton("Ver anuncio · +10 monedas", onClick = { rewardDialog = RewardFormat.STANDARD })
                }
                if (ads.rewardedInterstitialAvailable && ads.hasFreshReward(true)) {
                    TvActionButton("Ver anuncio alternativo · +10", onClick = { rewardDialog = RewardFormat.INTERSTITIAL })
                }
                if (!ads.hasFreshReward(false) && !ads.hasFreshReward(true)) {
                    Text(if (ads.rewardsLoading) "Cargando opciones…" else "No hay anuncios disponibles en este momento.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
                    if (!ads.rewardsLoading && !ads.fullscreenShowing) {
                        TvActionButton("Reintentar", onClick = { ads.loadRewards() })
                    }
                }
            }
        }
        TvInfoPanel {
            Text("Canjear monedas", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
            Text("10: omitir la invitación en una película o episodio, o durante una hora en un canal. También puedes rechazarla gratis.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
            Text("40: una sección sin anuncios de SamPlay durante 24 horas. 100: toda la app durante 24 horas.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
            Text("No elimina anuncios incluidos por canales, sitios web o proveedores externos.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
            if (ads.isAdFree) {
                Text("Pase general activo${expiryText(wallet.appAdFreeUntil)}.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
            } else {
                TvActionButton("Toda la app · 100 monedas", onClick = { passDialog = PassChoice.App })
                AdSection.values().forEach { section ->
                    val until = wallet.sectionAdFreeUntil[section] ?: 0L
                    if (until > System.currentTimeMillis()) {
                        Text("${section.label}: activo${expiryText(until)}.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
                    } else {
                        TvActionButton("${section.label} · 40 monedas", onClick = { passDialog = PassChoice.Section(section) })
                    }
                }
            }
            if (message.isNotBlank()) Text(message, color = TvMuted, style = MaterialTheme.typography.bodySmall)
        }
    } else {
        TvInfoPanel {
            Text("Los anuncios de SamPlay están desactivados para esta instalación.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
        }
    }

    rewardDialog?.let { format ->
        AlertDialog(
            onDismissRequest = { rewardDialog = null },
            title = { Text("Anuncio opcional: 10 monedas") },
            text = { Text("Completa este anuncio para recibir 10 monedas de SamPlay. Puedes elegir no verlo y usar la app normalmente.") },
            confirmButton = {
                TextButton(onClick = {
                    rewardDialog = null
                    if (!ads.showRewarded(
                            activity = activity,
                            interstitial = format == RewardFormat.INTERSTITIAL,
                            onFinished = { message = "Si completaste el anuncio, se añadieron 10 monedas." },
                            onFailedToShow = { message = "No se pudo mostrar el anuncio. Inténtalo de nuevo." },
                        )
                    ) {
                        message = "El anuncio ya no está disponible. Puedes seguir usando SamPlay."
                        ads.loadRewards()
                    }
                }) { Text("Ver anuncio") }
            },
            dismissButton = { TextButton(onClick = { rewardDialog = null }) { Text("No, gracias") } },
        )
    }

    passDialog?.let { choice ->
        val price = if (choice is PassChoice.App) CoinRules.APP_PRICE else CoinRules.SECTION_PRICE
        val target = if (choice is PassChoice.Section) choice.section.label else "toda la app"
        AlertDialog(
            onDismissRequest = { passDialog = null },
            title = { Text("Canjear $price monedas") },
            text = { Text("¿Activar 24 horas sin anuncios de SamPlay en $target? Saldo: ${wallet.balance}. No afecta anuncios de proveedores externos.") },
            confirmButton = {
                TextButton(onClick = {
                    passDialog = null
                    // Do not charge when ads are disabled or consent is not available.
                    if (!ads.canShowAds) {
                        message = "Los anuncios están desactivados o no disponibles; no necesitas gastar monedas."
                    } else {
                        val status = when (choice) {
                            PassChoice.App -> coins.buyAppPass()
                            is PassChoice.Section -> coins.buySectionPass(choice.section)
                        }
                        message = when (status) {
                            CoinPurchaseStatus.ACTIVATED -> {
                                ads.onPassPurchased()
                                "Pase activado durante 24 horas."
                            }
                            CoinPurchaseStatus.INSUFFICIENT -> {
                                insufficientPrice = price
                                ""
                            }
                            CoinPurchaseStatus.ALREADY_ACTIVE -> "Ya tienes un pase activo para esta opción."
                        }
                    }
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { passDialog = null }) { Text("Cancelar") } },
        )
    }
    insufficientPrice?.let { price ->
        InsufficientCoinsDialog(balance = wallet.balance, price = price) { insufficientPrice = null }
    }
}

private fun expiryText(untilMs: Long): String = if (untilMs > 0L) {
    " hasta ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(untilMs))}"
} else ""

private enum class RewardFormat { STANDARD, INTERSTITIAL }
private sealed interface PassChoice {
    data object App : PassChoice
    data class Section(val section: AdSection) : PassChoice
}

internal tailrec fun Context.adActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.adActivity()
    else -> null
}
