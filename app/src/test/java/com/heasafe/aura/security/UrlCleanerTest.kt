package com.heasafe.aura.security

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlCleanerTest {

    @Test
    fun stripsUtmParams() {
        val cleaned = UrlCleaner.stripTrackingParams("https://example.com/page?utm_source=google&id=1")
        assertEquals("https://example.com/page?id=1", cleaned)
    }

    @Test
    fun stripsAllUtmPrefixed() {
        val cleaned = UrlCleaner.stripTrackingParams("https://example.com/?utm_foo=1&utm_bar=2&keep=yes")
        assertEquals("https://example.com/?keep=yes", cleaned)
    }

    @Test
    fun preservesBusinessParams() {
        val url = "https://example.com/?token=abc&sign=xyz&auth_key=123"
        assertEquals(url, UrlCleaner.stripTrackingParams(url))
    }

    @Test
    fun preservesFragment() {
        val cleaned = UrlCleaner.stripTrackingParams("https://example.com/p?utm_source=x#section")
        assertEquals("https://example.com/p#section", cleaned)
    }

    @Test
    fun handlesNoQuery() {
        assertEquals("https://example.com", UrlCleaner.stripTrackingParams("https://example.com"))
    }
}
