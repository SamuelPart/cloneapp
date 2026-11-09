package com.myiptv.clean.ads

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.myiptv.clean.R

/** Top-edge banner with a fixed, non-clickable buffer above the app's controls. */
@Composable
internal fun SamplayBanner(ads: AdsController, section: AdSection) {
    // Reserve the slot before the network responds, preventing delayed ad loads from
    // shifting buttons under the user's finger. It never exists for minors or ad-free time.
    if (ads.audience != AdAudience.ADULT || ads.isAdFree || ads.isSectionAdFree(section)) return
    val context = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = maxWidth.value.toInt()
        if (width < 320) return@BoxWithConstraints
        val adSize = remember(context, width) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width)
        }
        if (adSize.height <= 0) return@BoxWithConstraints
        Column(Modifier.fillMaxWidth().background(Color(0xFF151515))) {
            Text(
                text = "Anuncio",
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                color = Color(0xFFBDBDBD),
                fontSize = 10.sp,
            )
            if (ads.canShowAds) {
                BannerAdView(ads, adSize, width)
            } else {
                Spacer(Modifier.fillMaxWidth().height(adSize.height.dp))
            }
            // Non-clickable separation from the app bar and its navigation buttons.
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BannerAdView(ads: AdsController, adSize: AdSize, width: Int) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val banner = remember(context, width) {
        AdView(context).apply {
            adUnitId = AdUnits.banner
            setAdSize(adSize)
        }
    }
    DisposableEffect(banner, owner) {
        banner.adListener = object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.d("SamPlayAds", "Banner unavailable: ${error.code}")
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) banner.resume()
            if (event == Lifecycle.Event.ON_PAUSE) banner.pause()
        }
        owner.lifecycle.addObserver(observer)
        try {
            if (ads.canShowAds) banner.loadAd(AdRequest.Builder().build())
        } catch (error: RuntimeException) {
            Log.w("SamPlayAds", "Banner could not be requested", error)
        }
        onDispose {
            owner.lifecycle.removeObserver(observer)
            banner.destroy()
        }
    }
    AndroidView(factory = { banner }, modifier = Modifier.fillMaxWidth().height(adSize.height.dp))
}

/** A visibly labelled native ad in the home feed. No ad occupies space when loading fails. */
@Composable
internal fun SamplayNativeAd(ads: AdsController) {
    if (!ads.canShowAdsIn(AdSection.HOME)) return
    val context = LocalContext.current
    var nativeAd by remember(context) { mutableStateOf<NativeAd?>(null) }
    var loading by remember(context) { mutableStateOf(true) }
    DisposableEffect(context) {
        var disposed = false
        try {
            val loader = AdLoader.Builder(context, AdUnits.native)
                .forNativeAd { ad ->
                    if (disposed || !ads.canShowAdsIn(AdSection.HOME)) ad.destroy()
                    else {
                        loading = false
                        nativeAd?.destroy()
                        nativeAd = ad
                    }
                }
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                        .build()
                )
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        if (!disposed) loading = false
                        Log.d("SamPlayAds", "Native ad unavailable: ${error.code}")
                    }
                })
                .build()
            loader.loadAd(AdRequest.Builder().build())
        } catch (error: RuntimeException) {
            loading = false
            Log.w("SamPlayAds", "Native ad could not be requested", error)
        }
        onDispose {
            disposed = true
            nativeAd?.destroy()
            nativeAd = null
        }
    }
    val ad = nativeAd
    if (ad != null) {
        AndroidView(
            factory = { buildNativeAdView(it, ad) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).heightIn(min = 320.dp),
        )
    } else if (loading) {
        // Reserve the creative's space while loading to avoid shifting tappable content.
        Spacer(Modifier.fillMaxWidth().padding(vertical = 12.dp).height(320.dp))
    }
}

private fun buildNativeAdView(context: Context, ad: NativeAd): NativeAdView {
    val view = LayoutInflater.from(context).inflate(R.layout.samplay_native_ad, null) as NativeAdView
    val headline = view.findViewById<TextView>(R.id.ad_headline)
    val body = view.findViewById<TextView>(R.id.ad_body)
    val icon = view.findViewById<ImageView>(R.id.ad_icon)
    val media = view.findViewById<MediaView>(R.id.ad_media)
    val action = view.findViewById<Button>(R.id.ad_call_to_action)

    headline.text = ad.headline
    view.headlineView = headline
    body.text = ad.body
    body.visibility = if (ad.body == null) View.GONE else View.VISIBLE
    view.bodyView = body
    icon.setImageDrawable(ad.icon?.drawable)
    icon.visibility = if (ad.icon == null) View.GONE else View.VISIBLE
    view.iconView = icon
    media.mediaContent = ad.mediaContent
    view.mediaView = media
    action.text = ad.callToAction
    action.visibility = if (ad.callToAction == null) View.GONE else View.VISIBLE
    view.callToActionView = action
    // Registers the assets, impressions, clicks and Google's AdChoices overlay.
    view.setNativeAd(ad)
    return view
}
