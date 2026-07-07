# reCAPTCHA v3 Setup Guide

This guide explains how to set up and use reCAPTCHA v3 verification in your Firebase Functions.

## Overview

The reCAPTCHA v3 verification system provides:
- Token verification with Google's reCAPTCHA API
- Score-based risk assessment (0.0 to 1.0)
- Integration with Firebase Functions callable functions
- Support for emulator mode (bypass for testing)
- Optional and required verification modes
- Remote IP tracking for better verification

## Prerequisites

1. Google reCAPTCHA v3 Site Key and Secret Key
 - Go to Google reCAPTCHA Admin Console
 - Register your site (both web and Android apps)
 - Get your Site Key (for client) and Secret Key (for server)

2. Firebase Project
 - Firebase Functions already set up
 - Firebase Secrets Manager available

## Firebase Secret Setup

### 1. Add the reCAPTCHA Secret to Firebase

Run the following command to add your reCAPTCHA secret key:

bash
# Set the secret in Firebase
firebase functions:secrets:set RECAPTCHA_SECRET_KEY


You'll be prompted to enter the secret value. Paste your reCAPTCHA v3 Secret Key.

### 2. Verify the secret is set

bash
firebase functions:secrets:access RECAPTCHA_SECRET_KEY


## Client-Side Integration

### For Web Applications

Add reCAPTCHA v3 to your web admin panel:

html
<!-- Add this to your HTML head -->
<script src="https://www.google.com/recaptcha/api.js?render=YOUR_SITE_KEY"></script>

<!-- In your JavaScript -->
<script>
// Execute reCAPTCHA and get token
function executeRecaptcha(action) {
 return new Promise((resolve, reject) => {
 grecaptcha.ready(function() {
 grecaptcha.execute('YOUR_SITE_KEY', {action: action})
 .then(function(token) {
 resolve(token);
 })
 .catch(reject);
 });
 });
}

// Example: Verify before sending request
document.getElementById('submitButton').addEventListener('click', async function(e) {
 e.preventDefault();
 
 try {
 // Get reCAPTCHA token
 const token = await executeRecaptcha('login');
 
 // Include token in your request
 const response = await fetch('/api/your-function', {
 method: 'POST',
 body: JSON.stringify({
 recaptchaToken: token,
 // Your other data
 email: 'user@example.com',
 }),
 });
 
 const data = await response.json();
 console.log('Success:', data);
 } catch (error) {
 console.error('Error:', error);
 }
});
</script>


### For Android Applications

Add reCAPTCHA to your Android app:

kotlin
// Add dependency in build.gradle.kts
implementation("com.google.android.gms:play-services-safetynet:18.0.1")

// In your Kotlin code
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.gms.tasks.OnSuccessListener

fun verifyWithRecaptcha() {
 SafetyNet.getClient(this).verifyWithRecaptcha("YOUR_SITE_KEY")
 .addOnSuccessListener { response ->
 val token = response.tokenResult
 // Send this token to your Firebase Function
 sendTokenToFirebase(token)
 }
 .addOnFailureListener { error ->
 // Handle error
 Log.e("reCAPTCHA", "Verification failed", error)
 }
}

fun sendTokenToFirebase(token: String) {
 val functions = FirebaseFunctions.getInstance()
 val data = hashMapOf(
 "recaptchaToken" to token,
 // Your other data
 "userId" to userId,
 )
 
 functions.getHttpsCallable("verifyRecaptcha")
 .call(data)
 .addOnSuccessListener { result ->
 // Token verified successfully
 val verified = result.data as Map<*, *>
 Log.d("reCAPTCHA", "Verified with score: ${verified["score"]}")
 }
 .addOnFailureListener { error ->
 // Verification failed
 Log.e("reCAPTCHA", "Verification failed", error)
 }
}


## Firebase Functions Usage

### 1. Standalone Verification Function

Call the verifyRecaptcha function directly:

javascript
// Client-side (Web)
const verifyRecaptcha = firebase.functions().httpsCallable('verifyRecaptcha');

verifyRecaptcha({ recaptchaToken: token })
 .then((result) => {
 console.log('Verified with score:', result.data.score);
 })
 .catch((error) => {
 console.error('Verification failed:', error);
 });


### 2. Protected Verification (Requires Authentication)

javascript
const verifyRecaptchaProtected = firebase.functions().httpsCallable('verifyRecaptchaProtected');

verifyRecaptchaProtected({ recaptchaToken: token })
 .then((result) => {
 console.log('Verified for user:', result.data.userId);
 });


### 3. Using reCAPTCHA as Middleware

Protect sensitive operations with reCAPTCHA verification:

javascript
// This function requires reCAPTCHA verification before executing
const sensitiveOperation = firebase.functions().httpsCallable('sensitiveOperation');

const data = {
 recaptchaToken: token,
 operation: 'deleteUser',
 userId: 'user123',
};

sensitiveOperation(data)
 .then((result) => {
 console.log('Operation completed:', result.data);
 })
 .catch((error) => {
 console.error('Failed:', error);
 });


### 4. Optional Verification

javascript
// reCAPTCHA is optional but will be verified if provided
const optionalOperation = firebase.functions().httpsCallable('optionalOperation');

// With reCAPTCHA
optionalOperation({ recaptchaToken: token, data: '...' });

// Without reCAPTCHA (will still work)
optionalOperation({ data: '...' });


## Server-Side Configuration

### Customizing Score Threshold

The default score threshold is 0.5. You can customize it per function:

javascript
// In routes/recaptchaRoutes.js
const protectedFunction = callable(
 withRecaptchaVerification(
 handler,
 {
 scoreThreshold: 0.7, // Higher threshold for sensitive operations
 tokenField: 'recaptchaToken',
 required: true,
 }
 )
);


### Emulator Mode

In emulator mode, reCAPTCHA verification is bypassed automatically:

bash
# Run functions in emulator
firebase emulators:start


You can disable emulator bypass by setting ENABLE_EMULATOR_BYPASS: false in lib/recaptcha.js.

## Error Handling

### Common Error Codes

| Error Code | Description |
|------------|-------------|
| INVALID_ARGUMENT | Missing or invalid token |
| PERMISSION_DENIED | Token verification failed or score below threshold |
| DEADLINE_EXCEEDED | Verification timed out |
| UNAVAILABLE | Google reCAPTCHA API unavailable |
| INTERNAL | Unexpected server error |

### Client Error Handling

javascript
try {
 const result = await verifyRecaptcha({ recaptchaToken: token });
 // Handle success
} catch (error) {
 if (error.code === 'permission-denied') {
 // Score too low
 console.log('reCAPTCHA score too low');
 } else if (error.code === 'deadline-exceeded') {
 // Timeout
 console.log('Verification timed out');
 } else {
 // Other error
 console.log('Verification failed:', error.message);
 }
}


## Monitoring and Logging

### Firebase Console

Check the Firebase Console for function logs:

bash
firebase functions:log --only verifyRecaptcha


### ReCAPTCHA Admin Console

Monitor verification statistics:

1. Go to Google reCAPTCHA Admin Console
2. Select your site
3. View dashboard for traffic and score distributions

## Best Practices

1. Use reCAPTCHA v3 - It's the latest version with better user experience
2. Set appropriate thresholds - 0.5 is recommended for most cases, 0.7 for sensitive operations
3. Always verify on the server - Never trust client-side verification alone
4. Use Firebase Secrets - Never hardcode secret keys in your code
5. Monitor scores - Track reCAPTCHA scores to detect attacks
6. Handle failures gracefully - Provide user-friendly error messages
7. Test in emulator - Use emulator mode for development and testing
8. Rotate keys periodically - Update reCAPTCHA keys for security

## Troubleshooting

### Issue: "reCAPTCHA token is required"

- Ensure the client is executing reCAPTCHA correctly
- Check that the token field name matches (recaptchaToken by default)

### Issue: "reCAPTCHA score below threshold"

- The user might be a bot
- Consider reducing the threshold for less sensitive operations
- Check the reCAPTCHA admin console for site traffic

### Issue: "reCAPTCHA API returned error status"

- Verify your secret key is correct
- Check that your site is registered with Google reCAPTCHA
- Ensure your site domain is allowed in the reCAPTCHA admin console

### Issue: Verification fails in production

- Check that RECAPTCHA_SECRET_KEY is set in Firebase Secrets
- Verify the secret is accessible by your functions
- Check function logs for detailed error messages

## Security Considerations

1. Secret Management: Never expose the secret key on the client side
2. App Check: Combine with Firebase App Check for additional security
3. Rate Limiting: Implement rate limiting on functions to prevent abuse
4. Logging: Log all verification attempts for audit purposes
5. Threshold Adjustment: Regularly review and adjust score thresholds

## Additional Resources

- Google reCAPTCHA v3 Documentation
- Firebase Functions Secrets
- Firebase App Check
- SafeNet API Documentation

## Support

For issues or questions:
- Firebase Support: https://firebase.google.com/support
- Google reCAPTCHA Support: https://developers.google.com/recaptcha/support
- GitHub Issues: https://github.com/your-repo/issues

---

Last Updated: July 2026
Version: 1.0.0
