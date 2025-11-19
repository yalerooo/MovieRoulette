package com.movieroulette.app.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.movieroulette.app.data.model.GroupMemberWithProfile
import com.movieroulette.app.ui.components.LoadingScreen
import com.movieroulette.app.ui.navigation.Screen
import com.movieroulette.app.viewmodel.GroupDetailViewModel
import com.movieroulette.app.viewmodel.GroupViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMembersScreen(
    navController: NavController,
    groupId: String,
    viewModel: GroupDetailViewModel = viewModel(),
    groupViewModel: GroupViewModel = viewModel()
) {
    val membersState by viewModel.membersState.collectAsState()
    val groupsState by groupViewModel.groupsState.collectAsState()
    var showMemberDialog by remember { mutableStateOf<GroupMemberWithProfile?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showChangeRoleDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        viewModel.loadMembers(groupId)
        groupViewModel.loadUserGroups()
    }
    
    val group = (groupsState as? GroupViewModel.GroupsState.Success)?.groups?.find { it.id == groupId }
    val inviteCode = group?.inviteCode ?: ""
    val currentUserId = remember { mutableStateOf<String?>(null) }
    val currentUserRole = remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        val profile = groupViewModel.groupRepository.getUserProfile().getOrNull()
        currentUserId.value = profile?.id
    }
    
    // Obtener el rol del usuario actual
    LaunchedEffect(membersState, currentUserId.value) {
        when (val state = membersState) {
            is GroupDetailViewModel.MembersState.Success -> {
                currentUserRole.value = state.members.find { it.userId == currentUserId.value }?.role
            }
            else -> {}
        }
    }
    
    val isCreator = group?.createdBy == currentUserId.value
    val isAdmin = currentUserRole.value == "admin"
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = group?.name ?: stringResource(com.movieroulette.app.R.string.group_members),
                            fontWeight = FontWeight.Bold
                        )
                        when (val state = membersState) {
                            is GroupDetailViewModel.MembersState.Success -> {
                                Text(
                                    text = "${state.members.size} ${stringResource(com.movieroulette.app.R.string.members).lowercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            else -> {}
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, stringResource(com.movieroulette.app.R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showInviteDialog = true }) {
                        Icon(Icons.Default.PersonAdd, stringResource(com.movieroulette.app.R.string.invite))
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
        ) {
            when (val state = membersState) {
                is GroupDetailViewModel.MembersState.Loading -> LoadingScreen()
                is GroupDetailViewModel.MembersState.Success -> {
                    if (state.members.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(com.movieroulette.app.R.string.no_members),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(state.members, key = { it.id }) { member ->
                                val isCurrentUser = member.userId == currentUserId.value
                                MemberRow(
                                    member = member,
                                    isCreator = isCreator,
                                    isAdmin = isAdmin,
                                    isCurrentUser = isCurrentUser,
                                    onClick = { 
                                        if (!isCurrentUser) {
                                            showMemberDialog = member
                                        }
                                    }
                                )
                            }
                            
                            // Botones de acción al final
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Botón de eliminar grupo (solo para el creador)
                                    if (isCreator) {
                                        OutlinedButton(
                                            onClick = { showDeleteDialog = true },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.5.dp,
                                                MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = stringResource(com.movieroulette.app.R.string.delete_group),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Botón de salir del grupo (para todos menos el creador)
                                    if (!isCreator) {
                                        OutlinedButton(
                                            onClick = { 
                                                // TODO: Implementar lógica de salir del grupo
                                                navController.navigateUp()
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.5.dp,
                                                MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Text(
                                                text = stringResource(com.movieroulette.app.R.string.leave_group),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is GroupDetailViewModel.MembersState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
    
    // Invite dialog
    if (showInviteDialog && inviteCode.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text(stringResource(com.movieroulette.app.R.string.invite_to_group)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(com.movieroulette.app.R.string.share_code_message))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = inviteCode,
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(inviteCode))
                        android.widget.Toast.makeText(context, context.getString(com.movieroulette.app.R.string.code_copied), android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(com.movieroulette.app.R.string.copy))
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text(stringResource(com.movieroulette.app.R.string.close))
                }
            }
        )
    }
    
    // Member detail dialog
    showMemberDialog?.let { member ->
        Dialog(onDismissRequest = { showMemberDialog = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                if (member.role == "admin") 
                                    MaterialTheme.colorScheme.primaryContainer
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (member.avatarUrl != null) {
                            AsyncImage(
                                model = member.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (member.role == "admin") 
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    
                    // Username
                    Text(
                        text = member.username,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Role badge
                    if (member.role == "admin") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(com.movieroulette.app.R.string.administrator),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(com.movieroulette.app.R.string.member),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Action buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // View profile button - always visible
                        Button(
                            onClick = {
                                showMemberDialog = null
                                navController.navigate("user_profile/${member.userId}")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(com.movieroulette.app.R.string.view_profile))
                        }
                        
                        // Make admin / Remove admin button (for creator or any admin)
                        if (isCreator || isAdmin) {
                            if (member.role == "admin") {
                                // Remove admin (only creator can remove admins)
                                if (isCreator) {
                                    OutlinedButton(
                                        onClick = { showChangeRoleDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(com.movieroulette.app.R.string.remove_admin))
                                    }
                                }
                            } else {
                                // Make admin (creator or any admin can promote)
                                OutlinedButton(
                                    onClick = { showChangeRoleDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(com.movieroulette.app.R.string.make_admin))
                                }
                            }
                        }
                        
                        // Remove from group button
                        // - Creator can remove anyone
                        // - Secondary admins can remove members but not other admins
                        val canRemove = isCreator || (isAdmin && member.role != "admin")
                        
                        if (canRemove) {
                            Button(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(com.movieroulette.app.R.string.remove_from_group))
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Change role confirmation dialog
    if (showChangeRoleDialog && showMemberDialog != null) {
        val member = showMemberDialog!!
        val newRole = if (member.role == "admin") "member" else "admin"
        
        AlertDialog(
            onDismissRequest = { showChangeRoleDialog = false },
            title = { 
                Text(
                    if (newRole == "admin") 
                        stringResource(com.movieroulette.app.R.string.make_admin)
                    else 
                        stringResource(com.movieroulette.app.R.string.remove_admin)
                )
            },
            text = { 
                Text(
                    if (newRole == "admin")
                        stringResource(com.movieroulette.app.R.string.make_admin_confirm, member.username)
                    else
                        stringResource(com.movieroulette.app.R.string.remove_admin_confirm, member.username)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateMemberRole(groupId, member.userId, newRole)
                        showChangeRoleDialog = false
                        showMemberDialog = null
                    }
                ) {
                    Text(stringResource(com.movieroulette.app.R.string.accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeRoleDialog = false }) {
                    Text(stringResource(com.movieroulette.app.R.string.cancel))
                }
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && showMemberDialog != null) {
        val member = showMemberDialog!!
        
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(com.movieroulette.app.R.string.delete_member)) },
            text = { Text(stringResource(com.movieroulette.app.R.string.delete_member_confirm, member.username)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeMember(groupId, member.userId)
                        showDeleteDialog = false
                        showMemberDialog = null
                    }
                ) {
                    Text(stringResource(com.movieroulette.app.R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(com.movieroulette.app.R.string.cancel))
                }
            }
        )
    }
    
    // Delete group confirmation dialog
    if (showDeleteDialog && showMemberDialog == null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(com.movieroulette.app.R.string.delete_group_title)) },
            text = { 
                Text(
                    text = stringResource(com.movieroulette.app.R.string.delete_group_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            groupViewModel.deleteGroup(groupId)
                            showDeleteDialog = false
                            navController.popBackStack(Screen.Groups.route, false)
                        }
                    }
                ) {
                    Text(
                        text = stringResource(com.movieroulette.app.R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(com.movieroulette.app.R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun MemberRow(
    member: GroupMemberWithProfile,
    isCreator: Boolean,
    isAdmin: Boolean,
    isCurrentUser: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCurrentUser, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Avatar + Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (member.role == "admin") 
                            MaterialTheme.colorScheme.primaryContainer
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (member.avatarUrl != null) {
                    AsyncImage(
                        model = member.avatarUrl,
                        contentDescription = stringResource(com.movieroulette.app.R.string.avatar),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (member.role == "admin") 
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = member.username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (isCurrentUser) {
                    Text(
                        text = stringResource(com.movieroulette.app.R.string.you),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Right side: Role badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Role badge (only for admin)
            if (member.role == "admin") {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(com.movieroulette.app.R.string.administrator),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
