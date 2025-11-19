package com.movieroulette.app.ui.screens.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.movieroulette.app.R
import com.movieroulette.app.ui.navigation.Screen
import com.movieroulette.app.utils.ImageUtils
import com.movieroulette.app.viewmodel.AuthViewModel
import com.movieroulette.app.viewmodel.GroupViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetUsernameScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel(),
    groupViewModel: GroupViewModel = viewModel()
) {
    var username by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var isCheckingUsername by remember { mutableStateOf(false) }
    var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var checkUsernameJob by remember { mutableStateOf<Job?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val uiState by viewModel.uiState.collectAsState()
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            scope.launch {
                isUploadingImage = true
                uploadError = null
                
                // Compress and upload image
                val compressResult = ImageUtils.compressImage(context, uri, maxSizeKB = 500)
                if (compressResult.isFailure) {
                    uploadError = context.getString(R.string.error_compress_image)
                    isUploadingImage = false
                    return@launch
                }
                
                val uploadResult = groupViewModel.uploadAndUpdateUserAvatar(compressResult.getOrNull()!!)
                if (uploadResult.isSuccess) {
                    selectedImageUrl = uploadResult.getOrNull()
                    uploadError = null
                } else {
                    uploadError = uploadResult.exceptionOrNull()?.message ?: context.getString(R.string.error_upload_image)
                }
                
                isUploadingImage = false
            }
        }
    }
    
    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthViewModel.AuthUiState.Success -> {
                // Navegar a Groups después de configurar el username
                navController.navigate(Screen.Groups.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            else -> {}
        }
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.setup_username)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Avatar selection
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isUploadingImage) {
                    CircularProgressIndicator()
                } else if (selectedImageUrl != null || selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUrl ?: selectedImageUri,
                        contentDescription = stringResource(R.string.avatar),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            OutlinedButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                enabled = !isUploadingImage,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (selectedImageUrl != null || selectedImageUri != null) 
                        stringResource(R.string.change_photo)
                    else 
                        stringResource(R.string.add_photo)
                )
            }
            
            if (uploadError != null) {
                Text(
                    text = uploadError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
            
            // Campo de username con verificación en tiempo real
            OutlinedTextField(
                value = username,
                onValueChange = { newValue ->
                    username = newValue
                    usernameError = null
                    isUsernameAvailable = null
                    
                    // Cancelar verificación anterior
                    checkUsernameJob?.cancel()
                    
                    // Verificar disponibilidad después de 500ms de inactividad
                    if (newValue.length >= 3) {
                        checkUsernameJob = scope.launch {
                            delay(500)
                            isCheckingUsername = true
                            val result = viewModel.authRepository.checkUsernameAvailability(newValue)
                            isCheckingUsername = false
                            isUsernameAvailable = result.getOrNull()
                        }
                    }
                },
                label = { Text(stringResource(R.string.username)) },
                placeholder = { Text(stringResource(R.string.username_placeholder)) },
                isError = usernameError != null || isUsernameAvailable == false,
                supportingText = {
                    when {
                        usernameError != null -> Text(usernameError!!)
                        isUsernameAvailable == false -> Text(
                            stringResource(R.string.username_taken),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                trailingIcon = {
                    when {
                        isCheckingUsername -> CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        isUsernameAvailable == true -> Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        isUsernameAvailable == false -> Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Error message con colores del tema
            if (uiState is AuthViewModel.AuthUiState.Error) {
                val errorState = uiState as AuthViewModel.AuthUiState.Error
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorState.message ?: stringResource(errorState.messageResId),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Botón de confirmar
            Button(
                onClick = {
                    // Validar username
                    if (username.isBlank() || username.length < 3) {
                        usernameError = context.getString(R.string.username_error)
                        return@Button
                    }
                    
                    if (isUsernameAvailable == false) {
                        return@Button
                    }
                    
                    viewModel.updateUsername(username)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = uiState !is AuthViewModel.AuthUiState.Loading && 
                         username.isNotBlank() && 
                         isUsernameAvailable == true && 
                         !isCheckingUsername
            ) {
                if (uiState is AuthViewModel.AuthUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.continue_button))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Si el usuario cierra la app sin completar, cerrar sesión
    DisposableEffect(Unit) {
        onDispose {
            if (uiState !is AuthViewModel.AuthUiState.Success) {
                scope.launch {
                    viewModel.signOutAndDeleteIncompleteUser()
                }
            }
        }
    }
}
