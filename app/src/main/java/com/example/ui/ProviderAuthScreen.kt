package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.SupabaseManager
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

import io.github.jan.supabase.gotrue.SessionStatus

@Composable
fun ProviderAuthScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val sessionStatus by SupabaseManager.client.auth.sessionStatus.collectAsState()
    val isLoggedIn = sessionStatus is SessionStatus.Authenticated
    
    val scope = rememberCoroutineScope()

    if (isLoggedIn) {
        ProviderDashboardScreen(
            onSignOut = {
                scope.launch {
                    SupabaseManager.client.auth.signOut()
                }
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Provider Access", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_email")
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_password")
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (message.isNotEmpty()) {
                Text(message, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        isLoading = true
                        message = ""
                        val currentEmail = email
                        val currentPassword = password
                        scope.launch {
                            try {
                                SupabaseManager.client.auth.signUpWith(Email) {
                                    this.email = currentEmail
                                    this.password = currentPassword
                                }
                                message = "Sign up successful! Please log in."
                            } catch (e: Exception) {
                                message = e.message ?: "Sign up failed"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.testTag("auth_signup_button")
                ) {
                    Text("Sign Up")
                }
                
                Button(
                    onClick = {
                        isLoading = true
                        message = ""
                        val currentEmail = email
                        val currentPassword = password
                        scope.launch {
                            try {
                                SupabaseManager.client.auth.signInWith(Email) {
                                    this.email = currentEmail
                                    this.password = currentPassword
                                }
                                message = "Logged in successfully."
                            } catch (e: Exception) {
                                message = e.message ?: "Login failed"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.testTag("auth_login_button")
                ) {
                    Text("Log In")
                }
            }
        }
    }
}
