package com.myiptv.clean.ui.compose

/** Panoramic tiles stay usable on compact TV boxes as well as larger screens. */
internal fun tvCatalogColumnCount(widthDp: Float): Int = when {
    widthDp < 280f -> 1
    widthDp < 460f -> 2
    widthDp < 620f -> 3
    else -> 4
}

/** The first row can contain two large real titles; never reserve a blank hero. */
internal fun tvCatalogItemSpan(columns: Int, index: Int, itemCount: Int): Int =
    if (columns >= 4 && itemCount >= 2 && index in 0..1) 2 else 1

/** Cap the large artwork to the space left below the TV's search and filter controls. */
internal fun tvFeaturedTileHeight(widthDp: Float, heightDp: Float, columns: Int): Float {
    val count = columns.coerceAtLeast(1)
    val cellWidth = (widthDp - (count - 1) * 5f).coerceAtLeast(0f) / count
    val naturalHeight = (cellWidth * 2f + 5f) / 1.55f
    val availableHeight = (heightDp * 0.85f).coerceAtLeast(80f)
    return minOf(naturalHeight, availableHeight).coerceAtLeast(1f)
}
