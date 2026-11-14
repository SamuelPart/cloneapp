package com.myiptv.clean.ads

import java.util.Locale

// Also recognise combined provider categories such as "Infantiles / Familia".
private val kidsCategory = Regex(
    """(^|[^\p{L}])(?:kid|kids|children|family|familia|infantil(?:es)?|niños?|niñas?|niñez|ninos?|ninas?)(?=$|[^\p{L}])""",
)
private val unknownCategories = setOf(
    "sin categoría", "sin categoria", "sin género", "sin genero", "sin clasificar",
    "unknown", "uncategorized", "unclassified", "n/a", "none",
)

/** Conservative metadata safeguard in addition to the explicit Kids navigation context. */
internal fun isKidsGenre(genres: List<String>): Boolean = genres.any { genre ->
    kidsCategory.containsMatchIn(genre.trim().lowercase(Locale.ROOT))
}

/** Missing provider metadata is not enough to establish that content is suitable for ads. */
private fun hasAdSafeClassification(values: List<String>): Boolean {
    val categories = values.map { it.trim().lowercase(Locale.ROOT) }.filter(String::isNotBlank)
    return categories.isNotEmpty() && categories.none { it in unknownCategories } && !isKidsGenre(categories)
}

internal fun canOfferLiveChannelAds(sourceCategories: List<String>): Boolean =
    hasAdSafeClassification(sourceCategories)

internal fun canOfferMediaAds(genres: List<String>): Boolean = hasAdSafeClassification(genres)
