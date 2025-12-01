package com.example.dindoripranityadnyiki.core.navigation

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
import com.example.dindoripranityadnyiki.feature.common.RoleSelectionScreen
import com.example.dindoripranityadnyiki.feature.common.SplashScreen
import com.example.dindoripranityadnyiki.feature.user.AlertsScreen
import com.example.dindoripranityadnyiki.feature.user.BookingFormScreen
import com.example.dindoripranityadnyiki.feature.user.CancelRequestScreen
import com.example.dindoripranityadnyiki.feature.user.DashboardScreen
import com.example.dindoripranityadnyiki.feature.user.HelpSupportScreen
import com.example.dindoripranityadnyiki.feature.user.HistoryScreen
import com.example.dindoripranityadnyiki.feature.user.InstantReceiptScreen
import com.example.dindoripranityadnyiki.feature.user.LoginScreen
import com.example.dindoripranityadnyiki.feature.user.OnboardingScreen
import com.example.dindoripranityadnyiki.feature.user.PoojaSelectionScreen
import com.example.dindoripranityadnyiki.feature.user.PrivacyPolicyScreen
import com.example.dindoripranityadnyiki.feature.user.ProfileScreen
import com.example.dindoripranityadnyiki.feature.user.RegistrationScreen
import com.example.dindoripranityadnyiki.feature.user.SettingsScreen
import com.example.dindoripranityadnyiki.feature.user.TermsAndConditionsScreen
import kotlinx.coroutines.delay

// ---------------------- Routes ----------------------
object Routes {
    const val SPLASH = "splash"
    const val ROLE_SELECTION = "roleSelection"
    const val ONBOARDING = "onboarding"
    const val REGISTRATION = "registration"
    const val LOGIN = "login"

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

    // bookingDetails/{bookingId} route (future use)
    const val BOOKING_DETAILS = "bookingDetails/{bookingId}"
    const val BOOKING_DETAILS_PREFIX = "bookingDetails"
}

// ---------------------- AppNavGraph ----------------------
@Composable
fun AppNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        // 🔹 Initial Flow
        composable(Routes.SPLASH) {
            SplashScreen(navController)
        }
        composable(Routes.ROLE_SELECTION) {
            RoleSelectionScreen(navController)
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(navController)
        }
        composable(Routes.REGISTRATION) {
            RegistrationScreen(navController)
        }
        composable(Routes.LOGIN) {
            LoginScreen(navController)
        }

        // 🔹 User Flow / Booking
        composable(Routes.POOJA_SELECTION) {
            PoojaSelectionScreen(navController)
        }
        composable(Routes.BOOKING_FORM) {
            BookingFormScreen(navController)
        }

        // 🏠 Dashboard (optional deep-link param openPujaUpdate)
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
                    uriPattern =
                        "app://dindori/${Routes.USER_DASHBOARD}?openPujaUpdate={openPujaUpdate}"
                }
            )
        ) { backStackEntry ->
            val shouldOpenUpdate =
                backStackEntry.arguments?.getString("openPujaUpdate") == "true"

            // DashboardScreen(navController, openPujaUpdateFromDeepLink = shouldOpenUpdate)
            DashboardScreen(
                navController = navController,
                openPujaUpdateFromDeepLink = shouldOpenUpdate
            )
        }

        // ✅ Other user screens
        composable(Routes.HISTORY) {
            HistoryScreen(navController)
        }
        composable(Routes.CANCEL_REQUEST) {
            CancelRequestScreen(navController)
        }
        composable(Routes.INSTANT_RECEIPT) {
            InstantReceiptScreen(navController)
        }
        composable(Routes.ALERTS) {
            AlertsScreen(navController)
        }
        composable(Routes.PROFILE) {
            ProfileScreen(navController)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(navController)
        }
        composable(Routes.PRIVACY) {
            PrivacyPolicyScreen(navController)
        }
        composable(Routes.TERMS) {
            TermsAndConditionsScreen(navController)
        }
        composable(Routes.HELP_SUPPORT) {
            HelpSupportScreen(navController)
        }

        // इथे पुढे Guruji routes add करायचे (userGraph + gurujiGraph विभाजित करून)
    }
}

// ---------------------- Helper extensions ----------------------
fun NavHostController.safeNavigate(
    route: String,
    builder: (NavOptionsBuilder.() -> Unit)? = null
) {
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
