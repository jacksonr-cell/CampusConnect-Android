package com.campusconnect.app.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campusconnect.app.ui.components.*
import com.campusconnect.app.ui.theme.*

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = com.campusconnect.app.di.AppViewModelProvider.Factory),
) {
    val registerState by viewModel.registerState.collectAsStateWithLifecycle()
    val registerStep  by viewModel.registerStep.collectAsStateWithLifecycle()
    val form          by viewModel.registerForm.collectAsStateWithLifecycle()
    val focusManager  = LocalFocusManager.current
    val isLoading     = registerState is AuthState.Loading
    val errorMsg      = (registerState as? AuthState.Error)?.message

    LaunchedEffect(registerState) {
        if (registerState is AuthState.Success) onRegisterSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RichBlack),
    ) {
        // Purple glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(DeepRoyalPurple.copy(alpha = 0.2f), RichBlack.copy(alpha = 0f)),
                        radius = 500f,
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
            Spacer(Modifier.height(64.dp))

            // Logo + step indicator
            Row(
                modifier          = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Brush.linearGradient(PurpleGradient),
                            androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("CC", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                }

                StepIndicator(
                    totalSteps  = 2,
                    currentStep = registerStep.ordinal,
                )
            }

            Spacer(Modifier.height(32.dp))

            // Animated title / subtitle swap on step change
            AnimatedContent(
                targetState   = registerStep,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn(tween(250)) togetherWith
                    slideOutHorizontally { -it } + fadeOut(tween(200))
                },
                label = "stepTitle",
            ) { step ->
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = when (step) {
                            RegisterStep.CREDENTIALS -> "Create your account"
                            RegisterStep.PROFILE     -> "Tell us about yourself"
                        },
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = when (step) {
                            RegisterStep.CREDENTIALS -> "Step 1 of 2 — Set up your login credentials"
                            RegisterStep.PROFILE     -> "Step 2 of 2 — Complete your student profile"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Error banner
            AnimatedVisibility(visible = errorMsg != null) {
                if (errorMsg != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ErrorRed.copy(alpha = 0.12f), ChipShape)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Outlined.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                        Text(errorMsg, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Step-specific form fields
            AnimatedContent(
                targetState   = registerStep,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn(tween(250)) togetherWith
                    slideOutHorizontally { -it } + fadeOut(tween(200))
                },
                label = "stepForm",
            ) { step ->
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (step) {
                        RegisterStep.CREDENTIALS -> CredentialsStep(
                            form         = form,
                            onChange     = { viewModel.onRegisterFieldChange(it) },
                            onClearError = { viewModel.resetRegisterState() },
                            focusManager = focusManager,
                            isLoading    = isLoading,
                            errorMsg     = errorMsg,
                        )
                        RegisterStep.PROFILE -> ProfileStep(
                            form         = form,
                            onChange     = { viewModel.onRegisterFieldChange(it) },
                            onClearError = { viewModel.resetRegisterState() },
                            focusManager = focusManager,
                            isLoading    = isLoading,
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Action buttons
            when (registerStep) {
                RegisterStep.CREDENTIALS -> {
                    PrimaryButton(
                        text    = "Continue",
                        onClick = { viewModel.validateAndAdvance() },
                        enabled = form.email.isNotBlank() && form.username.isNotBlank()
                                && form.password.isNotBlank() && form.confirmPassword.isNotBlank(),
                    )
                }
                RegisterStep.PROFILE -> {
                    PrimaryButton(
                        text      = "Create Account",
                        onClick   = { viewModel.register(onRegisterSuccess) },
                        isLoading = isLoading,
                        enabled   = form.firstName.isNotBlank() && form.lastName.isNotBlank(),
                    )
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(
                        text    = "Back",
                        onClick = { viewModel.goBackToCredentials() },
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Login link
            Row(horizontalArrangement = Arrangement.Center) {
                Text("Already have an account? ", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text       = "Sign in",
                    color      = ElectricPurple,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.clickable(onClick = onNavigateToLogin),
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Step 1: Credentials ────────────────────────────────────────────────────

@Composable
private fun CredentialsStep(
    form:         RegisterForm,
    onChange:     (RegisterForm.() -> RegisterForm) -> Unit,
    onClearError: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    isLoading:    Boolean,
    errorMsg:     String?,
) {
    CampusTextField(
        value          = form.email,
        onValueChange  = { v -> onChange { copy(email = v) }; onClearError() },
        label          = "University email",
        leadingIcon    = Icons.Outlined.Email,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        isError        = errorMsg != null && form.email.isBlank(),
        enabled        = !isLoading,
        modifier       = Modifier.fillMaxWidth(),
    )
    CampusTextField(
        value          = form.username,
        onValueChange  = { v -> onChange { copy(username = v) }; onClearError() },
        label          = "Username (e.g. janedoe)",
        leadingIcon    = Icons.Outlined.AlternateEmail,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        enabled        = !isLoading,
        modifier       = Modifier.fillMaxWidth(),
    )
    CampusTextField(
        value          = form.password,
        onValueChange  = { v -> onChange { copy(password = v) }; onClearError() },
        label          = "Password (min. 8 characters)",
        leadingIcon    = Icons.Outlined.Lock,
        isPassword     = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        enabled        = !isLoading,
        modifier       = Modifier.fillMaxWidth(),
    )
    CampusTextField(
        value          = form.confirmPassword,
        onValueChange  = { v -> onChange { copy(confirmPassword = v) }; onClearError() },
        label          = "Confirm password",
        leadingIcon    = Icons.Outlined.LockReset,
        isPassword     = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        isError        = form.password.isNotBlank() && form.confirmPassword.isNotBlank() && form.password != form.confirmPassword,
        errorMessage   = if (form.password != form.confirmPassword && form.confirmPassword.isNotBlank()) "Passwords don't match" else null,
        enabled        = !isLoading,
        modifier       = Modifier.fillMaxWidth(),
    )
    // Password strength hint
    if (form.password.isNotBlank()) {
        PasswordStrengthIndicator(password = form.password)
    }
}

// ── Step 2: Profile ────────────────────────────────────────────────────────

@Composable
private fun ProfileStep(
    form:         RegisterForm,
    onChange:     (RegisterForm.() -> RegisterForm) -> Unit,
    onClearError: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    isLoading:    Boolean,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CampusTextField(
            value          = form.firstName,
            onValueChange  = { v -> onChange { copy(firstName = v) }; onClearError() },
            label          = "First name",
            leadingIcon    = Icons.Outlined.Person,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            enabled        = !isLoading,
            modifier       = Modifier.weight(1f),
        )
        CampusTextField(
            value          = form.lastName,
            onValueChange  = { v -> onChange { copy(lastName = v) }; onClearError() },
            label          = "Last name",
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            enabled        = !isLoading,
            modifier       = Modifier.weight(1f),
        )
    }
    CampusTextField(
        value          = form.university,
        onValueChange  = { v -> onChange { copy(university = v) } },
        label          = "University (e.g. Kabarak University)",
        leadingIcon    = Icons.Outlined.School,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        enabled        = !isLoading,
        modifier       = Modifier.fillMaxWidth(),
    )

    // Role selector
    Text("I am a:", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("student" to "Student", "landlord" to "Landlord", "club_leader" to "Club Leader").forEach { (value, label) ->
            val selected = form.role == value
            FilterChip(
                selected = selected,
                onClick  = { onChange { copy(role = value) } },
                label    = { Text(label, style = MaterialTheme.typography.labelMedium) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DeepRoyalPurple.copy(alpha = 0.3f),
                    selectedLabelColor     = ElectricPurple,
                    containerColor         = ElevatedSurface,
                    labelColor             = TextSecondary,
                ),
                border   = FilterChipDefaults.filterChipBorder(
                    enabled         = true,
                    selected        = selected,
                    borderColor     = GlassBorder,
                    selectedBorderColor = ElectricPurple,
                ),
            )
        }
    }
}

// ── Password strength meter ────────────────────────────────────────────────

@Composable
private fun PasswordStrengthIndicator(password: String) {
    val strength = when {
        password.length >= 12 && password.any { it.isUpperCase() } && password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() } -> 4
        password.length >= 10 && password.any { it.isUpperCase() } && password.any { it.isDigit() } -> 3
        password.length >= 8 && (password.any { it.isUpperCase() } || password.any { it.isDigit() }) -> 2
        password.length >= 6 -> 1
        else -> 0
    }
    val (label, color) = when (strength) {
        4    -> "Very strong" to SuccessGreen
        3    -> "Strong"      to OnlineGreen
        2    -> "Fair"        to WarningAmber
        1    -> "Weak"        to WowOrange
        else -> "Too short"   to ErrorRed
    }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (index < strength) color else Color(0xFF3D3D3D),
                            ChipShape,
                        )
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
