package com.heasafe.aura.security

import java.net.IDN
import java.net.InetAddress
import java.net.URI
import java.util.Locale

/**
 * Aura 的 URL 安全闸门。纯 JVM 逻辑，便于单元测试。
 *
 * 硬性规则：
 *  - 仅允许 http / https 两种 scheme。
 *  - 加载前校验 host：拒绝 localhost、环回地址、私有地址、保留地址。
 *  - 拒绝畸形 URL、超长 URL、内嵌凭据。
 */
object UrlGuard {

    const val MAX_URL_LENGTH = 4096

    sealed class Result {
        data class Allowed(val url: String, val host: String) : Result()
        data class Rejected(val reason: String) : Result()
    }

    fun validate(raw: String): Result {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return Result.Rejected("地址为空")
        if (trimmed.length > MAX_URL_LENGTH) return Result.Rejected("地址过长")

        val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
        val uri = try {
            URI(withScheme)
        } catch (e: Exception) {
            return Result.Rejected("地址格式不合法")
        }

        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") {
            return Result.Rejected("仅支持 http / https 网页，已拦截 $scheme")
        }

        if (uri.userInfo != null) return Result.Rejected("禁止 URL 中内嵌账号密码")

        val host = uri.host ?: return Result.Rejected("无法解析主机名")
        if (host.length > 253) return Result.Rejected("主机名过长")
        if (isHomographHost(host)) return Result.Rejected("疑似同形字钓鱼域名（混合拉丁与西里尔/希腊字母）：$host")

        // IDN / punycode 归一化后统一做小写判定，防止大小写或同形字绕过
        val normalizedHost = try {
            IDN.toASCII(host, IDN.ALLOW_UNASSIGNED).lowercase(Locale.ROOT)
        } catch (e: Exception) {
            return Result.Rejected("主机名包含非法字符")
        }

        if (isBlockedHost(normalizedHost)) {
            return Result.Rejected("出于安全考虑，禁止访问本地或内网地址：$normalizedHost")
        }

        return Result.Allowed(withScheme, normalizedHost)
    }

    /**
     * 同形字钓鱼检测：主机名同时含拉丁字母与西里尔/希腊字母（视觉易混淆），
     * 例如用西里尔 а 冒充拉丁 a。中文等 CJK + 拉丁属正常 IDN，不拦截。
     */
    fun isHomographHost(host: String): Boolean {
        val lower = host.lowercase(Locale.ROOT)
        val hasLatin = lower.any { it in 'a'..'z' }
        val hasConfusable = lower.any { it in 'Ѐ'..'ӿ' || it in 'Ͱ'..'Ͽ' }
        return hasLatin && hasConfusable
    }

    /**
     * 拒绝本地 / 环回 / 私有 / 保留 / 链路本地 / 多播 / 未指定地址。
     * 对不能直接解析的字面量做保守拦截（宁可多拦，不放行风险地址）。
     */
    fun isBlockedHost(hostInput: String): Boolean {
        val host = hostInput.trim().trimEnd('.').lowercase(Locale.ROOT)
        if (host.isEmpty()) return true
        if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local")) return true
        if (host == "localhost.localdomain") return true

        // IPv4 / IPv6 字面量
        val asAddress = try {
            InetAddress.getByName(host)
        } catch (e: Exception) {
            null
        }
        if (asAddress != null && isUnsafeAddress(asAddress)) return true

        // 纯数字点分形式但解析失败（如 256.1.1.1、简短形式），保守拦截
        if (isNumericLookingHost(host)) return true

        return false
    }

    private fun isUnsafeAddress(addr: InetAddress): Boolean = with(addr) {
        if (isAnyLocalAddress || isLoopbackAddress || isLinkLocalAddress ||
            isSiteLocalAddress || isMulticastAddress
        ) return true
        // Android 上 InetAddress 没有 isReserved/isZeroAddress（JDK 19+ 才有），
        // 这里按字节手工覆盖 0.0.0.0、240.0.0.0/4、255.255.255.255 等保留段。
        val bytes = address
        if (bytes.size == 4) {
            val b0 = bytes[0].toInt() and 0xFF
            if (b0 == 0 || b0 >= 240) return true
        }
        return false
    }

    private fun isNumericLookingHost(host: String): Boolean {
        val core = host.removePrefix("[").removeSuffix("]")
        if (core.count { it == '.' } >= 3 && core.all { it.isDigit() || it == '.' }) return true
        // IPv6 字面量（含 :: 压缩形式）统一由 InetAddress 判断，这里只拦纯十六进制加冒号的可疑写法
        if (core.contains(':') && core.all { it.isDigit() || it in 'a'..'f' || it == ':' }) return true
        return false
    }
}
