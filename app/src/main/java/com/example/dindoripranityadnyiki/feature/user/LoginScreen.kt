package com.example.dindoripranityadnyiki.feature.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.R
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.data.dataStore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()

    var emailOrMobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    val gradientBrush = Brush.Companion.verticalGradient(
        colors = listOf(Color(0xFFE3F2FD), Color.Companion.White, Color(0xFFBBDEFB))
    )

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(
            modifier = Modifier.Companion
                .fillMaxSize()
                .background(gradientBrush)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                    keyboard?.hide()
                }
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.Companion.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 🔷 App Logo
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.Companion.size(110.dp)
                )

                Spacer(modifier = Modifier.Companion.height(16.dp))

                Text(
                    text = "🙏 सुस्वागत आहे 🙏\nवेद विज्ञान संशोधन विभाग, दिंडोरी",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Companion.Bold,
                    textAlign = TextAlign.Companion.Center,
                    color = Color(0xFF0D47A1)
                )

                Spacer(modifier = Modifier.Companion.height(30.dp))

                // 🔹 Email Field
                OutlinedTextField(
                    value = emailOrMobile,
                    onValueChange = { emailOrMobile = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Companion.Email),
                    modifier = Modifier.Companion.fillMaxWidth()
                )

                Spacer(modifier = Modifier.Companion.height(14.dp))

                // 🔹 Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.Companion.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            val icon =
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            Icon(icon, contentDescription = null)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Companion.Password),
                    modifier = Modifier.Companion.fillMaxWidth()
                )

                Spacer(modifier = Modifier.Companion.height(26.dp))

                // 🔘 Login Button
                Box(
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(32.dp))
                        .background(
                            brush = Brush.Companion.horizontalGradient(
                                colors = listOf(Color(0xFF1976D2), Color(0xFF0D47A1))
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
                        )
                        .clickable(enabled = !isLoading) {
                            focusManager.clearFocus()
                            keyboard?.hide()

                            scope.launch {
                                if (emailOrMobile.isBlank() || password.isBlank()) {
                                    snackbarHostState.showSnackbar("कृपया सर्व माहिती भरा.")
                                    return@launch
                                }

                                isLoading = true

                                auth.signInWithEmailAndPassword(emailOrMobile, password)
                                    .addOnSuccessListener {
                                        scope.launch {
                                            // ✅ Save login state
                                            context.dataStore.edit { prefs ->
                                                prefs[PrefKeys.IS_LOGGED_IN] = true
                                                prefs[PrefKeys.USER_ROLE] = "user"
                                            }

                                            delay(500)
                                            isLoading = false

                                            // ✅ Direct to Pooja Selection after successful login
                                            navController.navigate("poojaSelection") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Login Failed: ${e.localizedMessage}")
                                        }
                                    }
                            }
                        },
                    contentAlignment = Alignment.Companion.Center
                ) {
                    if (isLoading)
                        CircularProgressIndicator(
                            color = Color.Companion.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.Companion.size(22.dp)
                        )
                    else
                        Text(
                            text = "Login →",
                            fontSize = 18.sp,
                            color = Color.Companion.White,
                            fontWeight = FontWeight.Companion.Bold
                        )
                }

                Spacer(modifier = Modifier.Companion.height(20.dp))

                // 🔹 Forgot Password
                Text(
                    text = "Forgot Password?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Companion.SemiBold,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.Companion.clickable {
                        focusManager.clearFocus()
                        keyboard?.hide()
                        if (emailOrMobile.contains("@")) {
                            auth.sendPasswordResetEmail(emailOrMobile)
                                .addOnSuccessListener {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Reset link sent to your email.")
                                    }
                                }
                                .addOnFailureListener { e ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Error: ${e.localizedMessage}")
                                    }
                                }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please enter a valid email to reset password.")
                            }
                        }
                    }
                )
            }
        }
    }
}