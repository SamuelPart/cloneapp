package com.myiptv.clean.ui.compose

import com.myiptv.clean.navigation.SettingsRoutes

/** The portrait-style catalog belongs to phones/tablets, never to an Android TV device. */
internal val PHONE_CATALOG_ROUTES = setOf("home", "movies", "series", "kids", "anime")

internal fun usePhoneCatalogLayout(isTvDevice: Boolean, route: String): Boolean =
    !isTvDevice && route in PHONE_CATALOG_ROUTES

/** Search has its own touch layout on phones, including wide or landscape windows. */
internal fun usePhoneSearchLayout(isTvDevice: Boolean, route: String): Boolean =
    !isTvDevice && route == "search"

/** Settings and its coin subpage stay touch-oriented even when a phone is wide. */
internal fun usePhoneSettingsLayout(isTvDevice: Boolean, route: String): Boolean =
    !isTvDevice && (route == SettingsRoutes.SETTINGS || route == SettingsRoutes.COINS)

/** TV is unchanged; phone catalog, search and settings override width-based TV layout. */
internal fun useSamplayTvLayout(
    isTvDevice: Boolean,
    route: String,
    isPlayer: Boolean,
    isChannelDestination: Boolean,
    widthDp: Float
): Boolean = !usePhoneCatalogLayout(isTvDevice, route) && !usePhoneSearchLayout(isTvDevice, route) &&
    !usePhoneSettingsLayout(isTvDevice, route) &&
    useTvContentLayout(isTvDevice, isPlayer, isChannelDestination, widthDp)

/** The catalog tabs share the Inicio destination in the fixed five-item bottom bar. */
internal fun phoneBottomCatalogRoute(route: String): String =
    if (route in PHONE_CATALOG_ROUTES) "home" else route

/** Show three portrait covers on normal phones and adapt for compact or wider windows. */
internal fun phonePosterColumns(widthDp: Float): Int = when {
    widthDp < 280f -> 2
    widthDp < 600f -> 3
    widthDp < 840f -> 4
    else -> 5
}
