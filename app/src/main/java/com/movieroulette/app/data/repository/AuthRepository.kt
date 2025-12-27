package com.movieroulette.app.data.repository

import com.movieroulette.app.data.remote.SupabaseConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.seconds

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
                
                // Cerrar la sesión automática si existe
                supabase.auth.signOut()
                
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
    
    suspend fun signOutAndDeleteIncompleteUser(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                
                if (userId != null) {
                    android.util.Log.d("AuthRepository", "Deleting incomplete user: $userId")
                    
                    // Eliminar perfil de la base de datos si existe
                    try {
                        supabase.from("profiles").delete {
                            filter {
                                eq("id", userId)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AuthRepository", "Error deleting profile", e)
                    }
                }
                
                // Cerrar sesión
                supabase.auth.signOut()
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Error in signOutAndDeleteIncompleteUser", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun getCurrentUser() = withContext(Dispatchers.IO) {
        val user = supabase.auth.currentUserOrNull()
        android.util.Log.d("AuthRepository", "getCurrentUser: ${user?.email ?: "null"}")
        user
    }
    
    suspend fun isUserLoggedIn(): Boolean {
        val session = supabase.auth.currentSessionOrNull()
        val user = getCurrentUser()
        val isLogged = user != null
        android.util.Log.d("AuthRepository", "isUserLoggedIn: $isLogged (session=${session != null}, user=${user?.email})")
        return isLogged
    }
    
    suspend fun checkUsernameAvailability(username: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("AuthRepository", "Checking username availability: $username")
            
            // Buscar si el username ya existe en la tabla profiles (case-insensitive)
            val response = supabase.from("profiles")
                .select {
                    filter {
                        ilike("username", username)
                    }
                }
            
            val data = response.data
            val isAvailable = data == "[]" || data.isEmpty()
            
            android.util.Log.d("AuthRepository", "Username '$username' available: $isAvailable")
            Result.success(isAvailable)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error checking username availability", e)
            Result.failure(e)
        }
    }
    
    suspend fun needsUsernameSetup(): Boolean = withContext(Dispatchers.IO) {
        try {
            val user = supabase.auth.currentUserOrNull() ?: return@withContext false
            val metadata = user.userMetadata
            val username = metadata?.get("username")?.toString()
            val displayName = metadata?.get("display_name")?.toString()
            
            android.util.Log.d("AuthRepository", "Checking username setup - username: $username, displayName: $displayName")
            
            // Si no tiene username o display_name, necesita configurarlo
            username.isNullOrBlank() || displayName.isNullOrBlank()
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error checking username setup", e)
            false
        }
    }
    
    suspend fun updateUsername(username: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = supabase.auth.currentUserOrNull() ?: return@withContext Result.failure(Exception("User not logged in"))
            val userId = user.id
            
            android.util.Log.d("AuthRepository", "Updating username to: $username for user: $userId")
            
            // Verificar que el username no esté ya en uso por otro usuario (case-insensitive)
            val response = supabase.from("profiles")
                .select {
                    filter {
                        ilike("username", username)
                    }
                }
            
            val data = response.data
            if (data != "[]" && data.isNotEmpty()) {
                // El username ya existe, verificar si es del usuario actual
                val existingProfiles = response.decodeList<Map<String, String>>()
                val isCurrentUser = existingProfiles.any { it["id"] == userId }
                
                if (!isCurrentUser) {
                    android.util.Log.d("AuthRepository", "Username already taken by another user")
                    return@withContext Result.failure(Exception("USERNAME_TAKEN"))
                }
            }
            
            // Actualizar metadata del usuario
            supabase.auth.updateUser {
                data {
                    put("username", username)
                    put("display_name", username)
                }
            }
            
            android.util.Log.d("AuthRepository", "User metadata updated, now updating profile table...")
            
            // Actualizar o crear el perfil en la tabla profiles
            try {
                // Intentar actualizar primero
                supabase.from("profiles").update({
                    set("username", username)
                    set("display_name", username)
                }) {
                    filter {
                        eq("id", userId)
                    }
                }
                android.util.Log.d("AuthRepository", "Profile updated successfully")
            } catch (e: Exception) {
                // Si falla, intentar crear el perfil
                android.util.Log.d("AuthRepository", "Update failed, trying to insert profile: ${e.message}")
                supabase.from("profiles").insert(
                    buildJsonObject {
                        put("id", userId)
                        put("username", username)
                        put("display_name", username)
                        put("avatar_url", null as String?)
                    }
                )
                android.util.Log.d("AuthRepository", "Profile created successfully")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error updating username", e)
            Result.failure(e)
        }
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
    
    suspend fun signInWithGoogle(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Construir URL con deep link redirect
                val redirectUrl = "com.movieroulette.app://login"
                val url = "https://qaxnypaxddemznhpvppi.supabase.co/auth/v1/authorize?provider=google&redirect_to=$redirectUrl"
                Result.success(url)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun setSessionFromOAuth(accessToken: String, refreshToken: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("AuthRepository", "setSessionFromOAuth: Starting with accessToken=${accessToken.take(20)}...")
                
                // Verificar que los tokens sean válidos obteniendo el usuario
                android.util.Log.d("AuthRepository", "Calling retrieveUser to validate token...")
                val user = supabase.auth.retrieveUser(accessToken)
                android.util.Log.d("AuthRepository", "User retrieved: ${user.email}")
                
                // Crear y guardar la sesión manualmente
                android.util.Log.d("AuthRepository", "Creating UserSession object...")
                val session = UserSession(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    user = user,
                    expiresIn = 3600L, // 1 hora en segundos
                    tokenType = "bearer"
                )
                
                android.util.Log.d("AuthRepository", "Importing session...")
                supabase.auth.importSession(session)
                
                android.util.Log.d("AuthRepository", "Session imported successfully")
                
                // Verificar sesión y usuario actual
                val currentSession = supabase.auth.currentSessionOrNull()
                val currentUser = supabase.auth.currentUserOrNull()
                
                android.util.Log.d("AuthRepository", "Current session: ${currentSession?.user?.email ?: "null"}")
                android.util.Log.d("AuthRepository", "Current user: ${currentUser?.email ?: "null"}")
                
                if (currentSession == null || currentUser == null) {
                    android.util.Log.e("AuthRepository", "Session or user is NULL after importSession!")
                    return@withContext Result.failure(Exception("Failed to import session"))
                } else {
                    android.util.Log.d("AuthRepository", "Session imported OK: user=${currentUser.email}")
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Error in setSessionFromOAuth", e)
                Result.failure(e)
            }
        }
    }

    suspend fun ensureSessionValid() {
        withContext(Dispatchers.IO) {
            try {
                var session = supabase.auth.currentSessionOrNull()
                
                // Si no hay sesión, esperar un momento por si se está restaurando
                if (session == null) {
                    android.util.Log.d("AuthRepository", "Session is null in ensureSessionValid, waiting...")
                    kotlinx.coroutines.delay(1500)
                    session = supabase.auth.currentSessionOrNull()
                }
                
                if (session != null) {
                    // Forzar refresco de sesión para asegurar que el token es válido
                    // Esto ayuda cuando el usuario lleva días sin entrar
                    try {
                        supabase.auth.refreshCurrentSession()
                        android.util.Log.d("AuthRepository", "Session refreshed successfully")
                    } catch (e: Exception) {
                        android.util.Log.w("AuthRepository", "Failed to refresh session: ${e.message}")
                        // No relanzar la excepción para no bloquear la carga si el token aún es válido
                    }
                } else {
                    android.util.Log.w("AuthRepository", "Session is still null after waiting")
                    // Intentar cargar desde almacenamiento si es posible
                    try {
                        supabase.auth.loadFromStorage()
                        android.util.Log.d("AuthRepository", "Attempted loadFromStorage")
                    } catch (e: Exception) {
                        android.util.Log.e("AuthRepository", "Error loading from storage", e)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Error in ensureSessionValid", e)
            }
        }
    }
}
