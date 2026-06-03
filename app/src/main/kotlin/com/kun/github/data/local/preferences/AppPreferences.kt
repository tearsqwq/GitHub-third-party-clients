package com.kun.github.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class AppPreferences(private val context: Context) {

    companion object {
        private val HAS_ACCEPTED_TERMS_KEY = booleanPreferencesKey("has_accepted_terms")
        private val HAS_SHOWN_SPLASH_KEY = booleanPreferencesKey("has_shown_splash")
    }

    // 是否已同意服务协议和隐私政策
    val hasAcceptedTermsFlow: Flow<Boolean> = context.appDataStore.data.map { preferences ->
        preferences[HAS_ACCEPTED_TERMS_KEY] ?: false
    }

    // 是否已显示过启动页
    val hasShownSplashFlow: Flow<Boolean> = context.appDataStore.data.map { preferences ->
        preferences[HAS_SHOWN_SPLASH_KEY] ?: false
    }

    suspend fun setTermsAccepted(accepted: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[HAS_ACCEPTED_TERMS_KEY] = accepted
        }
    }

    suspend fun setSplashShown(shown: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[HAS_SHOWN_SPLASH_KEY] = shown
        }
    }

    suspend fun clearAll() {
        context.appDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    fun saveThemeMode(mode: String) {
        val prefs = context.getSharedPreferences("github_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun getThemeMode(): String {
        val prefs = context.getSharedPreferences("github_prefs", Context.MODE_PRIVATE)
        return prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
    }
}
