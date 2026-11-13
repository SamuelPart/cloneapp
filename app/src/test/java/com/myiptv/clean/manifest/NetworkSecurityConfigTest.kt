package com.myiptv.clean.manifest

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class NetworkSecurityConfigTest {
    private val mainDirectory = listOf(File("src/main"), File("app/src/main"))
        .firstOrNull(File::isDirectory) ?: error("No se encuentra el código de la aplicación")
    private val androidNamespace = "http://schemas.android.com/apk/res/android"

    @Test fun cleartextIsAllowedForLegacyHttpPlaylistAndLiveStreams() {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val manifest = factory.newDocumentBuilder().parse(File(mainDirectory, "AndroidManifest.xml"))
        val application = manifest.getElementsByTagName("application").item(0) as Element
        assertEquals("true", application.getAttributeNS(androidNamespace, "usesCleartextTraffic"))
        assertEquals("@xml/network_security_config", application.getAttributeNS(androidNamespace, "networkSecurityConfig"))

        val config = factory.newDocumentBuilder().parse(File(mainDirectory, "res/xml/network_security_config.xml"))
        val base = config.getElementsByTagName("base-config").item(0) as Element
        assertEquals("true", base.getAttribute("cleartextTrafficPermitted"))
    }
}
