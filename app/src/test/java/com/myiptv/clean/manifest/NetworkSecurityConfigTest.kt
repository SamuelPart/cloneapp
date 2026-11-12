package com.myiptv.clean.manifest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class NetworkSecurityConfigTest {
    private val mainDirectory = listOf(File("src/main"), File("app/src/main"))
        .firstOrNull(File::isDirectory) ?: error("No se encuentra el código de la aplicación")
    private val androidNamespace = "http://schemas.android.com/apk/res/android"

    @Test fun cleartextIsDisabledByDefaultAndLimitedToLegacyMediaDomain() {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val manifest = factory.newDocumentBuilder().parse(File(mainDirectory, "AndroidManifest.xml"))
        val application = manifest.getElementsByTagName("application").item(0) as Element
        assertEquals("false", application.getAttributeNS(androidNamespace, "usesCleartextTraffic"))
        assertEquals("@xml/network_security_config", application.getAttributeNS(androidNamespace, "networkSecurityConfig"))

        val config = factory.newDocumentBuilder().parse(File(mainDirectory, "res/xml/network_security_config.xml"))
        val base = config.getElementsByTagName("base-config").item(0) as Element
        assertEquals("false", base.getAttribute("cleartextTrafficPermitted"))
        val domainConfig = config.getElementsByTagName("domain-config").item(0) as Element
        assertEquals("true", domainConfig.getAttribute("cleartextTrafficPermitted"))
        val domain = domainConfig.getElementsByTagName("domain").item(0) as Element
        assertEquals("archive.org", domain.textContent.trim())
        assertTrue(domain.getAttribute("includeSubdomains").toBoolean())
    }
}
