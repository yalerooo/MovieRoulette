package com.movieroulette.app.utils

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.movieroulette.app.data.remote.SupabaseConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object FCMTokenManager {
    private const val TAG = "FCMTokenManager"
    
    /**
     * Obtiene el token FCM actual, lo guarda en Supabase y lo registra en Knock mediante Edge Function.
     * Debe llamarse después de que el usuario inicie sesión.
     */
    suspend fun refreshAndSaveToken() {
        try {
            val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id
            if (userId == null) {
                Log.w(TAG, "Usuario no autenticado, no se puede guardar token")
                return
            }
            
            // Obtener token actual de Firebase
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "Token FCM obtenido: $token")
            
            // Guardar en Supabase usando upsert
            try {
                SupabaseConfig.client.from("device_tokens")
                    .upsert(
                        value = buildJsonObject {
                            put("user_id", userId)
                            put("token", token)
                            put("device_type", "android")
                        }
                    )
                Log.d(TAG, "Token guardado exitosamente en Supabase para usuario: $userId")
            } catch (e: Exception) {
                // Si el token ya existe, ignorar el error y continuar
                Log.w(TAG, "Token ya existe en Supabase, continuando...")
            }
            
            // Registrar el token en Knock usando Edge Function
            // NOTA: Esto puede fallar si el canal FCM no está correctamente configurado en Knock
            // pero no es crítico porque el trigger de DB también registrará el token cuando se envíe una notificación
            try {
                SupabaseConfig.client.functions.invoke(
                    "register-knock-user",
                    buildJsonObject {
                        put("user_id", userId)
                        put("fcm_token", token)
                    }
                )
                Log.d(TAG, "Token registrado en Knock para usuario: $userId")
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo registrar token en Knock (se registrará automáticamente al enviar notificación): ${e.message}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener/guardar token FCM: ${e.message}", e)
        }
    }
}
