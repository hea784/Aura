package com.heasafe.aura

import android.app.Application

class AuraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 所有安全策略在进程内执行，无网络上报、无后台服务。
    }
}
