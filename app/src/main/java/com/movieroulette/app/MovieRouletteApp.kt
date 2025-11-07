package com.movieroulette.app

import android.app.Application

class MovieRouletteApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    companion object {
        lateinit var instance: MovieRouletteApp
            private set
    }
}
