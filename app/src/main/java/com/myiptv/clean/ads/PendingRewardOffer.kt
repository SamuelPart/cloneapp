package com.myiptv.clean.ads

/** Re-check the target and ad availability when a user acts on an older invitation. */
internal enum class PendingRewardDecision { CANCEL, CONTINUE_FREE, AVAILABLE }

internal fun pendingRewardDecision(
    sameOrigin: Boolean,
    sameContent: Boolean,
    eligibleForOffer: Boolean,
): PendingRewardDecision = when {
    !sameOrigin || !sameContent -> PendingRewardDecision.CANCEL
    !eligibleForOffer -> PendingRewardDecision.CONTINUE_FREE
    else -> PendingRewardDecision.AVAILABLE
}
