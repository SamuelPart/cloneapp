package com.myiptv.clean.ui.compose

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class TvShelvesTest {
    @Test
    fun optionalRequestsRunConcurrentlyAndKeepTheirOwnResults() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        var started = 0
        suspend fun source(value: String): List<String> {
            started++
            if (started == 3) gate.complete(Unit)
            gate.await()
            return listOf(value)
        }

        val result = withTimeout(2_000) {
            loadTvShelfData(
                loadTrends = { source("trend") },
                loadDocumentaryMovies = { source("movie") },
                loadDocumentarySeries = { source("series") }
            )
        }
        assertEquals(listOf("trend"), result.trends)
        assertEquals(listOf("movie"), result.documentaryMovies)
        assertEquals(listOf("series"), result.documentarySeries)
        assertTrue(result.allSucceeded)
    }

    @Test
    fun partialFailureKeepsOtherSourcesAndIsEligibleForRetry() = runBlocking {
        val result = loadTvShelfData(
            loadTrends = { throw IOException("offline") },
            loadDocumentaryMovies = { listOf("shared", "film") },
            loadDocumentarySeries = { listOf("shared", "series") }
        )
        assertNull(result.trends)
        assertEquals(listOf("shared", "film", "series"), homeGridCatalog(
            result.documentaryMovies.orEmpty(), result.documentarySeries.orEmpty(), key = { it }
        ))
        assertFalse(result.allSucceeded)
    }

    @Test
    fun emptyResponsesAreSuccessfulAndNotFailures() = runBlocking {
        val result = loadTvShelfData(
            loadTrends = { emptyList<String>() },
            loadDocumentaryMovies = { emptyList() },
            loadDocumentarySeries = { emptyList() }
        )
        assertTrue(result.allSucceeded)
        assertEquals(emptyList<String>(), result.trends)
    }

    @Test
    fun cancellationIsPropagated() = runBlocking {
        var cancelled = false
        try {
            loadTvShelfData<String>(
                loadTrends = { throw CancellationException("cancelled") },
                loadDocumentaryMovies = { emptyList() },
                loadDocumentarySeries = { emptyList() }
            )
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
    }
}
