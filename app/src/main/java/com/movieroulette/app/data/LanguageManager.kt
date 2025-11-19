package com.movieroulette.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppLanguage(val code: String) {
    SPANISH("es"),
    ENGLISH("en");
    
    fun getDisplayName(context: Context): String {
        return when (this) {
            SPANISH -> "Español"
            ENGLISH -> "English"
        }
    }
}

class LanguageManager(private val context: Context) {
    private val LANGUAGE_KEY = stringPreferencesKey("app_language")

    val currentLanguage: Flow<AppLanguage> = context.dataStore.data.map { preferences ->
        val languageName = preferences[LANGUAGE_KEY] ?: AppLanguage.SPANISH.name
        try {
            AppLanguage.valueOf(languageName)
        } catch (e: IllegalArgumentException) {
            AppLanguage.SPANISH
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.name
        }
    }
}
