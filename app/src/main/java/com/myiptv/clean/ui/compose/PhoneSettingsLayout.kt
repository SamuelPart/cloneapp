package com.myiptv.clean.ui.compose

import kotlin.math.roundToInt

/** Identifiers are stable so an expanded section survives phone rotation. */
internal enum class PhoneSettingsSection(val key: String) {
    CATALOG("catalog"),
    NOTIFICATIONS("notifications"),
    UPDATES("updates"),
    AD_PRIVACY("ad_privacy"),
    ABOUT("about")
}

/** Only one section is expanded at a time; tapping it again closes it. */
internal fun togglePhoneSettingsSection(current: String?, selected: PhoneSettingsSection): String? =
    if (current == selected.key) null else selected.key

/** App-update notifications take the user directly to the actual update controls. */
internal fun phoneSettingsSectionForUpdate(current: String?, request: Long?): String? =
    if (request == null) current else PhoneSettingsSection.UPDATES.key

/** A fading-out settings entry must not consume the next entry's notification. */
internal fun phoneSettingsUpdateForEntry(request: Long?, isCurrentEntry: Boolean): Long? =
    if (isCurrentEntry) request else null

/** Completion of an older request must not discard a newer notification. */
internal fun phoneSettingsUpdateAfterHandled(pending: Long?, handled: Long): Long? =
    if (pending == handled) null else pending

/** Wait for the update panel to open, unless the user has chosen another section. */
internal fun phoneSettingsUpdateWaitFinished(section: String?, fraction: Float): Boolean =
    section != PhoneSettingsSection.UPDATES.key || fraction >= 1f

/** The full content stays composed while the visible height animates to or from zero. */
internal fun phoneSettingsVisibleHeight(contentHeight: Int, fraction: Float): Int {
    if (contentHeight <= 0) return 0
    return (contentHeight * fraction.coerceIn(0f, 1f)).roundToInt().coerceIn(0, contentHeight)
}
