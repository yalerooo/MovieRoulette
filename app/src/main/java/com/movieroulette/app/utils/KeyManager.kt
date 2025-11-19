package com.movieroulette.app.utils

import android.content.Context
import com.movieroulette.app.data.remote.SupabaseConfig
import com.movieroulette.app.data.model.UserPublicKey
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.KeyPair

/**
 * Utilidades para gestionar claves de cifrado E2E
 */
object KeyManager {
    
    /**
     * Asegura que el usuario actual tenga un par de claves generado y subido
     */
    suspend fun ensureUserHasKeys(context: Context) {
        try {
            val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return
            
            android.util.Log.d("KeyManager", "Ensuring keys exist for user: $currentUserId")
            
            // Verificar si ya tiene clave pública en servidor
            val hasPublicKey = try {
                SupabaseConfig.client.from("user_public_keys")
                    .select {
                        filter {
                            eq("user_id", currentUserId)
                        }
                    }
                    .decodeSingleOrNull<UserPublicKey>() != null
            } catch (e: Exception) {
                false
            }
            
            if (hasPublicKey) {
                android.util.Log.d("KeyManager", "User already has public key in database")
                return
            }
            
            android.util.Log.d("KeyManager", "Generating new keypair for user")
            
            // Obtener o generar par de claves
            val sharedPrefs = context.getSharedPreferences("e2e_keys", Context.MODE_PRIVATE)
            val privateKeyString = sharedPrefs.getString("private_key", null)
            
            val keyPair = if (privateKeyString != null) {
                // Recuperar existente
                try {
                    val privateKey = E2EEncryption.stringToPrivateKey(privateKeyString)
                    val publicKeyString = sharedPrefs.getString("public_key", null)!!
                    val publicKey = E2EEncryption.stringToPublicKey(publicKeyString)
                    KeyPair(publicKey, privateKey)
                } catch (e: Exception) {
                    // Si hay error, generar nuevo
                    generateAndSaveKeyPair(context, sharedPrefs)
                }
            } else {
                // Generar nuevo
                generateAndSaveKeyPair(context, sharedPrefs)
            }
            
            // Subir clave pública a servidor
            uploadPublicKey(currentUserId, keyPair.public)
            
            android.util.Log.d("KeyManager", "Keys ensured successfully")
        } catch (e: Exception) {
            android.util.Log.e("KeyManager", "Error ensuring user has keys", e)
        }
    }
    
    private fun generateAndSaveKeyPair(
        context: Context,
        sharedPrefs: android.content.SharedPreferences
    ): KeyPair {
        val keyPair = E2EEncryption.generateRSAKeyPair()
        
        sharedPrefs.edit()
            .putString("private_key", E2EEncryption.privateKeyToString(keyPair.private))
            .putString("public_key", E2EEncryption.publicKeyToString(keyPair.public))
            .apply()
        
        return keyPair
    }
    
    private suspend fun uploadPublicKey(userId: String, publicKey: java.security.PublicKey) {
        try {
            val publicKeyString = E2EEncryption.publicKeyToString(publicKey)
            
            // Insertar (con ON CONFLICT DO UPDATE en la consulta)
            SupabaseConfig.client.from("user_public_keys")
                .insert(buildJsonObject {
                    put("user_id", userId)
                    put("public_key", publicKeyString)
                })
            
            android.util.Log.d("KeyManager", "Public key uploaded successfully")
        } catch (e: Exception) {
            // Si falla el insert, probablemente ya existe, intentar actualizar
            try {
                val publicKeyString = E2EEncryption.publicKeyToString(publicKey)
                SupabaseConfig.client.from("user_public_keys")
                    .update({
                        set("public_key", publicKeyString)
                    }) {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                android.util.Log.d("KeyManager", "Public key updated successfully")
            } catch (e2: Exception) {
                android.util.Log.e("KeyManager", "Failed to upload/update public key", e2)
            }
        }
    }
}
