package com.shuvo.SmartDrive.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import com.shuvo.SmartDrive.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shuvo.SmartDrive.ui.theme.AppleIndigo
import com.shuvo.SmartDrive.ui.theme.AppleTeal
import com.shuvo.SmartDrive.ui.theme.AppleTealGlow
import com.shuvo.SmartDrive.ui.theme.AppleTealSoft
import com.shuvo.SmartDrive.ui.theme.DayNightSwitchButton
import com.shuvo.SmartDrive.ui.theme.OceanHeaderBottom
import com.shuvo.SmartDrive.ui.theme.OceanHeaderMid
import com.shuvo.SmartDrive.ui.theme.OceanHeaderTop
import com.shuvo.SmartDrive.ui.theme.StatusDanger
import com.shuvo.SmartDrive.ui.theme.appColors
import com.shuvo.SmartDrive.ui.viewmodel.MainStorageViewModel

@Composable
fun AuthScreen(
    viewModel: MainStorageViewModel,
    onAuthSuccess: () -> Unit,
    onGoogleSignInClicked: () -> Unit,
    onBypassLoginClicked: () -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading by viewModel.authLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()
    val colors = MaterialTheme.appColors
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top Oceanic Gradient Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(OceanHeaderTop, OceanHeaderMid, OceanHeaderBottom)
                    )
                )
        ) {
            // Day/Night switch button in top-right corner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, end = 20.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                DayNightSwitchButton(
                    isDark = isDark,
                    onToggle = { viewModel.toggleTheme() }
                )
            }
        }

        // Floating Auth Card
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, bottom = 30.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(colors.cardBorder, colors.cardBorder)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Logo
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "SmartDrive",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        letterSpacing = (-0.4).sp
                    )

                    Text(
                        text = "75 GB Unified Cloud Storage",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Error message banner
                    AnimatedVisibility(visible = authError != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = StatusDanger.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = authError ?: "",
                                color = StatusDanger,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Name Field (Only on Sign Up)
                    AnimatedVisibility(visible = isSignUp) {
                        Column {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name", color = colors.textSecondary) },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = "Name", tint = colors.textTertiary)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary,
                                    focusedBorderColor = AppleTeal,
                                    unfocusedBorderColor = colors.cardBorder,
                                    focusedContainerColor = colors.background,
                                    unfocusedContainerColor = colors.background
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (authError != null) viewModel.clearAuthError()
                        },
                        label = { Text("Email Address", color = colors.textSecondary) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = colors.textTertiary)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = AppleTeal,
                            unfocusedBorderColor = colors.cardBorder,
                            focusedContainerColor = colors.background,
                            unfocusedContainerColor = colors.background
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (authError != null) viewModel.clearAuthError()
                        },
                        label = { Text("Password", color = colors.textSecondary) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Password", tint = colors.textTertiary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = colors.textTertiary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            if (isSignUp) {
                                viewModel.signUpWithEmail(name, email, password, onAuthSuccess)
                            } else {
                                viewModel.signInWithEmail(email, password, onAuthSuccess)
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = AppleTeal,
                            unfocusedBorderColor = colors.cardBorder,
                            focusedContainerColor = colors.background,
                            unfocusedContainerColor = colors.background
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Action Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (isSignUp) {
                                viewModel.signUpWithEmail(name, email, password, onAuthSuccess)
                            } else {
                                viewModel.signInWithEmail(email, password, onAuthSuccess)
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppleTeal)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = OceanHeaderTop,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = if (isSignUp) "Create Account" else "Log In",
                                color = OceanHeaderTop,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.divider)
                        Text(
                            text = "OR",
                            color = colors.textTertiary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.divider)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // One-Tap Google Sign-In Button
                    OutlinedButton(
                        onClick = onGoogleSignInClicked,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.background),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = !isLoading).copy(brush = SolidColor(colors.cardBorder))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "G",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = AppleTeal
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continue with Google",
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Toggle Login / Register
                    TextButton(
                        onClick = {
                            isSignUp = !isSignUp
                            viewModel.clearAuthError()
                        }
                    ) {
                        Text(
                            text = if (isSignUp) "Already have an account? Log In" else "Need an account? Register",
                            color = AppleTeal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = colors.divider.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Testing Mode Bypass Button
                    OutlinedButton(
                        onClick = {
                            viewModel.bypassLoginForTesting(onBypassLoginClicked)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = AppleTealSoft),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(AppleTeal.copy(alpha = 0.5f)))
                    ) {
                        Text(
                            text = "⚡ Bypass Login (Testing Mode)",
                            color = AppleTeal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
