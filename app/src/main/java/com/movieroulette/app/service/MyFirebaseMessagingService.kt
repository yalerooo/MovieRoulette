package com.movieroulette.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.movieroulette.app.MainActivity
import com.movieroulette.app.R
import com.movieroulette.app.data.remote.SupabaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "movie_ratings_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * Llamado cuando se recibe un nuevo token de FCM.
     * Registra el token en Knock y Supabase.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo FCM token recibido: $token")
        
        scope.launch {
            try {
                // Usar FCMTokenManager para guardar en Supabase y registrar en Knock
                com.movieroulette.app.utils.FCMTokenManager.refreshAndSaveToken()
                
                Log.d(TAG, "Token registrado exitosamente en Supabase y Knock")
            } catch (e: Exception) {
                Log.e(TAG, "Error registrando token: ${e.message}", e)
            }
        }
    }

    /**
     * Llamado cuando se recibe una notificación push.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        Log.d(TAG, "Mensaje recibido de: ${message.from}")
        
        // Extraer datos de la notificación
        val movieId = message.data["movie_id"]
        val groupId = message.data["group_id"]
        val navigateTo = message.data["navigate_to"]
        
        val title = message.notification?.title ?: "Nueva notificación"
        val body = message.notification?.body ?: ""
        
        Log.d(TAG, "movieId: $movieId, groupId: $groupId, navigateTo: $navigateTo")
        
        // Mostrar notificación local
        if (movieId != null && groupId != null) {
            showNotification(title, body, movieId, groupId, navigateTo ?: "rate_movie")
        } else {
            showSimpleNotification(title, body)
        }
    }

    /**
     * Guarda el token en la tabla device_tokens de Supabase.
     */
    private suspend fun saveTokenToSupabase(token: String) {
        try {
            val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return
            
            SupabaseConfig.client.from("device_tokens")
                .upsert(buildJsonObject {
                    put("user_id", userId)
                    put("token", token)
                    put("device_type", "android")
                })
            
            Log.d(TAG, "Token guardado en Supabase")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando token en Supabase: ${e.message}", e)
        }
    }

    /**
     * Muestra una notificación con navegación a la pantalla de puntuación.
     */
    private fun showNotification(
        title: String,
        body: String,
        movieId: String,
        groupId: String,
        navigateTo: String
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", navigateTo)
            putExtra("movie_id", movieId)
            putExtra("group_id", groupId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            movieId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_filmlette)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(movieId.hashCode(), notification)
        
        Log.d(TAG, "Notificación mostrada: $title")
    }

    /**
     * Muestra una notificación simple sin navegación.
     */
    private fun showSimpleNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_filmlette)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Crea el canal de notificaciones (Android O+).
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Puntuaciones de películas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para puntuar películas vistas en grupo"
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
