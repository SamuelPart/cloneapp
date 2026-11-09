package com.myiptv.clean.ads

import java.util.Locale

/** Conservative metadata safeguard in addition to the explicit Kids navigation context. */
internal fun isKidsGenre(genres: List<String>): Boolean = genres.any { genre ->
    genre.trim().lowercase(Locale.ROOT) in setOf(
        "kids", "children", "family", "familia", "infantil", "niños", "ninos", "niñez",
    )
}
