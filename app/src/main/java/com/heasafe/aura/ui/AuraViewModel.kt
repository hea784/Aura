package com.heasafe.aura.ui

import android.app.Application
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.heasafe.aura.BuildConfig
import com.heasafe.aura.data.AppSettings
import com.heasafe.aura.data.DatabaseProvider
import com.heasafe.aura.data.LogProvider
import com.heasafe.aura.data.Logger
import com.heasafe.aura.data.SavedSite
import com.heasafe.aura.data.SettingsStore
import com.heasafe.aura.security.SecurityLog
import com.heasafe.aura.security.UrlGuard
import com.heasafe.aura.update.UpdateChecker
import com.heasafe.aura.update.UpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuraViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = DatabaseProvider.get(app).savedSiteDao()
    val sites = dao.all()
    val securityLog = SecurityLog()

    private val settingsStore = SettingsStore(app)
    val settings = settingsStore.flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _activeSite = MutableStateFlow<SavedSite?>(null)
    val activeSite: StateFlow<SavedSite?> = _activeSite.asStateFlow()

    private val _update = MutableStateFlow<UpdateInfo?>(null)
    val update: StateFlow<UpdateInfo?> = _update.asStateFlow()

    init {
        // 安全事件同步落盘到持久化日志
        securityLog.sink = { e -> Logger.add(app, e.type.name, e.detail) }
    }

    fun consumeMessage() { _message.value = null }
    fun consumeUpdate() { _update.value = null }

    fun logs(limit: Int = 500) = LogProvider.get(getApplication()).logDao().recent(limit)

    fun clearLogs() {
        Logger.clear(getApplication())
        _message.value = "日志已清空"
    }

    fun setSetting(key: Preferences.Key<Boolean>, value: Boolean) = viewModelScope.launch {
        settingsStore.set(key, value)
    }

    /** 按站放行广告/跟踪（修复站点破版）。 */
    fun setSiteAdsAllowed(host: String, allow: Boolean) = viewModelScope.launch {
        settingsStore.setAllowedHost(host, allow)
        _message.value = if (allow) "已为本站放行广告" else "已恢复本站广告拦截"
    }

    /** 检查 GitHub Release 更新；manual=true 时无更新也给出反馈。 */
    fun checkUpdate(manual: Boolean = false) = viewModelScope.launch {
        val info = UpdateChecker.check(BuildConfig.VERSION_NAME)
        Logger.add(
            getApplication(), "UPDATE",
            if (info != null) "发现新版本 ${info.version}" else "检查更新：已是最新或网络不可用"
        )
        if (info != null) _update.value = info
        else if (manual) _message.value = "已是最新版本 ${BuildConfig.VERSION_NAME}"
    }

    fun addSite(rawUrl: String, title: String? = null) {
        when (val result = UrlGuard.validate(rawUrl)) {
            is UrlGuard.Result.Rejected -> _message.value = result.reason
            is UrlGuard.Result.Allowed -> viewModelScope.launch {
                val displayTitle = title?.takeIf { it.isNotBlank() }
                    ?: result.host.removePrefix("www.").replaceFirstChar { it.uppercase() }
                // 去重：同 host + 同 URL 已存在则不重复添加
                val existing = dao.allOnce().any { it.url == result.url }
                if (existing) {
                    _message.value = "该站点已在你的空间里"
                } else {
                    dao.insert(
                        SavedSite(title = displayTitle, url = result.url, host = result.host)
                    )
                    _message.value = "已添加「$displayTitle」"
                }
            }
        }
    }

    fun deleteSite(site: SavedSite) = viewModelScope.launch {
        dao.delete(site)
        if (_activeSite.value?.id == site.id) _activeSite.value = null
        _message.value = "已删除「${site.title}」"
    }

    fun setFavorite(site: SavedSite, fav: Boolean) = viewModelScope.launch {
        dao.setFavorite(site.id, fav)
    }

    fun setVideoMode(site: SavedSite, mode: String) = viewModelScope.launch {
        dao.setVideoMode(site.id, mode)
        _message.value = when (mode) {
            "short" -> "已切换到短视频模式"
            "normal" -> "已切换到普通视频模式"
            else -> "已恢复自动模式"
        }
    }

    /** 浏览页内即时切换当前站点的视频模式（持久化）。 */
    fun setVideoModeOnCurrent(mode: String) {
        val site = _activeSite.value ?: return
        viewModelScope.launch { dao.setVideoMode(site.id, mode) }
    }

    fun openSite(site: SavedSite) = viewModelScope.launch {
        dao.touch(site.id)
        _activeSite.value = site.copy(lastOpenedAt = System.currentTimeMillis())
    }

    fun closeSite() { _activeSite.value = null }

    fun reportBlocked(reason: String) { _message.value = reason }
}
