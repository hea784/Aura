package com.heasafe.aura.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** 更新信息：来自 GitHub Releases 的公开 API（仅 https）。 */
data class UpdateInfo(
    val version: String,
    val notes: String,
    val pageUrl: String,
    val apkUrl: String?
)

object UpdateChecker {
    private const val API = "https://api.github.com/repos/hea784/Aura/releases/latest"

    /** 检查是否有比当前版本更新的 Release；网络/解析失败返回 null（静默）。 */
    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Aura-Android")
            }
            if (conn.responseCode != 200) { conn.disconnect(); return@withContext null }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val obj = JSONObject(body)
            val tag = obj.optString("tag_name", "").removePrefix("v")
            if (tag.isEmpty() || !isNewer(tag, currentVersion)) return@withContext null
            var apk: String? = null
            val assets = obj.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name", "").endsWith(".apk")) {
                        apk = a.optString("browser_download_url", null)
                        break
                    }
                }
            }
            UpdateInfo(
                version = tag,
                notes = obj.optString("body", "").take(600),
                pageUrl = obj.optString("html_url", "https://github.com/hea784/Aura/releases"),
                apkUrl = apk
            )
        } catch (_: Exception) {
            null
        }
    }

    /** 简单语义化比较：major.minor.patch 逐段数值比较。 */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = candidate.split(".").map { it.toIntOrNull() ?: 0 }
        val b = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
