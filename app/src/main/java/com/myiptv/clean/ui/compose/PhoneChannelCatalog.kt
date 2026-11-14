package com.myiptv.clean.ui.compose

import java.text.Normalizer
import java.util.Locale

internal const val ALL_PHONE_CHANNELS = "all"

internal data class PhoneChannelFilter(val key: String, val label: String)

internal data class PhoneChannelRow(
    val channel: LiveChannel,
    val countryCode: String?,
    val countryName: String?,
    val categoryName: String,
    internal val categoryKey: String,
    internal val searchText: String
) {
    val subtitle: String get() = listOfNotNull(categoryName.takeIf(String::isNotBlank), countryName).joinToString(" · ")
}

/** Position in the visible list, not a broadcaster's official channel number. */
internal fun phoneChannelNumber(index: Int): String {
    require(index >= 0)
    return (index.toLong() + 1L).toString().padStart(3, '0')
}

// Locale.of is newer than the app's API 21 minimum.
@Suppress("DEPRECATION")
private val spanish = Locale("es")
private val isoCountries = Locale.getISOCountries().toSet()
private val combiningMarks = Regex("\\p{M}+")
private val qualityPattern = Regex("\\d{3,4}[pi]", RegexOption.IGNORE_CASE)
private val categoryNames = mapOf(
    "animation" to "Animación", "auto" to "Motor", "business" to "Negocios",
    "classic" to "Clásicos", "comedy" to "Comedia", "cooking" to "Cocina",
    "culture" to "Cultura", "documentary" to "Documentales", "education" to "Educación",
    "entertainment" to "Entretenimiento", "family" to "Familia", "general" to "General",
    "kids" to "Infantiles", "legislative" to "Institucional", "lifestyle" to "Estilo de vida",
    "movies" to "Películas", "music" to "Música", "news" to "Noticias", "outdoor" to "Naturaleza",
    "religious" to "Religión", "science" to "Ciencia", "series" to "Series", "shop" to "Compras",
    "sports" to "Deportes", "travel" to "Viajes", "weather" to "Clima"
)

private fun searchKey(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD).replace(combiningMarks, "").lowercase(Locale.ROOT)

internal fun phoneCountryCode(value: String?): String? =
    value?.trim()?.uppercase(Locale.ROOT)?.takeIf { it in isoCountries }

@Suppress("DEPRECATION") // Keep the API 21-compatible Locale constructor.
internal fun phoneCountryName(code: String?): String? {
    val valid = phoneCountryCode(code) ?: return null
    return Locale("", valid).getDisplayCountry(spanish).takeIf(String::isNotBlank)
}

internal fun phoneCategoryName(value: String): String {
    val clean = value.trim()
    if (clean.isEmpty()) return "Sin categoría"
    return categoryNames[clean.lowercase(Locale.ROOT)] ?: clean.replaceFirstChar { it.titlecase(spanish) }
}

/** This is provider-reported maximum quality, not a measurement of the current video. */
internal fun phoneReportedQuality(value: String?): String? =
    value?.trim()?.takeIf(qualityPattern::matches)?.uppercase(Locale.ROOT)

/** Index once per catalog refresh; searches and favorite/filter changes preserve source order. */
internal class PhoneChannelCatalog(channels: List<LiveChannel>) {
    val rows: List<PhoneChannelRow> = channels.distinctBy(LiveChannel::id).map { channel ->
        val code = phoneCountryCode(channel.countryCode)
        val country = phoneCountryName(code)
        val category = phoneCategoryName(channel.category)
        PhoneChannelRow(
            channel = channel,
            countryCode = code,
            countryName = country,
            categoryName = category,
            categoryKey = searchKey(category),
            searchText = searchKey(listOfNotNull(channel.name, channel.category, category, country, code).joinToString(" "))
        )
    }

    val filters: List<PhoneChannelFilter> = run {
        val countries = rows.mapNotNull { row ->
            row.countryCode?.let { code -> row.countryName?.let { name -> code to name } }
        }.distinctBy { it.first }
        val priority = listOf("CO", "VE", "AR", "PE") // Only include countries present in the source.
        val featured = priority.mapNotNull { code -> countries.firstOrNull { it.first == code } }
        val remaining = countries.filterNot { it.first in priority }.sortedBy { searchKey(it.second) }
        // Equivalent source categories (e.g. News and Noticias) form one visible filter.
        val categories = rows.distinctBy(PhoneChannelRow::categoryKey)
            .map { PhoneChannelFilter("category:${it.categoryKey}", it.categoryName) }
            .sortedBy { searchKey(it.label) }
        listOf(PhoneChannelFilter(ALL_PHONE_CHANNELS, "Todos")) +
            featured.map { PhoneChannelFilter("country:${it.first}", it.second) } +
            categories + remaining.map { PhoneChannelFilter("country:${it.first}", it.second) }
    }

    fun filter(query: String, filter: String, favoritesOnly: Boolean, favorites: Set<String>): List<PhoneChannelRow> {
        val needle = searchKey(query.trim())
        return rows.filter { row ->
            val matchesFilter = when {
                filter == ALL_PHONE_CHANNELS -> true
                filter.startsWith("country:") -> row.countryCode == filter.removePrefix("country:")
                filter.startsWith("category:") -> row.categoryKey == filter.removePrefix("category:")
                else -> false
            }
            matchesFilter && (!favoritesOnly || row.channel.id in favorites) &&
                (needle.isEmpty() || needle in row.searchText)
        }
    }
}
