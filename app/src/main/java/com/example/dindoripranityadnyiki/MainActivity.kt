package com.example.dindoripranityadnyiki

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.data.AppLocaleStore
import com.example.dindoripranityadnyiki.core.data.dataStore
import com.example.dindoripranityadnyiki.core.design.DindoriPranitYadnyikiTheme
import com.example.dindoripranityadnyiki.core.navigation.AppNavGraph
import com.example.dindoripranityadnyiki.core.navigation.Routes
import com.example.dindoripranityadnyiki.core.util.BookingAccess
import com.example.dindoripranityadnyiki.core.util.BookingAccessGuard
import com.example.dindoripranityadnyiki.core.util.PaymentResultBus
import com.example.dindoripranityadnyiki.core.util.PaymentResultEvent
import com.google.android.libraries.places.api.Places
import com.razorpay.PaymentResultWithDataListener
import com.razorpay.PaymentData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

val LocalAppLanguage = compositionLocalOf { "Marathi" }

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    private var navController: NavHostController? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun attachBaseContext(newBase: Context) {
        val lang = AppLocaleStore.readLanguage(newBase)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.runtime_maps_key))
        }
        enableEdgeToEdge()
        askNotificationPermission()

        setContent {
            val controller = rememberNavController()
            navController = controller
            val languageFlow = remember { dataStore.data.map { it[PrefKeys.LANGUAGE] ?: "mr" } }
            val languageCode by languageFlow.collectAsState(initial = "mr")
            val languageName = if (languageCode == "mr") "Marathi" else "English"

            LaunchedEffect(languageCode) {
                AppLocaleStore.mirrorLanguage(this@MainActivity, languageCode)
            }

            CompositionLocalProvider(LocalAppLanguage provides languageName) {
                DindoriPranitYadnyikiTheme {
                    AppNavGraph(navController = controller)
                    LaunchedEffect(intent?.data) {
                        navigateFromDeepLink(intent?.data, controller)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navController?.let { navigateFromDeepLink(intent.data, it) }
    }

    private fun navigateFromDeepLink(uri: Uri?, controller: NavHostController?) {
        if (uri?.scheme != "dindori" || uri.host != "app" || controller == null) return

        val token = runBlocking { dataStore.data.first()[PrefKeys.JWT_TOKEN].orEmpty() }
        val mobile = runBlocking { dataStore.data.first()[PrefKeys.USER_MOBILE].orEmpty() }
        if (token.isBlank() || mobile.isBlank()) {
            controller.navigate(Routes.LOGIN)
            return
        }

        val segments = uri.pathSegments
        val screen = segments.getOrNull(0) ?: return
        val id = segments.getOrNull(1) ?: ""

        lifecycleScope.launch {
            val userType = dataStore.data.first()[PrefKeys.USER_TYPE] ?: "user"
            val route = when (screen) {
                "poojaPayment", "digitalReceipt", "ratingFeedback", "bookingConfirmed" -> {
                    if (id.isBlank()) return@launch
                    val access = BookingAccessGuard.verify(mobile, id, expectGuruji = false)
                    if (access != BookingAccess.USER_OWNER) {
                        controller.navigate(Routes.USER_HOME)
                        return@launch
                    }
                    when (screen) {
                        "poojaPayment" -> Routes.poojaPayment(id)
                        "digitalReceipt" -> Routes.digitalReceipt(id)
                        "ratingFeedback" -> Routes.ratingFeedback(id, segments.getOrNull(2) ?: "Guruji")
                        else -> Routes.bookingConfirmed(id)
                    }
                }
                "gurujiBookingDetails" -> {
                    if (id.isBlank()) return@launch
                    val access = BookingAccessGuard.verify(mobile, id, expectGuruji = true)
                    if (access != BookingAccess.GURUJI_ASSIGNED) {
                        controller.navigate(Routes.GURUJI_DASHBOARD)
                        return@launch
                    }
                    Routes.gurujiBookingDetails(id)
                }
                "gurujiWallet" -> {
                    if (userType != "guruji") {
                        controller.navigate(Routes.USER_HOME)
                        return@launch
                    }
                    Routes.GURUJI_WALLET
                }
                "notificationInbox" -> Routes.NOTIFICATION_INBOX
                else -> null
            }
            route?.let { controller.navigate(it) }
        }
    }

    override fun onPaymentSuccess(id: String?, data: PaymentData?) {
        PaymentResultBus.publish(PaymentResultEvent.Success(id, data))
    }

    override fun onPaymentError(c: Int, m: String?, d: PaymentData?) {
        PaymentResultBus.publish(PaymentResultEvent.Error(c, m, d))
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
