package com.myiptv.clean.ui.compose

/** Keeps both kinds of real catalog content visible without repeating a title. */
internal fun <T : Any> interleaveCatalogs(first: List<T>, second: List<T>): List<T> = buildList {
    repeat(maxOf(first.size, second.size)) { index ->
        first.getOrNull(index)?.let { add(it) }
        second.getOrNull(index)?.let { add(it) }
    }
}

internal fun <T : Any, K> featuredCatalog(
    featured: T?,
    movies: List<T>,
    series: List<T>,
    key: (T) -> K
): List<T> = (listOfNotNull(featured) + interleaveCatalogs(movies, series))
    .distinctBy(key)
    .take(4)

internal fun <T : Any, K> homeGridCatalog(movies: List<T>, series: List<T>, key: (T) -> K): List<T> =
    interleaveCatalogs(movies, series).distinctBy(key).take(12)

internal fun nextCarouselIndex(current: Int, count: Int): Int =
    if (count <= 1) 0 else (current.coerceAtLeast(0) % count + 1) % count
