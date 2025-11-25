// NavGraph.kt
package com.example.dindoripranityadnyiki.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.dindoripranityadnyiki.screens.*
import kotlinx.coroutines.delay

// Central route names to avoid typos
object Routes {
    const val SPLASH = "splash"
    const val ROLE_SELECTION = "roleSelection"
    const val ONBOARDING = "onboarding"
    const val REGISTRATION = "registration"
    const val LOGIN = "login"

    // keep base name simple; composable below will declare the optional query param
    const val USER_DASHBOARD = "userDashboard"
    const val POOJA_SELECTION = "poojaSelection"
    const val BOOKING_FORM = "bookingForm"

    const val HISTORY = "history"
    const val MY_REQUESTS = "myRequests"
    const val CANCEL_REQUEST = "cancelRequest"
    const val INSTANT_RECEIPT = "instantReceipt"
    const val ALERTS = "alerts"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val TERMS = "terms"
    const val HELP_SUPPORT = "helpSupport"

    // Booking details route (with bookingId arg)
    const val BOOKING_DETAILS = "bookingDetails/{bookingId}"
    const val BOOKING_DETAILS_PREFIX = "bookingDetails" // used for navigation building: "bookingDetails/$id"

    // add other routes as needed
}

// ------------ AppNavGraph: full list of routes used across the app ------------
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        // 🔹 Initial Flow
        composable(Routes.SPLASH) { SplashScreen(navController) }
        composable(Routes.ROLE_SELECTION) { RoleSelectionScreen(navController) }
        composable(Routes.ONBOARDING) { OnboardingScreen(navController) }
        composable(Routes.REGISTRATION) { RegistrationScreen(navController) }
        composable(Routes.LOGIN) { LoginScreen(navController) }

        // 🔹 User Flow / Booking
        composable(Routes.POOJA_SELECTION) { PoojaSelectionScreen(navController) }
        composable(Routes.BOOKING_FORM) { BookingFormScreen(navController) }

        // 🏠 Dashboard / Home
        //
        // Here we register a composable that accepts an optional query param `openPujaUpdate`.
        // Deep link pattern: app://dindori/userDashboard?openPujaUpdate={openPujaUpdate}
        // When opened via that deep link with openPujaUpdate=true, DashboardScreen will receive true.
        composable(
            route = "${Routes.USER_DASHBOARD}?openPujaUpdate={openPujaUpdate}",
            arguments = listOf(
                navArgument("openPujaUpdate") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "false"
                }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "app://dindori/${Routes.USER_DASHBOARD}?openPujaUpdate={openPujaUpdate}"
                }
            )
        ) { backStackEntry ->
            val shouldOpenUpdate = backStackEntry.arguments?.getString("openPujaUpdate") == "true"
            // IMPORTANT: DashboardScreen signature should accept the boolean param:
            // fun DashboardScreen(navController: NavController, openPujaUpdateFromDeepLink: Boolean = false) { ... }
            DashboardScreen(navController = navController, openPujaUpdateFromDeepLink = shouldOpenUpdate)
        }

        // ✅ Other screens referenced from Dashboard / Drawer / Bottom bar
        composable(Routes.HISTORY) { HistoryScreen(navController) }
        composable(Routes.CANCEL_REQUEST) { CancelRequestScreen(navController) }
        composable(Routes.INSTANT_RECEIPT) { InstantReceiptScreen(navController) }
        composable(Routes.ALERTS) { AlertsScreen(navController) }
        composable(Routes.PROFILE) { ProfileScreen(navController) }
        composable(Routes.SETTINGS) { SettingsScreen(navController) }
        composable(Routes.PRIVACY) { PrivacyPolicyScreen(navController) }
        composable(Routes.TERMS) { TermsAndConditionsScreen(navController) }
        composable(Routes.HELP_SUPPORT) { HelpSupportScreen(navController) }

        // -------------------------
        // Booking details (with bookingId argument)
        // Match route pattern "bookingDetails/{bookingId}"
        // Navigate to it like: navController.navigate("bookingDetails/$id")
        // -------------------------

        }

        // add other routes here
    }


// ----------------- Helpful navigation utilities -----------------

/**
 * Safe navigate: avoids exceptions when trying to navigate to a route that's already current,
 * and logs errors instead of crashing.
 */
fun NavHostController.safeNavigate(route: String, builder: (NavOptionsBuilder.() -> Unit)? = null) {
    try {
        val currentRoute = this.currentBackStackEntry?.destination?.route
        if (currentRoute == route) return
        if (builder != null) {
            this.navigate(route, builder)
        } else {
            this.navigate(route)
        }
    } catch (e: Exception) {
        Log.e("NavSafe", "safeNavigate failed for route=$route", e)
    }
}

/**
 * Navigate to a route and popUpTo the nav graph's start destination to clear stack
 * (used for logout -> login).
 */
fun NavHostController.navigateAndPopToStart(route: String) {
    try {
        this.navigate(route) {
            popUpTo(this@navigateAndPopToStart.graph.startDestinationId) {
                inclusive = true
            }
            launchSingleTop = true
        }
    } catch (e: Exception) {
        Log.e("NavSafe", "navigateAndPopToStart failed for $route", e)
    }
}

/**
 * Helper to close drawer then navigate. Example usage from a Composable scope with drawerState:
 *
 * drawerScope.launch {
 *   closeDrawerThenNavigate({ drawerState.close() }, navController, Routes.MY_REQUESTS)
 * }
 *
 * Note: the function expects a suspend drawerClose function (e.g., drawerState.close()).
 */
suspend fun closeDrawerThenNavigate(
    drawerClose: suspend () -> Unit,
    navController: NavController,
    route: String,
    delayMs: Long = 120
) {
    try {
        drawerClose()
        delay(delayMs)
        (navController as? NavHostController)?.safeNavigate(route)
    } catch (e: Exception) {
        Log.e("NavSafe", "closeDrawerThenNavigate failed for $route", e)
    }
}
