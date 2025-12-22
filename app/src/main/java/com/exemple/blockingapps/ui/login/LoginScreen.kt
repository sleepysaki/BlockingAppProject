package com.exemple.blockingapps.ui.login

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exemple.blockingapps.data.model.LoginRequest
import com.exemple.blockingapps.data.model.RegisterRequest
import com.exemple.blockingapps.model.network.RetrofitClient
import kotlinx.coroutines.launch

// --- VIEW MODEL FOR LOGIC HANDLING ---
class AuthViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // Call Login API
                val response = RetrofitClient.api.login(LoginRequest(email, pass))
                // TODO: Save response.token to SharedPreferences/DataStore for later use
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Login failed: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun register(email: String, pass: String, name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // Call Register API
                val response = RetrofitClient.api.register(RegisterRequest(email, pass, name))

                // Get text content from server (Convert ResponseBody -> String)
                val message = response.string()

                // Log for debugging
                android.util.Log.d("AUTH", "Server reply: $message")

                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Registration error: ${e.message}"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}

// --- COMPOSE UI ---
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val viewModel: AuthViewModel = viewModel()
    val context = LocalContext.current

    // Input state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) } // Toggle between Login/Register

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isRegisterMode) "CREATE ACCOUNT" else "LOGIN",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isRegisterMode) {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (viewModel.errorMessage != null) {
            Text(text = viewModel.errorMessage!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (viewModel.isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (isRegisterMode) {
                        viewModel.register(email, password, fullName) {
                            Toast.makeText(context, "Registration successful! Please login.", Toast.LENGTH_SHORT).show()
                            isRegisterMode = false // Return to Login screen
                        }
                    } else {
                        viewModel.login(email, password) {
                            Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess() // Navigate to next screen
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isRegisterMode) "Register" else "Login")
            }
        }

        TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
            Text(if (isRegisterMode) "Already have an account? Login now" else "Don't have an account? Register now")
        }
    }
}