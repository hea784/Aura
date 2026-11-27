package com.heasafe.aura

import android.webkit.WebSettings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.heasafe.aura.security.SecurityLog
import com.heasafe.aura.security.SecureWebView
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** 在真实 WebView 上验证安全加固项确实生效。 */
@RunWith(AndroidJUnit4::class)
class SecureWebViewInstrumentedTest {

    @Test
    fun hardeningDisablesDangerousCapabilities() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val log = SecurityLog()
        val container = SecureWebView(context, log)
        val settings: WebSettings = container.webView.settings

        assertFalse("文件访问必须关闭", settings.allowFileAccess)
        assertFalse("内容访问必须关闭", settings.allowContentAccess)
        assertFalse("文件 URL 读文件必须关闭", settings.allowFileAccessFromFileURLs)
        assertFalse("通用文件访问必须关闭", settings.allowUniversalAccessFromFileURLs)
        assertFalse("地理位置必须关闭", settings.geolocationEnabled)
        assertFalse("JS 自动开窗必须关闭", settings.javaScriptCanOpenWindowsAutomatically)
        assertTrue("混合内容必须禁止", settings.mixedContentMode == WebSettings.MIXED_CONTENT_NEVER_ALLOW)
    }
}
