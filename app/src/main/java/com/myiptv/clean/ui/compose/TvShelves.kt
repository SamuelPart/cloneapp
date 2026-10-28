package com.myiptv.clean.ui.compose

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** A null list means the source failed; an empty list is a successful empty response. */
internal data class TvShelfResult<T>(
    val trends: List<T>?,
    val documentaryMovies: List<T>?,
    val documentarySeries: List<T>?
) {
    val allSucceeded: Boolean
        get() = trends != null && documentaryMovies != null && documentarySeries != null
}

/** Loads optional TV rows independently so one unavailable source cannot discard the others. */
internal suspend fun <T> loadTvShelfData(
    loadTrends: suspend () -> List<T>,
    loadDocumentaryMovies: suspend () -> List<T>,
    loadDocumentarySeries: suspend () -> List<T>
): TvShelfResult<T> = coroutineScope {
    val trends = async { tryLoadTvShelf(loadTrends) }
    val movies = async { tryLoadTvShelf(loadDocumentaryMovies) }
    val series = async { tryLoadTvShelf(loadDocumentarySeries) }
    TvShelfResult(trends.await(), movies.await(), series.await())
}

private suspend fun <T> tryLoadTvShelf(load: suspend () -> List<T>): List<T>? = try {
    load()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
}
