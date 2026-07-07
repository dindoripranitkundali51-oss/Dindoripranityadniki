package com.example.dindoripranityadnyiki.core.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.dindoripranityadnyiki.core.data.Constants
import com.example.dindoripranityadnyiki.feature.common.AppSplashScreen
import com.example.dindoripranityadnyiki.feature.common.LegalInfoScreen
import com.example.dindoripranityadnyiki.feature.common.NotificationInboxScreen
import com.example.dindoripranityadnyiki.feature.common.SupportScreen
import com.example.dindoripranityadnyiki.feature.common.SupportTicketsScreen
import com.example.dindoripranityadnyiki.feature.guruji.*
import com.example.dindoripranityadnyiki.feature.user.*

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTRATION = "registration"
    
    const val USER_HOME = "userHome"
    const val USER_HISTORY = "userHistory"
    const val USER_PROFILE = "userProfile"
    const val POOJA_SELECTION = "poojaSelection"
    const val BOOKING_DETAILS = "bookingDetails/{poojaId}/{poojaName}"
    const val BOOKING_CONFIRMED = "bookingConfirmed/{bookingId}"
    const val POOJA_PAYMENT = "poojaPayment/{bookingId}"
    const val DIGITAL_RECEIPT = "digitalReceipt/{bookingId}"
    
    const val RATING_FEEDBACK = "ratingFeedback/{bookingId}/{gurujiName}"
    
    const val GURUJI_LOGIN = "gurujiLogin"
    const val GURUJI_REGISTRATION = "gurujiRegistration"
    const val GURUJI_DASHBOARD = "gurujiDashboard"
    const val GURUJI_AVAILABILITY = "gurujiAvailability"
    const val GURUJI_WALLET = "gurujiWallet"
    const val GURUJI_PROFILE = "gurujiProfile"
    const val GURUJI_BOOKING_DETAILS = "gurujiBookingDetails/{bookingId}"
    const val SUPPORT = "support/{userRole}"
    const val SUPPORT_TICKETS = "supportTickets/{userRole}"
    const val NOTIFICATION_INBOX = "notificationInbox"
    const val LEGAL_INFO = "legalInfo/{type}"
    const val SUPPORT_CHAT = "supportChat"

    fun ratingFeedback(bookingId: String, name: String) = "ratingFeedback/${Uri.encode(bookingId)}/${Uri.encode(name)}"
    fun bookingDetails(id: String, name: String) = "bookingDetails/${Uri.encode(id)}/${Uri.encode(name)}"
    fun bookingConfirmed(id: String) = "bookingConfirmed/${Uri.encode(id)}"
    fun poojaPayment(id: String) = "poojaPayment/${Uri.encode(id)}"
    fun digitalReceipt(id: String) = "digitalReceipt/${Uri.encode(id)}"
    fun gurujiBookingDetails(id: String) = "gurujiBookingDetails/${Uri.encode(id)}"
    fun support(userRole: String) = "support/${Uri.encode(userRole)}"
    fun supportTickets(userRole: String) = "supportTickets/${Uri.encode(userRole)}"
    fun legalInfo(type: String) = "legalInfo/${Uri.encode(type)}"
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    val context = LocalContext.current
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { AppSplashScreen(navController) }
        composable(Routes.LOGIN) { UserLoginScreen(navController) }
        composable(Routes.REGISTRATION) { UserRegistrationScreen(navController) }
        composable(Routes.USER_HOME) { UserHomeScreen(navController) }
        composable(Routes.USER_HISTORY) { UserHistoryScreen(navController) }
        composable(Routes.USER_PROFILE) { UserProfileScreen(navController) }
        composable(Routes.NOTIFICATION_INBOX) { NotificationInboxScreen(navController) }
        composable(
            route = Routes.LEGAL_INFO,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            LegalInfoScreen(navController, backStackEntry.arguments?.getString("type") ?: "privacy")
        }
        composable(Routes.POOJA_SELECTION) {
            PoojaSelectionScreen(
                onPoojaSelected = { id, name -> navController.navigate(Routes.bookingDetails(id, name)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.SUPPORT,
            arguments = listOf(navArgument("userRole") { type = NavType.StringType })
        ) { backStackEntry ->
            SupportScreen(navController, backStackEntry.arguments?.getString("userRole") ?: "user")
        }
        composable(
            route = Routes.SUPPORT_TICKETS,
            arguments = listOf(navArgument("userRole") { type = NavType.StringType })
        ) { backStackEntry ->
            SupportTicketsScreen(navController, backStackEntry.arguments?.getString("userRole") ?: "user")
        }
        
        composable(
            route = Routes.BOOKING_DETAILS,
            arguments = listOf(navArgument("poojaId") { type = NavType.StringType }, navArgument("poojaName") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("poojaId") ?: ""
            val name = backStackEntry.arguments?.getString("poojaName") ?: ""
            BookingDetailsScreen(id, name, navController)
        }

        composable(
            route = Routes.BOOKING_CONFIRMED,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("bookingId") ?: ""
            BookingConfirmedScreen(id, navController)
        }

        composable(
            route = Routes.POOJA_PAYMENT,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("bookingId") ?: ""
            PoojaPaymentScreen(navController, id)
        }

        composable(
            route = Routes.DIGITAL_RECEIPT,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("bookingId") ?: ""
            DigitalReceiptScreen(id, navController)
        }

        composable(
            route = Routes.RATING_FEEDBACK,
            arguments = listOf(
                navArgument("bookingId") { type = NavType.StringType },
                navArgument("gurujiName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val bId = backStackEntry.arguments?.getString("bookingId") ?: ""
            val gName = backStackEntry.arguments?.getString("gurujiName") ?: ""
            RatingFeedbackScreen(navController, bId, gName)
        }

        // Guruji Section
        composable(Routes.GURUJI_LOGIN) { GurujiLoginScreen(navController) }
        composable(Routes.GURUJI_REGISTRATION) {
            GurujiRegistrationScreen(
                onAffidavitDownloadClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.AFFIDAVIT_URL))
                    context.startActivity(intent)
                },
                onSubmitSuccess = { navController.navigate(Routes.GURUJI_LOGIN) },
                navController = navController
            )
        }
        composable(Routes.GURUJI_DASHBOARD) { GurujiDashboardScreen(navController) }
        composable(Routes.GURUJI_AVAILABILITY) { GurujiAvailabilityScreen(navController) }
        composable(Routes.GURUJI_WALLET) { GurujiWalletScreen(navController) }
        composable(Routes.GURUJI_PROFILE) { GurujiProfileScreen(navController) }
        composable(
            route = Routes.GURUJI_BOOKING_DETAILS,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("bookingId") ?: ""
            GurujiBookingDetailsScreen(id, navController)
        }
        composable(Routes.SUPPORT_CHAT) { LiveSupportChatScreen(navController) }
    }
}
