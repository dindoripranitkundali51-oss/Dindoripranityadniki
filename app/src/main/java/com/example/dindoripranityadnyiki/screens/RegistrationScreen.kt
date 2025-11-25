// File: RegistrationScreen.kt
package com.example.dindoripranityadnyiki.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Patterns
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.R
import androidx.datastore.preferences.core.edit
import com.example.dindoripranityadnyiki.data.PrefKeys
import com.example.dindoripranityadnyiki.data.dataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()

    // UI states
    var fullName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current

    // Background Gradient (subtle)
    val backgroundBrush = Brush.verticalGradient(
        listOf(Color(0xFFF6FBFF), Color(0xFFEFF6FF))
    )

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(padding)
                .imePadding(), // respects keyboard
            contentAlignment = Alignment.TopCenter
        ) {
            // Top spacer + central card container
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Icon + Title (centered)
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .shadow(6.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_registration),
                        contentDescription = "Registration icon",
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Create Your Account",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0D47A1)
                )
                Text(
                    text = "Join the Dindori Pranit Family",
                    fontSize = 14.sp,
                    color = Color(0xFF546E7A)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Centered card: constrain width so it looks good on tablets too
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(vertical = 12.dp)
                        .widthIn(max = 560.dp), // max width for large screens
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Inputs: larger spacing for touch targets
                        RegistrationField("Full Name", fullName) { fullName = it }
                        Spacer(modifier = Modifier.height(8.dp))

                        RegistrationField("Mobile (10 digits)", mobile.take(10)) {
                            if (it.length <= 10) mobile = it
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        RegistrationField("Email", email) { email = it }
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address") },
                            placeholder = { Text("Eg. 12, MG Road, Near Temple") },
                            modifier = Modifier
                                .fillMaxWidth(),
                            maxLines = 3,
                            singleLine = false,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = pincode,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }.take(6)
                                pincode = filtered
                            },
                            label = { Text("Pincode (6 digits)") },
                            placeholder = { Text("411001") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Primary CTA
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                focusManager.clearFocus()
                                keyboard?.hide()

                                val nameTrim = fullName.trim()
                                val mobileTrim = mobile.trim()
                                val emailTrim = email.trim()
                                val passwordTrim = password.trim()
                                val addressTrim = address.trim()
                                val pincodeTrim = pincode.trim()

                                if (!isInternetAvailable(context)) {
                                    scope.launch { snackbarHostState.showSnackbar("No Internet Connection.") }
                                    return@Button
                                }

                                if (!validateInputs(nameTrim, mobileTrim, emailTrim, addressTrim, pincodeTrim, passwordTrim)) {
                                    scope.launch { snackbarHostState.showSnackbar("Please fill all fields correctly.") }
                                    return@Button
                                }

                                isLoading = true

                                auth.createUserWithEmailAndPassword(emailTrim, passwordTrim)
                                    .addOnSuccessListener { result ->
                                        val uid = result.user?.uid ?: ""
                                        saveUserToFirestore(
                                            db,
                                            nameTrim,
                                            mobileTrim,
                                            emailTrim,
                                            addressTrim,
                                            pincodeTrim,
                                            uid,
                                            onSuccess = {
                                                scope.launch {
                                                    context.dataStore.edit {
                                                        it[PrefKeys.IS_REGISTERED] = true
                                                        it[PrefKeys.IS_LOGGED_IN] = false
                                                        it[PrefKeys.USER_ROLE] = "user"
                                                        it[PrefKeys.USER_ADDRESS] = addressTrim
                                                        it[PrefKeys.USER_PINCODE] = pincodeTrim
                                                    }
                                                    delay(700)
                                                    isLoading = false
                                                    navController.navigate("login") {
                                                        popUpTo("Registration") { inclusive = true }
                                                    }
                                                }
                                            },
                                            onError = { e ->
                                                isLoading = false
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Error: ${e.localizedMessage}")
                                                }
                                            }
                                        )
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        scope.launch {
                                            if (e is FirebaseAuthUserCollisionException)
                                                snackbarHostState.showSnackbar("Email already registered.")
                                            else
                                                snackbarHostState.showSnackbar("Auth failed: ${e.localizedMessage}")
                                        }
                                    }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                            } else {
                                Text("Sign Up →", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Small footnote
                Text(
                    text = "By signing up you agree to our Terms & Privacy Policy.",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(horizontal = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

// ----------------- Helpers (unchanged) -----------------
fun validateInputs(name: String, mobile: String, email: String, address: String, pincode: String, password: String): Boolean {
    if (name.isBlank() || mobile.isBlank() || email.isBlank() || address.isBlank() || pincode.isBlank() || password.isBlank()) return false
    if (mobile.length != 10) return false
    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return false
    if (pincode.length != 6) return false
    if (password.length < 6) return false
    return true
}

fun saveUserToFirestore(
    db: FirebaseFirestore,
    name: String,
    mobile: String,
    email: String,
    address: String,
    pincode: String,
    uid: String,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    val userData = hashMapOf(
        "uid" to uid,
        "fullName" to name,
        "mobile" to mobile,
        "email" to email,
        "address" to address,
        "pincode" to pincode,
        "role" to "user",
        "createdAt" to FieldValue.serverTimestamp()
    )
    db.collection("users").document(uid)
        .set(userData)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onError(it) }
}

fun isInternetAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Composable
fun RegistrationField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
        modifier = Modifier
            .fillMaxWidth()
    )
}
