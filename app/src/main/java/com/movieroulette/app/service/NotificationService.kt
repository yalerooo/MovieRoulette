package com.movieroulette.app.service

import android.content.Context
import android.util.Log
import com.movieroulette.app.data.remote.SupabaseConfig
import com.movieroulette.app.utils.NotificationHelper
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
data class NotificationData(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("movie_id")
    val movieId: String? = null,
    @SerialName("group_id")
    val groupId: String? = null,
    val type: String,
    val title: String,
    val body: String,
    val data: JsonObject? = null,
    val read: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null
)

object NotificationService {
    
    private const val TAG = "NotificationService"
    private val supabase = SupabaseConfig.client
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isListening = false
    
    fun startListening(context: Context, userId: String) {
        if (isListening) {
            Log.d(TAG, "Ya está escuchando notificaciones")
            return
        }
        
        Log.d(TAG, "Iniciando escucha de notificaciones para usuario: $userId")
        
        scope.launch {
            try {
                val channelId = "notifications-channel-$userId"
                Log.d(TAG, "Creando canal: $channelId")
                
                val channel = supabase.realtime.channel(channelId)
                
                // Configurar listener para todos los eventos
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "notifications"
                    // No usar filter aquí, filtraremos manualmente
                }
                
                Log.d(TAG, "PostgresChangeFlow configurado")
                
                changeFlow.onEach { action ->
                    Log.d(TAG, "=== EVENTO REALTIME RECIBIDO ===")
                    Log.d(TAG, "Tipo de acción: ${action.javaClass.simpleName}")
                    
                    when (action) {
                        is PostgresAction.Insert -> {
                            Log.d(TAG, "INSERT recibido!")
                            try {
                                val record = action.record
                                Log.d(TAG, "Record completo: $record")
                                Log.d(TAG, "Tipo de record: ${record.javaClass.name}")
                                
                                if (record is JsonObject) {
                                    val recordUserId = record["user_id"]?.jsonPrimitive?.content
                                    Log.d(TAG, "UserId del record: $recordUserId")
                                    Log.d(TAG, "UserId esperado: $userId")
                                    
                                    if (recordUserId == userId) {
                                        val notification = NotificationData(
                                            id = record["id"]?.jsonPrimitive?.content ?: "",
                                            userId = recordUserId ?: "",
                                            movieId = record["movie_id"]?.jsonPrimitive?.content,
                                            groupId = record["group_id"]?.jsonPrimitive?.content,
                                            type = record["type"]?.jsonPrimitive?.content ?: "",
                                            title = record["title"]?.jsonPrimitive?.content ?: "",
                                            body = record["body"]?.jsonPrimitive?.content ?: "",
                                            data = record["data"]?.jsonObject,
                                            read = record["read"]?.jsonPrimitive?.content?.toBoolean() ?: false,
                                            createdAt = record["created_at"]?.jsonPrimitive?.content
                                        )
                                        
                                        Log.d(TAG, "✅ Notificación para este usuario, procesando...")
                                        handleNewNotification(context, notification)
                                    } else {
                                        Log.d(TAG, "❌ Notificación para otro usuario, ignorando")
                                    }
                                } else {
                                    Log.e(TAG, "Record no es JsonObject: ${record.javaClass.name}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error procesando INSERT: ${e.message}", e)
                            }
                        }
                        is PostgresAction.Update -> {
                            Log.d(TAG, "UPDATE recibido (ignorado)")
                        }
                        is PostgresAction.Delete -> {
                            Log.d(TAG, "DELETE recibido (ignorado)")
                        }
                        is PostgresAction.Select -> {
                            Log.d(TAG, "SELECT recibido (ignorado)")
                        }
                    }
                }.launchIn(scope)
                
                Log.d(TAG, "Intentando suscribirse al canal...")
                channel.subscribe()
                Log.d(TAG, "✅ Suscripción exitosa al canal Realtime")
                isListening = true
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error iniciando NotificationService: ${e.message}", e)
            }
        }
    }
    
    private fun handleNewNotification(context: Context, notification: NotificationData) {
        Log.d(TAG, "handleNewNotification - type: ${notification.type}, read: ${notification.read}")
        
        if (notification.type == "rating_request" && !notification.read) {
            val movieId = notification.movieId ?: run {
                Log.w(TAG, "movieId es null, ignorando notificación")
                return
            }
            val groupId = notification.groupId ?: run {
                Log.w(TAG, "groupId es null, ignorando notificación")
                return
            }
            
            // Extraer el título de la película del data JSON
            val movieTitle = notification.data?.get("movieTitle")?.jsonPrimitive?.content 
                ?: notification.title
            
            // Extraer el nombre del grupo del body
            val groupName = notification.body.substringAfter("El grupo \"")
                .substringBefore("\" ha terminado")
            
            Log.d(TAG, "Mostrando notificación - movieTitle: $movieTitle, groupName: $groupName")
            
            NotificationHelper.showRatingNotification(
                context = context,
                movieId = movieId,
                groupId = groupId,
                movieTitle = movieTitle,
                groupName = groupName
            )
            
            // Marcar como leída
            scope.launch {
                try {
                    Log.d(TAG, "Marcando notificación ${notification.id} como leída")
                    supabase.from("notifications")
                        .update(
                            buildJsonObject {
                                put("read", true)
                            }
                        ) {
                            filter {
                                eq("id", notification.id)
                            }
                        }
                    Log.d(TAG, "Notificación marcada como leída exitosamente")
                } catch (e: Exception) {
                    Log.e(TAG, "Error marcando notificación como leída", e)
                    e.printStackTrace()
                }
            }
        } else {
            Log.d(TAG, "Notificación ignorada - type: ${notification.type}, read: ${notification.read}")
        }
    }
    
    fun stopListening() {
        isListening = false
        // El canal se cerrará automáticamente cuando se destruya el scope
    }
}
