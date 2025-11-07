package com.movieroulette.app.ui.screens.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.movieroulette.app.ui.components.*
import com.movieroulette.app.viewmodel.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    navController: NavController,
    viewModel: GroupViewModel = viewModel()
) {
    var groupName by remember { mutableStateOf("") }
    var groupNameError by remember { mutableStateOf<String?>(null) }
    
    val createGroupState by viewModel.createGroupState.collectAsState()
    
    LaunchedEffect(createGroupState) {
        if (createGroupState is GroupViewModel.CreateGroupState.Success) {
            navController.navigateUp()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Grupo") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Atrás")
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Crea un grupo nuevo",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Los miembros del grupo podrán añadir películas y votar juntos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            AppTextField(
                value = groupName,
                onValueChange = {
                    groupName = it
                    groupNameError = null
                },
                label = "Nombre del grupo",
                placeholder = "Ej: Familia, Amigos, Cine Club...",
                error = groupNameError,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (createGroupState is GroupViewModel.CreateGroupState.Error) {
                Text(
                    text = (createGroupState as GroupViewModel.CreateGroupState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            PrimaryButton(
                text = "Crear Grupo",
                onClick = {
                    if (groupName.isBlank()) {
                        groupNameError = "El nombre es requerido"
                    } else {
                        viewModel.createGroup(groupName)
                    }
                },
                isLoading = createGroupState is GroupViewModel.CreateGroupState.Loading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
