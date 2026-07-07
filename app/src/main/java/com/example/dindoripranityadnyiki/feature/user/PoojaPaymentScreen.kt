package com.example.dindoripranityadnyiki.feature.user

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.LocalAppLanguage
import com.example.dindoripranityadnyiki.R
import com.example.dindoripranityadnyiki.core.design.DivineScreen
import com.example.dindoripranityadnyiki.core.design.DivineShapes
import com.example.dindoripranityadnyiki.core.design.DivineTopBar
import com.example.dindoripranityadnyiki.core.design.DivineTypography
import com.example.dindoripranityadnyiki.core.design.SocialUi
import com.example.dindoripranityadnyiki.core.navigation.Routes
import com.example.dindoripranityadnyiki.core.util.PaymentResultBus
import com.example.dindoripranityadnyiki.core.util.PaymentResultEvent
import com.razorpay.Checkout
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoojaPaymentScreen(
    navController: NavController,
    bookingId: String,
    viewModel: PoojaPaymentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isMarathi = LocalAppLanguage.current == "Marathi"
    val uiState by viewModel.uiState.collectAsState()
    val paymentEvent by PaymentResultBus.events.collectAsState()
    var retryCount by remember { mutableStateOf(0) }
    val maxRetries = 3

    LaunchedEffect(bookingId) {
        viewModel.loadPaymentDetails(bookingId)
    }

    LaunchedEffect(paymentEvent) {
        when (val event = paymentEvent) {
            is PaymentResultEvent.Success -> {
                retryCount = 0
                event.paymentId?.let {
                    viewModel.verifyRazorpayPayment(
                        bookingId = bookingId,
                        paymentId = it,
                        orderId = event.data?.orderId,
                        signature = event.data?.signature
                    )
                }
                PaymentResultBus.consume()
            }

            is PaymentResultEvent.Error -> {
                if (
                    retryCount < maxRetries &&
                    (event.message?.contains("network", ignoreCase = true) == true ||
                        event.message?.contains("timeout", ignoreCase = true) == true)
                ) {
                    retryCount++
                    kotlinx.coroutines.delay(1000L * retryCount)
                    val activity = context.findActivity()
                    if (activity != null && uiState.razorpayOrderId.isNotBlank()) {
                        startRazorpayPayment(activity, uiState, bookingId, uiState.razorpayOrderId, uiState.razorpayKeyId)
                    }
                } else {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                PaymentResultBus.consume()
            }

            null -> Unit
        }
    }

    LaunchedEffect(uiState.accessDenied) {
        if (uiState.accessDenied) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(uiState.paymentProcessed) {
        if (uiState.paymentProcessed) {
            navController.navigate(Routes.digitalReceipt(bookingId)) {
                popUpTo(Routes.USER_HOME) { inclusive = false }
            }
        }
    }

    DivineScreen(
        topBar = {
            DivineTopBar(
                title = if (isMarathi) "दक्षिणा" else "Payment",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF202020))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoading) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Spacer(Modifier.height(32.dp))
                Surface(
                    modifier = Modifier.size(88.dp),
                    shape = CircleShape,
                    color = SocialUi.SuccessGreen.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Payment, null, tint = SocialUi.SuccessGreen, modifier = Modifier.size(42.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "₹${uiState.amount}",
                    style = DivineTypography.displayLarge.copy(fontSize = 42.sp, color = SocialUi.SuccessGreen),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isMarathi) "एकूण दक्षिणा" else "Total Dakshina",
                    style = DivineTypography.bodyMedium.copy(color = SocialUi.ValueColor)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = uiState.poojaName,
                    style = DivineTypography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = SocialUi.TitleColor
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(40.dp))

                PaymentMethodRow(
                    title = if (isMarathi) "सुरक्षित ऑनलाइन पेमेंट" else "Secure Online Payment",
                    subtitle = "UPI, Cards, NetBanking",
                    iconColor = MaterialTheme.colorScheme.primary,
                    loading = uiState.isVerifying || uiState.isCreatingOrder,
                    onClick = {
                        retryCount = 0
                        val activity = context.findActivity()
                        if (activity != null) {
                            viewModel.createRazorpayOrder(bookingId) { orderId, keyId ->
                                startRazorpayPayment(activity, uiState, bookingId, orderId, keyId)
                            }
                        } else {
                            Toast.makeText(
                                context,
                                if (isMarathi) "पेमेंट स्क्रीन उघडता आली नाही." else "Unable to open payment screen.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )

                Spacer(Modifier.height(24.dp))

                uiState.error?.let {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = DivineShapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = DivineTypography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = viewModel::clearError, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    retryCount = 0
                                    viewModel.clearError()
                                    val activity = context.findActivity()
                                    if (activity != null) {
                                        if (uiState.razorpayOrderId.isNotBlank()) {
                                            startRazorpayPayment(
                                                activity,
                                                uiState,
                                                bookingId,
                                                uiState.razorpayOrderId,
                                                uiState.razorpayKeyId
                                            )
                                        } else {
                                            viewModel.createRazorpayOrder(bookingId) { orderId, keyId ->
                                                startRazorpayPayment(activity, uiState, bookingId, orderId, keyId)
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            if (isMarathi) "पेमेंट स्क्रीन उघडता आली नाही." else "Unable to open payment screen.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(if (isMarathi) "पुन्हा प्रयत्न करा" else "Retry Payment")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun PaymentMethodRow(
    title: String,
    subtitle: String,
    iconColor: Color,
    loading: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !loading) { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = iconColor.copy(alpha = 0.1f)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Payment, null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = SocialUi.TitleColor)
            Text(subtitle, fontSize = 12.sp, color = SocialUi.ValueColor)
        }
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.Lock, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
        }
    }
}

private fun startRazorpayPayment(
    activity: Activity,
    state: PaymentUiState,
    bookingId: String,
    orderId: String,
    keyId: String
) {
    val checkout = Checkout()
    checkout.setKeyID(keyId.ifBlank { activity.getString(R.string.runtime_razorpay_key) })
    try {
        val options = JSONObject().apply {
            put("name", "Dindori Pranit Trust")
            put("description", state.poojaName)
            put("currency", "INR")
            put("amount", (state.amount * 100).toInt())
            put("order_id", orderId)
            put("prefill.email", state.userEmail)
            put("prefill.contact", state.userPhone)
            put("notes", JSONObject().apply { put("bookingId", bookingId) })
        }
        checkout.open(activity, options)
    } catch (e: Exception) {
        Toast.makeText(activity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}


