package com.myiptv.clean.ads

/** One reward per SDK impression; a failed-to-show impression must never earn coins. */
internal class RewardCallbackGate {
    private var earned = false
    private var failedToShow = false

    fun onEarned(grant: () -> Unit) {
        if (earned || failedToShow) return
        earned = true
        grant()
    }

    fun onFailedToShow() {
        failedToShow = true
    }
}
