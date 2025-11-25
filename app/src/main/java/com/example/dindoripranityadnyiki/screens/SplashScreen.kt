package com.example.dindoripranityadnyiki.screens

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.R
import com.example.dindoripranityadnyiki.data.PrefKeys
import com.example.dindoripranityadnyiki.data.dataStore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.math.max
import java.util.Locale

// -------------------------
// Small typed container for pref read
// -------------------------
private data class SplashPrefs(
    val isFirstTime: Boolean = true,
    val isRegistered: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userRole: String = "",
    val isFirstBookingDone: Boolean = false
)

// -------------------------
// Routes (single place)
// -------------------------
sealed class Routes(val route: String) {
    object Splash : Routes("splash")
    object RoleSelection : Routes("roleSelection")
    object Login : Routes("login")
    object UserDashboard : Routes("userDashboard")
    object GurujiDashboard : Routes("gurujiDashboard")
    // other routes are referenced by literal strings (poojaSelection) to keep compatibility
}

// -------------------------
// Helper: safe read DataStore prefs
// -------------------------
private suspend fun readSplashPrefsSafe(context: Context): SplashPrefs {
    return withContext(Dispatchers.IO) {
        runCatching {
            val prefs = context.dataStore.data.first()
            SplashPrefs(
                isFirstTime = prefs[PrefKeys.IS_FIRST_TIME] ?: true,
                isRegistered = prefs[PrefKeys.IS_REGISTERED] ?: false,
                isLoggedIn = prefs[PrefKeys.IS_LOGGED_IN] ?: false,
                userRole = prefs[PrefKeys.USER_ROLE] ?: "",
                isFirstBookingDone = prefs[PrefKeys.IS_FIRST_BOOKING_DONE] ?: false
            )
        }.getOrDefault(SplashPrefs()) // fallback defaults
    }
}

// -------------------------
// Reset minimal app-state (suspend)
// -------------------------
private suspend fun resetAppStateSafe(context: Context) {
    withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { prefs ->
                prefs[PrefKeys.IS_REGISTERED] = false
                prefs[PrefKeys.IS_LOGGED_IN] = false
                prefs[PrefKeys.USER_ROLE] = ""
                prefs[PrefKeys.IS_FIRST_BOOKING_DONE] = false
                // keep other keys untouched
            }
        } catch (e: Exception) {
            Log.w("Splash", "resetAppStateSafe failed: ${e.localizedMessage}")
        }
    }
}

// -------------------------
// Safe navigation extension
// -------------------------
fun NavController.navigateSafely(route: String) {
    runCatching {
        val cur = this.currentBackStackEntry?.destination?.route
        if (cur == route) return
        this.navigate(route) {
            popUpTo(Routes.Splash.route) { inclusive = true } // ensure splash cleared
            launchSingleTop = true
            restoreState = true
        }
    }.onFailure { ex ->
        Log.w("Nav", "navigateSafely failed to $route: ${ex.localizedMessage}")
    }
}

// -------------------------
// SplashScreen Composable
// -------------------------
@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current.applicationContext
    val auth = remember { FirebaseAuth.getInstance() }

    // UI animation states
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.9f) }
    val textAlpha = remember { Animatable(0f) }

    // local logic guards
    var navigated by rememberSaveable { mutableStateOf(false) }

    // background brush
    val backgroundBrush = remember {
        Brush.verticalGradient(listOf(Color(0xFFF8FAFF), Color(0xFFDDE3F0)))
    }

    // Launch effect: read prefs, run animations, then navigate
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()

        // 1) read prefs safe
        val prefs = readSplashPrefsSafe(context)
        Log.d("Splash", "Prefs read: $prefs")

        // 2) run UI animations in parallel
        coroutineScope {
            val a1 = launch { logoAlpha.animateTo(1f, tween(900, easing = LinearOutSlowInEasing)) }
            val a2 = launch { logoScale.animateTo(1.05f, tween(1000, easing = FastOutSlowInEasing)) }
            val a3 = launch { delay(300); textAlpha.animateTo(1f, tween(700, easing = LinearEasing)) }
            joinAll(a1, a2, a3)
        }

        // 3) ensure minimum display time (2.5s)
        val elapsed = System.currentTimeMillis() - start
        if (elapsed < 2500L) delay(2500L - elapsed)

        // 4) check Firebase auth user in a safe manner
        val currentUser = runCatching { auth.currentUser }.getOrNull()
        Log.d("Splash", "Firebase user: $currentUser")

        // 5) guard: avoid duplicate navigation if already navigated
        if (navigated) return@LaunchedEffect
        navigated = true

        // 6) Decision logic:
        // Priority order:
        //  - if first time OR not registered -> role selection (clean state)
        //  - else if registered but not logged in -> login
        //  - else if registered+loggedIn (or firebase user present):
        //       - if first booking not done -> poojaSelection for initial booking
        //       - else route to dashboard based on role
        when {
            prefs.isFirstTime -> {
                // reset a few flags (safe)
                resetAppStateSafe(context)
                // mark not-first-time
                try { context.dataStore.edit { it[PrefKeys.IS_FIRST_TIME] = false } } catch (_: Exception) {}
                navController.navigateSafely(Routes.RoleSelection.route)
            }

            !prefs.isRegistered -> {
                resetAppStateSafe(context)
                navController.navigateSafely(Routes.RoleSelection.route)
            }

            prefs.isRegistered && (currentUser == null || !prefs.isLoggedIn) -> {
                // ensure datastore logged_in flag is false to avoid loop
                try { context.dataStore.edit { it[PrefKeys.IS_LOGGED_IN] = false } } catch (_: Exception) {}
                navController.navigateSafely(Routes.Login.route)
            }

            prefs.isRegistered && (prefs.isLoggedIn || currentUser != null) -> {
                // logged-in flow
                if (!prefs.isFirstBookingDone) {
                    // send to poojaSelection for first booking
                    navController.navigateSafely("poojaSelection")
                } else {
                    when (prefs.userRole.lowercase(Locale.getDefault())) {
                        "user" -> navController.navigateSafely(Routes.UserDashboard.route)
                        "guruji" -> navController.navigateSafely(Routes.GurujiDashboard.route)
                        else -> navController.navigateSafely(Routes.Login.route)
                    }
                }
            }

            else -> {
                // safe fallback
                navController.navigateSafely(Routes.RoleSelection.route)
            }
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(40.dp))

            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App logo",
                modifier = Modifier
                    .size(180.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .padding(bottom = 42.dp)
            ) {
                Text(
                    "Shree Swami Samarth",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color(0xFF203864),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    "Seva & Adhyatmik Vikas Marg",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFF3E4D6E),
                        textAlign = TextAlign.Center
                    )
                )
                Text(
                    "(Dindori Pranit)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF627095),
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}
