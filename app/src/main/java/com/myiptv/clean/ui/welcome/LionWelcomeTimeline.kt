package com.myiptv.clean.ui.welcome

/** The expressions follow the user's reference from left to right, top to bottom. */
internal enum class LionExpression {
    HALF_CLOSED,
    WORRIED,
    WINK,
    DETERMINED,
    SKEPTICAL,
    NEUTRAL,
    HAPPY,
    SLEEPY,
    CURIOUS,
    DIZZY,
    CONNECTED,
    SQUEEZED,
}

internal object LionWelcomeTimeline {
    const val FRAME_DURATION_MS = 1_000L
    val expressions: List<LionExpression> = LionExpression.values().toList()
    val durationMs: Long = expressions.size * FRAME_DURATION_MS

    fun isFinished(elapsedMs: Long): Boolean = elapsedMs >= durationMs

    /** The final expression remains valid until the screen navigates away. */
    fun expressionAt(elapsedMs: Long): LionExpression {
        val bounded = elapsedMs.coerceIn(0L, durationMs - 1L)
        return expressions[(bounded / FRAME_DURATION_MS).toInt()]
    }

    fun millisUntilNextFrame(elapsedMs: Long): Long {
        if (isFinished(elapsedMs)) return 0L
        val bounded = elapsedMs.coerceAtLeast(0L)
        return FRAME_DURATION_MS - (bounded % FRAME_DURATION_MS)
    }
}

/** Ensures that simultaneous timer and user actions do not open the content twice. */
internal class WelcomeNavigationGate {
    private var opened = false

    fun tryOpen(): Boolean {
        if (opened) return false
        opened = true
        return true
    }
}

/** Counts only the time the welcome screen is resumed; backgrounding cannot launch the app. */
internal class LionVisibleClock(initialElapsedMs: Long = 0L) {
    private var accumulatedMs = initialElapsedMs.coerceIn(0L, LionWelcomeTimeline.durationMs)
    private var resumedAtMs: Long? = null

    fun resume(nowMs: Long) {
        if (resumedAtMs == null) resumedAtMs = nowMs
    }

    fun pause(nowMs: Long) {
        accumulatedMs = elapsedAt(nowMs)
        resumedAtMs = null
    }

    fun elapsedAt(nowMs: Long): Long {
        val start = resumedAtMs ?: return accumulatedMs
        val activeMs = (nowMs - start).coerceAtLeast(0L)
        val remainingMs = LionWelcomeTimeline.durationMs - accumulatedMs
        return accumulatedMs + activeMs.coerceAtMost(remainingMs)
    }
}
