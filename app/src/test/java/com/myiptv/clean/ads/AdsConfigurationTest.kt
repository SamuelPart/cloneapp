package com.myiptv.clean.ads

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class AdsConfigurationTest {
    private val mainDir = listOf(File("src/main"), File("app/src/main"))
        .firstOrNull(File::isDirectory) ?: error("No se encuentra el código de la aplicación")
    private val androidNs = "http://schemas.android.com/apk/res/android"

    private fun xml(path: String): Element = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(File(mainDir, path)).documentElement

    @Test fun manifestContainsSamplayAppIdAndDelayedMeasurementFlag() {
        val metadata = xml("AndroidManifest.xml").getElementsByTagName("meta-data")
        val values = (0 until metadata.length).map { metadata.item(it) as Element }
            .associate { it.getAttributeNS(androidNs, "name") to it.getAttributeNS(androidNs, "value") }
        assertEquals("ca-app-pub-8124327134735952~1021355393", values["com.google.android.gms.ads.APPLICATION_ID"])
        assertEquals("true", values["com.google.android.gms.ads.DELAY_APP_MEASUREMENT_INIT"])
    }

    @Test fun nativeCreativeHasVisibleAttributionAndVideoAsset() {
        val layout = xml("res/layout/samplay_native_ad.xml")
        assertEquals("com.google.android.gms.ads.nativead.NativeAdView", layout.tagName)
        assertEquals(1, layout.getElementsByTagName("com.google.android.gms.ads.nativead.MediaView").length)
        val textViews = layout.getElementsByTagName("TextView")
        assertTrue((0 until textViews.length).any {
            (textViews.item(it) as Element).getAttributeNS(androidNs, "text") == "@string/ad_attribution"
        })
        val media = layout.getElementsByTagName("com.google.android.gms.ads.nativead.MediaView").item(0) as Element
        assertEquals("120dp", media.getAttributeNS(androidNs, "layout_height"))
    }
}
