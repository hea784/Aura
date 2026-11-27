package com.heasafe.aura.core

import android.os.Handler
import android.os.Looper

/** 统一主线程执行，避免 WebView 回调跨线程触碰 View。 */
object MainThread {
    private val handler = Handler(Looper.getMainLooper())

    fun run(handler: Handler = this.handler, block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else handler.post(block)
    }
}
