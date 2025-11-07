package com.movieroulette.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieroulette.app.data.model.Group
import com.movieroulette.app.data.model.GroupMemberWithProfile
import com.movieroulette.app.data.model.UserProfile
import com.movieroulette.app.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupViewModel : ViewModel() {
    
    val groupRepository = GroupRepository()
    
    private val _uiState = MutableStateFlow<GroupUiState>(GroupUiState.Loading)
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()
    
    private val _createGroupState = MutableStateFlow<CreateGroupState>(CreateGroupState.Idle)
    val createGroupState: StateFlow<CreateGroupState> = _createGroupState.asStateFlow()
    
    private val _joinGroupState = MutableStateFlow<JoinGroupState>(JoinGroupState.Idle)
    val joinGroupState: StateFlow<JoinGroupState> = _joinGroupState.asStateFlow()
    
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
    
    fun resetCreateGroupState() {
        _createGroupState.value = CreateGroupState.Idle
    }
    
    fun resetJoinGroupState() {
        _joinGroupState.value = JoinGroupState.Idle
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
            groupRepository.removeMemberFromGroup(groupId, userId)
        }
    }
    
    sealed class MembersState {
        object Loading : MembersState()
        data class Success(val members: List<GroupMemberWithProfile>) : MembersState()
        data class Error(val message: String) : MembersState()
    }
}
