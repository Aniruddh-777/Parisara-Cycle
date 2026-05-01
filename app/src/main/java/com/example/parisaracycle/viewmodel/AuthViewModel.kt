package com.example.parisaracycle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.parisaracycle.data.model.AppUser
import com.example.parisaracycle.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isFirebaseConfigured: Boolean = true,
    val user: AppUser? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AuthUiState(isFirebaseConfigured = authRepository.isConfigured)
    )

    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authState.collect { user ->
                _uiState.update {
                    it.copy(user = user, isLoading = false, errorMessage = null)
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        submit(email, password) { normalizedEmail, rawPassword ->
            authRepository.signIn(normalizedEmail, rawPassword)
        }
    }

    fun register(email: String, password: String) {
        submit(email, password) { normalizedEmail, rawPassword ->
            authRepository.register(normalizedEmail, rawPassword)
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun submit(
        email: String,
        password: String,
        action: suspend (String, String) -> Result<Unit>
    ) {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank() || password.length < 6) {
            _uiState.update {
                it.copy(errorMessage = "Enter a valid email and a password with at least 6 characters.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = action(normalizedEmail, password)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    companion object {
        fun factory(repository: AuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AuthViewModel(repository) as T
            }
    }
}
