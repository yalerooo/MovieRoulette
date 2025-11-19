package com.movieroulette.app.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.movieroulette.app.ui.components.*
import com.movieroulette.app.ui.navigation.Screen
import com.movieroulette.app.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiState.collectLatest { state ->
            if (state is AuthViewModel.AuthUiState.Success) {
                showConfirmationDialog = true
            }
        }
    }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = {
                showConfirmationDialog = false
                viewModel.resetState()
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Register.route) { inclusive = true }
                }
            },
            title = { Text(stringResource(com.movieroulette.app.R.string.account_created)) },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(com.movieroulette.app.R.string.account_created_message))
                    Text(stringResource(com.movieroulette.app.R.string.verification_sent))
                    Text(
                        stringResource(com.movieroulette.app.R.string.check_inbox),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmationDialog = false
                        viewModel.resetState()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    }
                ) {
                    Text(stringResource(com.movieroulette.app.R.string.go_to_login))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.movieroulette.app.R.string.create_account)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(com.movieroulette.app.R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(com.movieroulette.app.R.string.join_adventure),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Username
            AppTextField(
                value = username,
                onValueChange = {
                    username = it
                    usernameError = null
                },
                label = stringResource(com.movieroulette.app.R.string.username),
                placeholder = stringResource(com.movieroulette.app.R.string.username_placeholder),
                error = usernameError,
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Email
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
            
            // Password
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Confirm Password
            AppTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    confirmPasswordError = null
                },
                label = stringResource(com.movieroulette.app.R.string.confirm_password),
                placeholder = stringResource(com.movieroulette.app.R.string.password_placeholder),
                isPassword = true,
                error = confirmPasswordError,
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Show error if any
            if (uiState is AuthViewModel.AuthUiState.Error) {
                Text(
                    text = (uiState as AuthViewModel.AuthUiState.Error).message ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Register button
            PrimaryButton(
                text = stringResource(com.movieroulette.app.R.string.create_account),
                onClick = {
                    // Validate
                    var hasError = false
                    
                    if (username.isBlank() || username.length < 3) {
                        usernameError = context.getString(com.movieroulette.app.R.string.username_error)
                        hasError = true
                    }
                    
                    if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        emailError = context.getString(com.movieroulette.app.R.string.email_invalid)
                        hasError = true
                    }
                    
                    if (password.isBlank() || password.length < 6) {
                        passwordError = context.getString(com.movieroulette.app.R.string.password_short)
                        hasError = true
                    }
                    
                    if (password != confirmPassword) {
                        confirmPasswordError = context.getString(com.movieroulette.app.R.string.passwords_dont_match)
                        hasError = true
                    }
                    
                    if (!hasError) {
                        viewModel.signUp(email, password, username)
                    }
                },
                isLoading = uiState is AuthViewModel.AuthUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
