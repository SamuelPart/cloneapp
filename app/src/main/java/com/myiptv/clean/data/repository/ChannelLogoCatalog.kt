package com.myiptv.clean.data.repository

import com.myiptv.clean.data.model.IptvOrgLogo
import com.myiptv.clean.ui.compose.LiveChannel
import java.net.URI
import java.util.Locale

/** An exact feed logo takes precedence; another region's feed must not be used as a fallback. */
internal data class ChannelLogoKey(val channel: String, val feed: String?)

private val supportedLogoFormats = setOf("PNG", "JPEG", "JPG", "WEBP")
private val logoSchemes = setOf("http", "https")

private fun safeLogoUrl(value: String?): String? {
    val url = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    return url.takeIf { uri.scheme?.lowercase(Locale.ROOT) in logoSchemes && !uri.host.isNullOrBlank() }
}

internal fun selectChannelLogos(records: List<IptvOrgLogo>): Map<ChannelLogoKey, String> {
    val result = LinkedHashMap<ChannelLogoKey, String>()
    for (record in records) {
        if (record.inUse != true) continue
        val format = record.format?.trim()?.uppercase(Locale.ROOT)
        // Coil's default Android decoder does not include an SVG/GIF decoder.
        if (format !in supportedLogoFormats) continue
        val channel = record.channel?.trim()?.takeIf(String::isNotEmpty) ?: continue
        val url = safeLogoUrl(record.url) ?: continue
        val feed = record.feed?.trim()?.takeIf(String::isNotEmpty)
        val key = ChannelLogoKey(channel, feed)
        if (key !in result) result[key] = url
    }
    return result
}

internal fun channelLogoUrl(channel: LiveChannel, logos: Map<ChannelLogoKey, String>): String? {
    safeLogoUrl(channel.logoUrl)?.let { return it }
    val providerId = channel.epgId?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val feed = channel.feedId?.trim()?.takeIf(String::isNotEmpty)
    return (feed?.let { logos[ChannelLogoKey(providerId, it)] }) ?: logos[ChannelLogoKey(providerId, null)]
}
