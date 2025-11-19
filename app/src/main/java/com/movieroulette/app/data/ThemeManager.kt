package com.movieroulette.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class AppTheme(val displayNameRes: Int) {
    BLUE(com.movieroulette.app.R.string.theme_blue),
    RED(com.movieroulette.app.R.string.theme_red),
    PINK(com.movieroulette.app.R.string.theme_pink),
    ORANGE(com.movieroulette.app.R.string.theme_orange),
    GREEN(com.movieroulette.app.R.string.theme_green),
    PURPLE(com.movieroulette.app.R.string.theme_purple)
}

class ThemeManager(private val context: Context) {
    private val THEME_KEY = stringPreferencesKey("app_theme")

    val currentTheme: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        val themeName = preferences[THEME_KEY] ?: AppTheme.BLUE.name
        try {
            AppTheme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppTheme.BLUE
        }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }
}
