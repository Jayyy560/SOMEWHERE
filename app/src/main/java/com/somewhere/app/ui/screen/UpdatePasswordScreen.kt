package com.somewhere.app.ui.screen

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.somewhere.app.ui.theme.SomewhereColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatePasswordScreen(
    onPasswordUpdated: () -> Unit,
    onSignOut: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val ambient = com.somewhere.app.ui.theme.LocalAmbientColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update Required") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SomewhereColors.Background,
                    titleContentColor = SomewhereColors.TextPrimary
                )
            )
        },
        containerColor = SomewhereColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "For your security, we require you to update your password to a stronger one.",
                color = SomewhereColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("New Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage ?: "",
                    color = SomewhereColors.Error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (password.length < 8 || password.length > 64) {
                        errorMessage = "Password must be 8-64 characters"
                        return@Button
                    }
                    if (!password.any { it.isUpperCase() } || !password.any { it.isLowerCase() } ||
                        !password.any { it.isDigit() } || !password.any { !it.isLetterOrDigit() }) {
                        errorMessage = "Password must contain uppercase, lowercase, number, and symbol"
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match"
                        return@Button
                    }

                    errorMessage = null
                    isLoading = true

                    coroutineScope.launch {
                        try {
                            com.somewhere.app.data.remote.SupabaseManager.updatePassword(password)
                            onPasswordUpdated()
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Failed to update password"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ambient.pulseColor),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SomewhereColors.Background)
                } else {
                    Text("Update Password", color = SomewhereColors.Background)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onSignOut) {
                Text("Sign out instead", color = SomewhereColors.TextMuted)
            }
        }
    }
}
