package com.campusconnect.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campusconnect.app.ui.theme.*

/**
 * Glassmorphic text field — CampusConnect dark aesthetic.
 */
@Composable
fun CampusTextField(
    value:           String,
    onValueChange:   (String) -> Unit,
    label:           String,
    modifier:        Modifier = Modifier,
    leadingIcon:     ImageVector? = null,
    isPassword:      Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError:         Boolean = false,
    errorMessage:    String? = null,
    enabled:         Boolean = true,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var isFocused       by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue   = when {
            isError   -> ErrorRed
            isFocused -> ElectricPurple
            else      -> GlassBorder
        },
        animationSpec = tween(200),
        label         = "fieldBorder",
    )
    val bgColor by animateColorAsState(
        targetValue   = if (isFocused) SurfaceDark else ElevatedSurface,
        animationSpec = tween(200),
        label         = "fieldBg",
    )

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ChipShape)
                .background(bgColor)
                .border(1.dp, borderColor, ChipShape),
        ) {
            TextField(
                value         = value,
                onValueChange = onValueChange,
                modifier      = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                placeholder   = {
                    Text(label, color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
                },
                leadingIcon   = leadingIcon?.let { icon ->
                    {
                        Icon(
                            imageVector        = icon,
                            contentDescription = null,
                            tint               = if (isFocused) ElectricPurple else TextTertiary,
                            modifier           = Modifier.size(20.dp),
                        )
                    }
                },
                trailingIcon  = if (isPassword) {
                    {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint               = TextTertiary,
                            )
                        }
                    }
                } else null,
                visualTransformation = if (isPassword && !passwordVisible)
                    PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                colors          = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor  = Color.Transparent,
                    errorContainerColor     = Color.Transparent,
                    focusedTextColor        = TextPrimary,
                    unfocusedTextColor      = TextPrimary,
                    cursorColor             = ElectricPurple,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor  = Color.Transparent,
                    errorIndicatorColor     = Color.Transparent,
                ),
                singleLine = true,
                enabled    = enabled,
            )
        }
        if (isError && errorMessage != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text     = errorMessage,
                style    = MaterialTheme.typography.labelSmall,
                color    = ErrorRed,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

/** Gradient CTA button. */
@Composable
fun PrimaryButton(
    text:      String,
    onClick:   () -> Unit,
    modifier:  Modifier = Modifier,
    isLoading: Boolean  = false,
    enabled:   Boolean  = true,
) {
    Button(
        onClick        = onClick,
        modifier       = modifier.fillMaxWidth().height(54.dp),
        enabled        = enabled && !isLoading,
        shape          = ChipShape,
        colors         = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled && !isLoading)
                        Brush.horizontalGradient(PurpleGradient)
                    else
                        Brush.horizontalGradient(listOf(Color(0xFF3D2A6B), Color(0xFF3D2A6B))),
                    shape = ChipShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text(text, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

/** Secondary outlined button. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick  = onClick,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape    = ChipShape,
        border   = androidx.compose.foundation.BorderStroke(1.dp, ElectricPurple),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = ElectricPurple),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

/** Step dots for multi-step forms. */
@Composable
fun StepIndicator(totalSteps: Int, currentStep: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(totalSteps) { index ->
            val isActive = index == currentStep
            val isPast   = index < currentStep
            val color    = when {
                isActive -> ElectricPurple
                isPast   -> DeepRoyalPurple
                else     -> Color(0xFF3D3D3D)
            }
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (isActive) 28.dp else 8.dp)
                    .clip(ChipShape)
                    .background(
                        if (isActive) Brush.horizontalGradient(PurpleGradient)
                        else Brush.horizontalGradient(listOf(color, color))
                    )
            )
        }
    }
}
