package com.heasafe.aura.security

import android.content.Context

/**
 * 跟踪 / 广告 / 恶意域名拦截。
 * 数据源：维护型名单 assets/blocklist.txt（StevenBlack unified hosts + EasyPrivacy，约 3 万域名）
 * + 内置补充名单。匹配采用 host 精确 + 父域后缀 walks（HashSet，O(标签数)，子资源高频路径安全）。
 */
object TrackerBlocklist {

    private val BUILT_IN = listOf(
        "google-analytics.com", "googletagmanager.com", "googleadservices.com",
        "doubleclick.net", "googlesyndication.com",
        "facebook.net", "connect.facebook.net",
        "scorecardresearch.com", "quantserve.com", "quantcast.com",
        "amazon-adsystem.com", "adnxs.com", "casalemedia.com",
        "pubmatic.com", "rubiconproject.com", "openx.net", "criteo.com",
        "taboola.com", "outbrain.com", "hotjar.com", "segment.com",
        "mixpanel.com", "amplitude.com", "bat.bing.com",
        "adsrvr.org", "adservice.google.com", "pagead2.googlesyndication.com",
        "analytics.tiktok.com", "cnzz.com", "umeng.com", "growingio.com", "sensorsdata.cn",
        "hm.baidu.com", "tongji.baidu.com", "pos.baidu.com", "cpro.baidu.com",
        "doubleverify.com", "adsafeprotected.com", "moatads.com", "chartbeat.com",
        "newrelic.com", "nr-data.net", "bugsnag.com", "crashlytics.com", "app-measurement.com",
        "branch.io", "appsflyer.com", "adjust.com", "kochava.com", "singular.net",
        "revcontent.com", "mgid.com", "popads.net", "popcash.net",
        "exoclick.com", "juicyads.com", "trafficjunky.com", "adcash.com", "propellerads.com"
    )

    @Volatile
    private var loaded: HashSet<String>? = null

    /** 启动时加载维护型名单（一次性，后台线程调用亦可）。 */
    fun load(context: Context) {
        if (loaded != null) return
        val set = HashSet<String>()
        try {
            context.assets.open("blocklist.txt").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val d = line.trim().lowercase()
                    if (d.isNotEmpty() && !d.startsWith("#")) set.add(d)
                }
            }
        } catch (_: Exception) { /* 名单缺失时仅用内置 */ }
        loaded = set
    }

    fun shouldBlock(url: String): String? {
        val lower = url.lowercase()
        val host = HOST_REGEX.find(lower)?.groupValues?.get(1) ?: return null
        BUILT_IN.firstOrNull { host == it || host.endsWith(".$it") || lower.contains("//$it/") }
            ?.let { return it }
        val set = loaded ?: return null
        var h = host
        while (true) {
            if (set.contains(h)) return h
            val idx = h.indexOf('.')
            if (idx < 0) break
            h = h.substring(idx + 1)
        }
        return null
    }

    private val HOST_REGEX = Regex("^[a-z]+://([^/?#]+)")
}
