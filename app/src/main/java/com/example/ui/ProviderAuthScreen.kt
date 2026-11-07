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
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = androidx.compose.ui.graphics.Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Provider Secure Login", 
                    style = MaterialTheme.typography.headlineMedium, 
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    color = androidx.compose.ui.graphics.Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "StreetRise Coordination Portal",
                    style = MaterialTheme.typography.bodyLarge,
                    color = androidx.compose.ui.graphics.Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", color = androidx.compose.ui.graphics.Color(0xFF94A3B8)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.ui.graphics.Color(0xFF4F46E5),
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color(0xFF334155),
                        focusedTextColor = androidx.compose.ui.graphics.Color.White,
                        unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
                        cursorColor = androidx.compose.ui.graphics.Color(0xFF4F46E5)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email")
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Secure Password", color = androidx.compose.ui.graphics.Color(0xFF94A3B8)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.ui.graphics.Color(0xFF4F46E5),
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color(0xFF334155),
                        focusedTextColor = androidx.compose.ui.graphics.Color.White,
                        unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
                        cursorColor = androidx.compose.ui.graphics.Color(0xFF4F46E5)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password")
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (message.isNotEmpty()) {
                    Text(message, color = androidx.compose.ui.graphics.Color(0xFFF43F5E), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                                    message = e.message ?: "Login failed. Check credentials."
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF4F46E5), contentColor = androidx.compose.ui.graphics.Color.White),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("auth_login_button")
                    ) {
                        Text("Log In", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }

                    OutlinedButton(
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = androidx.compose.ui.graphics.Color(0xFF4F46E5)),
                        border = androidx.compose.foundation.BorderStroke(2.dp, androidx.compose.ui.graphics.Color(0xFF4F46E5)),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("auth_signup_button")
                    ) {
                        Text("Register", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }
        }
    }
}
