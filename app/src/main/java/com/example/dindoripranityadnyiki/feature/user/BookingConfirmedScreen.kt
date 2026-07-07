package com.example.dindoripranityadnyiki.feature.user

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.LocalAppLanguage
import com.example.dindoripranityadnyiki.core.design.DivineScreen
import com.example.dindoripranityadnyiki.core.design.DivineTypography
import com.example.dindoripranityadnyiki.core.design.SocialUi
import com.example.dindoripranityadnyiki.core.navigation.Routes
import com.example.dindoripranityadnyiki.core.util.KalmanFilter
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun BookingConfirmedScreen(
    bookingId: String,
    navController: NavController,
    viewModel: BookingConfirmedViewModel = viewModel()
) {
    val isMarathi = LocalAppLanguage.current == "Marathi"
    val uiState by viewModel.uiState.collectAsState()
    val bookingData = uiState.bookingData
    val sacredCopper = MaterialTheme.colorScheme.primary
    val cameraPositionState = rememberCameraPositionState()
    val kalmanFilter = remember { KalmanFilter() }
    var filteredGurujiPos by remember { mutableStateOf<LatLng?>(null) }
    var requestType by remember { mutableStateOf<String?>(null) }
    var requestedDate by remember { mutableStateOf("") }
    var requestReason by remember { mutableStateOf("") }
    var hasPromptedFeedback by remember(bookingId) { mutableStateOf(false) }
    var showFeedbackPrompt by remember(bookingId) { mutableStateOf(false) }

    LaunchedEffect(bookingId) {
        viewModel.monitorBooking(bookingId)
    }

    LaunchedEffect(uiState.accessDenied) {
        if (uiState.accessDenied) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(bookingId, bookingData) {
        val data = bookingData ?: return@LaunchedEffect
        val shouldPromptFeedback =
            data["feedbackSubmittedAt"] == null &&
                data["rating"] == null &&
                (((data["status"] as? String) in listOf("Completed", "Paid")) ||
                    (data["paymentStatus"] as? String) == "Paid")
        if (shouldPromptFeedback && !hasPromptedFeedback) {
            hasPromptedFeedback = true
            showFeedbackPrompt = true
        }
    }

    LaunchedEffect(bookingData?.get("gurujiLocation")) {
        val gLoc = bookingData?.get("gurujiLocation") as? com.google.firebase.firestore.GeoPoint
        if (gLoc != null) {
            filteredGurujiPos = kalmanFilter.receiveLocation(
                gLoc.latitude,
                gLoc.longitude,
                10f,
                System.currentTimeMillis()
            )
        }
    }

    requestType?.let { type ->
        val isCancelRequest = type == "Cancel"
        AlertDialog(
            onDismissRequest = { requestType = null },
            title = {
                Text(
                    if (isCancelRequest) {
                        if (isMarathi) "रद्द विनंती" else "Cancel request"
                    } else {
                        if (isMarathi) "तारीख बदल विनंती" else "Reschedule request"
                    },
                    style = DivineTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = SocialUi.TitleColor)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!isCancelRequest) {
                        OutlinedTextField(
                            value = requestedDate,
                            onValueChange = { requestedDate = it },
                            label = { Text(if (isMarathi) "नवीन तारीख (YYYY-MM-DD)" else "New date (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = requestReason,
                        onValueChange = { requestReason = it },
                        label = { Text(if (isMarathi) "कारण" else "Reason") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !uiState.isRequestLoading,
                    onClick = {
                        viewModel.submitBookingRequest(bookingId, type, requestedDate.trim(), requestReason.trim())
                        requestType = null
                        requestedDate = ""
                        requestReason = ""
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (isMarathi) "पाठवा" else "Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { requestType = null }) {
                    Text(if (isMarathi) "बंद करा" else "Close", color = SocialUi.ValueColor)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showFeedbackPrompt) {
        AlertDialog(
            onDismissRequest = { showFeedbackPrompt = false },
            title = {
                Text(if (isMarathi) "अभिप्राय प्रलंबित आहे" else "Feedback pending")
            },
            text = {
                Text(
                    if (isMarathi) {
                        "सेवा पूर्ण झाली आहे. आता अभिप्राय द्यायचा का?"
                    } else {
                        "This seva is complete. Do you want to submit feedback now?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFeedbackPrompt = false
                        navController.navigate(Routes.ratingFeedback(bookingId, bookingData?.get("gurujiName") as? String ?: ""))
                    }
                ) {
                    Text(if (isMarathi) "आता द्या" else "Rate now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackPrompt = false }) {
                    Text(if (isMarathi) "नंतर" else "Later")
                }
            }
        )
    }

    DivineScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(18.dp))
            Surface(
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                color = SocialUi.SuccessGreen.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CheckCircle, null, tint = SocialUi.SuccessGreen, modifier = Modifier.size(42.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (isMarathi) "बुकिंग तपशील" else "Booking Status",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = SocialUi.TitleColor
            )

            val status = bookingData?.get("status") as? String ?: "Pending"
            val poojaName = bookingData?.get("poojaName") as? String ?: ""
            val date = bookingData?.get("date") as? String ?: ""
            val gurujiName = bookingData?.get("gurujiName") as? String ?: ""
            val currentAction = bookingData?.get("currentUserAction") as? String ?: ""
            val actionTitle = bookingData?.get("currentUserActionTitle") as? String
            val actionDescription = bookingData?.get("currentUserActionDescription") as? String
            val userLat = (bookingData?.get("userLat") as? Number)?.toDouble() ?: 0.0
            val userLng = (bookingData?.get("userLng") as? Number)?.toDouble() ?: 0.0
            val isPaymentFailed = bookingData?.get("paymentStatus") == "Failed"

            Spacer(Modifier.height(16.dp))
            when {
                uiState.isLoading -> CircularProgressIndicator(color = sacredCopper)
                uiState.error != null -> AssistChip(
                    onClick = viewModel::clearMessage,
                    label = { Text(uiState.error ?: "", color = Color(0xFFDC2626)) },
                    shape = RoundedCornerShape(50)
                )
                uiState.message != null -> AssistChip(
                    onClick = viewModel::clearMessage,
                    label = { Text(uiState.message ?: "", color = SocialUi.SuccessGreen) },
                    shape = RoundedCornerShape(50)
                )
            }

            BookingStatusSummaryCard(
                poojaName = poojaName,
                date = date,
                gurujiName = gurujiName,
                status = status,
                actionTitle = actionTitle,
                actionDescription = actionDescription,
                isMarathi = isMarathi
            )

            ArrivedStatusBanner(
                bookingData = bookingData,
                isMarathi = isMarathi
            )

            if (shouldShowTrackingMap(status, filteredGurujiPos, userLat, userLng)) {
                Spacer(Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(16.dp))) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(zoomControlsEnabled = false)
                    ) {
                        filteredGurujiPos?.let { pos ->
                            Marker(
                                state = MarkerState(position = pos),
                                title = if (isMarathi) "गुरुजी लोकेशन" else "Guruji location",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                            )
                        }
                        Marker(
                            state = MarkerState(position = LatLng(userLat, userLng)),
                            title = if (isMarathi) "तुमचे घर" else "Your Home"
                        )
                    }
                }
            }

            BookingActionButtons(
                currentAction = currentAction,
                status = status,
                bookingId = bookingId,
                gurujiName = gurujiName,
                navController = navController,
                isMarathi = isMarathi,
                isRetry = isPaymentFailed,
                onCancel = { requestType = "Cancel" },
                onReschedule = { requestType = "Reschedule" }
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { navController.navigate(Routes.USER_HOME) { popUpTo(0) } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = sacredCopper)
            ) {
                Text(if (isMarathi) "मुख्यपृष्ठावर जा" else "Go to Dashboard", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BookingStatusSummaryCard(
    poojaName: String,
    date: String,
    gurujiName: String,
    status: String,
    actionTitle: String?,
    actionDescription: String?,
    isMarathi: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                poojaName.ifBlank { if (isMarathi) "पूजा बुकिंग" else "Pooja booking" },
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = SocialUi.TitleColor
            )
            Spacer(Modifier.height(8.dp))
            BookingInfoLine(Icons.Default.CalendarMonth, if (isMarathi) "तारीख" else "Date", date.ifBlank { "-" })
            BookingInfoLine(
                Icons.Default.Person,
                if (isMarathi) "गुरुजी" else "Guruji",
                gurujiName.ifBlank { if (isMarathi) "अजून नियुक्त नाही" else "Not assigned yet" }
            )
            BookingInfoLine(Icons.Default.Info, if (isMarathi) "स्थिती" else "Status", localizedBookingStatus(status, isMarathi))
            if (!actionTitle.isNullOrBlank()) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Text(actionTitle, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
            if (!actionDescription.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(actionDescription, style = MaterialTheme.typography.bodySmall, color = SocialUi.ValueColor)
            }
        }
    }
}

@Composable
private fun ArrivedStatusBanner(
    bookingData: Map<String, Any>?,
    isMarathi: Boolean
) {
    val eta = bookingData?.get("eta") as? String ?: ""
    val hasArrived = bookingData?.get("hasMarkedArrived") as? Boolean ?: false
    val arrivalState = bookingData?.get("arrivalState") as? String ?: ""
    val showArrived = hasArrived || arrivalState.equals("Arrived", ignoreCase = true) || eta.contains("Arrived", ignoreCase = true)
    if (!showArrived) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(12.dp),
        color = SocialUi.SuccessGreen.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, SocialUi.SuccessGreen.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, null, tint = SocialUi.SuccessGreen)
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (isMarathi) "गुरुजी पोहोचले आहेत. सेवा आता सुरू होण्यासाठी तयार आहे." else "Guruji has arrived and the booking is now ready on-site.",
                style = DivineTypography.bodyMedium.copy(
                    color = SocialUi.SuccessGreen,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun BookingInfoLine(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = SocialUi.TitleColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(value, color = SocialUi.ValueColor, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun BookingActionButtons(
    currentAction: String,
    status: String,
    bookingId: String,
    gurujiName: String,
    navController: NavController,
    isMarathi: Boolean,
    isRetry: Boolean = false,
    onCancel: () -> Unit,
    onReschedule: () -> Unit
) {
    val sacredCopper = MaterialTheme.colorScheme.primary
    val cancellable = status in setOf("Pending", "Assigned", "Accepted")

    Spacer(Modifier.height(16.dp))
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (currentAction) {
            "PAY_NOW" -> {
                Button(
                    onClick = { navController.navigate(Routes.poojaPayment(bookingId)) },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRetry) Color(0xFFE65100) else sacredCopper
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(if (isRetry) Icons.Default.Refresh else Icons.Default.Payment, null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (isRetry) {
                            if (isMarathi) "पेमेंट पुन्हा प्रयत्न करा" else "Retry Payment"
                        } else {
                            if (isMarathi) "पेमेंट करा" else "Pay Now"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
            }

            "RATE_SEVA" -> Button(
                onClick = { navController.navigate(Routes.ratingFeedback(bookingId, gurujiName)) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = sacredCopper)
            ) {
                Text(if (isMarathi) "अभिप्राय द्या" else "Give Feedback", fontWeight = FontWeight.Bold)
            }

            "VIEW_RECEIPT" -> OutlinedButton(
                onClick = { navController.navigate(Routes.digitalReceipt(bookingId)) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isMarathi) "पावती पहा" else "View Receipt", fontWeight = FontWeight.Bold)
            }

            "CHECK_OTP_NOTIFICATION" -> OutlinedButton(
                onClick = { navController.navigate(Routes.NOTIFICATION_INBOX) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isMarathi) "OTP नोटिफिकेशन पहा" else "Check OTP Notification", fontWeight = FontWeight.Bold)
            }
        }

        if (cancellable) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onReschedule, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Text(if (isMarathi) "तारीख बदला" else "Reschedule")
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                ) {
                    Text(if (isMarathi) "रद्द करा" else "Cancel")
                }
            }
        }
    }
}

private fun shouldShowTrackingMap(
    status: String,
    gurujiLocation: LatLng?,
    userLat: Double,
    userLng: Double
): Boolean {
    if (gurujiLocation == null || userLat == 0.0 || userLng == 0.0) return false
    return status in setOf("Accepted", "In Progress", "Payment Pending", "Awaiting Verification")
}

private fun localizedBookingStatus(status: String, isMarathi: Boolean): String {
    if (!isMarathi) return status.ifBlank { "Pending" }
    return when (status) {
        "Pending" -> "प्रलंबित"
        "Assigned" -> "गुरुजी नेमले"
        "Accepted" -> "स्वीकारले"
        "In Progress" -> "सेवा सुरू"
        "Payment Pending" -> "पेमेंट बाकी"
        "Awaiting Verification" -> "तपासणी बाकी"
        "Completed" -> "पूर्ण"
        "Paid" -> "पैसे भरले"
        "Cancelled" -> "रद्द"
        "Rejected" -> "नाकारले"
        else -> status.ifBlank { "प्रलंबित" }
    }
}
