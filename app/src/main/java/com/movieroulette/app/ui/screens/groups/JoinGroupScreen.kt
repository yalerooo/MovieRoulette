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
fun JoinGroupScreen(
    navController: NavController,
    viewModel: GroupViewModel = viewModel()
) {
    var inviteCode by remember { mutableStateOf("") }
    var inviteCodeError by remember { mutableStateOf<String?>(null) }
    
    val joinGroupState by viewModel.joinGroupState.collectAsState()
    
    LaunchedEffect(joinGroupState) {
        if (joinGroupState is GroupViewModel.JoinGroupState.Success) {
            navController.navigateUp()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unirse a Grupo") },
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
                text = "Únete con código",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Ingresa el código de invitación que te compartieron",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            AppTextField(
                value = inviteCode,
                onValueChange = {
                    inviteCode = it.filterNot(Char::isWhitespace).uppercase()
                    inviteCodeError = null
                },
                label = "Código de invitación",
                placeholder = "ABCD1234",
                error = inviteCodeError,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (joinGroupState is GroupViewModel.JoinGroupState.Error) {
                Text(
                    text = (joinGroupState as GroupViewModel.JoinGroupState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            PrimaryButton(
                text = "Unirse al Grupo",
                onClick = {
                    val sanitizedCode = inviteCode.trim()
                    if (sanitizedCode.isBlank()) {
                        inviteCodeError = "El código es requerido"
                    } else {
                        viewModel.joinGroup(sanitizedCode)
                    }
                },
                isLoading = joinGroupState is GroupViewModel.JoinGroupState.Loading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
