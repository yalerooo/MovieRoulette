package com.movieroulette.app.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movieroulette.app.R
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.movieroulette.app.ui.components.PrimaryButton
import com.movieroulette.app.utils.ImageUtils
import com.movieroulette.app.utils.PermissionUtils
import com.movieroulette.app.viewmodel.GroupViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: GroupViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val profileState by viewModel.userProfileState.collectAsState()
    
    var username by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var isSavingUsername by remember { mutableStateOf(false) }
    
    // Image picker launcher (debe estar antes de permissionLauncher)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isUploading = true
                uploadError = null
                
                // Compress image
                val compressResult = ImageUtils.compressImage(context, uri, maxSizeKB = 500)
                if (compressResult.isFailure) {
                    uploadError = context.getString(R.string.error_compress_image)
                    isUploading = false
                    return@launch
                }
                
                // Upload and update
                val uploadResult = viewModel.uploadAndUpdateUserAvatar(compressResult.getOrNull()!!)
                if (uploadResult.isSuccess) {
                    selectedImageUrl = uploadResult.getOrNull()
                    uploadError = null
                } else {
                    uploadError = uploadResult.exceptionOrNull()?.message ?: context.getString(R.string.error_upload_image)
                }
                
                isUploading = false
            }
        }
    }
    
    // Permission launcher for media access
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Launch image picker after permissions granted
            imagePickerLauncher.launch("image/*")
        } else {
            Toast.makeText(
                context,
                "Se necesita permiso para acceder a las fotos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }
    
    LaunchedEffect(profileState) {
        if (profileState is GroupViewModel.UserProfileState.Success) {
            val profile = (profileState as GroupViewModel.UserProfileState.Success).profile
            selectedImageUrl = profile.avatarUrl
            username = profile.username ?: ""
        }
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_profile), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            when (val state = profileState) {
                is GroupViewModel.UserProfileState.Success -> {
                    Text(
                        text = stringResource(R.string.profile_photo),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Avatar Preview
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator()
                        } else if (selectedImageUrl != null) {
                            AsyncImage(
                                model = selectedImageUrl,
                                contentDescription = stringResource(R.string.avatar),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Error message para imagen
                    if (uploadError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uploadError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Image Picker Button con forma cuadrada y bordes redondeados
                    OutlinedButton(
                        onClick = {
                            // Check permissions before opening picker
                            if (PermissionUtils.hasMediaPermissions(context)) {
                                imagePickerLauncher.launch("image/*")
                            } else {
                                permissionLauncher.launch(PermissionUtils.getMediaPermissions())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUploading,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.select_photo))
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Campo de username
                    Text(
                        text = stringResource(R.string.username),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            usernameError = null
                        },
                        label = { Text(stringResource(R.string.username)) },
                        isError = usernameError != null,
                        supportingText = usernameError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isSavingUsername
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Botón para guardar username
                    Button(
                        onClick = {
                            if (username.isBlank() || username.length < 3) {
                                usernameError = context.getString(R.string.username_error)
                                return@Button
                            }
                            
                            scope.launch {
                                isSavingUsername = true
                                usernameError = null
                                
                                val result = viewModel.updateUserUsername(username)
                                if (result.isFailure) {
                                    val exception = result.exceptionOrNull()
                                    usernameError = if (exception?.message == "USERNAME_TAKEN") {
                                        context.getString(R.string.username_taken)
                                    } else {
                                        context.getString(R.string.error_update_username)
                                    }
                                }
                                
                                isSavingUsername = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSavingUsername && username != state.profile.username
                    ) {
                        if (isSavingUsername) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(R.string.save))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp))
                }
                is GroupViewModel.UserProfileState.Loading -> {
                    CircularProgressIndicator()
                }
                is GroupViewModel.UserProfileState.Error -> {
                    Text(
                        text = state.message ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {}
            }
            }
        }
    }
}
