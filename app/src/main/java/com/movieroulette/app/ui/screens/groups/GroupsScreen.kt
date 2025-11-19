package com.movieroulette.app.ui.screens.groups

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.movieroulette.app.R
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
    authViewModel: AuthViewModel = viewModel(),
    friendsViewModel: com.movieroulette.app.viewmodel.FriendsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val userProfileState by viewModel.userProfileState.collectAsState()
    val leaveGroupState by viewModel.leaveGroupState.collectAsState()
    val followersState by friendsViewModel.followersState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var groupToLeave by remember { mutableStateOf<Group?>(null) }
    var leavingGroupId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.loadGroups()
        viewModel.loadUserGroups() // Cargar también para GroupDetailScreen
        viewModel.loadUserProfile()
        friendsViewModel.loadFollowers(context)
        
        // Asegurar que el usuario tenga claves E2E generadas
        com.movieroulette.app.utils.KeyManager.ensureUserHasKeys(context)
    }

    LaunchedEffect(leaveGroupState) {
        when (val state = leaveGroupState) {
            GroupViewModel.LeaveGroupState.Success -> {
                leavingGroupId = null
                groupToLeave = null
                viewModel.resetLeaveGroupState()
                snackbarHostState.showSnackbar(context.getString(R.string.left_group))
            }
            is GroupViewModel.LeaveGroupState.Error -> {
                leavingGroupId = null
                groupToLeave = null
                viewModel.resetLeaveGroupState()
                snackbarHostState.showSnackbar(state.message ?: "")
            }
            else -> Unit
        }
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.my_groups),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Icono de notificaciones - cambia cuando hay notificaciones
                    val notificationCount by friendsViewModel.notificationCount.collectAsState()
                    
                    IconButton(onClick = { 
                        friendsViewModel.clearNotifications(context)
                        navController.navigate("notifications")
                    }) {
                        Icon(
                            imageVector = if (notificationCount > 0) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Notificaciones",
                            tint = if (notificationCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    val profileState = userProfileState
                    IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                        if (profileState is GroupViewModel.UserProfileState.Success && 
                            profileState.profile.avatarUrl != null) {
                            AsyncImage(
                                model = profileState.profile.avatarUrl,
                                contentDescription = stringResource(R.string.avatar),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, stringResource(R.string.profile))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Friends button
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.Friends.route) },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.People, "Amigos")
                }
                
                // Create group button
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.CreateGroup.route) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.create_group))
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
                        title = stringResource(R.string.no_groups),
                        message = stringResource(R.string.no_groups_message),
                        showIcon = false,
                        action = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                PrimaryButton(
                                    text = stringResource(R.string.create_group),
                                    onClick = { navController.navigate(Screen.CreateGroup.route) },
                                    modifier = Modifier.widthIn(max = 200.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                SecondaryButton(
                                    text = stringResource(R.string.join_group),
                                    onClick = { navController.navigate(Screen.JoinGroup.route) },
                                    modifier = Modifier.widthIn(max = 200.dp)
                                )
                            }
                        }
                    )
                }
                
                is GroupViewModel.GroupUiState.Success -> {
                    val currentUserId = (userProfileState as? GroupViewModel.UserProfileState.Success)?.profile?.id
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Join Group Card
                        item {
                            Card(
                                onClick = { navController.navigate(Screen.JoinGroup.route) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = stringResource(R.string.join_group),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Group Cards
                        items(state.groups) { group ->
                            val canLeave = currentUserId != null && group.createdBy != currentUserId
                            val isLeaving = leaveGroupState is GroupViewModel.LeaveGroupState.Loading && leavingGroupId == group.id
                            GroupCardGrid(
                                group = group,
                                canLeave = canLeave,
                                isLeaving = isLeaving,
                                onClick = {
                                    navController.navigate(Screen.GroupDetail.createRoute(group.id))
                                },
                                onLeaveRequest = {
                                    if (!isLeaving && canLeave) {
                                        groupToLeave = group
                                    }
                                }
                            )
                        }
                    }
                }
                
                is GroupViewModel.GroupUiState.Error -> {
                    ErrorView(
                        message = state.message ?: "",
                        onRetry = { viewModel.loadGroups() }
                    )
                }
            }
        }
    }

    groupToLeave?.let { group ->
        val isLeaving = leaveGroupState is GroupViewModel.LeaveGroupState.Loading
        AlertDialog(
            onDismissRequest = {
                if (!isLeaving) {
                    groupToLeave = null
                }
            },
            title = { Text(stringResource(R.string.leave_group_title, group.name)) },
            text = { Text(stringResource(R.string.leave_group_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        leavingGroupId = group.id
                        groupToLeave = null
                        viewModel.leaveGroup(group.id)
                    },
                    enabled = !isLeaving
                ) {
                    if (isLeaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Text(stringResource(R.string.leave))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { groupToLeave = null },
                    enabled = !isLeaving
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun GroupCardGrid(
    group: Group,
    canLeave: Boolean,
    isLeaving: Boolean,
    onClick: () -> Unit,
    onLeaveRequest: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (canLeave) {
                    { onLeaveRequest() }
                } else {
                    null
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image with Gradient
            if (group.imageUrl != null) {
                AsyncImage(
                    model = group.imageUrl,
                    contentDescription = group.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                ),
                                startY = 100f,
                                endY = 500f
                            )
                        )
                )
            } else {
                // Default background with icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.weight(1f))
                
                // Group Name
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (group.imageUrl != null) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
            }

            // Loading indicator
            if (isLeaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}
