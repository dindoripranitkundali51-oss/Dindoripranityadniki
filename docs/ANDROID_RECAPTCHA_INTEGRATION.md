# Android reCAPTCHA Integration Guide

This guide explains how to integrate reCAPTCHA v3 verification in your Android app.

## Overview

This guide covers:
- Adding reCAPTCHA to your Android app
- Getting verification tokens
- Sending tokens to Firebase Functions
- Handling verification results

## Prerequisites

1. Google reCAPTCHA v3 Site Key (for Android)
2. Firebase Functions with reCAPTCHA verification set up
3. Android SDK (API 23+)
4. Google Play Services (SafetyNet API)

## Step 1: Add Dependencies

Add the SafetyNet API dependency to your app/build.gradle.kts:

kotlin
dependencies {
 // SafetyNet API for reCAPTCHA
 implementation("com.google.android.gms:play-services-safetynet:18.0.1")
 
 // Firebase Functions (if not already added)
 implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
 implementation("com.google.firebase:firebase-functions")
}


## Step 2: Create a reCAPTCHA Helper Class

Create a new file RecaptchaHelper.kt:

kotlin
package com.example.dindoripranityadnyiki.core.util

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Helper class for reCAPTCHA v3 verification
 */
class RecaptchaHelper(private val context: Context) {
 
 companion object {
 // Replace with your reCAPTCHA v3 Site Key
 private const val RECAPTCHA_SITE_KEY = "YOUR_ANDROID_SITE_KEY"
 
 // Default action names
 const val ACTION_LOGIN = "login"
 const val ACTION_REGISTER = "register"
 const val ACTION_BOOKING = "booking"
 const val ACTION_PAYMENT = "payment"
 const val ACTION_SENSITIVE = "sensitive_operation"
 }
 
 /**
 * Execute reCAPTCHA verification and get token
 * @param action The action name (e.g., "login", "register")
 * @return reCAPTCHA token string
 * @throws Exception if verification fails
 */
 suspend fun verifyWithRecaptcha(action: String = ACTION_LOGIN): String = 
 suspendCancellableCoroutine { continuation ->
 try {
 val client = SafetyNet.getClient(context)
 
 client.verifyWithRecaptcha(RECAPTCHA_SITE_KEY)
 .addOnSuccessListener { response ->
 if (continuation.isActive) {
 val token = response.tokenResult
 if (token.isNullOrEmpty()) {
 continuation.resumeWithException(
 IllegalStateException("reCAPTCHA token is empty")
 )
 } else {
 continuation.resume(token)
 }
 }
 }
 .addOnFailureListener { error ->
 if (continuation.isActive) {
 val exception = if (error is ApiException) {
 IllegalStateException(
 "reCAPTCHA verification failed: ${error.statusCode}",
 error
 )
 } else {
 IllegalStateException(
 "reCAPTCHA verification failed: ${error.message}",
 error
 )
 }
 continuation.resumeWithException(exception)
 }
 }
 } catch (error: Exception) {
 if (continuation.isActive) {
 continuation.resumeWithException(
 IllegalStateException("Failed to start reCAPTCHA verification", error)
 )
 }
 }
 }
 
 /**
 * Verify reCAPTCHA token with Firebase Functions
 * @param token The reCAPTCHA token from client
 * @param firebaseFunctions Firebase Functions instance
 * @return Verification result with score
 */
 suspend fun verifyTokenWithBackend(
 token: String,
 firebaseFunctions: com.google.firebase.functions.FirebaseFunctions
 ): RecaptchaVerificationResult {
 return suspendCancellableCoroutine { continuation ->
 val data = hashMapOf(
 "recaptchaToken" to token,
 )
 
 firebaseFunctions
 .getHttpsCallable("verifyRecaptcha")
 .call(data)
 .addOnSuccessListener { result ->
 if (continuation.isActive) {
 val response = result.data as? Map<*, *>
 if (response != null) {
 val verified = response["verified"] as? Boolean ?: false
 val score = (response["score"] as? Number)?.toDouble() ?: 0.0
 val timestamp = response["timestamp"] as? String ?: ""
 val hostname = response["hostname"] as? String ?: ""
 
 continuation.resume(
 RecaptchaVerificationResult(
 verified = verified,
 score = score,
 timestamp = timestamp,
 hostname = hostname
 )
 )
 } else {
 continuation.resumeWithException(
 IllegalStateException("Invalid response from server")
 )
 }
 }
 }
 .addOnFailureListener { error ->
 if (continuation.isActive) {
 continuation.resumeWithException(
 IllegalStateException("Backend verification failed: ${error.message}", error)
 )
 }
 }
 }
 }
 
 /**
 * Protected operation with reCAPTCHA verification
 * Use this for sensitive operations that require reCAPTCHA
 */
 suspend fun executeProtectedOperation(
 operationName: String,
 data: Map<String, Any>,
 firebaseFunctions: com.google.firebase.functions.FirebaseFunctions
 ): Map<String, Any> {
 return suspendCancellableCoroutine { continuation ->
 // First, get reCAPTCHA token
 try {
 val token = verifyWithRecaptcha(operationName)
 
 // Then, execute the protected operation
 val requestData = hashMapOf<String, Any>(
 "recaptchaToken" to token,
 "operation" to operationName,
 )
 requestData.putAll(data)
 
 firebaseFunctions
 .getHttpsCallable("sensitiveOperation")
 .call(requestData)
 .addOnSuccessListener { result ->
 if (continuation.isActive) {
 val response = result.data as? Map<*, *>
 if (response != null) {
 @Suppress("UNCHECKED_CAST")
 continuation.resume(response as Map<String, Any>)
 } else {
 continuation.resumeWithException(
 IllegalStateException("Invalid response from server")
 )
 }
 }
 }
 .addOnFailureListener { error ->
 if (continuation.isActive) {
 continuation.resumeWithException(error)
 }
 }
 } catch (error: Exception) {
 if (continuation.isActive) {
 continuation.resumeWithException(error)
 }
 }
 }
 }
}

/**
 * Result of reCAPTCHA verification
 */
data class RecaptchaVerificationResult(
 val verified: Boolean,
 val score: Double,
 val timestamp: String,
 val hostname: String
)


## Step 3: Use in ViewModel

Example usage in a ViewModel:

kotlin
package com.example.dindoripranityadnyiki.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.core.util.RecaptchaHelper
import com.example.dindoripranityadnyiki.core.util.RecaptchaVerificationResult
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserLoginViewModel(
 private val recaptchaHelper: RecaptchaHelper,
 private val firebaseFunctions: FirebaseFunctions
) : ViewModel() {
 
 private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)
 val verificationState: StateFlow<VerificationState> = _verificationState
 
 fun loginWithRecaptcha(email: String, password: String) {
 viewModelScope.launch {
 try {
 _verificationState.value = VerificationState.Loading
 
 // Get reCAPTCHA token
 val token = recaptchaHelper.verifyWithRecaptcha(RecaptchaHelper.ACTION_LOGIN)
 
 // Verify token with backend
 val result = recaptchaHelper.verifyTokenWithBackend(token, firebaseFunctions)
 
 if (result.verified && result.score >= 0.5) {
 // Proceed with login
 _verificationState.value = VerificationState.Success(result)
 // Your login logic here
 } else {
 _verificationState.value = VerificationState.Error(
 "reCAPTCHA verification failed. Score: ${result.score}"
 )
 }
 } catch (error: Exception) {
 _verificationState.value = VerificationState.Error(
 error.message ?: "Verification failed"
 )
 }
 }
 }
 
 fun performSensitiveOperation(operationData: Map<String, Any>) {
 viewModelScope.launch {
 try {
 _verificationState.value = VerificationState.Loading
 
 val result = recaptchaHelper.executeProtectedOperation(
 operationName = "delete_account",
 data = operationData,
 firebaseFunctions = firebaseFunctions
 )
 
 _verificationState.value = VerificationState.Success(
 RecaptchaVerificationResult(
 verified = true,
 score = 1.0,
 timestamp = result["timestamp"] as? String ?: "",
 hostname = ""
 )
 )
 } catch (error: Exception) {
 _verificationState.value = VerificationState.Error(
 error.message ?: "Operation failed"
 )
 }
 }
 }
}

sealed class VerificationState {
 object Idle : VerificationState()
 object Loading : VerificationState()
 data class Success(val result: RecaptchaVerificationResult) : VerificationState()
 data class Error(val message: String) : VerificationState()
}


## Step 4: Use in Compose UI

Example usage in Jetpack Compose:

kotlin
@Composable
fun LoginScreen(
 viewModel: UserLoginViewModel,
 onLoginSuccess: () -> Unit
) {
 val verificationState by viewModel.verificationState.collectAsState()
 var email by remember { mutableStateOf("") }
 var password by remember { mutableStateOf("") }
 
 Column(
 modifier = Modifier
 .fillMaxSize()
 .padding(16.dp),
 horizontalAlignment = Alignment.CenterHorizontally,
 verticalArrangement = Arrangement.Center
 ) {
 Text(
 text = "Login",
 fontSize = 24.sp,
 fontWeight = FontWeight.Bold
 )
 
 Spacer(modifier = Modifier.height(16.dp))
 
 OutlinedTextField(
 value = email,
 onValueChange = { email = it },
 label = { Text("Email") },
 modifier = Modifier.fillMaxWidth()
 )
 
 Spacer(modifier = Modifier.height(8.dp))
 
 OutlinedTextField(
 value = password,
 onValueChange = { password = it },
 label = { Text("Password") },
 visualTransformation = PasswordVisualTransformation(),
 modifier = Modifier.fillMaxWidth()
 )
 
 Spacer(modifier = Modifier.height(16.dp))
 
 when (verificationState) {
 is VerificationState.Loading -> {
 CircularProgressIndicator()
 }
 is VerificationState.Error -> {
 Text(
 text = verificationState.message,
 color = MaterialTheme.colorScheme.error
 )
 }
 is VerificationState.Success -> {
 LaunchedEffect(Unit) {
 onLoginSuccess()
 }
 }
 else -> { /* Idle state */ }
 }
 
 Button(
 onClick = {
 viewModel.loginWithRecaptcha(email, password)
 },
 enabled = verificationState !is VerificationState.Loading,
 modifier = Modifier.fillMaxWidth()
 ) {
 Text("Login")
 }
 }
}


## Step 5: Initialize in Activity/Fragment

kotlin
class MainActivity : ComponentActivity() {
 private lateinit var recaptchaHelper: RecaptchaHelper
 private lateinit var firebaseFunctions: FirebaseFunctions
 
 override fun onCreate(savedInstanceState: Bundle?) {
 super.onCreate(savedInstanceState)
 
 // Initialize helpers
 recaptchaHelper = RecaptchaHelper(this)
 firebaseFunctions = FirebaseFunctions.getInstance()
 
 setContent {
 // Use the ViewModel with reCAPTCHA
 val viewModel: UserLoginViewModel = viewModel(
 factory = object : ViewModelProvider.Factory {
 override fun <T : ViewModel> create(modelClass: Class<T>): T {
 return UserLoginViewModel(recaptchaHelper, firebaseFunctions) as T
 }
 }
 )
 
 LoginScreen(
 viewModel = viewModel,
 onLoginSuccess = {
 // Navigate to home screen
 }
 )
 }
 }
}


## Step 6: Error Handling

Implement comprehensive error handling:

kotlin
sealed class RecaptchaError {
 object TokenGenerationFailed : RecaptchaError()
 object TokenEmpty : RecaptchaError()
 object VerificationFailed : RecaptchaError()
 object ScoreTooLow : RecaptchaError()
 object NetworkError : RecaptchaError()
 data class Unknown(val message: String) : RecaptchaError()
}

fun Throwable.toRecaptchaError(): RecaptchaError {
 return when {
 this.message?.contains("token is empty") == true -> RecaptchaError.TokenEmpty
 this.message?.contains("score") == true -> RecaptchaError.ScoreTooLow
 this is java.net.UnknownHostException -> RecaptchaError.NetworkError
 this is java.net.SocketTimeoutException -> RecaptchaError.NetworkError
 else -> RecaptchaError.Unknown(this.message ?: "Unknown error")
 }
}


## Best Practices

1. Use different actions for different operations (login, register, booking, payment)
2. Cache tokens for a short time to avoid repeated verification
3. Check scores and handle low scores gracefully
4. Combine with Firebase App Check for additional security
5. Log verification attempts for debugging and monitoring
6. Handle network failures with retry logic
7. Show user-friendly messages for verification failures

## Troubleshooting

### Issue: "Google Play services not available"

Ensure Google Play services are updated on the device.

### Issue: "reCAPTCHA site key is invalid"

Verify that:
- The site key is correct
- The site key is for Android (not web)
- The app package name matches the registered package

### Issue: Verification always fails

Check:
- Firebase Functions are deployed
- RECAPTCHA_SECRET_KEY is set in Firebase Secrets
- Network connectivity is available

## Testing

To test reCAPTCHA integration:

1. Emulator Mode: Run Firebase Functions in emulator to bypass verification
2. Test Keys: Use test site keys for development
3. Mock SafetyNet: Use Android test instrumentation

kotlin
// Example test
@Test
fun testRecaptchaVerification() = runTest {
 val mockHelper = mockk<RecaptchaHelper>()
 coEvery { mockHelper.verifyWithRecaptcha(any()) } returns "mock-token"
 
 val viewModel = UserLoginViewModel(mockHelper, mockk())
 viewModel.loginWithRecaptcha("test@example.com", "password")
 
 // Verify the flow
 assertEquals(VerificationState.Success::class, viewModel.verificationState.value::class)
}


## Security Considerations

1. Never log tokens - They are sensitive
2. Validate on server - Never trust client-side verification alone
3. Use HTTPS - Ensure all communications are encrypted
4. Rate limit - Prevent abuse of verification endpoints
5. Combine with other security measures (App Check, authentication)

## Additional Resources

- SafetyNet API Documentation
- reCAPTCHA for Android
- Firebase Functions Documentation

---

Last Updated: July 2026
Version: 1.0.0
