package com.myiptv.clean.ads

import com.myiptv.clean.BuildConfig

internal data class AdUnitIds(
    val banner: String,
    val appOpen: String,
    val rewardedInterstitial: String,
    val native: String,
    val rewarded: String,
)

/** Google's demo units prevent accidental live ad traffic in debug builds. */
internal fun adUnitsForBuild(debug: Boolean): AdUnitIds = if (debug) {
    AdUnitIds(
        banner = "ca-app-pub-3940256099942544/9214589741",
        appOpen = "ca-app-pub-3940256099942544/9257395921",
        rewardedInterstitial = "ca-app-pub-3940256099942544/5354046379",
        native = "ca-app-pub-3940256099942544/2247696110",
        rewarded = "ca-app-pub-3940256099942544/5224354917",
    )
} else {
    AdUnitIds(
        banner = "ca-app-pub-8124327134735952/9929993078",
        appOpen = "ca-app-pub-8124327134735952/3632003026",
        rewardedInterstitial = "ca-app-pub-8124327134735952/1706733175",
        native = "ca-app-pub-8124327134735952/7913048878",
        rewarded = "ca-app-pub-8124327134735952/5286885530",
    )
}

internal object AdUnits {
    private val ids = adUnitsForBuild(BuildConfig.DEBUG)
    val banner get() = ids.banner
    val appOpen get() = ids.appOpen
    val rewardedInterstitial get() = ids.rewardedInterstitial
    val native get() = ids.native
    val rewarded get() = ids.rewarded
}
