package com.myiptv.clean.ads

import com.myiptv.clean.BuildConfig

internal data class AdUnitIds(
    val banner: String,
    val appOpen: String,
    val rewardedInterstitial: String,
    val native: String,
    val rewarded: String,
)

/** The owner requested production unit IDs for both builds. Test devices must be registered first. */
@Suppress("UNUSED_PARAMETER")
internal fun adUnitsForBuild(debug: Boolean): AdUnitIds = AdUnitIds(
    banner = "ca-app-pub-8124327134735952/9929993078",
    appOpen = "ca-app-pub-8124327134735952/3632003026",
    rewardedInterstitial = "ca-app-pub-8124327134735952/1706733175",
    native = "ca-app-pub-8124327134735952/7913048878",
    rewarded = "ca-app-pub-8124327134735952/5286885530",
)

internal object AdUnits {
    private val ids = adUnitsForBuild(BuildConfig.DEBUG)
    val banner get() = ids.banner
    val appOpen get() = ids.appOpen
    val rewardedInterstitial get() = ids.rewardedInterstitial
    val native get() = ids.native
    val rewarded get() = ids.rewarded
}
