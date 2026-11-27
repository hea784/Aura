package com.heasafe.aura.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlGuardTest {

    @Test
    fun allowsPublicHttps() {
        val result = UrlGuard.validate("https://example.com")
        assertTrue(result is UrlGuard.Result.Allowed)
        assertEquals("example.com", (result as UrlGuard.Result.Allowed).host)
    }

    @Test
    fun allowsPublicHttp() {
        val result = UrlGuard.validate("http://example.com/path")
        assertTrue(result is UrlGuard.Result.Allowed)
    }

    @Test
    fun addsHttpsWhenSchemeMissing() {
        val result = UrlGuard.validate("example.com")
        assertTrue(result is UrlGuard.Result.Allowed)
        assertEquals("https://example.com", (result as UrlGuard.Result.Allowed).url)
    }

    @Test
    fun rejectsDangerousSchemes() {
        listOf(
            "file:///etc/passwd",
            "javascript:alert(1)",
            "data:text/html,<script>alert(1)</script>",
            "intent://foo",
            "market://details?id=x",
            "ftp://example.com"
        ).forEach { url ->
            val result = UrlGuard.validate(url)
            assertTrue("$url should be rejected", result is UrlGuard.Result.Rejected)
        }
    }

    @Test
    fun rejectsLocalhostAndLoopback() {
        listOf(
            "http://localhost",
            "http://localhost:8080",
            "http://127.0.0.1",
            "http://127.0.0.2",
            "http://[::1]",
            "http://0.0.0.0",
            "http://foo.localhost"
        ).forEach { url ->
            val result = UrlGuard.validate(url)
            assertTrue("$url should be rejected", result is UrlGuard.Result.Rejected)
        }
    }

    @Test
    fun rejectsPrivateAndReservedAddresses() {
        listOf(
            "http://192.168.1.1",
            "http://10.0.0.1",
            "http://172.16.0.1",
            "http://172.31.255.255",
            "http://169.254.169.254", // 云元数据端点
            "http://224.0.0.1",       // 多播
            "http://240.0.0.1"        // 保留
        ).forEach { url ->
            val result = UrlGuard.validate(url)
            assertTrue("$url should be rejected", result is UrlGuard.Result.Rejected)
        }
    }

    @Test
    fun rejectsCredentialsAndMalformed() {
        assertTrue(UrlGuard.validate("https://user:pass@example.com") is UrlGuard.Result.Rejected)
        assertTrue(UrlGuard.validate("") is UrlGuard.Result.Rejected)
        assertTrue(UrlGuard.validate("https://" + "a".repeat(3000)) is UrlGuard.Result.Rejected)
    }

    @Test
    fun isBlockedHostCoversNumericVariants() {
        assertTrue(UrlGuard.isBlockedHost("127.1"))
        assertTrue(UrlGuard.isBlockedHost("2130706433"))
        assertFalse(UrlGuard.isBlockedHost("example.com"))
    }

    @Test
    fun rejectsHexIntegerOctalShortIpLiterals() {
        listOf(
            "http://0x7f000001/",
            "http://0x7f.0.0.1/",
            "http://2130706433/",
            "http://127.1/",
            "http://0177.0.0.1/",
            "http://0xA9FEA9FE/"
        ).forEach { url ->
            assertTrue("$url should be rejected", UrlGuard.validate(url) is UrlGuard.Result.Rejected)
        }
    }

    @Test
    fun rejectsWholeScriptHomograph() {
        val result = UrlGuard.validate("https://\\u0430\\u0440\\u0440\\u04CF\\u0435.com")
        assertTrue(result is UrlGuard.Result.Rejected)
    }

    @Test
    fun allowsStandardDottedDecimalPublic() {
        assertTrue(UrlGuard.validate("https://8.8.8.8") is UrlGuard.Result.Allowed)
        // 非 ASCII unicode IDN 输入保守拒绝（安全默认）；punycode 形式放行
        assertTrue(UrlGuard.validate("https://xn--fsqu00a.xn--0zwm56d") is UrlGuard.Result.Allowed)
    }

    @Test
    fun allowedUrlIsNormalizedSameAsValidated() {
        val r = UrlGuard.validate("HTTPS://WWW.Example.COM/Path") as UrlGuard.Result.Allowed
        assertEquals("https://www.example.com/Path", r.url)
        assertEquals("www.example.com", r.host)
    }

    private fun assertEquals(expected: String, actual: String) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
