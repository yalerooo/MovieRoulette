package com.movieroulette.app.ui.screens.groups

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.movieroulette.app.R
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.movieroulette.app.ui.components.PrimaryButton
import com.movieroulette.app.utils.ImageUtils
import com.movieroulette.app.utils.PermissionUtils
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
    val leaveGroupState by viewModel.leaveGroupState.collectAsState()
    
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var currentUserRole by remember { mutableStateOf<String?>(null) }

    // Image picker launcher (debe estar antes de permissionLauncher)
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
                    uploadError = context.getString(R.string.error_compress_image)
                    isUploading = false
                    return@launch
                }
                
                // Upload and update
                val uploadResult = viewModel.uploadAndUpdateGroupImage(groupId, compressResult.getOrNull()!!)
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
        viewModel.loadUserGroups()
    }
    
    val group = (groupsState as? GroupViewModel.GroupsState.Success)?.groups?.find { it.id == groupId }
    
    LaunchedEffect(group) {
        group?.let {
            selectedImageUrl = it.imageUrl
            // Cargar el rol del usuario actual
            scope.launch {
                val membersResult = viewModel.groupRepository.getGroupMembers(groupId)
                if (membersResult.isSuccess) {
                    val currentUser = viewModel.authRepository.getCurrentUser()
                    val currentUserId = currentUser?.id
                    val currentMember = membersResult.getOrNull()?.find { member -> member.userId == currentUserId }
                    currentUserRole = currentMember?.role
                }
            }
        }
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_group), fontWeight = FontWeight.SemiBold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            group?.let {
                // Group Image Preview
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        CircularProgressIndicator()
                    } else if (selectedImageUrl != null) {
                        AsyncImage(
                            model = selectedImageUrl,
                            contentDescription = stringResource(R.string.group_image_description),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = it.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Error message
                if (uploadError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uploadError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Image Picker Button
                PrimaryButton(
                    text = stringResource(R.string.select_image),
                    onClick = {
                        // Check permissions before opening picker
                        if (PermissionUtils.hasMediaPermissions(context)) {
                            imagePickerLauncher.launch("image/*")
                        } else {
                            permissionLauncher.launch(PermissionUtils.getMediaPermissions())
                        }
                    },
                    enabled = !isUploading,
                    isLoading = isUploading
                )
                
                Spacer(modifier = Modifier.weight(1f))
            }
            
            // No más diálogos de eliminar/salir del grupo
        }
    }
    
    // Navigate back when leave is successful
    LaunchedEffect(leaveGroupState) {
        if (leaveGroupState is GroupViewModel.LeaveGroupState.Success) {
            viewModel.resetLeaveGroupState()
            navController.popBackStack(navController.graph.startDestinationId, false)
        }
    }
}