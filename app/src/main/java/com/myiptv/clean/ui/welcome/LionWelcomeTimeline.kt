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
    const val BLINK_CLOSE_MS = 80L
    const val BLINK_OPEN_MS = 100L
    const val MIN_EYE_SCALE = 0.08f
    val expressions: List<LionExpression> = LionExpression.values().toList()
    val durationMs: Long = expressions.size * FRAME_DURATION_MS

    fun isFinished(elapsedMs: Long): Boolean = elapsedMs >= durationMs

    /** The final expression remains valid until the screen navigates away. */
    fun expressionAt(elapsedMs: Long): LionExpression {
        val bounded = elapsedMs.coerceIn(0L, durationMs - 1L)
        return expressions[(bounded / FRAME_DURATION_MS).toInt()]
    }

    /** Close before each one-second boundary and reopen with the next expression. */
    fun eyeScaleAt(elapsedMs: Long): Float {
        val bounded = elapsedMs.coerceIn(0L, durationMs - 1L)
        val index = (bounded / FRAME_DURATION_MS).toInt()
        val phase = bounded % FRAME_DURATION_MS
        val closedAmount = when {
            index > 0 && phase < BLINK_OPEN_MS -> 1f - smooth(phase.toFloat() / BLINK_OPEN_MS)
            index < expressions.lastIndex && phase >= FRAME_DURATION_MS - BLINK_CLOSE_MS ->
                smooth((phase - (FRAME_DURATION_MS - BLINK_CLOSE_MS)).toFloat() / BLINK_CLOSE_MS)
            else -> 0f
        }
        return 1f - closedAmount * (1f - MIN_EYE_SCALE)
    }

    /** Zero means an active blink needs another display frame; otherwise sleep until it starts. */
    fun millisUntilVisualUpdate(elapsedMs: Long): Long {
        if (isFinished(elapsedMs)) return 0L
        val bounded = elapsedMs.coerceAtLeast(0L)
        val index = (bounded / FRAME_DURATION_MS).toInt()
        val phase = bounded % FRAME_DURATION_MS
        if ((index > 0 && phase < BLINK_OPEN_MS) ||
            (index < expressions.lastIndex && phase >= FRAME_DURATION_MS - BLINK_CLOSE_MS)
        ) return 0L
        return if (index < expressions.lastIndex) {
            FRAME_DURATION_MS - BLINK_CLOSE_MS - phase
        } else {
            durationMs - bounded
        }
    }

    private fun smooth(progress: Float): Float = progress * progress * (3f - 2f * progress)
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
