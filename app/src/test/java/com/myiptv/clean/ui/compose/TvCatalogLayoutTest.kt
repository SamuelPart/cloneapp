package com.myiptv.clean.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Test

class TvCatalogLayoutTest {
    @Test
    fun `grid adapts to narrow and wide tv screens`() {
        assertEquals(1, tvCatalogColumnCount(0f))
        assertEquals(1, tvCatalogColumnCount(279f))
        assertEquals(2, tvCatalogColumnCount(280f))
        assertEquals(2, tvCatalogColumnCount(459f))
        assertEquals(3, tvCatalogColumnCount(460f))
        assertEquals(3, tvCatalogColumnCount(619f))
        assertEquals(4, tvCatalogColumnCount(620f))
        assertEquals(4, tvCatalogColumnCount(1200f))
    }

    @Test
    fun `featured artwork fits below the controls on short screens`() {
        assertEquals(102f, tvFeaturedTileHeight(widthDp = 620f, heightDp = 120f, columns = 4), 0.01f)
        assertEquals(198.39f, tvFeaturedTileHeight(widthDp = 620f, heightDp = 300f, columns = 4), 0.02f)
    }

    @Test
    fun `large first row uses only existing real titles`() {
        assertEquals(1, tvCatalogItemSpan(columns = 4, index = 0, itemCount = 1))
        assertEquals(2, tvCatalogItemSpan(columns = 4, index = 0, itemCount = 2))
        assertEquals(2, tvCatalogItemSpan(columns = 4, index = 1, itemCount = 2))
        assertEquals(1, tvCatalogItemSpan(columns = 4, index = 2, itemCount = 3))
        assertEquals(1, tvCatalogItemSpan(columns = 3, index = 0, itemCount = 10))
        assertEquals(1, tvCatalogItemSpan(columns = 1, index = 0, itemCount = 10))
    }
}
