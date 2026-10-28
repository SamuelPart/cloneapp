package com.myiptv.clean.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignCatalogTest {
    @Test
    fun catalogContainsAllTwelveExampleTitlesInReferenceOrder() {
        assertEquals(
            listOf("night", "forest", "moto", "river", "alley", "bridge", "towers", "aerial", "street", "crowd", "car", "window"),
            DesignCatalog.titles.keys.toList()
        )
        val night = DesignCatalog.titles.getValue("night")
        assertEquals("Ciudad Eléctrica", night.title)
        assertEquals("Serie", night.kind)
        assertEquals("16+", night.ageRating)
        assertEquals(98, night.match)
        assertEquals(night, DesignCatalog.heroItem)
        assertEquals("Documental", DesignCatalog.titles.getValue("river").kind)
    }

    @Test
    fun imageUrlsUseTheSuppliedPhotoIdsAndRequestedWidth() {
        val forest = DesignCatalog.titles.getValue("forest")
        assertEquals(
            "https://images.unsplash.com/photo-1650464179774-caf42373dc17?crop=entropy&cs=tinysrgb&fit=crop&fm=jpg&q=80&w=1100",
            forest.imageUrl(1100)
        )
        assertTrue(forest.imageUrl().endsWith("&w=1600"))
        val photoIds = mapOf(
            "night" to "1514439827219-9137a0b99245", "forest" to "1650464179774-caf42373dc17",
            "moto" to "1599060052009-24d6d0b0161c", "river" to "1475070929565-c985b496cb9f",
            "alley" to "1651221186975-61f13b4ab7af", "bridge" to "1542642839-83adadcbe645",
            "towers" to "1535391879778-3bae11d29a24", "aerial" to "1671877321315-e114825a475c",
            "street" to "1541702467897-41915a07d3a7", "crowd" to "1631044176346-804c33ade61c",
            "car" to "1675587069926-2d9a51992420", "window" to "1714972593463-3beecd6f7c1c"
        )
        photoIds.forEach { (id, photo) ->
            assertTrue(DesignCatalog.titles.getValue(id).imageUrl().startsWith("https://images.unsplash.com/photo-$photo?"))
        }
    }

    @Test
    fun featuredAndGridOrderMatchTheReference() {
        assertEquals(listOf("forest", "night", "alley", "bridge"), DesignCatalog.carousel.map(DesignTitle::id))
        assertEquals(listOf("forest", "moto"), DesignCatalog.gridRows(0)[0].map(DesignTitle::id))
        assertEquals(listOf("night", "moto"), DesignCatalog.gridRows(1)[0].map(DesignTitle::id))
        assertEquals(DesignCatalog.gridRows(0), DesignCatalog.gridRows(4))
        assertEquals(listOf("river", "street", "towers", "crowd"), DesignCatalog.gridRows(0)[1].map(DesignTitle::id))
        assertEquals(listOf("car", "window", "aerial", "night"), DesignCatalog.gridRows(0)[2].map(DesignTitle::id))
        assertEquals(listOf("alley", "bridge", "forest", "moto"), DesignCatalog.gridRows(0)[3].map(DesignTitle::id))
    }

    @Test
    fun sectionsAndExampleProgressArePreserved() {
        assertEquals(listOf("Continuar viendo", "Tendencias esta semana", "Series originales", "Documentales"), DesignCatalog.rows.map(DesignRow::title))
        val continuing = DesignCatalog.rows.first().items
        assertEquals(listOf("night", "alley", "bridge", "window", "car"), continuing.map { it.title.id })
        assertEquals(listOf(0.62f, 0.25f, 0.8f, 0.4f, 0.1f), continuing.map(DesignEntry::progress))
        assertEquals(listOf("moto", "forest", "towers", "street", "crowd", "aerial", "river"), DesignCatalog.rows[1].items.map { it.title.id })
        assertEquals(listOf("night", "alley", "towers", "crowd", "window"), DesignCatalog.rows[2].items.map { it.title.id })
        assertEquals(listOf("river", "aerial", "forest", "car"), DesignCatalog.rows[3].items.map { it.title.id })
        assertTrue(DesignCatalog.rows.drop(1).flatMap(DesignRow::items).all { it.progress == null })
        assertTrue(continuing.all { it.progress != null && it.progress in 0f..1f })
    }
}
