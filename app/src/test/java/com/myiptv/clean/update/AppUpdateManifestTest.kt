package com.myiptv.clean.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AppUpdateManifestTest {
    private val main = listOf(File("src/main"), File("app/src/main"))
        .firstOrNull(File::isDirectory) ?: error("No se encuentra la aplicación")
    private val android = "http://schemas.android.com/apk/res/android"
    private fun xml(path: String): Element = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        .newDocumentBuilder().parse(File(main, path)).documentElement
    private fun Element.namedElement(tag: String, name: String): Element? {
        val elements = getElementsByTagName(tag)
        return (0 until elements.length).map { elements.item(it) as Element }
            .firstOrNull { it.getAttributeNS(android, "name") == name }
    }

    @Test fun updaterUsesPrivateCacheProviderAndBoundJobService() {
        val manifest = xml("AndroidManifest.xml")
        assertNotNull(manifest.namedElement("uses-permission", "android.permission.REQUEST_INSTALL_PACKAGES"))
        val provider = manifest.namedElement("provider", ".update.AppUpdateFileProvider")
            ?: error("Falta el proveedor privado de APK")
        assertEquals("false", provider.getAttributeNS(android, "exported"))
        assertEquals("true", provider.getAttributeNS(android, "grantUriPermissions"))
        assertEquals("\${applicationId}.appupdates", provider.getAttributeNS(android, "authorities"))
        assertEquals("@xml/app_update_paths", provider.namedElement("meta-data", "android.support.FILE_PROVIDER_PATHS")?.getAttributeNS(android, "resource"))
        val paths = xml("res/xml/app_update_paths.xml")
        assertEquals(1, paths.childNodes.let { nodes -> (0 until nodes.length).count { nodes.item(it) is Element } })
        val cache = paths.getElementsByTagName("cache-path").item(0) as Element
        assertEquals("samplay-app-updates/", cache.getAttribute("path"))
        assertTrue(cache.getAttribute("name").isNotBlank())
        val job = manifest.namedElement("service", ".update.AppUpdateSyncJob") ?: error("Falta el trabajo de versiones")
        assertEquals("android.permission.BIND_JOB_SERVICE", job.getAttributeNS(android, "permission"))
        assertNotNull("El trabajo existente del catálogo debe conservarse", manifest.namedElement("service", ".notifications.CatalogSyncJob"))
    }
}
