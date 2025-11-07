package com.movieroulette.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.movieroulette.app.data.AppTheme
import com.movieroulette.app.data.ThemeManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val currentTheme by themeManager.currentTheme.collectAsState(initial = AppTheme.DEFAULT)
    val scope = rememberCoroutineScope()
    
    val themes = listOf(
        AppTheme.DEFAULT,
        AppTheme.BLUE,
        AppTheme.RED,
        AppTheme.PINK,
        AppTheme.ORANGE,
        AppTheme.GREEN,
        AppTheme.PURPLE
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                Text(
                    text = "Apariencia",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            item {
                Text(
                    text = "Tema de color",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            items(themes) { theme ->
                ThemeItem(
                    theme = theme,
                    isSelected = theme == currentTheme,
                    onClick = {
                        scope.launch {
                            themeManager.setTheme(theme)
                        }
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ThemeItem(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val themeColor = when (theme) {
        AppTheme.DEFAULT -> MaterialTheme.colorScheme.primary
        AppTheme.BLUE -> androidx.compose.ui.graphics.Color(0xFFADD8E6)
        AppTheme.RED -> androidx.compose.ui.graphics.Color(0xFFFFB3BA)
        AppTheme.PINK -> androidx.compose.ui.graphics.Color(0xFFF9CCCC)
        AppTheme.ORANGE -> androidx.compose.ui.graphics.Color(0xFFFFDAB9)
        AppTheme.GREEN -> androidx.compose.ui.graphics.Color(0xFFB4E7CE)
        AppTheme.PURPLE -> androidx.compose.ui.graphics.Color(0xFFE0BBE4)
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(themeColor)
                )
                Text(
                    text = theme.displayName,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
