package com.myiptv.clean.update

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class AppUpdateProtocolTest {
    @get:Rule val temporary = TemporaryFolder()
    private val bytes = "verified test apk bytes".toByteArray()
    private val digest = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
    private val assetUrl = "https://github.com/SamuelPart/cloneapp/releases/download/v1.2.1/SamPlay-4-1.2.1.apk"
    private val release = AppUpdateRelease(4L, "1.2.1", assetUrl, bytes.size.toLong(), digest)

    private fun json(
        name: String = "SamPlay-4-1.2.1.apk",
        url: String = assetUrl,
        size: Long = bytes.size.toLong(),
        sha: String = "sha256:$digest",
        state: String = "uploaded",
        tag: String = "v1.2.1",
        draft: Boolean = false,
        prerelease: Boolean = false,
    ) = """{"tag_name":"$tag","draft":$draft,"prerelease":$prerelease,"assets":[{"name":"$name","state":"$state","browser_download_url":"$url","size":$size,"digest":"$sha"}]}"""

    private class Response(
        url: URL,
        private val status: Int,
        private val body: ByteArray = byteArrayOf(),
        private val headers: Map<String, String> = emptyMap(),
    ) : HttpURLConnection(url) {
        var closed = false
        override fun getResponseCode(): Int = status
        override fun getHeaderField(name: String): String? = headers[name]
        override fun getInputStream() = ByteArrayInputStream(body)
        override fun connect() = Unit
        override fun disconnect() { closed = true }
        override fun usingProxy() = false
    }

    private fun expectIOException(block: () -> Unit) {
        try { block(); fail("Se esperaba IOException") } catch (_: IOException) { }
    }

    @Test fun discoversOnlyNewerPublishedNamedApks() {
        assertEquals(AppReleaseCheck.Available(release), parseLatestAppRelease(json(), 3))
        assertEquals(AppReleaseCheck.UpToDate, parseLatestAppRelease(json(), 4))
        assertEquals(AppReleaseCheck.UpToDate, parseLatestAppRelease(json(), 5))
        assertEquals(AppReleaseCheck.NoCompatibleApk, parseLatestAppRelease(json(draft = true), 3))
        assertEquals(AppReleaseCheck.NoCompatibleApk, parseLatestAppRelease(json(prerelease = true), 3))
        assertEquals(AppReleaseCheck.NoCompatibleApk, parseLatestAppRelease("""{"tag_name":"v1","draft":false,"prerelease":false,"assets":[]}""", 3))
        expectIOException { parseLatestAppRelease("<html>no release</html>", 3) }
    }

    @Test fun rejectsUntrustedOrIncompleteReleaseMetadata() {
        for (invalid in listOf(
            json(name = "other.apk"),
            json(name = "SamPlay-4-1.2.1.apk.exe"),
            json(url = "http://github.com/SamuelPart/cloneapp/releases/download/v1.2.1/SamPlay-4-1.2.1.apk"),
            json(url = "https://github.com/other/repo/releases/download/v1.2.1/SamPlay-4-1.2.1.apk"),
            json(url = "https://github.com.evil.test/SamuelPart/cloneapp/releases/download/v1.2.1/SamPlay-4-1.2.1.apk"),
            json(tag = "../main"), json(state = "new"), json(size = 0), json(size = MAX_UPDATE_APK_BYTES + 1),
            json(sha = "sha256:short"), json(sha = "md5:$digest"),
        )) assertEquals(AppReleaseCheck.NoCompatibleApk, parseLatestAppRelease(invalid, 3))
        val duplicate = json().replace("]}", ",{" + json().substringAfter("[{"))
        assertEquals(AppReleaseCheck.NoCompatibleApk, parseLatestAppRelease(duplicate, 3))
    }

    @Test fun handlesMissingReleaseErrorsAndTruncatedMetadataWithoutPretendingToBeCurrent() {
        val notPublished = Response(URL(GITHUB_LATEST_RELEASE), 404)
        assertEquals(AppReleaseCheck.NotPublished, GitHubReleaseClient { notPublished }.check(3))
        assertTrue(notPublished.closed)
        val failing = Response(URL(GITHUB_LATEST_RELEASE), 403)
        expectIOException { GitHubReleaseClient { failing }.check(3) }
        assertTrue(failing.closed)
        val body = json().toByteArray()
        val truncated = Response(URL(GITHUB_LATEST_RELEASE), 200, body, mapOf("Content-Length" to "999999"))
        expectIOException { GitHubReleaseClient { truncated }.check(3) }
        val valid = Response(URL(GITHUB_LATEST_RELEASE), 200, body, mapOf("Content-Length" to body.size.toString()))
        assertEquals(AppReleaseCheck.Available(release), GitHubReleaseClient { valid }.check(3))
    }

    @Test fun streamsAHashedApkThroughAllowedGithubRedirects() {
        val connections = mutableListOf<Response>()
        val downloader = GitHubApkDownloader { url ->
            Response(
                url,
                if (connections.isEmpty()) 302 else 200,
                if (connections.isEmpty()) byteArrayOf() else bytes,
                if (connections.isEmpty()) mapOf("Location" to "https://release-assets.githubusercontent.com/token?sig=ok")
                else mapOf("Content-Length" to bytes.size.toString()),
            ).also(connections::add)
        }
        val output = temporary.newFolder("update").resolve("SamPlay.apk")
        var progress = 0L
        downloader.download(release, output, onProgress = { progress = it })
        assertArrayEquals(bytes, output.readBytes())
        assertEquals(bytes.size.toLong(), progress)
        assertTrue(connections.all { it.closed })
        assertFalse(output.resolveSibling("SamPlay.apk.part").exists())
    }

    @Test fun rejectsForeignRedirectsCorruptionTruncationAndCancellation() {
        val output = temporary.newFolder("failures").resolve("SamPlay.apk")
        val foreign = GitHubApkDownloader { url -> Response(url, 302, headers = mapOf("Location" to "https://evil.test/fake.apk")) }
        expectIOException { foreign.download(release, output) }
        assertFalse(output.exists())
        val corrupt = GitHubApkDownloader { url -> Response(url, 200, ByteArray(bytes.size) { 1 }) }
        expectIOException { corrupt.download(release, output) }
        assertFalse(output.exists())
        val truncated = GitHubApkDownloader { url -> Response(url, 200, bytes.dropLast(1).toByteArray()) }
        expectIOException { truncated.download(release, output) }
        assertFalse(output.exists())
        val tooLarge = GitHubApkDownloader { url -> Response(url, 200, bytes + byteArrayOf(1)) }
        expectIOException { tooLarge.download(release, output) }
        assertFalse(output.exists())
        var cancellationChecks = 0
        try {
            GitHubApkDownloader { url -> Response(url, 200, bytes) }.download(release, output, checkCancellation = {
                cancellationChecks++
                if (cancellationChecks >= 3) throw IllegalStateException("cancelled after writing")
            })
            fail("La cancelación debe propagarse")
        } catch (_: IllegalStateException) { }
        assertTrue(cancellationChecks >= 3)
        assertFalse(output.exists())
        assertFalse(output.resolveSibling("SamPlay.apk.part").exists())
    }

    @Test fun onlySamePackageAndSameSignerWithMatchingNewerVersionCanBeInstalled() {
        val installed = AppPackageIdentity("com.myiptv.clean", 3, "1.2.0", setOf("same-cert"))
        val archive = AppPackageIdentity("com.myiptv.clean", 4, "1.2.1", setOf("same-cert"))
        assertTrue(isMatchingUpdate(installed, archive, release))
        assertFalse(isMatchingUpdate(installed, archive.copy(packageName = "com.other.app"), release))
        assertFalse(isMatchingUpdate(installed, archive.copy(signerSha256 = setOf("wrong")), release))
        assertFalse(isMatchingUpdate(installed.copy(signerSha256 = emptySet()), archive.copy(signerSha256 = emptySet()), release))
        assertFalse(isMatchingUpdate(installed, archive.copy(versionCode = 3), release))
        assertFalse(isMatchingUpdate(installed, archive.copy(versionName = "different"), release))
        assertFalse(isMatchingUpdate(installed.copy(versionCode = 4), archive, release))
    }
}
