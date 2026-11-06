package com.myiptv.clean.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** These checks cover source-set isolation; the actual merged manifests must also be checked. */
class AppUpdateManifestTest {
    private val src = listOf(File("src"), File("app/src"))
        .firstOrNull { File(it, "main/AndroidManifest.xml").isFile } ?: error("No se encuentra la aplicación")
    private val android = "http://schemas.android.com/apk/res/android"
    private val tools = "http://schemas.android.com/tools"
    private fun xml(path: String): Element = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        .newDocumentBuilder().parse(File(src, path)).documentElement
    private fun Element.namedElement(tag: String, name: String): Element? {
        val elements = getElementsByTagName(tag)
        return (0 until elements.length).map { elements.item(it) as Element }
            .firstOrNull { it.getAttributeNS(android, "name") == name }
    }

    @Test fun githubUpdaterUsesPrivateCacheProviderAndBoundJobService() {
        val manifest = xml("github/AndroidManifest.xml")
        assertNotNull(manifest.namedElement("uses-permission", "android.permission.REQUEST_INSTALL_PACKAGES"))
        val provider = manifest.namedElement("provider", ".update.AppUpdateFileProvider")
            ?: error("Falta el proveedor privado de APK")
        assertEquals("false", provider.getAttributeNS(android, "exported"))
        assertEquals("true", provider.getAttributeNS(android, "grantUriPermissions"))
        assertEquals("\${applicationId}.appupdates", provider.getAttributeNS(android, "authorities"))
        assertEquals("@xml/app_update_paths", provider.namedElement("meta-data", "android.support.FILE_PROVIDER_PATHS")?.getAttributeNS(android, "resource"))
        val paths = xml("github/res/xml/app_update_paths.xml")
        assertEquals(1, paths.childNodes.let { nodes -> (0 until nodes.length).count { nodes.item(it) is Element } })
        val cache = paths.getElementsByTagName("cache-path").item(0) as Element
        assertEquals("samplay-app-updates/", cache.getAttribute("path"))
        assertTrue(cache.getAttribute("name").isNotBlank())
        val job = manifest.namedElement("service", ".update.AppUpdateSyncJob") ?: error("Falta el trabajo de versiones")
        assertEquals("android.permission.BIND_JOB_SERVICE", job.getAttributeNS(android, "permission"))
    }

    @Test fun playRemovesInstallPermissionButRetainsCatalogSync() {
        val main = xml("main/AndroidManifest.xml")
        val play = xml("play/AndroidManifest.xml")
        val permission = "android.permission.REQUEST_INSTALL_PACKAGES"
        assertNull(main.namedElement("uses-permission", permission))
        assertEquals("remove", play.namedElement("uses-permission", permission)?.getAttributeNS(tools, "node"))
        for (manifest in listOf(main, play)) {
            assertNull(manifest.namedElement("service", ".update.AppUpdateSyncJob"))
            assertNull(manifest.namedElement("provider", ".update.AppUpdateFileProvider"))
        }
        assertNotNull("El trabajo del catálogo debe conservarse", main.namedElement("service", ".notifications.CatalogSyncJob"))
        assertNotNull("El trabajo persistente del catálogo necesita este permiso", main.namedElement("uses-permission", "android.permission.RECEIVE_BOOT_COMPLETED"))
    }

    @Test fun playTargetsModernAndroidWithoutRaisingTheMinimumOrChangingPackage() {
        val build = File(src.absoluteFile.parentFile, "build.gradle").readText()
        val play = Regex("""(?s)\bplay\s*\{([^}]*)}""").find(build)?.groupValues?.get(1)
            ?: error("Falta la variante Play")
        val compileSdk = Regex("""\bcompileSdk\s+(\d+)""").find(build)?.groupValues?.get(1)?.toInt()
        val minSdk = Regex("""\bminSdk\s+(\d+)""").find(build)?.groupValues?.get(1)?.toInt()
        val playTarget = Regex("""\btargetSdk\s+(\d+)""").find(play)?.groupValues?.get(1)?.toInt()
        assertTrue("La compilación necesita Android SDK 36 o superior", (compileSdk ?: 0) >= 36)
        assertEquals("Debe conservarse la compatibilidad con Android 5", 21, minSdk)
        assertTrue("La variante Play debe apuntar a API 36 o superior", (playTarget ?: 0) >= 36)
        assertTrue(build.contains("applicationId 'com.myiptv.clean'"))
        assertFalse("No cambiar el ID con sufijos", build.contains("applicationIdSuffix"))
    }

    @Test fun apkDownloaderAndItsPrivateResourcesExistOnlyInGithubSources() {
        val relative = "java/com/myiptv/clean/update"
        val updaterFiles = listOf("AppUpdateManager.kt", "AppUpdateProtocol.kt", "AppUpdateFileProvider.kt", "InstalledApkVerifier.kt", "AppUpdateSyncJob.kt")
        for (file in updaterFiles) {
            assertTrue("El actualizador de GitHub debe conservar $file", File(src, "github/$relative/$file").isFile)
            assertFalse(File(src, "main/$relative/$file").exists())
            assertFalse(File(src, "play/$relative/$file").exists())
        }
        assertTrue(File(src, "github/res/xml/app_update_paths.xml").isFile)
        assertFalse(File(src, "main/res/xml/app_update_paths.xml").exists())
        assertFalse(File(src, "play/res/xml/app_update_paths.xml").exists())
        val play = File(src, "play/$relative/PlayAppUpdates.kt").readText()
        assertTrue(play.contains("const val ENABLED = false"))
        assertFalse(play.contains("ACTION_INSTALL_PACKAGE"))
        assertFalse(play.contains("application/vnd.android.package-archive"))
    }
}
