package com.movieroulette.app.ui.screens.groups

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.movieroulette.app.ui.components.PrimaryButton
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
    val groupsState by viewModel.groupsState.collectAsState()
    
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var imageUrlInput by remember { mutableStateOf("") }
    var showUrlDialog by remember { mutableStateOf(false) }
    
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
                    if (selectedImageUrl != null) {
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // URL Input Button
                OutlinedButton(
                    onClick = { showUrlDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cambiar Imagen (URL)")
                }
                
                if (selectedImageUrl != it.imageUrl) {
                    PrimaryButton(
                        text = "Guardar Cambios",
                        onClick = {
                            scope.launch {
                                selectedImageUrl?.let { url ->
                                    viewModel.updateGroupImage(groupId, url)
                                    navController.navigateUp()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    
    // URL Input Dialog
    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("URL de la Imagen") },
            text = {
                OutlinedTextField(
                    value = imageUrlInput,
                    onValueChange = { imageUrlInput = it },
                    label = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (imageUrlInput.isNotBlank()) {
                            selectedImageUrl = imageUrlInput
                            imageUrlInput = ""
                            showUrlDialog = false
                        }
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
