package com.heasafe.aura.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "aura_settings")

/** 持久化偏好：工具栏自动隐藏 / 防指纹 / 仅 HTTPS / 画质增强。 */
data class AppSettings(
    val autoHideChrome: Boolean = true,
    val antiFingerprint: Boolean = true,
    val strictHttps: Boolean = false,
    val enhance: Boolean = true,
    val allowedHosts: Set<String> = emptySet()
)

object SettingsKeys {
    val AUTO_HIDE = booleanPreferencesKey("auto_hide_chrome")
    val ANTI_FP = booleanPreferencesKey("anti_fingerprint")
    val STRICT_HTTPS = booleanPreferencesKey("strict_https")
    val ENHANCE = booleanPreferencesKey("enhance")
    val ALLOWED_HOSTS = stringSetPreferencesKey("allowed_hosts")
}

class SettingsStore(private val context: Context) {
    val flow: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            autoHideChrome = p[SettingsKeys.AUTO_HIDE] ?: true,
            antiFingerprint = p[SettingsKeys.ANTI_FP] ?: true,
            strictHttps = p[SettingsKeys.STRICT_HTTPS] ?: false,
            enhance = p[SettingsKeys.ENHANCE] ?: true,
            allowedHosts = p[SettingsKeys.ALLOWED_HOSTS] ?: emptySet()
        )
    }

    suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        context.settingsDataStore.edit { it[key] = value }
    }

    suspend fun setAllowedHost(host: String, allow: Boolean) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[SettingsKeys.ALLOWED_HOSTS]?.toMutableSet() ?: mutableSetOf()
            if (allow) current.add(host.lowercase()) else current.remove(host.lowercase())
            prefs[SettingsKeys.ALLOWED_HOSTS] = current
        }
    }
}
