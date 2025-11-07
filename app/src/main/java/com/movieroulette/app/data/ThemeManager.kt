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

enum class AppTheme(val displayName: String) {
    DEFAULT("Predeterminado"),
    BLUE("Azul"),
    RED("Rojo"),
    PINK("Rosa"),
    ORANGE("Naranja"),
    GREEN("Verde"),
    PURPLE("Morado")
}

class ThemeManager(private val context: Context) {
    private val THEME_KEY = stringPreferencesKey("app_theme")

    val currentTheme: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        val themeName = preferences[THEME_KEY] ?: AppTheme.DEFAULT.name
        try {
            AppTheme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppTheme.DEFAULT
        }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }
}
