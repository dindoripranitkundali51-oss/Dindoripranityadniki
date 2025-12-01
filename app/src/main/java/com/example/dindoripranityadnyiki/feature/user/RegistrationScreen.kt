package com.example.dindoripranityadnyiki.feature.user

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
    val backgroundBrush = Brush.Companion.verticalGradient(
        listOf(Color(0xFFF6FBFF), Color(0xFFEFF6FF))
    )

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(
            modifier = Modifier.Companion
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(padding)
                .imePadding(), // respects keyboard
            contentAlignment = Alignment.Companion.TopCenter
        ) {
            // Top spacer + central card container
            Column(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.Companion.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.Companion.height(32.dp))

                // Icon + Title (centered)
                Box(
                    modifier = Modifier.Companion
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color.Companion.White)
                        .shadow(6.dp, CircleShape),
                    contentAlignment = Alignment.Companion.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_registration),
                        contentDescription = "Registration icon",
                        modifier = Modifier.Companion.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.Companion.height(16.dp))

                Text(
                    text = "Create Your Account",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Companion.ExtraBold,
                    color = Color(0xFF0D47A1)
                )
                Text(
                    text = "Join the Dindori Pranit Family",
                    fontSize = 14.sp,
                    color = Color(0xFF546E7A)
                )

                Spacer(modifier = Modifier.Companion.height(20.dp))

                // Centered card: constrain width so it looks good on tablets too
                Card(
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(vertical = 12.dp)
                        .widthIn(max = 560.dp), // max width for large screens
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Companion.White)
                ) {
                    Column(
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .padding(22.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.Companion.CenterHorizontally
                    ) {
                        // Inputs: larger spacing for touch targets
                        RegistrationField("Full Name", fullName) { fullName = it }
                        Spacer(modifier = Modifier.Companion.height(8.dp))

                        RegistrationField("Mobile (10 digits)", mobile.take(10)) {
                            if (it.length <= 10) mobile = it
                        }
                        Spacer(modifier = Modifier.Companion.height(8.dp))

                        RegistrationField("Email", email) { email = it }
                        Spacer(modifier = Modifier.Companion.height(8.dp))

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address") },
                            placeholder = { Text("Eg. 12, MG Road, Near Temple") },
                            modifier = Modifier.Companion
                                .fillMaxWidth(),
                            maxLines = 3,
                            singleLine = false,
                            keyboardOptions = KeyboardOptions.Companion.Default.copy(imeAction = ImeAction.Companion.Next)
                        )
                        Spacer(modifier = Modifier.Companion.height(8.dp))

                        OutlinedTextField(
                            value = pincode,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }.take(6)
                                pincode = filtered
                            },
                            label = { Text("Pincode (6 digits)") },
                            placeholder = { Text("411001") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Companion.Number),
                            modifier = Modifier.Companion.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.Companion.height(8.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.Companion.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Companion.Password,
                                imeAction = ImeAction.Companion.Done
                            ),
                            modifier = Modifier.Companion.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.Companion.height(18.dp))

                        // Primary CTA
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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

                                if (!validateInputs(
                                        nameTrim,
                                        mobileTrim,
                                        emailTrim,
                                        addressTrim,
                                        pincodeTrim,
                                        passwordTrim
                                    )
                                ) {
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
                            modifier = Modifier.Companion
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.Companion.White,
                                    modifier = Modifier.Companion.size(22.dp)
                                )
                            } else {
                                Text(
                                    "Sign Up →",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Companion.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.Companion.height(18.dp))

                // Small footnote
                Text(
                    text = "By signing up you agree to our Terms & Privacy Policy.",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.Companion.padding(horizontal = 12.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.Companion.height(28.dp))
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions.Companion.Default.copy(imeAction = ImeAction.Companion.Next),
        modifier = Modifier.Companion
            .fillMaxWidth()
    )
}