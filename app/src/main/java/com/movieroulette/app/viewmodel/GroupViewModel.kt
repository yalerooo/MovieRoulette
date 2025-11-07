package com.movieroulette.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieroulette.app.data.model.Group
import com.movieroulette.app.data.model.GroupMemberWithProfile
import com.movieroulette.app.data.model.UserProfile
import com.movieroulette.app.data.repository.GroupRepository
import com.movieroulette.app.data.repository.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupViewModel : ViewModel() {
    
    val groupRepository = GroupRepository()
    private val storageRepository = StorageRepository()
    
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
    
    fun loadUserGroups() {
        viewModelScope.launch {
            _groupsState.value = GroupsState.Loading
            
            val result = groupRepository.getUserGroups()
            _groupsState.value = if (result.isSuccess) {
                GroupsState.Success(result.getOrNull() ?: emptyList())
            } else {
                GroupsState.Error(result.exceptionOrNull()?.message ?: "Error al cargar grupos")
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
                UserProfileState.Error(result.exceptionOrNull()?.message ?: "Error al cargar perfil")
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
                return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Error al subir imagen"))
            }

            val imageUrl = uploadResult.getOrNull()!!

            // Update user profile
            val updateResult = groupRepository.updateUserAvatar(imageUrl)
            if (updateResult.isFailure) {
                return Result.failure(updateResult.exceptionOrNull() ?: Exception("Error al actualizar perfil"))
            }

            // Reload profile
            loadUserProfile()
            Result.success(imageUrl)
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
                return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Error al subir imagen"))
            }

            val imageUrl = uploadResult.getOrNull()!!

            // Update group
            val updateResult = groupRepository.updateGroupImage(groupId, imageUrl)
            if (updateResult.isFailure) {
                return Result.failure(updateResult.exceptionOrNull() ?: Exception("Error al actualizar grupo"))
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
                GroupUiState.Error(result.exceptionOrNull()?.message ?: "Error al cargar grupos")
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
                CreateGroupState.Error(result.exceptionOrNull()?.message ?: "Error al crear grupo")
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
                JoinGroupState.Error(result.exceptionOrNull()?.message ?: "Error al unirse al grupo")
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
                LeaveGroupState.Error(result.exceptionOrNull()?.message ?: "Error al salir del grupo")
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
        data class Error(val message: String) : GroupUiState()
    }
    
    sealed class CreateGroupState {
        object Idle : CreateGroupState()
        object Loading : CreateGroupState()
        data class Success(val group: Group) : CreateGroupState()
        data class Error(val message: String) : CreateGroupState()
    }
    
    sealed class JoinGroupState {
        object Idle : JoinGroupState()
        object Loading : JoinGroupState()
        data class Success(val group: Group) : JoinGroupState()
        data class Error(val message: String) : JoinGroupState()
    }

    sealed class LeaveGroupState {
        object Idle : LeaveGroupState()
        object Loading : LeaveGroupState()
        object Success : LeaveGroupState()
        data class Error(val message: String) : LeaveGroupState()
    }
    
    sealed class GroupsState {
        object Loading : GroupsState()
        data class Success(val groups: List<Group>) : GroupsState()
        data class Error(val message: String) : GroupsState()
    }
    
    sealed class UserProfileState {
        object Loading : UserProfileState()
        data class Success(val profile: UserProfile) : UserProfileState()
        data class Error(val message: String) : UserProfileState()
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
                MembersState.Error(result.exceptionOrNull()?.message ?: "Error al cargar miembros")
            }
        }
    }
    
    fun removeMember(groupId: String, userId: String) {
        viewModelScope.launch {
            val result = groupRepository.removeMemberFromGroup(groupId, userId)
            if (result.isSuccess) {
                loadMembers(groupId)
            } else {
                _membersState.value = MembersState.Error(result.exceptionOrNull()?.message ?: "Error al eliminar miembro")
            }
        }
    }
    
    sealed class MembersState {
        object Loading : MembersState()
        data class Success(val members: List<GroupMemberWithProfile>) : MembersState()
        data class Error(val message: String) : MembersState()
    }
}
