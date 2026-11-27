package com.heasafe.aura.security

/**
 * 跟踪 / 广告域名拦截名单。精确域名 + 后缀匹配，按子域继承。
 * 通过 shouldBlock 返回命中项，便于写入安全事件日志。
 */
object TrackerBlocklist {

    private val TRACKER_DOMAINS = listOf(
        "google-analytics.com", "googletagmanager.com", "googleadservices.com",
        "doubleclick.net", "googlesyndication.com", "google.com/recaptcha",
        "facebook.net", "facebook.com/tr", "connect.facebook.net",
        "scorecardresearch.com", "quantserve.com", "quantcast.com",
        "amazon-adsystem.com", "adnxs.com", "casalemedia.com",
        "pubmatic.com", "rubiconproject.com", "openx.net", "criteo.com",
        "taboola.com", "outbrain.com", "hotjar.com", "segment.com",
        "mixpanel.com", "amplitude.com", "sentry.io", "bat.bing.com",
        "bing.com/click", "twitter.com/i/adsct", "t.co", "pixel.quantserve.com",
        "adsrvr.org", "adservice.google.com", "pagead2.googlesyndication.com",
        "analytics.tiktok.com", "byteimg.com", "snapchat.com"
    )

    fun shouldBlock(url: String): String? {
        val lower = url.lowercase()
        val host = HOST_REGEX.find(lower)?.groupValues?.get(1) ?: return null
        return TRACKER_DOMAINS.firstOrNull { domain ->
            host == domain || host.endsWith(".$domain") || lower.contains("//$domain/")
        }
    }

    private val HOST_REGEX = Regex("^[a-z]+://([^/?#]+)")
}
