package com.movieroulette.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class AuthPreferences(private val context: Context) {
    
    companion object {
        private val REMEMBER_ME_KEY = booleanPreferencesKey("remember_me")
        private val SAVED_EMAIL_KEY = stringPreferencesKey("saved_email")
        private val SAVED_PASSWORD_KEY = stringPreferencesKey("saved_password")
    }
    
    val rememberMe: Flow<Boolean> = context.authDataStore.data.map { preferences ->
        preferences[REMEMBER_ME_KEY] ?: false
    }
    
    val savedEmail: Flow<String> = context.authDataStore.data.map { preferences ->
        preferences[SAVED_EMAIL_KEY] ?: ""
    }
    
    val savedPassword: Flow<String> = context.authDataStore.data.map { preferences ->
        preferences[SAVED_PASSWORD_KEY] ?: ""
    }
    
    suspend fun saveCredentials(email: String, password: String, remember: Boolean) {
        context.authDataStore.edit { preferences ->
            preferences[REMEMBER_ME_KEY] = remember
            if (remember) {
                preferences[SAVED_EMAIL_KEY] = email
                preferences[SAVED_PASSWORD_KEY] = password
            } else {
                preferences.remove(SAVED_EMAIL_KEY)
                preferences.remove(SAVED_PASSWORD_KEY)
            }
        }
    }
    
    suspend fun clearCredentials() {
        context.authDataStore.edit { preferences ->
            preferences.remove(REMEMBER_ME_KEY)
            preferences.remove(SAVED_EMAIL_KEY)
            preferences.remove(SAVED_PASSWORD_KEY)
        }
    }
}
