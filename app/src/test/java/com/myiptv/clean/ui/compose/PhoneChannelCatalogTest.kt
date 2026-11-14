package com.myiptv.clean.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneChannelCatalogTest {
    private fun channel(id: String, name: String, category: String, country: String? = null, quality: String? = null) =
        LiveChannel(id, name, category, "TV", "https://example.com/$id.m3u8", 0L, countryCode = country, quality = quality)

    private val colombia = channel("a", "Caracol", "News", "CO", "480p")
    private val venezuela = channel("b", "Música Uno", "Music", "ve")
    private val unknown = channel("c", "Canal local", "General", null)
    private val catalog = PhoneChannelCatalog(listOf(colombia, venezuela, unknown, colombia))

    @Test fun keepsRealSourceOrderAndNumbersTheVisibleList() {
        assertEquals(listOf("a", "b", "c"), catalog.rows.map { it.channel.id })
        assertEquals(listOf("001", "002", "003"), catalog.rows.indices.map(::phoneChannelNumber))
        val venezuelaRows = catalog.filter("", "country:VE", false, emptySet())
        assertEquals(listOf("b"), venezuelaRows.map { it.channel.id })
        assertEquals(listOf("001"), venezuelaRows.indices.map(::phoneChannelNumber))
        assertEquals("1000", phoneChannelNumber(999))
        assertEquals(listOf("a"), catalog.filter("", "category:noticias", false, emptySet()).map { it.channel.id })
    }

    @Test fun onlyShowsActualCountriesAndCategories() {
        val options = catalog.filters.associate { it.key to it.label }
        assertEquals("Todos", options[ALL_PHONE_CHANNELS])
        assertEquals("Colombia", options["country:CO"])
        assertEquals("Venezuela", options["country:VE"])
        assertFalse(options.containsKey("country:AR"))
        assertEquals("Noticias", options["category:noticias"])
        assertTrue(options.values.none { it.contains("popular", ignoreCase = true) })
    }

    @Test fun equivalentProviderCategoriesShareOneFilter() {
        val fromAnotherProvider = channel("d", "Noticias local", "Noticias", "PE")
        val combined = PhoneChannelCatalog(listOf(colombia, fromAnotherProvider))
        assertEquals(1, combined.filters.count { it.key == "category:noticias" })
        assertEquals(listOf("a", "d"), combined.filter("", "category:noticias", false, emptySet()).map { it.channel.id })
    }

    @Test fun missingCategoriesShareOneFilterWithoutInventingMetadata() {
        val first = channel("d", "Canal uno", "")
        val second = channel("e", "Canal dos", "  ")
        val combined = PhoneChannelCatalog(listOf(first, second))
        assertEquals(1, combined.filters.count { it.key == "category:sin categoria" && it.label == "Sin categoría" })
        assertEquals(listOf("d", "e"), combined.filter("", "category:sin categoria", false, emptySet()).map { it.channel.id })
    }

    @Test fun favoritesSearchAndFilterWorkTogetherWithoutInventingPrograms() {
        assertEquals(listOf("b"), catalog.filter("musica", ALL_PHONE_CHANNELS, true, setOf("a", "b")).map { it.channel.id })
        assertEquals(listOf("a"), catalog.filter("colombia", "category:noticias", true, setOf("a")).map { it.channel.id })
        assertTrue(catalog.filter("musica", "country:CO", false, emptySet()).isEmpty())
        assertTrue(catalog.filter("", ALL_PHONE_CHANNELS, true, emptySet()).isEmpty())
        assertTrue(catalog.filter("", "unknown-filter", false, emptySet()).isEmpty())
        assertEquals("Noticias · Colombia", catalog.rows.first().subtitle)
        assertEquals("General", catalog.rows.last().subtitle)
    }

    @Test fun rejectsUnknownMetadataAndShowsOnlyReportedQuality() {
        assertNull(phoneCountryName(null))
        assertNull(phoneCountryName("ZZ"))
        assertEquals("Colombia", phoneCountryName(" co "))
        assertEquals("480P", phoneReportedQuality("480p"))
        assertEquals("1080I", phoneReportedQuality("1080i"))
        assertNull(phoneReportedQuality(null))
        assertNull(phoneReportedQuality("best"))
        assertNull(phoneReportedQuality("480p fake"))
    }
}
