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

    fun login(context: Context, request: LoginRequest, onSuccess: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.login(request)

                withContext(Dispatchers.Main) {
                    SessionManager.saveUserSession(
                        context,
                        userId = response.user.id,
                        fullName = response.user.fullName,
                        role = response.user.role

                    )

                    Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                    onSuccess()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val errorMsg = e.message ?: "Login failed"
                    Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(context: Context, request: RegisterRequest, onSuccess: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.register(request)
                withContext(Dispatchers.Main) {
                    if (response.status == "success") {
                        Toast.makeText(context, "Registered! Please Login.", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    } else {
                        val msg = response.message ?: "Registration failed"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val errorMsg = e.message ?: "Unknown network error"
                    Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}