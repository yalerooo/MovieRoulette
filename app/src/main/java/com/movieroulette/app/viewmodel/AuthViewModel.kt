package com.movieroulette.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieroulette.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    
    val authRepository = AuthRepository()
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val result = authRepository.signIn(email, password)
            _uiState.value = if (result.isSuccess) {
                AuthUiState.Success
            } else {
                AuthUiState.Error(result.exceptionOrNull()?.message, com.movieroulette.app.R.string.error_login)
            }
        }
    }
    
    fun signUp(email: String, password: String, username: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val result = authRepository.signUp(email, password, username)
            _uiState.value = if (result.isSuccess) {
                AuthUiState.Success
            } else {
                AuthUiState.Error(result.exceptionOrNull()?.message, com.movieroulette.app.R.string.error_create_account)
            }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
    
    fun signOutAndDeleteIncompleteUser() {
        viewModelScope.launch {
            authRepository.signOutAndDeleteIncompleteUser()
        }
    }
    
    sealed class AuthUiState {
        object Idle : AuthUiState()
        object Loading : AuthUiState()
        object Success : AuthUiState()
        data class Error(val message: String?, val messageResId: Int) : AuthUiState()
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
    
    suspend fun signInWithGoogle(): String? {
        val result = authRepository.signInWithGoogle()
        return if (result.isSuccess) {
            result.getOrNull()
        } else {
            _uiState.value = AuthUiState.Error(
                result.exceptionOrNull()?.message,
                com.movieroulette.app.R.string.error_login
            )
            null
        }
    }
    
    fun updateUsername(username: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.updateUsername(username)
            _uiState.value = if (result.isSuccess) {
                AuthUiState.Success
            } else {
                val exception = result.exceptionOrNull()
                val errorMessage = exception?.message
                
                // Detectar si el error es por username duplicado
                if (errorMessage == "USERNAME_TAKEN") {
                    AuthUiState.Error(
                        null,
                        com.movieroulette.app.R.string.username_taken
                    )
                } else {
                    AuthUiState.Error(
                        errorMessage,
                        com.movieroulette.app.R.string.error_update_username
                    )
                }
            }
        }
    }
}
