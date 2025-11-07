package com.movieroulette.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.movieroulette.app.data.AppTheme
import com.movieroulette.app.data.ThemeManager
import com.movieroulette.app.ui.navigation.AppNavigation
import com.movieroulette.app.ui.theme.MovieRouletteTheme

class MainActivity : ComponentActivity() {
    private lateinit var themeManager: ThemeManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeManager = ThemeManager(applicationContext)
        
        setContent {
            val currentTheme by themeManager.currentTheme.collectAsState(initial = AppTheme.DEFAULT)
            
            MovieRouletteTheme(appTheme = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
