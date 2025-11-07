package com.movieroulette.app.data.repository

import com.movieroulette.app.data.remote.SupabaseConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {
    
    private val supabase = SupabaseConfig.client
    
    suspend fun signUp(email: String, password: String, username: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Registrar el usuario
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    data = buildJsonObject {
                        put("username", username)
                        put("display_name", username)
                    }
                }
                
                // Verificar si el usuario necesita confirmar email
                val currentUser = supabase.auth.currentUserOrNull()
                if (currentUser == null) {
                    // Si no hay sesión después de registro, intentar login
                    supabase.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun signOut(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                supabase.auth.signOut()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getCurrentUser() = withContext(Dispatchers.IO) {
        supabase.auth.currentUserOrNull()
    }
    
    suspend fun isUserLoggedIn(): Boolean {
        return getCurrentUser() != null
    }
    
    suspend fun resetPassword(email: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                supabase.auth.resetPasswordForEmail(email)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
