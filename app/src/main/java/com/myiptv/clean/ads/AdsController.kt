package com.myiptv.clean.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.myiptv.clean.CleanIPTVApp
import com.myiptv.clean.ui.compose.isTelevisionDevice
import java.lang.ref.WeakReference

/**
 * All public entry points are called on the main thread. No Activity is retained strongly.
 * Android TV, unknown/under-age audiences, and unavailable consent never request ads.
 */
internal class AdsController(private val app: Application) {
    private val prefs = app.getSharedPreferences("samplay_ads", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    private val isTv = app.isTelevisionDevice()
    private val coins get() = (app as CleanIPTVApp).coins
    private val consent by lazy { UserMessagingPlatform.getConsentInformation(app) }
    private var resumedActivity: WeakReference<Activity>? = null
    private var resumedOnWelcome = false
    private var contentAllowsConsentUi = false

    var audience by mutableStateOf(readAudience())
        private set
    var consentUiActive by mutableStateOf(false)
        private set
    var privacyOptionsRequired by mutableStateOf(false)
        private set
    private var consentAllowsRequests by mutableStateOf(false)
    private var sdkReady by mutableStateOf(false)
    var adFreeUntilMs by mutableLongStateOf(prefs.getLong(KEY_AD_FREE_UNTIL, 0L))
        private set
    var rewardedAvailable by mutableStateOf(false)
        private set
    var rewardedInterstitialAvailable by mutableStateOf(false)
        private set
    var rewardsLoading by mutableStateOf(false)
        private set
    var fullscreenShowing by mutableStateOf(false)
        private set

    private var updateAttempted = false
    private var updateInFlight = false
    private var formPending = false
    private var formLoading = false
    private var loadedConsentForm: ConsentForm? = null
    private var consentGeneration = 0
    private var sdkStarting = false
    private var generation = 0
    private var openAd: AppOpenAd? = null
    private var openLoadedAt = 0L
    private var openLoading = false
    private var rewardedAd: RewardedAd? = null
    private var rewardedLoadedAt = 0L
    private var rewardedLoading = false
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var rewardedInterstitialLoadedAt = 0L
    private var rewardedInterstitialLoading = false

    val canShowAds: Boolean
        get() = AdsRules.canRequest(
            isTv = isTv,
            audience = audience,
            consentAllowsRequests = consentAllowsRequests && !consentUiActive,
            sdkReady = sdkReady,
            // Preserve the old 30-minute reward until it expires when users update.
            adFreeUntilMs = maxOf(adFreeUntilMs, coins.state.appAdFreeUntil),
            nowMs = System.currentTimeMillis(),
        )

    val isAdFree: Boolean get() = AdsRules.isAdFree(adFreeUntilMs, System.currentTimeMillis()) || coins.isAppAdFree()

    fun isSectionAdFree(section: AdSection): Boolean = coins.isSectionAdFree(section)
    fun canShowAdsIn(section: AdSection): Boolean = canShowAds && !isSectionAdFree(section)

    /** Drop cached creatives after a purchase; they must not appear under a new pass. */
    fun onPassPurchased() {
        invalidatePreloadedAds()
    }

    /** Called on the first resume of a launcher/notification start and subsequent resumes. */
    fun onActivityResumed(activity: Activity, newLaunch: Boolean = false, isWelcome: Boolean = false) {
        if (isTv) return
        resumedActivity = WeakReference(activity)
        resumedOnWelcome = isWelcome
        refreshAdFreeExpiry()
        if (newLaunch && !updateInFlight && !consentUiActive) {
            updateAttempted = false
            consentGeneration++
            formPending = false
            formLoading = false
            loadedConsentForm = null
            consentAllowsRequests = false
            invalidatePreloadedAds()
        }
        if (audience != AdAudience.ADULT) return
        if (formPending) showPendingConsentForm() else requestConsentIfNeeded(activity)
        if (isWelcome) loadAppOpenIfAppropriate()
    }

    /** Consent UI waits until a safe browsing screen is resumed, never during playback. */
    fun setContentAllowsConsentUi(allowed: Boolean) {
        contentAllowsConsentUi = allowed
        if (!allowed || isTv || audience != AdAudience.ADULT) return
        if (formPending) showPendingConsentForm()
        else if (consentAllowsRequests) initializeSdk()
    }

    fun onActivityPaused(activity: Activity) {
        if (resumedActivity?.get() === activity) {
            resumedActivity = null
            resumedOnWelcome = false
        }
    }

    fun saveAudience(value: AdAudience, activity: Activity) {
        if (isTv || audience != AdAudience.UNKNOWN || value == AdAudience.UNKNOWN) return
        // Store only the category; never persist or send the submitted birth date.
        audience = value
        prefs.edit().putString(KEY_AUDIENCE, value.name).apply()
        if (value == AdAudience.ADULT) {
            resumedActivity = WeakReference(activity)
            requestConsentIfNeeded(activity)
        }
    }

    fun markAppUsed() {
        if (!isTv) prefs.edit().putBoolean(KEY_HAS_USED_APP, true).apply()
    }

    private fun readAudience(): AdAudience = when (prefs.getString(KEY_AUDIENCE, null)) {
        AdAudience.ADULT.name -> AdAudience.ADULT
        AdAudience.UNDER_AGE.name -> AdAudience.UNDER_AGE
        else -> AdAudience.UNKNOWN
    }

    private fun requestConsentIfNeeded(activity: Activity) {
        if (isTv || audience != AdAudience.ADULT || updateAttempted || updateInFlight || consentUiActive) return
        updateAttempted = true
        updateInFlight = true
        val requestGeneration = consentGeneration
        try {
            // UMP requires an update for each app launch, before any ad request.
            consent.requestConsentInfoUpdate(
                activity,
                ConsentRequestParameters.Builder().build(),
                {
                    if (requestGeneration == consentGeneration) {
                        updateInFlight = false
                        handleUpdatedConsent()
                    }
                },
                { error ->
                    if (requestGeneration == consentGeneration) {
                        updateInFlight = false
                        Log.w(TAG, "Consent update unavailable: ${error.message}")
                        finishConsentFlow()
                    }
                },
            )
        } catch (error: RuntimeException) {
            updateInFlight = false
            Log.w(TAG, "Consent update could not start", error)
            // Fail closed if UMP cannot be consulted.
            consentAllowsRequests = false
        }
    }

    /** Equivalent to UMP's load-and-show-if-required, but showing is deferred until safe. */
    private fun handleUpdatedConsent() {
        try {
            if (consent.consentStatus != ConsentInformation.ConsentStatus.REQUIRED || !consent.isConsentFormAvailable) {
                formPending = false
                finishConsentFlow()
                return
            }
            formPending = true
            if (formLoading) return
            formLoading = true
            val requestGeneration = consentGeneration
            UserMessagingPlatform.loadConsentForm(
                app,
                { form ->
                    if (requestGeneration == consentGeneration) {
                        formLoading = false
                        loadedConsentForm = form
                        showPendingConsentForm()
                    }
                },
                { error ->
                    if (requestGeneration == consentGeneration) {
                        formLoading = false
                        formPending = false
                        Log.w(TAG, "Consent form unavailable: ${error.message}")
                        finishConsentFlow()
                    }
                },
            )
        } catch (error: RuntimeException) {
            Log.w(TAG, "Consent form could not be loaded", error)
            formLoading = false
            formPending = false
            consentAllowsRequests = false
        }
    }

    private fun showPendingConsentForm() {
        if (!formPending || consentUiActive || resumedOnWelcome || !contentAllowsConsentUi) return
        val form = loadedConsentForm ?: return
        val activity = resumedActivity?.get()?.takeUnless { it.isFinishing || it.isDestroyed } ?: return
        loadedConsentForm = null // UMP forms may only be shown once.
        formPending = false
        consentUiActive = true
        try {
            form.show(activity) { error ->
                if (error != null) Log.w(TAG, "Consent form could not be completed: ${error.message}")
                consentUiActive = false
                finishConsentFlow()
            }
        } catch (error: RuntimeException) {
            Log.w(TAG, "Consent form could not be shown", error)
            consentUiActive = false
            consentAllowsRequests = false
        }
    }

    private fun finishConsentFlow() {
        if (isTv || audience != AdAudience.ADULT) return
        try {
            privacyOptionsRequired = consent.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
            // UMP may use a valid prior-session choice when an update fails.
            consentAllowsRequests = consent.canRequestAds()
            if (consentAllowsRequests) initializeSdk() else invalidatePreloadedAds()
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not check consent", error)
            consentAllowsRequests = false
            invalidatePreloadedAds()
        }
    }

    fun showPrivacyOptions(activity: Activity, onComplete: (Boolean) -> Unit) {
        if (isTv || audience != AdAudience.ADULT || !privacyOptionsRequired || consentUiActive ||
            activity.isFinishing || activity.isDestroyed
        ) {
            onComplete(false)
            return
        }
        consentUiActive = true
        consentAllowsRequests = false
        invalidatePreloadedAds()
        try {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
                if (error != null) Log.w(TAG, "Privacy options unavailable: ${error.message}")
                consentUiActive = false
                finishConsentFlow()
                onComplete(error == null)
            }
        } catch (error: RuntimeException) {
            Log.w(TAG, "Privacy options could not be shown", error)
            consentUiActive = false
            // Do not assume prior consent if we cannot query UMP.
            finishConsentFlow()
            onComplete(false)
        }
    }

    private fun initializeSdk() {
        if (isTv || audience != AdAudience.ADULT || !consentAllowsRequests || consentUiActive ||
            (!resumedOnWelcome && !contentAllowsConsentUi)
        ) return
        if (sdkReady) {
            loadAppOpenIfAppropriate()
            return
        }
        if (sdkStarting) return
        sdkStarting = true
        // Google's SDK can perform initialization work off the UI thread.
        Thread {
            try {
                MobileAds.initialize(app) {
                    main.post {
                        sdkStarting = false
                        sdkReady = true
                        loadAppOpenIfAppropriate()
                    }
                }
            } catch (error: RuntimeException) {
                main.post {
                    sdkStarting = false
                    Log.w(TAG, "Mobile Ads initialization failed", error)
                }
            }
        }.start()
    }

    private fun invalidatePreloadedAds() {
        generation++
        openAd = null
        openLoadedAt = 0L
        openLoading = false
        rewardedAd = null
        rewardedLoadedAt = 0L
        rewardedLoading = false
        rewardedAvailable = false
        rewardedInterstitialAd = null
        rewardedInterstitialLoadedAt = 0L
        rewardedInterstitialLoading = false
        rewardedInterstitialAvailable = false
        rewardsLoading = false
    }

    private fun loadAppOpenIfAppropriate() {
        // Never spend bandwidth preloading an app-open ad during browsing or playback.
        val now = System.currentTimeMillis()
        if (!resumedOnWelcome || !canShowAdsIn(AdSection.HOME) || fullscreenShowing || openLoading || openAd != null ||
            !AdsRules.canShowAppOpen(
                prefs.getBoolean(KEY_HAS_USED_APP, false), now, prefs.getLong(KEY_LAST_OPEN, 0L), now,
            )
        ) return
        val requestGeneration = generation
        openLoading = true
        try {
            AppOpenAd.load(app, AdUnits.appOpen, AdRequest.Builder().build(), object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    if (requestGeneration != generation) return
                    openLoading = false
                    if (canShowAdsIn(AdSection.HOME) && resumedOnWelcome) {
                        openAd = ad
                        openLoadedAt = System.currentTimeMillis()
                    }
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (requestGeneration != generation) return
                    openLoading = false
                    Log.d(TAG, "App-open ad unavailable: ${error.code}")
                }
            })
        } catch (error: RuntimeException) {
            openLoading = false
            Log.w(TAG, "App-open ad could not be requested", error)
        }
    }

    /** Never waits for an ad: it is shown only on the welcome screen if already loaded. */
    fun showAppOpenOnWelcome(activity: Activity, onFinished: () -> Unit): Boolean {
        val now = System.currentTimeMillis()
        val ad = openAd ?: return false
        if (!resumedOnWelcome || resumedActivity?.get() !== activity || !canShowAdsIn(AdSection.HOME) || fullscreenShowing ||
            activity.isFinishing || activity.isDestroyed || !AdsRules.canShowAppOpen(
                prefs.getBoolean(KEY_HAS_USED_APP, false), openLoadedAt,
                prefs.getLong(KEY_LAST_OPEN, 0L), now,
            )
        ) return false
        openAd = null
        openLoadedAt = 0L
        fullscreenShowing = true
        var finished = false
        fun finishOnce() {
            if (!finished) {
                finished = true
                fullscreenShowing = false
                onFinished()
            }
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                prefs.edit().putLong(KEY_LAST_OPEN, System.currentTimeMillis()).apply()
            }
            override fun onAdDismissedFullScreenContent() = finishOnce()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = finishOnce()
        }
        return try {
            ad.show(activity)
            true
        } catch (error: RuntimeException) {
            Log.w(TAG, "App-open ad could not be shown", error)
            finishOnce()
            true
        }
    }

    /** Preload on eligible browsing/detail screens or a safe phone preview, never in Kids/TV. */
    fun loadRewards() {
        refreshAdFreeExpiry()
        if (!canShowAds || fullscreenShowing) return
        val now = System.currentTimeMillis()
        if (rewardedAd != null && !AdsRules.isRewardFresh(rewardedLoadedAt, now)) {
            rewardedAd = null
            rewardedAvailable = false
        }
        if (rewardedInterstitialAd != null && !AdsRules.isRewardFresh(rewardedInterstitialLoadedAt, now)) {
            rewardedInterstitialAd = null
            rewardedInterstitialAvailable = false
        }
        val requestGeneration = generation
        if (rewardedAd == null && !rewardedLoading) {
            rewardedLoading = true
            syncRewardLoading()
            try {
                RewardedAd.load(app, AdUnits.rewarded, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        if (requestGeneration != generation) return
                        rewardedLoading = false
                        if (canShowAds) {
                            rewardedAd = ad
                            rewardedLoadedAt = System.currentTimeMillis()
                            rewardedAvailable = true
                        }
                        syncRewardLoading()
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        if (requestGeneration != generation) return
                        rewardedLoading = false
                        syncRewardLoading()
                        Log.d(TAG, "Rewarded ad unavailable: ${error.code}")
                    }
                })
            } catch (error: RuntimeException) {
                rewardedLoading = false
                syncRewardLoading()
                Log.w(TAG, "Rewarded ad could not be requested", error)
            }
        }
        if (rewardedInterstitialAd == null && !rewardedInterstitialLoading) {
            rewardedInterstitialLoading = true
            syncRewardLoading()
            try {
                RewardedInterstitialAd.load(app, AdUnits.rewardedInterstitial, AdRequest.Builder().build(), object : RewardedInterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedInterstitialAd) {
                        if (requestGeneration != generation) return
                        rewardedInterstitialLoading = false
                        if (canShowAds) {
                            rewardedInterstitialAd = ad
                            rewardedInterstitialLoadedAt = System.currentTimeMillis()
                            rewardedInterstitialAvailable = true
                        }
                        syncRewardLoading()
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        if (requestGeneration != generation) return
                        rewardedInterstitialLoading = false
                        syncRewardLoading()
                        Log.d(TAG, "Rewarded interstitial unavailable: ${error.code}")
                    }
                })
            } catch (error: RuntimeException) {
                rewardedInterstitialLoading = false
                syncRewardLoading()
                Log.w(TAG, "Rewarded interstitial could not be requested", error)
            }
        }
    }

    private fun syncRewardLoading() {
        rewardsLoading = rewardedLoading || rewardedInterstitialLoading
    }

    fun hasFreshReward(interstitial: Boolean, nowMs: Long = System.currentTimeMillis()): Boolean =
        if (interstitial) rewardedInterstitialAd != null && AdsRules.isRewardFresh(rewardedInterstitialLoadedAt, nowMs)
        else rewardedAd != null && AdsRules.isRewardFresh(rewardedLoadedAt, nowMs)

    /** Only explicit user consent calls this; the SDK's reward callback is the sole credit source. */
    fun showRewarded(
        activity: Activity,
        interstitial: Boolean,
        onFinished: () -> Unit,
        onFailedToShow: () -> Unit = {},
    ): Boolean {
        if (!canShowAds || fullscreenShowing || resumedActivity?.get() !== activity || activity.isFinishing || activity.isDestroyed) return false
        if (!hasFreshReward(interstitial)) {
            loadRewards()
            return false
        }
        val standardAd = if (interstitial) null else rewardedAd
        val interstitialAd = if (interstitial) rewardedInterstitialAd else null
        if (standardAd == null && interstitialAd == null) return false
        if (interstitial) {
            rewardedInterstitialAd = null
            rewardedInterstitialAvailable = false
        } else {
            rewardedAd = null
            rewardedAvailable = false
        }
        fullscreenShowing = true
        val rewardGate = RewardCallbackGate()
        var finished = false
        fun finishOnce(callback: () -> Unit) {
            if (!finished) {
                finished = true
                fullscreenShowing = false
                callback()
            }
        }
        fun creditOnce() {
            // Some mediated networks may deliver this callback after dismissal.
            rewardGate.onEarned { coins.creditCompletedAd() }
        }
        val callback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = finishOnce(onFinished)
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardGate.onFailedToShow()
                Log.d(TAG, "Reward ad could not be shown: ${error.code}")
                finishOnce(onFailedToShow)
            }
        }
        return try {
            if (interstitialAd != null) {
                interstitialAd.fullScreenContentCallback = callback
                interstitialAd.show(activity) { creditOnce() }
            } else {
                standardAd!!.fullScreenContentCallback = callback
                standardAd.show(activity) { creditOnce() }
            }
            true
        } catch (error: RuntimeException) {
            rewardGate.onFailedToShow()
            fullscreenShowing = false
            Log.w(TAG, "Reward ad could not be shown", error)
            false
        }
    }

    fun refreshAdFreeExpiry() {
        if (adFreeUntilMs > 0L && !AdsRules.isAdFree(adFreeUntilMs, System.currentTimeMillis())) {
            adFreeUntilMs = 0L
            prefs.edit().remove(KEY_AD_FREE_UNTIL).apply()
            loadAppOpenIfAppropriate()
        }
    }

    private companion object {
        const val TAG = "SamPlayAds"
        const val KEY_AUDIENCE = "audience"
        const val KEY_HAS_USED_APP = "has_used_app"
        const val KEY_AD_FREE_UNTIL = "ad_free_until"
        const val KEY_LAST_OPEN = "last_app_open"
    }
}
