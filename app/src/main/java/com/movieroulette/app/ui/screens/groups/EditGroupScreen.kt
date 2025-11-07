package com.movieroulette.app.ui.screens.groups

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.movieroulette.app.ui.components.PrimaryButton
import com.movieroulette.app.utils.ImageUtils
import com.movieroulette.app.viewmodel.GroupViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGroupScreen(
    navController: NavController,
    groupId: String,
    viewModel: GroupViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val groupsState by viewModel.groupsState.collectAsState()
    
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isUploading = true
                uploadError = null
                
                // Compress image (larger size for group images)
                val compressResult = ImageUtils.compressImage(context, uri, maxSizeKB = 1000)
                if (compressResult.isFailure) {
                    uploadError = "Error al comprimir imagen"
                    isUploading = false
                    return@launch
                }
                
                // Upload and update
                val uploadResult = viewModel.uploadAndUpdateGroupImage(groupId, compressResult.getOrNull()!!)
                if (uploadResult.isSuccess) {
                    selectedImageUrl = uploadResult.getOrNull()
                    uploadError = null
                } else {
                    uploadError = uploadResult.exceptionOrNull()?.message ?: "Error al subir imagen"
                }
                
                isUploading = false
            }
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadUserGroups()
    }
    
    val group = (groupsState as? GroupViewModel.GroupsState.Success)?.groups?.find { it.id == groupId }
    
    LaunchedEffect(group) {
        group?.let {
            selectedImageUrl = it.imageUrl
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Grupo", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            group?.let {
                Text(
                    text = "Imagen del Grupo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                // Group Image Preview
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        CircularProgressIndicator()
                    } else if (selectedImageUrl != null) {
                        AsyncImage(
                            model = selectedImageUrl,
                            contentDescription = "Imagen del grupo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Text(
                    text = it.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Error message
                if (uploadError != null) {
                    Text(
                        text = uploadError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Image Picker Button
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Seleccionar Imagen")
                }
            }
        }
    }
}
