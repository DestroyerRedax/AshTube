package com.ashrin.ashtube.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ashtube_preferences")

class AppPreferences(private val context: Context) {

    companion object {
        val KEY_NOTICE_ACCEPTED = booleanPreferencesKey("notice_accepted")
        val KEY_ADULT_ACCESS_ENABLED = booleanPreferencesKey("adult_access_enabled")
        val KEY_DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val KEY_THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    }

    val noticeAcceptedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTICE_ACCEPTED] ?: false
    }

    val adultAccessFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ADULT_ACCESS_ENABLED] ?: false
    }

    val defaultQualityFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_DEFAULT_QUALITY] ?: "1080p"
    }

    val themePreferenceFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_PREFERENCE] ?: "Dark"
    }

    suspend fun setNoticeAccepted(accepted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTICE_ACCEPTED] = accepted
        }
    }

    suspend fun setAdultAccessEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ADULT_ACCESS_ENABLED] = enabled
        }
    }

    suspend fun setDefaultQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_QUALITY] = quality
        }
    }

    suspend fun setThemePreference(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_PREFERENCE] = theme
        }
    }
}
