package com.movieroulette.app.ui.screens.auth

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.movieroulette.app.data.local.AuthPreferences
import com.movieroulette.app.ui.components.*
import com.movieroulette.app.ui.navigation.Screen
import com.movieroulette.app.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val authPreferences = remember { AuthPreferences(context) }
    val scope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    
    val uiState by viewModel.uiState.collectAsState()
    
    // Load saved credentials
    LaunchedEffect(Unit) {
        authPreferences.rememberMe.collect { remember ->
            if (remember) {
                authPreferences.savedEmail.collect { savedEmail ->
                    email = savedEmail
                }
                authPreferences.savedPassword.collect { savedPassword ->
                    password = savedPassword
                }
                rememberMe = true
            }
        }
    }
    
    LaunchedEffect(uiState) {
        if (uiState is AuthViewModel.AuthUiState.Success) {
            // Save credentials if remember me is checked
            scope.launch {
                authPreferences.saveCredentials(email, password, rememberMe)
                
                // Guardar token FCM en Supabase
                try {
                    com.movieroulette.app.utils.FCMTokenManager.refreshAndSaveToken()
                } catch (e: Exception) {
                    // Log pero no falla el login
                    android.util.Log.e("LoginScreen", "Error guardando FCM token: ${e.message}")
                }
            }
            navController.navigate(Screen.Groups.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Text(
                text = "Filmlette",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = stringResource(com.movieroulette.app.R.string.tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
            )
            
            // Form
            AppTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                },
                label = stringResource(com.movieroulette.app.R.string.email),
                placeholder = stringResource(com.movieroulette.app.R.string.email_placeholder),
                error = emailError,
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null)
                },
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            AppTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                },
                label = stringResource(com.movieroulette.app.R.string.password),
                placeholder = stringResource(com.movieroulette.app.R.string.password_placeholder),
                isPassword = true,
                error = passwordError,
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Remember me checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it }
                )
                Text(
                    text = stringResource(com.movieroulette.app.R.string.remember_me),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            
            // Show error if any
            if (uiState is AuthViewModel.AuthUiState.Error) {
                Text(
                    text = (uiState as AuthViewModel.AuthUiState.Error).message ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Login button
            PrimaryButton(
                text = stringResource(com.movieroulette.app.R.string.sign_in),
                onClick = {
                    // Validate
                    var hasError = false
                    if (email.isBlank()) {
                        emailError = context.getString(com.movieroulette.app.R.string.email_required)
                        hasError = true
                    }
                    if (password.isBlank()) {
                        passwordError = context.getString(com.movieroulette.app.R.string.password_required)
                        hasError = true
                    }
                    
                    if (!hasError) {
                        viewModel.signIn(email, password)
                    }
                },
                isLoading = uiState is AuthViewModel.AuthUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Divider con "O"
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f))
                Text(
                    text = "O",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider(modifier = Modifier.weight(1f))
            }
            
            // Botón de Google Sign-In
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val url = viewModel.signInWithGoogle()
                        url?.let {
                            val intent = CustomTabsIntent.Builder().build()
                            intent.launchUrl(context, Uri.parse(it))
                        }
                    }
                },
                enabled = uiState !is AuthViewModel.AuthUiState.Loading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Icon(
                    painter = painterResource(com.movieroulette.app.R.drawable.google_logo),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Continuar con Google",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Register button
            SecondaryButton(
                text = stringResource(com.movieroulette.app.R.string.create_account),
                onClick = {
                    navController.navigate(Screen.Register.route)
                },
                enabled = uiState !is AuthViewModel.AuthUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
