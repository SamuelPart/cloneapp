package com.myiptv.clean.ui.player

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.FutureTask
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Reads response headers only. A separate request can be rejected by a provider that allows only
 * one concurrent stream, so even a 404 is corroborating evidence, not a reason by itself to skip.
 * The small bounded pool prevents stuck DNS lookups from accumulating when users surf channels.
 */
internal object LiveChannelProbe {
    private const val DEADLINE_MS = 3_500L
    private val executor = ThreadPoolExecutor(
        2, 2, 30L, TimeUnit.SECONDS, ArrayBlockingQueue(4),
        { work -> Thread(work, "samplay-channel-check").apply { isDaemon = true } }
    ).apply { allowCoreThreadTimeOut(true) }

    suspend fun check(url: String, userAgent: String?, referrer: String?): ChannelProbeResult =
        withTimeoutOrNull(DEADLINE_MS) {
            suspendCancellableCoroutine { continuation ->
                val task = FutureTask {
                    val result = probeChannelHeaders(url, userAgent, referrer)
                    if (continuation.isActive) continuation.resume(result)
                }
                continuation.invokeOnCancellation {
                    task.cancel(true)
                    executor.remove(task)
                }
                try {
                    executor.execute(task)
                } catch (_: RejectedExecutionException) {
                    if (continuation.isActive) continuation.resume(ChannelProbeResult.INCONCLUSIVE)
                }
            }
        } ?: ChannelProbeResult.INCONCLUSIVE
}

/** The connection factory and deadline are injectable for tests; no response body is downloaded. */
internal fun probeChannelHeaders(
    url: String,
    userAgent: String?,
    referrer: String?,
    openConnection: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
    deadlineMs: Long = 3_500L
): ChannelProbeResult {
    val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(deadlineMs.coerceAtLeast(1L))
    return try {
        var target = URL(url)
        for (hop in 0..3) {
            if (target.protocol.lowercase(Locale.ROOT) !in setOf("http", "https")) return ChannelProbeResult.INCONCLUSIVE
            val remaining = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime()).coerceAtMost(deadlineMs)
            if (remaining <= 0L || Thread.currentThread().isInterrupted) return ChannelProbeResult.INCONCLUSIVE
            val connection = openConnection(target)
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = minOf(2_000L, remaining).toInt().coerceAtLeast(1)
                connection.readTimeout = minOf(1_500L, remaining).toInt().coerceAtLeast(1)
                connection.useCaches = false
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", safePlaybackHeader(userAgent, 512) ?: "SamPlay/1.0")
                safePlaybackHeader(referrer, 2_048)?.let { connection.setRequestProperty("Referer", it) }
                // Some servers ignore Range or reject a second request; neither should stop VLC.
                connection.setRequestProperty("Range", "bytes=0-0")
                val response = connection.responseCode
                when {
                    response in 200..299 -> return ChannelProbeResult.RESPONDING
                    response == 404 || response == 410 -> return ChannelProbeResult.UNAVAILABLE
                    response in 300..399 && hop < 3 -> {
                        val location = connection.getHeaderField("Location") ?: return ChannelProbeResult.INCONCLUSIVE
                        target = URL(target, location)
                    }
                    else -> return ChannelProbeResult.INCONCLUSIVE
                }
            } finally {
                connection.disconnect()
            }
        }
        ChannelProbeResult.INCONCLUSIVE
    } catch (_: Exception) {
        // Includes timeouts, TLS errors, DNS failures, malformed URLs and transient network loss.
        ChannelProbeResult.INCONCLUSIVE
    }
}
