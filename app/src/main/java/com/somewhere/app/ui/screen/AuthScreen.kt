package com.somewhere.app.ui.screen

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.somewhere.app.BuildConfig
import com.somewhere.app.ui.component.SomewhereButton
import com.somewhere.app.ui.theme.SomewhereColors
import com.somewhere.app.util.rememberReduceMotionEnabled
import com.somewhere.app.viewmodel.AuthViewModel
import java.net.URI

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var visible by remember { mutableStateOf(false) }
    val reduceMotion = rememberReduceMotionEnabled()
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 500, easing = EaseOutCubic),
        label = "authFade"
    )

    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SomewhereColors.Background)
            .systemBarsPadding()
            .padding(24.dp)
            .alpha(contentAlpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (uiState.mode == AuthViewModel.Mode.SIGN_IN) "Sign in" else "Create account",
            style = MaterialTheme.typography.headlineMedium
        )

        val host = remember {
            runCatching { URI(BuildConfig.SUPABASE_URL).host ?: BuildConfig.SUPABASE_URL }
                .getOrDefault(BuildConfig.SUPABASE_URL)
        }
        if (host.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Supabase: $host",
                style = MaterialTheme.typography.labelSmall,
                color = SomewhereColors.TextMuted
            )
        }

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChanged,
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChanged,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (!uiState.errorMessage.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = uiState.errorMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = SomewhereColors.Accent
            )
        }

        Spacer(Modifier.height(20.dp))

        SomewhereButton(
            text = if (uiState.mode == AuthViewModel.Mode.SIGN_IN) "Sign in" else "Sign up",
            onClick = viewModel::submit,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        SomewhereButton(
            text = if (uiState.mode == AuthViewModel.Mode.SIGN_IN) {
                "Need an account? Sign up"
            } else {
                "Already have an account? Sign in"
            },
            onClick = viewModel::toggleMode,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
