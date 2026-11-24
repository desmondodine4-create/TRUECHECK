package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserProfile
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.DividerMint
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedSoft
import com.example.ui.theme.LightMintBackground
import com.example.ui.theme.MainText
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftMint
import com.example.ui.theme.TrueCheckWordmark

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: (UserProfile) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val formState by authViewModel.formState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = PureWhite) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            TrueCheckWordmark(shieldSize = 36.dp)
            Spacer(modifier = Modifier.height(28.dp))

            Column(modifier = Modifier.widthIn(max = 520.dp)) {
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MainText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Sign in to verify products and access your scan history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (formState.errorMessage != null) {
                    AuthBanner(text = formState.errorMessage!!, isError = true)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (formState.infoMessage != null) {
                    AuthBanner(text = formState.infoMessage!!, isError = false)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; authViewModel.clearMessages() },
                    label = { Text("Email address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = trueCheckFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_email_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; authViewModel.clearMessages() },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = trueCheckFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input")
                )

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(
                        onClick = {
                            authViewModel.clearMessages()
                            onNavigateToForgotPassword()
                        },
                        modifier = Modifier.testTag("forgot_password_link")
                    ) {
                        Text("Forgot Password?", color = DeepTeal)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { authViewModel.login(email, password, onLoginSuccess) },
                    enabled = !formState.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal, contentColor = PureWhite),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("login_submit_button")
                ) {
                    if (formState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = PureWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Sign In", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Don't have an account?", color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
                    TextButton(
                        onClick = {
                            authViewModel.clearMessages()
                            onNavigateToRegister()
                        },
                        modifier = Modifier.testTag("go_to_register_button")
                    ) {
                        Text("Create Account", color = DeepTeal, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onRegisterSuccess: (UserProfile) -> Unit,
    onBackToLogin: () -> Unit
) {
    val formState by authViewModel.formState.collectAsStateWithLifecycle()
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = PureWhite) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 520.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackToLogin) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to sign in", tint = MainText)
                    }
                    TrueCheckWordmark(shieldSize = 28.dp)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Create Account", style = MaterialTheme.typography.headlineMedium, color = MainText)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Register a consumer account to track verified products and alerts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (formState.errorMessage != null) {
                    AuthBanner(text = formState.errorMessage!!, isError = true)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; authViewModel.clearMessages() },
                    label = { Text("Full name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Full name") },
                    singleLine = true,
                    colors = trueCheckFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; authViewModel.clearMessages() },
                    label = { Text("Email address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = trueCheckFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_email_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; authViewModel.clearMessages() },
                    label = { Text("Password (min 8 characters)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = trueCheckFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_password_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; authViewModel.clearMessages() },
                    label = { Text("Confirm password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm password") },
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Icon(
                                if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showConfirmPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = trueCheckFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_confirm_password_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        authViewModel.register(
                            fullName = fullName,
                            email = email,
                            password = password,
                            confirmPassword = confirmPassword,
                            onSuccess = onRegisterSuccess
                        )
                    },
                    enabled = !formState.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal, contentColor = PureWhite),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("register_submit_button")
                ) {
                    if (formState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = PureWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create Account", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Already registered?", color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onBackToLogin) {
                        Text("Sign In", color = DeepTeal, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    authViewModel: AuthViewModel,
    onBackToLogin: () -> Unit
) {
    val formState by authViewModel.formState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = PureWhite) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 520.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackToLogin) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MainText)
                    }
                    Text("Password Recovery", style = MaterialTheme.typography.titleLarge, color = MainText)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Reset Your Password", style = MaterialTheme.typography.headlineMedium, color = MainText)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter your registered email address and choose a new password (minimum 8 characters).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (formState.errorMessage != null) {
                    AuthBanner(text = formState.errorMessage!!, isError = true)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (formState.infoMessage != null) {
                    AuthBanner(text = formState.infoMessage!!, isError = false)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; authViewModel.clearMessages() },
                    label = { Text("Registered email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = trueCheckFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_email_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; authViewModel.clearMessages() },
                    label = { Text("New password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "New password") },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    colors = trueCheckFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_new_password_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; authViewModel.clearMessages() },
                    label = { Text("Confirm new password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm password") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    colors = trueCheckFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_confirm_password_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        authViewModel.resetPassword(email, newPassword, confirmPassword) { }
                    },
                    enabled = !formState.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal, contentColor = PureWhite),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("forgot_submit_button")
                ) {
                    Text("Update Password", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun AuthBanner(text: String, isError: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) ErrorRedSoft else SoftMint
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            color = if (isError) ErrorRed else DeepTeal,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
fun trueCheckFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DeepTeal,
    unfocusedBorderColor = DividerMint,
    focusedLabelColor = DeepTeal,
    cursorColor = DeepTeal,
    focusedContainerColor = LightMintBackground.copy(alpha = 0.35f),
    unfocusedContainerColor = PureWhite
)
