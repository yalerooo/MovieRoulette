package com.movieroulette.app.ui.screens.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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
import com.movieroulette.app.data.model.Group
import com.movieroulette.app.ui.components.*
import com.movieroulette.app.ui.navigation.Screen
import com.movieroulette.app.viewmodel.AuthViewModel
import com.movieroulette.app.viewmodel.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    navController: NavController,
    viewModel: GroupViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfileState by viewModel.userProfileState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadGroups()
        viewModel.loadUserProfile()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mis Grupos",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Box {
                        val profileState = userProfileState
                        IconButton(onClick = { showMenu = true }) {
                            if (profileState is GroupViewModel.UserProfileState.Success && 
                                profileState.profile.avatarUrl != null) {
                                AsyncImage(
                                    model = profileState.profile.avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, "Menú")
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar Perfil") },
                                onClick = {
                                    showMenu = false
                                    navController.navigate(Screen.EditProfile.route)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Ajustes") },
                                onClick = {
                                    showMenu = false
                                    navController.navigate(Screen.Settings.route)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Cerrar sesión") },
                                onClick = {
                                    authViewModel.signOut()
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ExitToApp, null)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.CreateGroup.route) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Crear grupo")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is GroupViewModel.GroupUiState.Loading -> {
                    LoadingScreen()
                }
                
                is GroupViewModel.GroupUiState.Empty -> {
                    EmptyState(
                        title = "No tienes grupos",
                        message = "Crea un grupo nuevo o únete a uno existente con un código de invitación",
                        emoji = "👥",
                        action = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                PrimaryButton(
                                    text = "Crear Grupo",
                                    onClick = { navController.navigate(Screen.CreateGroup.route) },
                                    modifier = Modifier.widthIn(max = 200.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                SecondaryButton(
                                    text = "Unirse a Grupo",
                                    onClick = { navController.navigate(Screen.JoinGroup.route) },
                                    modifier = Modifier.widthIn(max = 200.dp)
                                )
                            }
                        }
                    )
                }
                
                is GroupViewModel.GroupUiState.Success -> {
                    Column {
                        SecondaryButton(
                            text = "Unirse con código",
                            onClick = { navController.navigate(Screen.JoinGroup.route) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.groups) { group ->
                                GroupCard(
                                    group = group,
                                    onClick = {
                                        navController.navigate(Screen.GroupDetail.createRoute(group.id))
                                    }
                                )
                            }
                        }
                    }
                }
                
                is GroupViewModel.GroupUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = { viewModel.loadGroups() }
                    )
                }
            }
        }
    }
}

@Composable
fun GroupCard(
    group: Group,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group Image or Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (group.imageUrl != null) {
                    AsyncImage(
                        model = group.imageUrl,
                        contentDescription = group.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
