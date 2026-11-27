package com.heasafe.aura.security

import java.net.URI
import java.util.Locale

/**
 * 剥离 URL 中常见的跟踪参数；只按精确白名单 + utm_ 前缀处理，
 * 绝不模糊匹配，避免误伤 token / sign / auth_key 等真实业务参数。
 */
object UrlCleaner {

    private val TRACKING_KEYS = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "utm_id", "utm_name", "utm_reader", "utm_brand", "utm_social",
        "fbclid", "gclid", "gbraid", "wbraid", "msclkid", "ttclid",
        "igshid", "mc_cid", "mc_eid", "_ga", "_gl", "spm", "scm"
    )

    fun stripTrackingParams(url: String): String {
        val uri = try { URI(url) } catch (e: Exception) { return url }
        val query = uri.rawQuery ?: return url
        val kept = query.split('&').mapNotNull { pair ->
            val key = pair.substringBefore('=').urlDecode().lowercase(Locale.ROOT)
            if (key in TRACKING_KEYS || key.startsWith("utm_")) null else pair
        }
        if (kept.size == query.split('&').size) return url

        val rebuilt = StringBuilder().append(uri.scheme).append("://").append(uri.rawAuthority)
        rebuilt.append(uri.rawPath ?: "")
        if (kept.isNotEmpty()) rebuilt.append('?').append(kept.joinToString("&"))
        if (uri.rawFragment != null) rebuilt.append('#').append(uri.rawFragment)
        return rebuilt.toString()
    }

    private fun String.urlDecode(): String = try {
        java.net.URLDecoder.decode(this, "UTF-8")
    } catch (e: Exception) {
        this
    }
}
