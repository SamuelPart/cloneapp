package com.myiptv.clean.ui.player

import java.net.URI
import java.util.Locale

/** Conservative rules for an untrusted third-party player; they do not bypass provider restrictions. */
internal class EmbedNavigationPolicy(initialUrl: String) {
    private val initial = parseHttpUrl(initialUrl)
    private val trustedHost = initial?.host?.normalizedHost()
    // A www/apex alias is allowed only as an exact host, not as a suffix: otherwise
    // an initial host such as www.co.uk could accidentally trust every *.co.uk site.
    private val exactWwwAlias = trustedHost?.let { if (it.startsWith("www.")) it.removePrefix("www.") else "www.$it" }
    private val requiresHttps = initial?.scheme.equals("https", ignoreCase = true)

    fun isAdRequest(url: String): Boolean {
        val host = parseHttpUrl(url)?.host?.normalizedHost() ?: return false
        return AD_HOSTS.any { host == it || host.endsWith(".$it") }
    }

    /** Unknown non-ad HTTPS destinations may be approved explicitly for this playback session. */
    fun approvableHost(url: String): String? {
        val target = parseHttpUrl(url) ?: return null
        if (isAdRequest(url) || (requiresHttps && !target.scheme.equals("https", ignoreCase = true))) return null
        return target.host.normalizedHost()
    }

    /** The top-level page cannot leave its provider by default; child video frames may use other non-ad hosts. */
    fun allowsNavigation(url: String, isMainFrame: Boolean, approvedHosts: Set<String> = emptySet()): Boolean {
        if (!isMainFrame && (url == "about:blank" || url.startsWith("blob:", ignoreCase = true))) return true
        val target = parseHttpUrl(url) ?: return false
        if (isAdRequest(url)) return false
        if (!isMainFrame) return true
        val host = target.host.normalizedHost()
        val trusted = trustedHost ?: return false
        if (requiresHttps && !target.scheme.equals("https", ignoreCase = true)) return false
        return host == trusted || host.endsWith(".$trusted") || host == exactWwwAlias || host in approvedHosts
    }

    private fun parseHttpUrl(value: String): URI? = runCatching {
        val uri = URI(value.trim())
        uri.takeIf {
            it.scheme?.lowercase(Locale.ROOT) in setOf("http", "https") &&
                !it.host.isNullOrBlank() && it.userInfo == null
        }
    }.getOrNull()

    private fun String.normalizedHost(): String = lowercase(Locale.ROOT).trimEnd('.')

    private companion object {
        // Exact host suffixes only: never block an unrelated hostname containing these words.
        val AD_HOSTS = setOf(
            "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
            "adnxs.com", "adsafeprotected.com", "taboola.com", "outbrain.com", "popads.net",
            "popcash.net", "propellerads.com", "clickadu.com", "adsterra.com", "exoclick.com",
            "trafficjunky.net", "mgid.com", "adskeeper.com"
        )
    }
}
