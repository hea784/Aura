package com.heasafe.aura.security

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.heasafe.aura.core.MainThread
import java.io.ByteArrayInputStream

/**
 * 安全的 WebView 容器：
 *  - 关闭文件访问、Cookie、第三方 Cookie、定位、弹窗、下载、混合内容；
 *  - 导航与资源请求前统一做 UrlGuard + TrackerBlocklist 判定；
 *  - 与页面的数据通道只用 addWebMessageListener（无 addJavascriptInterface）；
 *  - 所有回调切主线程，规避跨线程触碰 View 的崩溃。
 */
class SecureWebView(
    context: Context,
    private val securityLog: SecurityLog,
    private val onBlocked: (String) -> Unit = {},
    private val onPageFinished: (String) -> Unit = {},
    private val onMessage: (String) -> Unit = {},
    private val onScroll: (scrollY: Int, oldScrollY: Int) -> Unit = { _, _ -> },
    private val strictHttps: Boolean = false
) {

    val webView: WebView = WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentHost: String = ""

    init {
        TrackerBlocklist.load(context)
        applyHardening()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun applyHardening() {
        // 关闭 WebView 远程调试，避免 release 包被 adb/Chrome inspect 注入
        WebView.setWebContentsDebuggingEnabled(false)
        // 滚动回调：供 UI 自动隐藏顶部工具层
        webView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            onScroll(scrollY, oldScrollY)
        }
        webView.settings.apply {
            javaScriptEnabled = true // 现代站点必需；危险能力在下面逐项关闭
            domStorageEnabled = false
            databaseEnabled = false
            loadWithOverviewMode = false
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            allowFileAccess = false
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_NO_CACHE
            setGeolocationEnabled(false)
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = false
            setNeedInitialFocus(false)
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(false)
            setAcceptThirdPartyCookies(webView, false)
            removeAllCookies(null)
            flush()
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return true
                return decideNavigation(url)
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                // 子资源与主文档同一闸门：scheme/本地/私有/同形字 + 维护型拦截名单（防 SSRF/跟踪）
                val verdict = UrlGuard.validate(url)
                val tracker = TrackerBlocklist.shouldBlock(url)
                if (verdict is UrlGuard.Result.Rejected || tracker != null) {
                    MainThread.run(mainHandler) {
                        if (verdict is UrlGuard.Result.Rejected) {
                            securityLog.add(SecurityLog.Event.Type.BLOCKED_URL, "拦截子资源: ${verdict.reason}")
                        }
                        tracker?.let { securityLog.add(SecurityLog.Event.Type.TRACKER, "拦截跟踪: $it") }
                    }
                    return emptyResponse()
                }
                return null
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { onPageFinished(it) }
                injectVideoProbe() // 页面就绪后探测站内视频，供自动模式判断
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    securityLog.add(SecurityLog.Event.Type.OTHER, "页面加载失败: ${error?.description}")
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onReceivedError(
                view: WebView?, errorCode: Int, description: String?, failingUrl: String?
            ) {
                @Suppress("DEPRECATION")
                super.onReceivedError(view, errorCode, description, failingUrl)
            }

            override fun onRenderProcessGone(
                view: WebView?,
                detail: android.webkit.RenderProcessGoneDetail?
            ): Boolean {
                securityLog.add(SecurityLog.Event.Type.OTHER, "渲染进程崩溃，已隔离")
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                // 摄像头 / 麦克风 / 定位一律拒绝
                securityLog.add(SecurityLog.Event.Type.PERMISSION, "拒绝权限请求: ${request?.resources?.joinToString()}")
                request?.deny()
            }

            override fun onPermissionRequestCanceled(request: PermissionRequest?) {
                super.onPermissionRequestCanceled(request)
            }

            override fun onJsAlert(
                view: WebView?, url: String?, message: String?, result: JsResult?
            ): Boolean {
                result?.cancel() // 禁止页面弹窗打扰
                return true
            }

            override fun onJsConfirm(
                view: WebView?, url: String?, message: String?, result: JsResult?
            ): Boolean {
                result?.cancel()
                return true
            }

            override fun onJsPrompt(
                view: WebView?, url: String?, message: String?, defaultValue: String?, result: android.webkit.JsPromptResult?
            ): Boolean {
                result?.cancel()
                return true
            }

            override fun onCreateWindow(
                view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?
            ): Boolean {
                securityLog.add(SecurityLog.Event.Type.OTHER, "拦截页面新窗口")
                return false
            }

            override fun onShowFileChooser(
                webView: WebView?, filePathCallback: android.webkit.ValueCallback<Array<android.net.Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                securityLog.add(SecurityLog.Event.Type.OTHER, "拦截文件选择")
                filePathCallback?.onReceiveValue(null)
                return true
            }
        }

        webView.setDownloadListener { url, _, _, _, _ ->
            securityLog.add(SecurityLog.Event.Type.DOWNLOAD, "拦截下载: $url")
            onBlocked("已拦截下载请求：下载可能携带风险")
        }

        installMessageListener()
    }

    private fun decideNavigation(url: String): Boolean {
        return when (val result = UrlGuard.validate(url)) {
            is UrlGuard.Result.Rejected -> {
                securityLog.add(SecurityLog.Event.Type.BLOCKED_URL, result.reason)
                onBlocked(result.reason)
                true
            }
            is UrlGuard.Result.Allowed -> {
                if (strictHttps && result.url.startsWith("http://")) {
                    val reason = "仅 HTTPS 模式已开启，拦截明文请求：${result.host}"
                    securityLog.add(SecurityLog.Event.Type.BLOCKED_URL, reason)
                    onBlocked(reason)
                    true
                } else {
                    currentHost = result.host
                    false // 放行，由本 WebView 自己加载
                }
            }
        }
    }

    /** 页面 -> 原生只用 WebMessageListener，绝不 addJavascriptInterface。 */
    private fun installMessageListener() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return
        try {
            WebViewCompat.addWebMessageListener(
                webView, "AuraBridge", setOf("*"),
                androidx.webkit.WebViewCompat.WebMessageListener { _, message, _, _, _ ->
                    MainThread.run(mainHandler) {
                        if (message.type == WebMessageCompat.TYPE_STRING) {
                            message.data?.let { onMessage(it) }
                        }
                    }
                }
            )
        } catch (e: Exception) {
            securityLog.add(SecurityLog.Event.Type.OTHER, "消息通道不可用: ${e.message}")
        }
    }

    fun load(url: String) {
        MainThread.run(mainHandler) {
            when (val result = UrlGuard.validate(url)) {
                is UrlGuard.Result.Rejected -> {
                    securityLog.add(SecurityLog.Event.Type.BLOCKED_URL, result.reason)
                    onBlocked(result.reason)
                }
                is UrlGuard.Result.Allowed -> {
                    currentHost = result.host
                    // 第二道闸门：再次校验 host，确保满足 http/https + 非本地/私有/保留地址
                    if (UrlGuard.isBlockedHost(result.host)) {
                        val reason = "出于安全考虑，禁止访问本地或内网地址：${result.host}"
                        securityLog.add(SecurityLog.Event.Type.BLOCKED_URL, reason)
                        onBlocked(reason)
                    } else if (strictHttps && result.url.startsWith("http://")) {
                        val reason = "仅 HTTPS 模式已开启，拦截明文地址：${result.host}"
                        securityLog.add(SecurityLog.Event.Type.BLOCKED_URL, reason)
                        onBlocked(reason)
                    } else {
                        val clean = UrlCleaner.stripTrackingParams(result.url)
                        webView.loadUrl(clean)
                    }
                }
            }
        }
    }

    /** 画质增强：注入 CSS filter（对比度 / 饱和度 / 锐化观感），参考 Kazumi 的清晰度取向。 */
    fun applyVisualEnhancement(contrast: Float, saturate: Float, brightness: Float) {
        MainThread.run(mainHandler) {
            val js = """
                (function(){
                  var s=document.getElementById('aura-visual-style');
                  if(!s){s=document.createElement('style');s.id='aura-visual-style';document.head.appendChild(s);}
                  s.textContent='html,body{filter:contrast($contrast) saturate($saturate) brightness($brightness) !important;}';
                })();
            """.trimIndent()
            webView.evaluateJavascript(js, null)
        }
    }

    fun clearVisualEnhancement() {
        MainThread.run(mainHandler) {
            webView.evaluateJavascript(
                "(function(){var s=document.getElementById('aura-visual-style');if(s)s.remove();})();", null
            )
        }
    }

    /** 页面加载完成后注入视频探测脚本：识别站内视频形态并回传元数据。 */
    fun injectVideoProbe() {
        MainThread.run(mainHandler) {
            try {
                val script = webView.context.assets.open("video_probe.js")
                    .bufferedReader().use { it.readText() }
                webView.evaluateJavascript(script, null)
            } catch (e: Exception) {
                securityLog.add(SecurityLog.Event.Type.OTHER, "视频探测脚本加载失败: ${e.message}")
            }
        }
    }

    /** 防指纹：在文档开始前注入反跟踪脚本（不支持时回退页面完成后注入）。 */
    fun injectAntiFingerprint() {
        MainThread.run(mainHandler) {
            try {
                val script = webView.context.assets.open("anti_fingerprint.js")
                    .bufferedReader().use { it.readText() }
                if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                    WebViewCompat.addDocumentStartJavaScript(webView, script, setOf("*"))
                } else {
                    webView.evaluateJavascript(script, null)
                }
            } catch (e: Exception) {
                securityLog.add(SecurityLog.Event.Type.OTHER, "防指纹脚本加载失败: ${e.message}")
            }
        }
    }

    /** 短视频模式：注入滚动吸附与沉浸式样式（竖向 feed 观感）。 */
    fun enableShortVideoMode() {
        MainThread.run(mainHandler) {
            val js = """
                (function(){
                  var s=document.getElementById('aura-short-mode');
                  if(!s){s=document.createElement('style');s.id='aura-short-mode';document.head.appendChild(s);}
                  s.textContent='html{scroll-snap-type:y mandatory !important;} video{scroll-snap-align:start !important;max-height:100vh !important;object-fit:cover !important;} ::-webkit-scrollbar{display:none !important;}';
                })();
            """.trimIndent()
            webView.evaluateJavascript(js, null)
        }
    }

    fun purge() {
        MainThread.run(mainHandler) {
            webView.clearCache(true)
            webView.clearHistory()
            webView.clearFormData()
            CookieManager.getInstance().removeAllCookies(null)
            webView.destroy()
        }
    }

    private fun emptyResponse(): WebResourceResponse = WebResourceResponse(
        "text/plain", "UTF-8", ByteArrayInputStream("".toByteArray())
    )
}
