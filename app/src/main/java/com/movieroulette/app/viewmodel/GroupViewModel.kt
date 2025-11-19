package com.movieroulette.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieroulette.app.data.model.Group
import com.movieroulette.app.data.model.GroupMemberWithProfile
import com.movieroulette.app.data.model.UserProfile
import com.movieroulette.app.data.repository.AuthRepository
import com.movieroulette.app.data.repository.GroupRepository
import com.movieroulette.app.data.repository.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupViewModel : ViewModel() {
    
    val groupRepository = GroupRepository()
    private val storageRepository = StorageRepository()
    val authRepository = AuthRepository()
    
    private val _uiState = MutableStateFlow<GroupUiState>(GroupUiState.Loading)
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()
    
    private val _createGroupState = MutableStateFlow<CreateGroupState>(CreateGroupState.Idle)
    val createGroupState: StateFlow<CreateGroupState> = _createGroupState.asStateFlow()
    
    private val _joinGroupState = MutableStateFlow<JoinGroupState>(JoinGroupState.Idle)
    val joinGroupState: StateFlow<JoinGroupState> = _joinGroupState.asStateFlow()

    private val _leaveGroupState = MutableStateFlow<LeaveGroupState>(LeaveGroupState.Idle)
    val leaveGroupState: StateFlow<LeaveGroupState> = _leaveGroupState.asStateFlow()
    
    private val _groupsState = MutableStateFlow<GroupsState>(GroupsState.Loading)
    val groupsState: StateFlow<GroupsState> = _groupsState.asStateFlow()
    
    private val _userProfileState = MutableStateFlow<UserProfileState>(UserProfileState.Loading)
    val userProfileState: StateFlow<UserProfileState> = _userProfileState.asStateFlow()
    
    private val _membersState = MutableStateFlow<MembersState>(MembersState.Loading)
    val membersState: StateFlow<MembersState> = _membersState.asStateFlow()
    
    // Caché de nombres de grupos para evitar parpadeos
    private val groupNamesCache = mutableMapOf<String, String>()
    
    fun getGroupName(groupId: String): String {
        // Primero intentar del caché
        groupNamesCache[groupId]?.let { return it }
        
        // Luego buscar en el estado actual
        val groups = (groupsState.value as? GroupsState.Success)?.groups
        val group = groups?.find { it.id == groupId }
        if (group != null) {
            groupNamesCache[groupId] = group.name
            return group.name
        }
        
        return ""
    }
    
    fun loadUserGroups() {
        viewModelScope.launch {
            _groupsState.value = GroupsState.Loading
            
            val result = groupRepository.getUserGroups()
            _groupsState.value = if (result.isSuccess) {
                val groups = result.getOrNull() ?: emptyList()
                // Actualizar caché con todos los nombres
                groups.forEach { group ->
                    groupNamesCache[group.id] = group.name
                }
                GroupsState.Success(groups)
            } else {
                GroupsState.Error(result.exceptionOrNull()?.message, com.movieroulette.app.R.string.error_load_groups)
            }
        }
    }
    
    fun loadUserProfile() {
        viewModelScope.launch {
            _userProfileState.value = UserProfileState.Loading
            
            val result = groupRepository.getUserProfile()
            _userProfileState.value = if (result.isSuccess) {
                UserProfileState.Success(result.getOrNull()!!)
            } else {
                UserProfileState.Error(result.exceptionOrNull()?.message, com.movieroulette.app.R.string.error_load_profile)
            }
        }
    }
    
    fun updateGroupImage(groupId: String, imageUrl: String) {
        viewModelScope.launch {
            val result = groupRepository.updateGroupImage(groupId, imageUrl)
            if (result.isSuccess) {
                loadUserGroups()
            }
        }
    }
    
    fun updateUserAvatar(avatarUrl: String) {
        viewModelScope.launch {
            val result = groupRepository.updateUserAvatar(avatarUrl)
            if (result.isSuccess) {
                loadUserProfile()
            }
        }
    }

    /**
     * Upload profile picture to Supabase Storage and update user avatar
     */
    suspend fun uploadAndUpdateUserAvatar(imageData: ByteArray): Result<String> {
        return try {
            // Upload to storage
            val uploadResult = storageRepository.uploadProfilePicture(imageData)
            if (uploadResult.isFailure) {
                return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Error uploading profile picture"))
            }

            val imageUrl = uploadResult.getOrNull()!!

            // Update user profile
            val updateResult = groupRepository.updateUserAvatar(imageUrl)
            if (updateResult.isFailure) {
                return Result.failure(updateResult.exceptionOrNull() ?: Exception("Error updating profile"))
            }

            // Reload profile
            loadUserProfile()
            Result.success(imageUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUserUsername(newUsername: String): Result<Unit> {
        return try {
            val result = authRepository.updateUsername(newUsername)
            if (result.isSuccess) {
                // Reload profile after updating username
                loadUserProfile()
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload group picture to Supabase Storage and update group image
     */
    suspend fun uploadAndUpdateGroupImage(groupId: String, imageData: ByteArray): Result<String> {
        return try {
            // Upload to storage
            val uploadResult = storageRepository.uploadGroupPicture(imageData)
            if (uploadResult.isFailure) {
                return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Error uploading group picture"))
            }

            val imageUrl = uploadResult.getOrNull()!!

            // Update group
            val updateResult = groupRepository.updateGroupImage(groupId, imageUrl)
            if (updateResult.isFailure) {
                return Result.failure(updateResult.exceptionOrNull() ?: Exception("Error updating group"))
            }

            // Reload groups
            loadUserGroups()
            Result.success(imageUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun loadGroups() {
        viewModelScope.launch {
            _uiState.value = GroupUiState.Loading
            
            val result = groupRepository.getUserGroups()
            _uiState.value = if (result.isSuccess) {
                val groups = result.getOrNull() ?: emptyList()
                if (groups.isEmpty()) {
                    GroupUiState.Empty
                } else {
                    GroupUiState.Success(groups)
                }
            } else {
                GroupUiState.Error(result.exceptionOrNull()?.message, com.movieroulette.app.R.string.error_load_groups)
            }
        }
    }
    
    fun createGroup(name: String) {
        viewModelScope.launch {
            _createGroupState.value = CreateGroupState.Loading
            
            val result = groupRepository.createGroup(name)
            _createGroupState.value = if (result.isSuccess) {
                CreateGroupState.Success(result.getOrNull()!!)
            } else {
                CreateGroupState.Error(result.exceptionOrNull()?.message, com.movieroulette.app.R.string.error_create_group)
            }
        }
    }
    
    fun joinGroup(inviteCode: String) {
        viewModelScope.launch {
            _joinGroupState.value = JoinGroupState.Loading
            
            val result = groupRepository.joinGroupByCode(inviteCode)
            _joinGroupState.value = if (result.isSuccess) {
                JoinGroupState.Success(result.getOrNull()!!)
            } else {
                JoinGroupState.Error(result.exceptionOrNull()?.message, com.movieroulette.app.R.string.error_join_group)
            }
        }
    }

    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            _leaveGroupState.value = LeaveGroupState.Loading

            val result = groupRepository.leaveGroup(groupId)
            _leaveGroupState.value = if (result.isSuccess) {
                loadUserGroups()
                loadGroups()
                LeaveGroupState.Success
            } else {
                LeaveGroupState.Error(result.exceptionOrNull()?.message, com.movieroulette.app.R.string.error_leave_group)
            }
        }
    }
    
    fun resetCreateGroupState() {
        _createGroupState.value = CreateGroupState.Idle
    }
    
    fun resetJoinGroupState() {
        _joinGroupState.value = JoinGroupState.Idle
    }

    fun resetLeaveGroupState() {
        _leaveGroupState.value = LeaveGroupState.Idle
    }
    
    sealed class GroupUiState {
        object Loading : GroupUiState()
        object Empty : GroupUiState()
        data class Success(val groups: List<Group>) : GroupUiState()
        data class Error(val message: String?, val messageResId: Int) : GroupUiState()
    }
    
    sealed class CreateGroupState {
        object Idle : CreateGroupState()
        object Loading : CreateGroupState()
        data class Success(val group: Group) : CreateGroupState()
        data class Error(val message: String?, val messageResId: Int) : CreateGroupState()
    }
    
    sealed class JoinGroupState {
        object Idle : JoinGroupState()
        object Loading : JoinGroupState()
        data class Success(val group: Group) : JoinGroupState()
        data class Error(val message: String?, val messageResId: Int) : JoinGroupState()
    }

    sealed class LeaveGroupState {
        object Idle : LeaveGroupState()
        object Loading : LeaveGroupState()
        object Success : LeaveGroupState()
        data class Error(val message: String?, val messageResId: Int) : LeaveGroupState()
    }
    
    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            val result = groupRepository.deleteGroup(groupId)
            if (result.isSuccess) {
                loadUserGroups()
            }
        }
    }
    
    fun loadMembers(groupId: String) {
        viewModelScope.launch {
            _membersState.value = MembersState.Loading
            
            val result = groupRepository.getGroupMembers(groupId)
            _membersState.value = if (result.isSuccess) {
                MembersState.Success(result.getOrNull() ?: emptyList())
            } else {
                MembersState.Error(result.exceptionOrNull()?.message, com.movieroulette.app.R.string.error_load_members)
            }
        }
    }
    
    sealed class GroupsState {
        object Loading : GroupsState()
        data class Success(val groups: List<Group>) : GroupsState()
        data class Error(val message: String?, val messageResId: Int) : GroupsState()
    }
    
    sealed class UserProfileState {
        object Loading : UserProfileState()
        data class Success(val profile: UserProfile) : UserProfileState()
        data class Error(val message: String?, val messageResId: Int) : UserProfileState()
    }
    
    sealed class MembersState {
        object Loading : MembersState()
        data class Success(val members: List<GroupMemberWithProfile>) : MembersState()
        data class Error(val message: String?, val messageResId: Int) : MembersState()
    }
}

class GroupDetailViewModel : ViewModel() {
    
    val groupRepository = GroupRepository()
    
    private val _membersState = MutableStateFlow<MembersState>(MembersState.Loading)
    val membersState: StateFlow<MembersState> = _membersState.asStateFlow()
    
    fun loadMembers(groupId: String) {
        viewModelScope.launch {
            _membersState.value = MembersState.Loading
            
            val result = groupRepository.getGroupMembers(groupId)
            _membersState.value = if (result.isSuccess) {
                MembersState.Success(result.getOrNull() ?: emptyList())
            } else {
                MembersState.Error(result.exceptionOrNull()?.message, com.movieroulette.app.R.string.error_load_members)
            }
        }
    }
    
    fun removeMember(groupId: String, userId: String) {
        viewModelScope.launch {
            val result = groupRepository.removeMemberFromGroup(groupId, userId)
            if (result.isSuccess) {
                loadMembers(groupId)
            } else {
                _membersState.value = MembersState.Error(result.exceptionOrNull()?.message, com.movieroulette.app.R.string.error_remove_member)
            }
        }
    }
    
    fun updateMemberRole(groupId: String, userId: String, newRole: String) {
        viewModelScope.launch {
            val result = groupRepository.updateMemberRole(groupId, userId, newRole)
            if (result.isSuccess) {
                loadMembers(groupId)
            } else {
                _membersState.value = MembersState.Error(result.exceptionOrNull()?.message, com.movieroulette.app.R.string.error_update_role)
            }
        }
    }
    
    sealed class MembersState {
        object Loading : MembersState()
        data class Success(val members: List<GroupMemberWithProfile>) : MembersState()
        data class Error(val message: String?, val messageResId: Int) : MembersState()
    }
}
