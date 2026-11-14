package com.myiptv.clean.navigation

/** Coins are a subpage of settings, not a new top-level destination. */
internal object SettingsRoutes {
    const val SETTINGS = "settings"
    const val COINS = "settings/coins"

    fun topLevel(route: String): String = if (route == COINS) SETTINGS else route
}
