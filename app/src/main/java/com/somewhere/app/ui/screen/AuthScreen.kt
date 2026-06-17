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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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

        if (uiState.mode == AuthViewModel.Mode.SIGN_UP) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
        }

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

        if (uiState.mode == AuthViewModel.Mode.SIGN_IN) {
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = viewModel::resetPassword,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Forgot Password?", color = SomewhereColors.TextMuted)
            }
        }

        Spacer(Modifier.height(20.dp))

        var isAgreed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        var activeLegalDoc by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Checkbox(
                checked = isAgreed,
                onCheckedChange = { isAgreed = it },
                colors = androidx.compose.material3.CheckboxDefaults.colors(
                    checkedColor = SomewhereColors.GlowAccent,
                    uncheckedColor = SomewhereColors.TextMuted
                )
            )
            
            val annotatedString = androidx.compose.ui.text.buildAnnotatedString {
                append("I agree to the ")
                pushStringAnnotation("TOS", "Terms of Service")
                withStyle(androidx.compose.ui.text.SpanStyle(color = SomewhereColors.GlowAccent, fontWeight = FontWeight.Bold)) {
                    append("Terms of Service")
                }
                pop()
                append(", ")
                pushStringAnnotation("PRIVACY", "Privacy Policy")
                withStyle(androidx.compose.ui.text.SpanStyle(color = SomewhereColors.GlowAccent, fontWeight = FontWeight.Bold)) {
                    append("Privacy Policy")
                }
                pop()
                append(", and ")
                pushStringAnnotation("COMMUNITY", "Community Guidelines")
                withStyle(androidx.compose.ui.text.SpanStyle(color = SomewhereColors.GlowAccent, fontWeight = FontWeight.Bold)) {
                    append("Community Guidelines")
                }
                pop()
                append(".")
            }

            val context = LocalContext.current
            val openLegalUrl = {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://somewhere-privacy-policy.vercel.app/"))
                context.startActivity(intent)
            }

            ClickableText(
                text = annotatedString,
                onClick = { offset ->
                    annotatedString.getStringAnnotations("TOS", offset, offset).firstOrNull()?.let { openLegalUrl() }
                    annotatedString.getStringAnnotations("PRIVACY", offset, offset).firstOrNull()?.let { openLegalUrl() }
                    annotatedString.getStringAnnotations("COMMUNITY", offset, offset).firstOrNull()?.let { openLegalUrl() }
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = SomewhereColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            )
        }

        Spacer(Modifier.height(16.dp))

        SomewhereButton(
            text = if (uiState.mode == AuthViewModel.Mode.SIGN_IN) "Sign in" else "Sign up",
            onClick = viewModel::submit,
            enabled = !uiState.isLoading && isAgreed,
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
