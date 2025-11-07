package com.movieroulette.app.data.repository

import com.movieroulette.app.data.remote.SupabaseConfig
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class StorageRepository {
    
    private val supabase = SupabaseConfig.client
    private val storage = supabase.storage
    
    suspend fun uploadProfilePicture(imageData: ByteArray): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "profile_${UUID.randomUUID()}.jpg"
                
                storage.from("profile-pictures").upload(
                    path = fileName,
                    data = imageData
                ) {
                    upsert = false
                }
                
                val publicUrl = storage.from("profile-pictures").publicUrl(fileName)
                Result.success(publicUrl)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun uploadGroupPicture(imageData: ByteArray): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "group_${UUID.randomUUID()}.jpg"
                
                storage.from("group-pictures").upload(
                    path = fileName,
                    data = imageData
                ) {
                    upsert = false
                }
                
                val publicUrl = storage.from("group-pictures").publicUrl(fileName)
                Result.success(publicUrl)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun deleteImage(bucketName: String, fileName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                storage.from(bucketName).delete(fileName)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
