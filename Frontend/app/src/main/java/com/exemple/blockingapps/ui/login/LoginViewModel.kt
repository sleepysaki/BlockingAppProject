package com.exemple.blockingapps.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exemple.blockingapps.data.repo.UserRepository
import androidx.compose.runtime.getValue // Cần import
import androidx.compose.runtime.mutableStateOf // Cần import
import androidx.compose.runtime.setValue // Cần import
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginViewModel(
    private val userRepository: UserRepository
) : ViewModel() {


    var uiState by mutableStateOf(LoginUiState())
        private set

    fun onEmailChange(value: String) {
        uiState = uiState.copy(email = value)
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(password = value)
    }

    fun login() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            delay(800)

            val user =
                userRepository.login(uiState.email.trim(), uiState.password.trim())

            if (user == null) {

                uiState = uiState.copy(
                    isLoading = false,
                    error = "Wrong email or password"
                )
            } else {

                uiState = uiState.copy(
                    isLoading = false,
                    loginSuccess = true
                )
            }
        }
    }
}