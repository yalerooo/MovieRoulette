package com.movieroulette.app

import android.app.Application
import com.movieroulette.app.utils.NotificationHelper
import com.movieroulette.app.data.remote.SupabaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MovieRouletteApp : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Crear canal de notificaciones
        NotificationHelper.createNotificationChannel(this)
    }
    
    companion object {
        lateinit var instance: MovieRouletteApp
            private set
    }
}
