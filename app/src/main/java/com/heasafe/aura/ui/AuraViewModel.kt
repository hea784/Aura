package com.heasafe.aura.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.heasafe.aura.data.DatabaseProvider
import com.heasafe.aura.data.SavedSite
import com.heasafe.aura.security.SecurityLog
import com.heasafe.aura.security.UrlGuard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuraViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = DatabaseProvider.get(app).savedSiteDao()
    val sites = dao.all()
    val securityLog = SecurityLog()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _activeSite = MutableStateFlow<SavedSite?>(null)
    val activeSite: StateFlow<SavedSite?> = _activeSite.asStateFlow()

    fun consumeMessage() { _message.value = null }

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
