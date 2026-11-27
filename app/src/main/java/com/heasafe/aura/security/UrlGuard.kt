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

        // 仅当完全没有 scheme 时才补 https；已有 scheme（含 javascript:/data: 等）保留原样交由白名单拒绝
        val hasScheme = SCHEME_REGEX.containsMatchIn(trimmed)
        val withScheme = if (hasScheme) trimmed else "https://$trimmed"
        // URI 解析前先做 IDN 归一化（java.net.URI 不接受非 ASCII host）
        val asciiInput = idnNormalize(withScheme)
        val uri = try {
            URI(asciiInput)
        } catch (e: Exception) {
            return Result.Rejected("地址格式不合法")
        }

        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") {
            return Result.Rejected("仅支持 http / https 网页，已拦截 $scheme")
        }

        if (uri.userInfo != null) return Result.Rejected("禁止 URL 中内嵌账号密码")

        // java.net.URI 对非 ASCII（IDN）host 返回 null，回退从 authority 做 IDN 归一化
        val host = uri.host
            ?: uri.rawAuthority?.substringBefore(':')?.let {
                runCatching { IDN.toASCII(it, IDN.ALLOW_UNASSIGNED) }.getOrNull()
            }
            ?: return Result.Rejected("无法解析主机名")
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

        // 校验什么就加载什么：返回归一化后的 URL（scheme/host 小写 punycode），避免校验/加载对象不一致
        val port = if (uri.port != -1) ":${uri.port}" else ""
        val path = uri.rawPath ?: ""
        val query = uri.rawQuery?.let { "?$it" } ?: ""
        val frag = uri.rawFragment?.let { "#$it" } ?: ""
        val normalizedUrl = "$scheme://$normalizedHost$port$path$query$frag"
        return Result.Allowed(normalizedUrl, normalizedHost)
    }

    /**
     * 同形字钓鱼检测：主机名含西里尔/希腊等易混淆字母即拒绝（无论混合拉丁或整域替换，
     * 如 аррӏе.com 全西里尔仿冒 apple）。中文等 CJK 属正常 IDN，不拦截。
     */
    fun isHomographHost(host: String): Boolean {
        val lower = host.lowercase(Locale.ROOT)
        return lower.any { it in 'Ѐ'..'ӿ' || it in 'Ͱ'..'Ͽ' }
    }

    /**
     * 拒绝本地 / 环回 / 私有 / 保留 / 链路本地 / 多播 / 未指定地址。
     * 对不能直接解析的字面量做保守拦截（宁可多拦，不放行风险地址）。
     */
    private val hostVerdict = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, Boolean>>()

    fun isBlockedHost(hostInput: String): Boolean {
        val host = hostInput.trim().trimEnd('.').lowercase(Locale.ROOT)
        val now = System.currentTimeMillis()
        hostVerdict[host]?.let { (t, v) -> if (now - t < 30_000) return v }
        val v = computeBlocked(host)
        hostVerdict[host] = now to v
        return v
    }

    private fun computeBlocked(host: String): Boolean {
        if (host.isEmpty()) return true
        if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local")) return true
        if (host == "localhost.localdomain") return true

        // 先拦非标准 IP 字面量（十六进制/八进制/整数/短格式），不依赖 InetAddress 解析差异
        if (isSuspiciousIpLiteral(host)) return true

        // 标准 IPv4 / IPv6 字面量交由 InetAddress 判定私有/环回等
        val asAddress = try {
            InetAddress.getByName(host)
        } catch (e: Exception) {
            null
        }
        if (asAddress != null && isUnsafeAddress(asAddress)) return true

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

    /**
     * 非标准 IP 字面量保守拦截：十六进制(0x7f000001/0x7f.0.0.1)、八进制(0177.0.0.1)、
     * 整数(2130706433)、短格式(127.1)。Chromium 会解析这些而 InetAddress 未必，
     * 故一律拒绝，避免校验/加载解析器差异造成 SSRF 绕过。标准四段十进制/标准 IPv6 放行交由 InetAddress。
     */
    private fun isSuspiciousIpLiteral(host: String): Boolean {
        val core = host.removePrefix("[").removeSuffix("]")
        if (core.contains(':')) return false // 标准 IPv6 交由 InetAddress
        if (core.contains("0x") || core.contains("0X")) return true
        val labels = core.split('.')
        val allNumeric = labels.all { it.isNotEmpty() && it.all { c -> c.isDigit() } }
        if (allNumeric) {
            if (labels.size != 4) return true // 整数 / 短格式
            if (labels.any { it.length > 1 && it.startsWith("0") }) return true // 前导零=八进制
            if (labels.any { (it.toIntOrNull() ?: -1) !in 0..255 }) return true
            return false // 标准四段十进制
        }
        // 混合十六进制分量（0x7f.0.0.1）
        if (labels.any { it.startsWith("0x") || it.startsWith("0X") }) return true
        return false
    }

    private val SCHEME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")

    /** 把 http/https URL 的 authority host 预先转 punycode，保留 userinfo/port/path。 */
    private fun idnNormalize(input: String): String {
        val m = Regex("^(https?://)([^/?#]*)(.*)$", RegexOption.IGNORE_CASE).find(input) ?: return input
        val prefix = m.groupValues[1]
        val auth = m.groupValues[2]
        val rest = m.groupValues[3]
        val userinfo = if (auth.contains('@')) auth.substringBeforeLast('@') + "@" else ""
        val hostPort = auth.substringAfterLast('@')
        val host = hostPort.substringBefore(':')
        val port = if (hostPort.contains(':')) ":" + hostPort.substringAfter(':') else ""
        val ascii = runCatching { IDN.toASCII(host, IDN.ALLOW_UNASSIGNED) }.getOrDefault(host)
        return prefix + userinfo + ascii + port + rest
    }
}
