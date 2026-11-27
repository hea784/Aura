package com.heasafe.aura.security

import android.content.Context

/**
 * 广告 / 跟踪 / 恶意资源拦截引擎（EasyList + EasyPrivacy + StevenBlack 合并规则）。
 * 规则格式（assets/blocklist.txt / filesDir/rules_v2.txt）：
 *   D <domain>            域名及其子域
 *   P <domain>/<prefix>   域名 + URL 路径前缀
 *   A <domain>            例外（ABP @@ 允许）
 * 另加载 assets/cosmetic.txt / filesDir/cosmetic.txt：通用 cosmetic 选择器（注入 CSS 隐藏广告位）。
 * 匹配使用 HashSet + 父域 walks，仅 682 条路径规则按域索引，适配子资源高频路径。
 */
object TrackerBlocklist {

    @Volatile private var domainSet: HashSet<String> = HashSet()
    @Volatile private var allowSet: HashSet<String> = HashSet()
    @Volatile private var pathIndex: HashMap<String, MutableList<String>> = HashMap()
    @Volatile private var cosmetic: List<String> = emptyList()
    @Volatile private var loaded = false

    /** 通用广告路径启发式（低误报子串）。 */
    private val AD_PATH_MARKERS = listOf(
        "/ads/", "/adserver", "/ad_banner", "/banner_ad", "/popunder", "/prebid",
        "/advert/", "/adframe", "/adsense/", "googlesyndication", "doubleclick",
        "/ad_service", "/adservice", "/ad_click", "/adview", "/adslot"
    )

    fun load(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            try {
                // 优先使用可更新的 filesDir 规则，否则回退打包 assets
                val rulesFile = java.io.File(context.filesDir, "rules_v2.txt")
                val rulesStream = if (rulesFile.exists()) rulesFile.inputStream()
                else context.assets.open("blocklist.txt")
                val d = HashSet<String>(); val a = HashSet<String>(); val p = HashMap<String, MutableList<String>>()
                rulesStream.bufferedReader().useLines { lines ->
                    lines.forEach { raw ->
                        val line = raw.trim()
                        if (line.isEmpty() || line.startsWith("#")) return@forEach
                        if (line.length < 3) return@forEach
                        when (line[0]) {
                            'D' -> if (line.length > 2) d.add(line.substring(2).trim().lowercase())
                            'A' -> if (line.length > 2) a.add(line.substring(2).trim().lowercase())
                            'P' -> run {
                                val body = line.substring(2).trim().lowercase()
                                val idx = body.indexOf('/')
                                if (idx > 0) {
                                    val host = body.substring(0, idx)
                                    val prefix = body.substring(idx + 1)
                                    if (host.contains('.') && prefix.isNotEmpty()) {
                                        p.getOrPut(host) { mutableListOf() }.add(prefix)
                                    }
                                }
                            }
                            else -> if (line.contains('.')) d.add(line.lowercase()) // 兼容旧格式
                        }
                    }
                }
                domainSet = d; allowSet = a; pathIndex = p

                val cosFile = java.io.File(context.filesDir, "cosmetic.txt")
                val cosStream = if (cosFile.exists()) cosFile.inputStream() else context.assets.open("cosmetic.txt")
                cosmetic = cosStream.bufferedReader().useLines { s -> s.filter { it.isNotBlank() }.take(3000).toList() }
                loaded = true
            } catch (_: Exception) {
                loaded = true
            }
        }
    }

    fun reload(context: Context) {
        synchronized(this) { loaded = false }
        load(context)
    }

    /** 返回命中的规则标识；null = 放行。 */
    fun shouldBlock(url: String): String? {
        val lower = url.lowercase()
        val m = HOST_REGEX.find(lower) ?: return null
        val host = m.groupValues[1]
        val path = PATH_REGEX.find(lower)?.value ?: "/"

        if (matchesAllow(host)) return null
        if (matchesDomain(host)) return host
        matchPath(host, path)?.let { return it }
        if (AD_PATH_MARKERS.any { path.contains(it) || lower.contains(it) }) return "heuristic:$host"
        return null
    }

    private fun matchesAllow(host: String): Boolean {
        if (allowSet.isEmpty()) return false
        var h = host
        while (true) {
            if (allowSet.contains(h)) return true
            val i = h.indexOf('.'); if (i < 0) break; h = h.substring(i + 1)
        }
        return false
    }

    private fun matchesDomain(host: String): Boolean {
        if (domainSet.isEmpty()) return false
        var h = host
        while (true) {
            if (domainSet.contains(h)) return true
            val i = h.indexOf('.'); if (i < 0) break; h = h.substring(i + 1)
        }
        return false
    }

    private fun matchPath(host: String, path: String): String? {
        var h = host
        while (true) {
            pathIndex[h]?.firstOrNull { path.startsWith("/$it") }?.let { return "$h/$it" }
            val i = h.indexOf('.'); if (i < 0) break; h = h.substring(i + 1)
        }
        return null
    }

    /** 生成注入用的 cosmetic CSS（隐藏通用广告位）。 */
    fun cosmeticCss(): String {
        if (cosmetic.isEmpty()) return ""
        return buildString {
            append("html body ")
            cosmetic.forEachIndexed { i, sel ->
                if (i > 0) append(",html body ")
                append(sel)
            }
            append("{display:none!important;}")
        }
    }

    fun ruleCount(): Int = domainSet.size + pathIndex.values.sumOf { it.size }

    private val HOST_REGEX = Regex("^[a-z]+://([^/?#]+)")
    private val PATH_REGEX = Regex("^[a-z]+://[^/?#]+([/?#][^\\s]*)")
}
