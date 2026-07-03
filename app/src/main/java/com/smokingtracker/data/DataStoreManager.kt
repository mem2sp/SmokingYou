package com.smokingtracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "smoking_tracker_prefs")

enum class ThemePreference {
    SYSTEM, LIGHT, DARK
}

class DataStoreManager(private val context: Context) {
    private val gson = Gson()

    companion object {
        val IS_REGISTERED = booleanPreferencesKey("is_registered")
        val SMOKING_ENTRIES = stringPreferencesKey("smoking_entries")
        val APP_THEME = stringPreferencesKey("app_theme")
        val UNLOCKED_ACHIEVEMENTS = stringPreferencesKey("unlocked_achievements")
        val DAILY_LIMIT = intPreferencesKey("daily_limit")
        val APP_LAUNCH_DATES = stringPreferencesKey("app_launch_dates")
        val FONT_PRESET = stringPreferencesKey("font_preset")
        val AMOLED_THEME = booleanPreferencesKey("amoled_theme")
    }

    val isRegistered: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_REGISTERED] ?: false
    }

    val smokingEntries: Flow<List<Long>> = context.dataStore.data.map { preferences ->
        val entriesJson = preferences[SMOKING_ENTRIES] ?: "[]"
        val listType = object : TypeToken<List<Long>>() {}.type
        gson.fromJson(entriesJson, listType) ?: emptyList()
    }

    val appTheme: Flow<ThemePreference> = context.dataStore.data.map { preferences ->
        val themeName = preferences[APP_THEME] ?: ThemePreference.SYSTEM.name
        try {
            ThemePreference.valueOf(themeName)
        } catch (e: Exception) {
            ThemePreference.SYSTEM
        }
    }

    val unlockedAchievements: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        val json = preferences[UNLOCKED_ACHIEVEMENTS] ?: "[]"
        val listType = object : TypeToken<Set<String>>() {}.type
        gson.fromJson(json, listType) ?: emptySet()
    }
    
    val dailyLimit: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DAILY_LIMIT] ?: 0
    }

    val appLaunchDates: Flow<List<Long>> = context.dataStore.data.map { preferences ->
        val json = preferences[APP_LAUNCH_DATES] ?: "[]"
        val listType = object : TypeToken<List<Long>>() {}.type
        gson.fromJson(json, listType) ?: emptyList()
    }

    val fontPreset: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[FONT_PRESET] ?: "WIDE"
    }

    val amoledTheme: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AMOLED_THEME] ?: false
    }

    suspend fun saveAmoledTheme(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AMOLED_THEME] = enabled
        }
    }

    suspend fun saveFontPreset(preset: String) {
        context.dataStore.edit { preferences ->
            preferences[FONT_PRESET] = preset
        }
    }

    suspend fun saveUserProfile() {
        context.dataStore.edit { preferences ->
            preferences[IS_REGISTERED] = true
        }
    }

    suspend fun addSmokingEntry(timestamp: Long) {
        context.dataStore.edit { preferences ->
            val entriesJson = preferences[SMOKING_ENTRIES] ?: "[]"
            val listType = object : TypeToken<List<Long>>() {}.type
            val currentEntries: MutableList<Long> = gson.fromJson(entriesJson, listType) ?: mutableListOf()
            currentEntries.add(timestamp)
            currentEntries.sort()
            preferences[SMOKING_ENTRIES] = gson.toJson(currentEntries)
        }
    }

    suspend fun removeSmokingEntry(timestamp: Long) {
        context.dataStore.edit { preferences ->
            val entriesJson = preferences[SMOKING_ENTRIES] ?: "[]"
            val listType = object : TypeToken<List<Long>>() {}.type
            val currentEntries: MutableList<Long> = gson.fromJson(entriesJson, listType) ?: mutableListOf()
            currentEntries.remove(timestamp)
            preferences[SMOKING_ENTRIES] = gson.toJson(currentEntries)
        }
    }

    suspend fun recordAppLaunch(timestamp: Long) {
        context.dataStore.edit { preferences ->
            val json = preferences[APP_LAUNCH_DATES] ?: "[]"
            val listType = object : TypeToken<List<Long>>() {}.type
            val current: MutableList<Long> = gson.fromJson(json, listType) ?: mutableListOf()
            current.add(timestamp)
            current.sort()
            preferences[APP_LAUNCH_DATES] = gson.toJson(current)
        }
    }

    suspend fun saveThemePreference(theme: ThemePreference) {
        context.dataStore.edit { preferences ->
            preferences[APP_THEME] = theme.name
        }
    }

    suspend fun saveUnlockedAchievement(achievementId: String) {
        context.dataStore.edit { preferences ->
            val json = preferences[UNLOCKED_ACHIEVEMENTS] ?: "[]"
            val listType = object : TypeToken<MutableSet<String>>() {}.type
            val current: MutableSet<String> = gson.fromJson(json, listType) ?: mutableSetOf()
            current.add(achievementId)
            preferences[UNLOCKED_ACHIEVEMENTS] = gson.toJson(current)
        }
    }

    suspend fun setUnlockedAchievements(achievements: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[UNLOCKED_ACHIEVEMENTS] = gson.toJson(achievements)
        }
    }
    
    suspend fun setDailyLimit(limit: Int) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_LIMIT] = limit
        }
    }

    suspend fun restoreFromBackup(
        isReg: Boolean,
        entries: List<Long>,
        theme: String,
        achievements: Set<String>,
        limit: Int
    ) {
        context.dataStore.edit { preferences ->
            preferences[IS_REGISTERED] = isReg
            preferences[SMOKING_ENTRIES] = gson.toJson(entries)
            preferences[APP_THEME] = theme
            preferences[UNLOCKED_ACHIEVEMENTS] = gson.toJson(achievements)
            preferences[DAILY_LIMIT] = limit
        }
    }
}
