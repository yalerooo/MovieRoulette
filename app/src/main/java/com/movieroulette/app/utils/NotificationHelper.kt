package com.movieroulette.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.movieroulette.app.MainActivity
import com.movieroulette.app.R

object NotificationHelper {
    
    private const val CHANNEL_ID = "movie_ratings_channel"
    private const val CHANNEL_NAME = "Puntuaciones de Películas"
    private const val CHANNEL_DESCRIPTION = "Notificaciones para puntuar películas vistas"
    
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showRatingNotification(
        context: Context,
        movieId: String,
        groupId: String,
        movieTitle: String,
        groupName: String
    ) {
        createNotificationChannel(context)
        
        // Intent para abrir la app en la pantalla de puntuación
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "rate_movie")
            putExtra("movie_id", movieId)
            putExtra("group_id", groupId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            movieId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_filmlette)
            .setContentTitle("¡Ya puedes puntuar '$movieTitle'!")
            .setContentText("Toca para dar tu opinión cuando quieras")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("El grupo '$groupName' ha terminado de ver '$movieTitle'. Puedes puntuar cuando quieras.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(movieId.hashCode(), notification)
        } catch (e: SecurityException) {
            // Permisos de notificación no concedidos
            e.printStackTrace()
        }
    }
    
    fun cancelRatingNotification(context: Context, movieId: String) {
        NotificationManagerCompat.from(context).cancel(movieId.hashCode())
    }
}
