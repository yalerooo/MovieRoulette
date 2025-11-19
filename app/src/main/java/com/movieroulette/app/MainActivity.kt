package com.movieroulette.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.movieroulette.app.data.AppLanguage
import com.movieroulette.app.data.AppTheme
import com.movieroulette.app.data.ThemeManager
import com.movieroulette.app.data.LanguageManager
import com.movieroulette.app.data.remote.SupabaseConfig
import com.movieroulette.app.service.NotificationService
import com.movieroulette.app.ui.navigation.AppNavigation
import com.movieroulette.app.utils.PermissionUtils
import io.github.jan.supabase.auth.auth
import com.movieroulette.app.ui.theme.MovieRouletteTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var themeManager: ThemeManager
    private lateinit var languageManager: LanguageManager
    private var currentLocale: String? = null
    private var hasOAuthCallback = false
    private var permissionsRequested = false
    
    // Permission launcher for multiple permissions
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d("MainActivity", "Permissions result: $permissions")
        permissionsRequested = true
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeManager = ThemeManager(applicationContext)
        languageManager = LanguageManager(applicationContext)
        
        // Verificar si es un deep link de OAuth
        hasOAuthCallback = intent?.data?.let { uri ->
            uri.scheme == "com.movieroulette.app" && uri.host == "login" && uri.fragment != null
        } ?: false
        
        if (hasOAuthCallback) {
            Log.d("MainActivity", "OAuth callback detected: ${intent?.data}")
        }
        
        // Request permissions on first launch
        requestInitialPermissions()
        
        setContent {
            val currentTheme by themeManager.currentTheme.collectAsState(initial = AppTheme.BLUE)
            val currentLanguage by languageManager.currentLanguage.collectAsState(initial = AppLanguage.SPANISH)
            
            LaunchedEffect(currentLanguage) {
                val newLocale = currentLanguage.code
                if (currentLocale != null && currentLocale != newLocale) {
                    setLocale(newLocale)
                }
                currentLocale = newLocale
            }
            
            // Iniciar servicio de notificaciones cuando el usuario se autentique
            var hasStartedService by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                // Esperar un poco para que la sesión se restaure
                kotlinx.coroutines.delay(500)
                
                val user = SupabaseConfig.client.auth.currentUserOrNull()
                if (user != null && !hasStartedService) {
                    Log.d("MainActivity", "Iniciando NotificationService para usuario: ${user.id}")
                    NotificationService.startListening(applicationContext, user.id)
                    hasStartedService = true
                } else if (user == null) {
                    Log.d("MainActivity", "No hay usuario autenticado, esperando login...")
                }
            }
            
            // También intentar iniciar cuando cambie la navegación
            LaunchedEffect(hasOAuthCallback) {
                if (hasOAuthCallback && !hasStartedService) {
                    kotlinx.coroutines.delay(1000)
                    val user = SupabaseConfig.client.auth.currentUserOrNull()
                    if (user != null) {
                        Log.d("MainActivity", "Usuario autenticado después de OAuth: ${user.id}")
                        NotificationService.startListening(applicationContext, user.id)
                        hasStartedService = true
                    }
                }
            }
            
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
    
    /**
     * Request initial permissions when app starts
     */
    private fun requestInitialPermissions() {
        if (permissionsRequested) return
        
        val permissionsToRequest = mutableListOf<String>()
        
        // Check media permissions
        if (!PermissionUtils.hasMediaPermissions(this)) {
            permissionsToRequest.addAll(PermissionUtils.getMediaPermissions())
        }
        
        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionUtils.hasNotificationPermission(this)) {
                permissionsToRequest.add(PermissionUtils.getNotificationPermission())
            }
        }
        
        // Request all permissions at once
        if (permissionsToRequest.isNotEmpty()) {
            Log.d("MainActivity", "Requesting permissions: $permissionsToRequest")
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.d("MainActivity", "All permissions already granted")
            permissionsRequested = true
        }
    }
    
    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        
        recreate()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        // Verificar si es OAuth callback
        val isCallback = intent.data?.let { uri ->
            uri.scheme == "com.movieroulette.app" && uri.host == "login" && uri.fragment != null
        } ?: false
        
        if (isCallback) {
            hasOAuthCallback = true
            recreate()
        }
    }
}
