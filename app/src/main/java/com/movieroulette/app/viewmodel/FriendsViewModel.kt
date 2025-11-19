package com.movieroulette.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieroulette.app.data.model.*
import com.movieroulette.app.data.remote.SupabaseConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class FriendsViewModel : ViewModel() {
    
    private val _friendsState = MutableStateFlow<FriendsState>(FriendsState.Loading)
    val friendsState: StateFlow<FriendsState> = _friendsState
    
    private val _followingState = MutableStateFlow<FollowingState>(FollowingState.Loading)
    val followingState: StateFlow<FollowingState> = _followingState
    
    private val _followersState = MutableStateFlow<FollowersState>(FollowersState.Loading)
    val followersState: StateFlow<FollowersState> = _followersState
    
    private val _notificationCount = MutableStateFlow(0)
    val notificationCount: StateFlow<Int> = _notificationCount
    
    private var notificationsCleared = false
    
    private val _pendingRequestsState = MutableStateFlow<PendingRequestsState>(PendingRequestsState.Loading)
    val pendingRequestsState: StateFlow<PendingRequestsState> = _pendingRequestsState
    
    private val _searchResultsState = MutableStateFlow<SearchResultsState>(SearchResultsState.Idle)
    val searchResultsState: StateFlow<SearchResultsState> = _searchResultsState
    
    sealed class FriendsState {
        object Loading : FriendsState()
        data class Success(val friends: List<FriendWithProfile>) : FriendsState()
        data class Error(val message: String) : FriendsState()
    }
    
    sealed class FollowingState {
        object Loading : FollowingState()
        data class Success(val following: List<FollowWithProfile>) : FollowingState()
        data class Error(val message: String) : FollowingState()
    }
    
    sealed class FollowersState {
        object Loading : FollowersState()
        data class Success(val followers: List<FollowWithProfile>) : FollowersState()
        data class Error(val message: String) : FollowersState()
    }
    
    sealed class PendingRequestsState {
        object Loading : PendingRequestsState()
        data class Success(val requests: List<FriendWithProfile>) : PendingRequestsState()
        data class Error(val message: String) : PendingRequestsState()
    }
    
    sealed class SearchResultsState {
        object Idle : SearchResultsState()
        object Loading : SearchResultsState()
        data class Success(val users: List<UserProfile>) : SearchResultsState()
        data class Error(val message: String) : SearchResultsState()
    }
    
    fun loadFriends() {
        viewModelScope.launch {
            try {
                _friendsState.value = FriendsState.Loading
                val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
                
                // Obtener amistades aceptadas
                val friendships = SupabaseConfig.client.from("friendships")
                    .select {
                        filter {
                            or {
                                eq("user_id", currentUserId)
                                eq("friend_id", currentUserId)
                            }
                            eq("status", "accepted")
                        }
                    }
                    .decodeList<Friendship>()
                
                // Obtener perfiles de amigos
                val friendsWithProfiles = friendships.mapNotNull { friendship ->
                    val friendId = if (friendship.userId == currentUserId) friendship.friendId else friendship.userId
                    try {
                        val profile = SupabaseConfig.client.from("profiles")
                            .select {
                                filter {
                                    eq("id", friendId)
                                }
                            }
                            .decodeSingle<UserProfile>()
                        FriendWithProfile(friendship, profile)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                _friendsState.value = FriendsState.Success(friendsWithProfiles)
            } catch (e: Exception) {
                _friendsState.value = FriendsState.Error(e.message ?: "Error al cargar amigos")
            }
        }
    }
    
    fun loadFollowing() {
        viewModelScope.launch {
            try {
                _followingState.value = FollowingState.Loading
                val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
                
                val follows = SupabaseConfig.client.from("follows")
                    .select {
                        filter {
                            eq("follower_id", currentUserId)
                        }
                    }
                    .decodeList<Follow>()
                
                val followingWithProfiles = follows.mapNotNull { follow ->
                    try {
                        val profile = SupabaseConfig.client.from("profiles")
                            .select {
                                filter {
                                    eq("id", follow.followingId)
                                }
                            }
                            .decodeSingle<UserProfile>()
                        
                        // Verificar si el usuario también te sigue (isFollowingBack)
                        val isFollowingBack = try {
                            val followBack = SupabaseConfig.client.from("follows")
                                .select {
                                    filter {
                                        eq("follower_id", follow.followingId)
                                        eq("following_id", currentUserId)
                                    }
                                }
                                .decodeList<Follow>()
                            followBack.isNotEmpty()
                        } catch (e: Exception) {
                            false
                        }
                        
                        FollowWithProfile(follow, profile, isFollowingBack)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                _followingState.value = FollowingState.Success(followingWithProfiles)
            } catch (e: Exception) {
                _followingState.value = FollowingState.Error(e.message ?: "Error al cargar seguidos")
            }
        }
    }
    
    fun loadPendingRequests() {
        viewModelScope.launch {
            try {
                _pendingRequestsState.value = PendingRequestsState.Loading
                val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
                
                val requests = SupabaseConfig.client.from("friendships")
                    .select {
                        filter {
                            eq("friend_id", currentUserId)
                            eq("status", "pending")
                        }
                    }
                    .decodeList<Friendship>()
                
                val requestsWithProfiles = requests.mapNotNull { friendship ->
                    try {
                        val profile = SupabaseConfig.client.from("profiles")
                            .select {
                                filter {
                                    eq("id", friendship.userId)
                                }
                            }
                            .decodeSingle<UserProfile>()
                        FriendWithProfile(friendship, profile)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                _pendingRequestsState.value = PendingRequestsState.Success(requestsWithProfiles)
            } catch (e: Exception) {
                _pendingRequestsState.value = PendingRequestsState.Error(e.message ?: "Error al cargar solicitudes")
            }
        }
    }
    
    fun searchUsers(query: String) {
        viewModelScope.launch {
            try {
                _searchResultsState.value = SearchResultsState.Loading
                val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
                
                val users = SupabaseConfig.client.from("profiles")
                    .select {
                        filter {
                            ilike("username", "%$query%")
                            neq("id", currentUserId)
                        }
                    }
                    .decodeList<UserProfile>()
                
                _searchResultsState.value = SearchResultsState.Success(users)
            } catch (e: Exception) {
                _searchResultsState.value = SearchResultsState.Error(e.message ?: "Error al buscar usuarios")
            }
        }
    }
    
    fun sendFriendRequest(userId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
                
                // Verificar si ya existe una solicitud o amistad
                val existing = SupabaseConfig.client.from("friendships")
                    .select {
                        filter {
                            or {
                                and {
                                    eq("user_id", currentUserId)
                                    eq("friend_id", userId)
                                }
                                and {
                                    eq("user_id", userId)
                                    eq("friend_id", currentUserId)
                                }
                            }
                        }
                    }
                    .decodeList<Friendship>()
                
                // Si ya existe, no hacer nada
                if (existing.isNotEmpty()) {
                    return@launch
                }
                
                SupabaseConfig.client.from("friendships").insert(
                    buildJsonObject {
                        put("user_id", currentUserId)
                        put("friend_id", userId)
                        put("status", "pending")
                    }
                )
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun acceptFriendRequest(friendshipId: String) {
        viewModelScope.launch {
            try {
                SupabaseConfig.client.from("friendships").update(
                    buildJsonObject {
                        put("status", "accepted")
                    }
                ) {
                    filter {
                        eq("id", friendshipId)
                    }
                }
                loadPendingRequests()
                loadFriends()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun rejectFriendRequest(friendshipId: String) {
        viewModelScope.launch {
            try {
                SupabaseConfig.client.from("friendships").delete {
                    filter {
                        eq("id", friendshipId)
                    }
                }
                loadPendingRequests()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun followUser(userId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
                
                // Verificar si ya está siguiendo
                val existing = SupabaseConfig.client.from("follows")
                    .select {
                        filter {
                            eq("follower_id", currentUserId)
                            eq("following_id", userId)
                        }
                    }
                    .decodeList<Follow>()
                
                // Si ya existe, no hacer nada
                if (existing.isNotEmpty()) {
                    return@launch
                }
                
                SupabaseConfig.client.from("follows").insert(
                    buildJsonObject {
                        put("follower_id", currentUserId)
                        put("following_id", userId)
                    }
                )
                loadFollowing()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun unfollowUser(userId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
                
                SupabaseConfig.client.from("follows").delete {
                    filter {
                        eq("follower_id", currentUserId)
                        eq("following_id", userId)
                    }
                }
                loadFollowing()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun removeFriend(friendshipId: String) {
        viewModelScope.launch {
            try {
                SupabaseConfig.client.from("friendships").delete {
                    filter {
                        eq("id", friendshipId)
                    }
                }
                loadFriends()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    suspend fun checkIfFriends(userId: String): Boolean {
        return try {
            val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return false
            
            val friendships = SupabaseConfig.client.from("friendships")
                .select {
                    filter {
                        or {
                            and {
                                eq("user1_id", currentUserId)
                                eq("user2_id", userId)
                            }
                            and {
                                eq("user1_id", userId)
                                eq("user2_id", currentUserId)
                            }
                        }
                        eq("status", "accepted")
                    }
                }
                .decodeList<Friendship>()
            
            friendships.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun checkIfFollowing(userId: String): Boolean {
        return try {
            val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return false
            
            val follows = SupabaseConfig.client.from("follows")
                .select {
                    filter {
                        eq("follower_id", currentUserId)
                        eq("following_id", userId)
                    }
                }
                .decodeList<Follow>()
            
            follows.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun checkIfFollowsBack(userId: String): Boolean {
        return try {
            val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return false
            
            // Verificar si el otro usuario me sigue de vuelta
            val follows = SupabaseConfig.client.from("follows")
                .select {
                    filter {
                        eq("follower_id", userId)
                        eq("following_id", currentUserId)
                    }
                }
                .decodeList<Follow>()
            
            follows.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun checkIfRequestSent(userId: String): Boolean {
        return try {
            val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return false
            
            val requests = SupabaseConfig.client.from("friendships")
                .select {
                    filter {
                        eq("user1_id", currentUserId)
                        eq("user2_id", userId)
                        eq("status", "pending")
                    }
                }
                .decodeList<Friendship>()
            
            requests.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    fun loadFollowers(context: android.content.Context? = null) {
        viewModelScope.launch {
            try {
                _followersState.value = FollowersState.Loading
                val currentUserId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
                
                // Obtener quiénes me siguen
                val followers = SupabaseConfig.client.from("follows")
                    .select {
                        filter {
                            eq("following_id", currentUserId)
                        }
                    }
                    .decodeList<Follow>()
                
                // Obtener perfiles y verificar si los sigo de vuelta
                val followersWithProfiles = followers.mapNotNull { follow ->
                    try {
                        val profile = SupabaseConfig.client.from("profiles")
                            .select {
                                filter {
                                    eq("id", follow.followerId)
                                }
                            }
                            .decodeSingle<UserProfile>()
                        
                        // Verificar si sigo a este usuario de vuelta
                        val isFollowingBack = checkIfFollowing(follow.followerId)
                        
                        FollowWithProfile(follow, profile, isFollowingBack)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                _followersState.value = FollowersState.Success(followersWithProfiles)
                
                // Actualizar el contador solo si no se han limpiado las notificaciones
                if (!notificationsCleared) {
                    // Obtener timestamp de última visualización
                    val lastSeenTimestamp = context?.let {
                        it.getSharedPreferences("notifications", android.content.Context.MODE_PRIVATE)
                            .getLong("last_seen_followers", 0L)
                    } ?: 0L
                    
                    // Contar solo seguidores nuevos (después del último timestamp visto)
                    val newFollowersCount = followersWithProfiles.count { follower ->
                        !follower.isFollowingBack && parseTimestamp(follower.follow.createdAt) > lastSeenTimestamp
                    }
                    
                    _notificationCount.value = newFollowersCount
                }
            } catch (e: Exception) {
                _followersState.value = FollowersState.Error(e.message ?: "Error al cargar seguidores")
            }
        }
    }
    
    private fun parseTimestamp(timestamp: String): Long {
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            format.parse(timestamp)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    fun clearNotifications(context: android.content.Context) {
        notificationsCleared = true
        _notificationCount.value = 0
        
        // Guardar timestamp actual para marcar como vistas
        context.getSharedPreferences("notifications", android.content.Context.MODE_PRIVATE)
            .edit()
            .putLong("last_seen_followers", System.currentTimeMillis())
            .apply()
    }
    
    fun resetNotificationsFlag() {
        notificationsCleared = false
    }
    
    fun getFollowersCount(): Int {
        return when (val state = _followersState.value) {
            is FollowersState.Success -> state.followers.count { !it.isFollowingBack }
            else -> 0
        }
    }
}
