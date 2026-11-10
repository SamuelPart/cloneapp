package com.myiptv.clean.ui.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myiptv.clean.BuildConfig
import com.myiptv.clean.CleanIPTVApp
import com.myiptv.clean.ads.AdAudience
import com.myiptv.clean.data.repository.MediaPlaylist
import com.myiptv.clean.data.repository.MediaPlaylistRepository
import com.myiptv.clean.data.repository.TmdbRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val PHONE_SETTINGS_ANIMATION_MS = 220
private val PhoneSettingsCard = Color(0xFF191A20)
private val PhoneSettingsBorder = Color.White.copy(alpha = 0.08f)

/** Touch-only settings. TV keeps its existing screen, background, controls and focus behavior. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PhoneSettingsScreen(
    playlist: MediaPlaylist,
    onOpenCoins: () -> Unit,
    openUpdateRequest: Long? = null,
    onUpdateRequestHandled: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as CleanIPTVApp
    val ads = app.ads
    val wallet = app.coins.state
    val scope = rememberCoroutineScope()
    val syncStatus by MediaPlaylistRepository.syncStatus.collectAsState()
    val scrollState = rememberScrollState()
    val updatesRequester = remember { BringIntoViewRequester() }
    val currentOnUpdateHandled by rememberUpdatedState(onUpdateRequestHandled)
    var refreshing by remember { mutableStateOf(false) }
    var expandedSection by rememberSaveable { mutableStateOf<String?>(null) }
    val updatesFraction by animateFloatAsState(
        targetValue = if (expandedSection == PhoneSettingsSection.UPDATES.key) 1f else 0f,
        animationSpec = tween(durationMillis = PHONE_SETTINGS_ANIMATION_MS, easing = FastOutSlowInEasing),
        label = "settingsUpdateSection"
    )

    // A tapped GitHub update notification must still open the update controls, not a closed card.
    LaunchedEffect(openUpdateRequest) {
        val request = openUpdateRequest ?: return@LaunchedEffect
        try {
            expandedSection = phoneSettingsSectionForUpdate(expandedSection, request)
            withFrameNanos { }
            // Scroll after expansion so the controls, not just the collapsed header, are visible.
            snapshotFlow { expandedSection to updatesFraction }.first { (section, fraction) ->
                phoneSettingsUpdateWaitFinished(section, fraction)
            }
            if (expandedSection == PhoneSettingsSection.UPDATES.key) updatesRequester.bringIntoView()
        } finally {
            currentOnUpdateHandled(request)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 4.dp)) {
            Text("Ajustes", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("Todo en su lugar", color = TvMuted, style = MaterialTheme.typography.bodySmall)
        }
        PhoneSettingsCoinCard(wallet.balance, onOpenCoins)
        Text(
            "PREFERENCIAS",
            modifier = Modifier.padding(start = 2.dp, top = 10.dp, bottom = 2.dp),
            color = TvMuted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        PhoneSettingsSectionCard(
            title = "Catálogo y fuentes",
            subtitle = "Películas, series y canales",
            icon = Icons.Filled.Movie,
            expanded = expandedSection == PhoneSettingsSection.CATALOG.key,
            onToggle = { expandedSection = togglePhoneSettingsSection(expandedSection, PhoneSettingsSection.CATALOG) }
        ) {
            PhoneSettingsSource(
                Icons.Filled.Key,
                "Metadatos TMDb",
                if (TmdbRepository.isConfigured) "Hay una API Key local configurada."
                else "Falta la API Key local. Añádela en local.properties o en TMDB_API_KEY."
            )
            PhoneSettingsSource(Icons.Filled.LiveTv, "Canales de televisión", "Catálogo de emisiones en directo de distintos países.")
            PhoneSettingsSource(Icons.Filled.Movie, "Catálogo M3U", "${playlist.statusMessage} $syncStatus")
            PhoneSettingsAction(
                label = if (refreshing) "Buscando novedades…" else "Actualizar catálogo",
                refreshing = refreshing,
                onClick = {
                    if (!refreshing) {
                        refreshing = true
                        scope.launch {
                            try { MediaPlaylistRepository.refresh(context.applicationContext) }
                            finally { refreshing = false }
                        }
                    }
                }
            )
        }
        PhoneSettingsSectionCard(
            title = "Notificaciones",
            subtitle = "Avisos de novedades",
            icon = Icons.Filled.Notifications,
            expanded = expandedSection == PhoneSettingsSection.NOTIFICATIONS.key,
            onToggle = { expandedSection = togglePhoneSettingsSection(expandedSection, PhoneSettingsSection.NOTIFICATIONS) }
        ) {
            CatalogNotificationSettings()
        }
        PhoneSettingsSectionCard(
            title = "Actualizaciones",
            subtitle = "SamPlay ${BuildConfig.VERSION_NAME}",
            icon = Icons.Filled.Download,
            expanded = expandedSection == PhoneSettingsSection.UPDATES.key,
            onToggle = { expandedSection = togglePhoneSettingsSection(expandedSection, PhoneSettingsSection.UPDATES) },
            modifier = Modifier.bringIntoViewRequester(updatesRequester),
            animationFraction = updatesFraction
        ) {
            AppUpdateSettings()
        }
        PhoneSettingsSectionCard(
            title = "Privacidad de anuncios",
            subtitle = "Opciones de privacidad",
            icon = Icons.Filled.Lock,
            expanded = expandedSection == PhoneSettingsSection.AD_PRIVACY.key,
            onToggle = { expandedSection = togglePhoneSettingsSection(expandedSection, PhoneSettingsSection.AD_PRIVACY) }
        ) {
            if (context.adActivity() != null && ads.privacyOptionsRequired && ads.audience == AdAudience.ADULT) {
                AdPrivacySettings()
            } else {
                Text("No hay opciones adicionales de anuncios disponibles por ahora.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        PhoneSettingsSectionCard(
            title = "Información y atribuciones",
            subtitle = "SamPlay, TMDb y licencias",
            icon = Icons.Filled.Info,
            expanded = expandedSection == PhoneSettingsSection.ABOUT.key,
            onToggle = { expandedSection = togglePhoneSettingsSection(expandedSection, PhoneSettingsSection.ABOUT) }
        ) {
            Text("La clave TMDb se lee desde local.properties o la variable de entorno TMDB_API_KEY; no está incluida en Git.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
            Text("Este producto utiliza la API de TMDb, pero no está avalado ni certificado por TMDb.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
            Text("Tipografía Inter © The Inter Project Authors, distribuida bajo SIL Open Font License 1.1.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
            Text("La disponibilidad de las emisiones de televisión puede variar según el país y las condiciones de cada señal.", color = TvMuted, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "SamPlay · versión ${BuildConfig.VERSION_NAME}",
            modifier = Modifier.padding(start = 2.dp, top = 8.dp),
            color = TvMuted,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun PhoneSettingsCoinCard(balance: Int, onOpenCoins: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier.fillMaxWidth().clip(shape).background(PhoneSettingsCard)
            .border(1.dp, PhoneSettingsBorder, shape)
            .clickable(role = Role.Button, onClickLabel = "Abrir Monedas SamPlay", onClick = onOpenCoins)
            .heightIn(min = 84.dp).padding(horizontal = 15.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SamplayCoinIcon(Modifier.size(42.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Monedas SamPlay", color = Color.White, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$balance monedas · Ver recompensas", color = TvMuted,
                style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun PhoneSettingsSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    animationFraction: Float? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val fraction = animationFraction ?: animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = PHONE_SETTINGS_ANIMATION_MS, easing = FastOutSlowInEasing),
        label = "settingsSection"
    ).value
    val shape = RoundedCornerShape(12.dp)
    Column(modifier = modifier.fillMaxWidth().clip(shape).background(PhoneSettingsCard).border(1.dp, PhoneSettingsBorder, shape)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp)
                .clickable(role = Role.Button, onClickLabel = if (expanded) "Cerrar $title" else "Abrir $title", onClick = onToggle)
                .semantics { stateDescription = if (expanded) "Expandido" else "Contraído" }
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(PhoneChannelAccent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PhoneChannelAccent, modifier = Modifier.size(21.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = TvMuted, style = MaterialTheme.typography.bodySmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(23.dp).graphicsLayer { rotationZ = 180f * fraction }
            )
        }
        PhoneSettingsDetails(expanded = expanded, fraction = fraction, content = content)
    }
}

/**
 * The details stay composed when closed: hiding a section must not cancel a catalog permission
 * result or an in-progress APK download. Clipping, focus and semantics hide closed controls.
 */
@Composable
private fun PhoneSettingsDetails(
    expanded: Boolean,
    fraction: Float,
    content: @Composable ColumnScope.() -> Unit
) {
    // Do not override a child's own disabled focus state when the section is open.
    val inactive = if (expanded) Modifier else Modifier
        .focusProperties { canFocus = false }
        .clearAndSetSemantics { }
        .pointerInput(Unit) {
            // During the closing animation the clipped children must not receive stray taps.
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            }
        }
    Layout(
        modifier = Modifier.fillMaxWidth().clipToBounds().then(inactive),
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(PhoneSettingsBorder))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 15.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content
                )
            }
        }
    ) { measurables, constraints ->
        val child = measurables.single().measure(constraints.copy(minHeight = 0))
        val visibleHeight = phoneSettingsVisibleHeight(child.height, fraction).coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(child.width, visibleHeight) { child.placeRelative(0, 0) }
    }
}

@Composable
private fun PhoneSettingsSource(icon: ImageVector, title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = PhoneChannelAccent, modifier = Modifier.size(19.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(description, color = TvMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PhoneSettingsAction(label: String, refreshing: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(PhoneChannelAccent.copy(alpha = 0.18f))
            .clickable(enabled = !refreshing, role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp).padding(horizontal = 13.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (refreshing) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        else Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}
