package com.myiptv.clean.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Test

class TvHomeLayoutTest {
    @Test
    fun interleavesCatalogsOfDifferentLengths() {
        assertEquals(
            listOf("m1", "s1", "m2", "s2", "m3"),
            interleaveCatalogs(listOf("m1", "m2", "m3"), listOf("s1", "s2"))
        )
        assertEquals(listOf("s1"), interleaveCatalogs(emptyList(), listOf("s1")))
    }

    @Test
    fun featuredTitlesAreUniqueAndLimited() {
        val result = featuredCatalog(
            featured = "m1",
            movies = listOf("m1", "m2", "m3"),
            series = listOf("s1", "s2", "s3"),
            key = { it }
        )
        assertEquals(listOf("m1", "s1", "m2", "s2"), result)
        assertEquals(emptyList<String>(), featuredCatalog(null, emptyList<String>(), emptyList(), key = { it }))
    }

    @Test
    fun gridShowsBothKindsWithoutDuplicatesAndCapsItsSize() {
        val movies = (1..10).map { "m$it" }
        val series = listOf("m1") + (1..10).map { "s$it" }
        val result = homeGridCatalog(movies, series, key = { it })
        assertEquals(12, result.size)
        assertEquals(result.size, result.distinct().size)
        assertEquals(listOf("m1", "m2", "s1", "m3"), result.take(4))
    }

    @Test
    fun carouselHandlesEmptyAndChangedCatalogSizes() {
        assertEquals(0, nextCarouselIndex(0, 0))
        assertEquals(0, nextCarouselIndex(4, 1))
        assertEquals(0, nextCarouselIndex(3, 4))
        assertEquals(2, nextCarouselIndex(5, 4))
        assertEquals(1, nextCarouselIndex(-1, 4))
    }

    @Test
    fun recognizesTelevisionModeFeaturesAndNonTouchDevices() {
        assertEquals(true, usesTvDeviceLayout(true, false, false, true))
        assertEquals(true, usesTvDeviceLayout(false, true, false, true))
        assertEquals(true, usesTvDeviceLayout(false, false, true, true))
        assertEquals(true, usesTvDeviceLayout(false, false, false, false))
        assertEquals(false, usesTvDeviceLayout(false, false, false, true))
    }
}
