package com.exemple.blockingapps.ui.login

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exemple.blockingapps.data.local.SessionManager
import com.exemple.blockingapps.data.model.LoginRequest
import com.exemple.blockingapps.data.model.RegisterRequest
import com.exemple.blockingapps.model.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Function to Register a new user
    fun register(context: Context, request: RegisterRequest, onSuccess: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Call Server API
                val response = RetrofitClient.api.register(request)

                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    val msg = response["message"] ?: "Registration successful"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    onSuccess() // Switch to Login mode or auto-login
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    Toast.makeText(context, "Register Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Function to Login
    fun login(context: Context, request: LoginRequest, onLoginSuccess: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Call Server API
                val response = RetrofitClient.api.login(request)

                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    if (response.user.id.isNotEmpty()) {
                        // 1. Save Session locally
                        SessionManager.saveUserSession(
                            context,
                            response.user.id,
                            response.user.fullName
                        )

                        Toast.makeText(context, "Welcome ${response.user.fullName}!", Toast.LENGTH_SHORT).show()

                        // 2. Navigate to Home
                        onLoginSuccess()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    Toast.makeText(context, "Login Error: Check email/password", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}