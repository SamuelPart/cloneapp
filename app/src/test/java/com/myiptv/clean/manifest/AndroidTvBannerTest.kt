package com.myiptv.clean.manifest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.DataInputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AndroidTvBannerTest {
    private val mainDirectory = listOf(File("src/main"), File("app/src/main"))
        .firstOrNull(File::isDirectory) ?: error("No se encuentra el código de la aplicación")

    @Test fun leanbackLauncherInheritsTheApplicationBanner() {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val manifest = factory.newDocumentBuilder().parse(File(mainDirectory, "AndroidManifest.xml"))
        val androidNamespace = "http://schemas.android.com/apk/res/android"
        val application = manifest.getElementsByTagName("application").item(0) as Element
        assertEquals("@drawable/samplay_tv_banner", application.getAttributeNS(androidNamespace, "banner"))
        val categories = application.getElementsByTagName("category")
        assertTrue((0 until categories.length).any { index ->
            (categories.item(index) as Element).getAttributeNS(androidNamespace, "name") ==
                "android.intent.category.LEANBACK_LAUNCHER"
        })
    }

    @Test fun tvBannerIsAPngWithTheRecommendedXhdpiDimensions() {
        val banner = File(mainDirectory, "res/drawable-xhdpi/samplay_tv_banner.png")
        DataInputStream(banner.inputStream()).use { input ->
            val signature = ByteArray(8).also(input::readFully)
            assertTrue(signature.contentEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)))
            assertEquals(13, input.readInt()) // IHDR length
            val chunkType = ByteArray(4).also(input::readFully)
            assertEquals("IHDR", String(chunkType, Charsets.US_ASCII))
            assertEquals(320, input.readInt())
            assertEquals(180, input.readInt())
        }
    }
}
