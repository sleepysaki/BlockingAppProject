package com.exemple.blockingapps.ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exemple.blockingapps.data.local.SessionManager // Đảm bảo import đúng
import com.exemple.blockingapps.data.model.LoginRequest
import com.exemple.blockingapps.data.model.RegisterRequest

enum class UserRole { PARENT, CHILD }

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    // State toggle between Login and Register mode
    var isRegisterMode by remember { mutableStateOf(false) }

    var selectedRole by remember { mutableStateOf(UserRole.PARENT) }


    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") } // Only for Register

    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    // 👇 QUAN TRỌNG: Xóa sạch dữ liệu cũ ngay khi vừa vào màn hình Login
    // Giúp đảm bảo không bị dính ID của người dùng trước đó
    LaunchedEffect(Unit) {
        SessionManager.clearSession(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isRegisterMode) "Create Account" else "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Full Name field (Only show in Register mode)
        if (isRegisterMode) {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))
        }


        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isRegisterMode) {
            Text(
                text = "Account Type",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedRole == UserRole.PARENT,
                        onClick = { selectedRole = UserRole.PARENT }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Parent")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedRole == UserRole.CHILD,
                        onClick = { selectedRole = UserRole.CHILD }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Child")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }


        // ACTION BUTTON (Login or Register)
        Button(
            onClick = {
                if (isRegisterMode) {
                    // Handle Register
                    val req = RegisterRequest(
                        email = email,
                        password = password,
                        fullName = fullName,
                        role = selectedRole.name // "PARENT" or "CHILD"
                    )
                    viewModel.register(context, req) {
                        // On success register, switch back to login to force user to sign in
                        isRegisterMode = false
                        // Xóa form để người dùng nhập lại
                        password = ""
                    }
                } else {
                    // Handle Login
                    val req = LoginRequest(email, password)
                    // Gọi hàm login trong ViewModel (Nơi sẽ thực hiện lưu Session)
                    viewModel.login(context, req, onLoginSuccess)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text(if (isRegisterMode) "REGISTER" else "LOGIN")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Toggle Text
        Text(
            text = if (isRegisterMode) "Already have an account? Login" else "Don't have an account? Register",
            modifier = Modifier.clickable { isRegisterMode = !isRegisterMode },
            color = MaterialTheme.colorScheme.secondary
        )
    }
}