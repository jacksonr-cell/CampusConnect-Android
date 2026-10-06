package com.campusconnect.app.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campusconnect.app.ui.components.*
import com.campusconnect.app.ui.theme.*

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess:       () -> Unit,
    onForgotPassword:     () -> Unit = {},
    viewModel: AuthViewModel = viewModel(factory = com.campusconnect.app.di.AppViewModelProvider.Factory),
) {
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    val form       by viewModel.loginForm.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val isLoading  = loginState is AuthState.Loading
    val errorMsg   = (loginState as? AuthState.Error)?.message

    LaunchedEffect(loginState) {
        if (loginState is AuthState.Success) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RichBlack),
    ) {
        // Top purple glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(
                    Brush.radialGradient(
                        colors   = listOf(DeepRoyalPurple.copy(alpha = 0.25f), RichBlack.copy(alpha = 0f)),
                        radius   = 600f,
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(72.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.linearGradient(PurpleGradient),
                        androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("CC", fontSize = 28.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text       = "Welcome back",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = "Sign in to your CampusConnect account",
                style     = MaterialTheme.typography.bodyMedium,
                color     = TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))

            // Error banner
            AnimatedVisibility(visible = errorMsg != null) {
                if (errorMsg != null) {
                    ErrorBanner(message = errorMsg, modifier = Modifier.padding(bottom = 16.dp))
                }
            }

            // Email field
            CampusTextField(
                value          = form.email,
                onValueChange  = {
                    viewModel.onLoginEmailChange(it)
                    viewModel.resetLoginState()
                },
                label          = "University email",
                leadingIcon    = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                isError        = errorMsg != null,
                enabled        = !isLoading,
                modifier       = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            // Password field
            CampusTextField(
                value          = form.password,
                onValueChange  = {
                    viewModel.onLoginPasswordChange(it)
                    viewModel.resetLoginState()
                },
                label          = "Password",
                leadingIcon    = Icons.Outlined.Lock,
                isPassword     = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.login(onLoginSuccess)
                    }
                ),
                isError        = errorMsg != null,
                enabled        = !isLoading,
                modifier       = Modifier.fillMaxWidth(),
            )

            // Forgot password
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text     = "Forgot password?",
                    style    = MaterialTheme.typography.labelMedium,
                    color    = ElectricPurple,
                    modifier = Modifier.clickable(onClick = onForgotPassword),
                )
            }

            Spacer(Modifier.height(32.dp))

            // Sign-in button
            PrimaryButton(
                text      = "Sign In",
                onClick   = { viewModel.login(onLoginSuccess) },
                isLoading = isLoading,
                enabled   = form.email.isNotBlank() && form.password.isNotBlank(),
            )

            Spacer(Modifier.height(20.dp))

            // Divider
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = GlassBorder)
                Text(" or ", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                HorizontalDivider(modifier = Modifier.weight(1f), color = GlassBorder)
            }

            Spacer(Modifier.height(20.dp))

            // Google sign-in (future)
            OutlinedButton(
                onClick  = { /* TODO: Google OAuth */ },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = ChipShape,
                border   = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            ) {
                Icon(Icons.Outlined.Language, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Continue with Google", fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(36.dp))

            // Register link
            Row(horizontalArrangement = Arrangement.Center) {
                Text("Don't have an account? ", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text     = "Sign up",
                    color    = ElectricPurple,
                    style    = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onNavigateToRegister),
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ErrorRed.copy(alpha = 0.12f), ChipShape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
        Text(message, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
    }
}
