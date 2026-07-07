package com.example.dindoripranityadnyiki.feature.guruji

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.LocalAppLanguage
import com.example.dindoripranityadnyiki.core.data.SacredSevaRepository
import com.example.dindoripranityadnyiki.core.data.toPresentationMap
import com.example.dindoripranityadnyiki.core.design.DivineScreen
import com.example.dindoripranityadnyiki.core.design.DivineTopBar
import com.example.dindoripranityadnyiki.core.design.DivineTypography
import com.example.dindoripranityadnyiki.core.design.SocialUi
import com.example.dindoripranityadnyiki.core.service.ServiceHelper
import com.example.dindoripranityadnyiki.core.util.PolylineUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.functions.FirebaseFunctions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GurujiBookingDetailsScreen(
    bookingId: String,
    navController: NavController
) {
    val isMarathi = LocalAppLanguage.current == "Marathi"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SacredSevaRepository.getInstance() }
    val functions = remember { FirebaseFunctions.getInstance() }
    val sacredCopper = MaterialTheme.colorScheme.primary
    val snackbarHostState = remember { SnackbarHostState() }

    var bookingData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isStatusLoading by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    var completionOtp by remember { mutableStateOf("") }
    var actualAmount by remember { mutableStateOf("") }
    var otpResendTimer by remember { mutableStateOf(60) }
    var canResendOtp by remember { mutableStateOf(false) }
    var hasStartedService by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState()
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var routeDistanceText by remember { mutableStateOf("") }
    var routeDurationText by remember { mutableStateOf("") }

    fun updateStatus(status: String, extraData: Map<String, Any> = emptyMap()) {
        isStatusLoading = true
        val data = hashMapOf("bookingId" to bookingId, "newStatus" to status) + extraData
        functions.getHttpsCallable("updateBookingStatus").call(data)
            .addOnSuccessListener {
                isStatusLoading = false
                if (status == "Completed") showCompleteDialog = false
            }
            .addOnFailureListener { e ->
                isStatusLoading = false
                scope.launch { snackbarHostState.showSnackbar(e.message ?: "Update Failed") }
            }
    }

    fun requestCompletionOtpAndOpenDialog() {
        isStatusLoading = true
        functions.getHttpsCallable("requestCompletionOtp").call(hashMapOf("bookingId" to bookingId))
            .addOnSuccessListener {
                isStatusLoading = false
                showCompleteDialog = true
            }
            .addOnFailureListener { e ->
                isStatusLoading = false
                scope.launch { snackbarHostState.showSnackbar(e.message ?: "OTP request failed") }
            }
    }

    fun fetchGoogleRoute(start: LatLng, end: LatLng) {
        val payload = hashMapOf(
            "originLat" to start.latitude,
            "originLng" to start.longitude,
            "destinationLat" to end.latitude,
            "destinationLng" to end.longitude
        )
        functions.getHttpsCallable("getRoutePreview").call(payload)
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *> ?: return@addOnSuccessListener
                val points = data["points"] as? String ?: ""
                if (points.isNotBlank()) routePoints = PolylineUtil.decodePolyline(points)
                routeDistanceText = data["distanceText"] as? String ?: ""
                routeDurationText = data["durationText"] as? String ?: ""
            }
            .addOnFailureListener {
                // Optional preview only.
            }
    }

    LaunchedEffect(bookingData, hasStartedService) {
        val status = bookingData?.get("status") as? String ?: ""
        val userLat = (bookingData?.get("userLat") as? Number)?.toDouble() ?: 0.0
        val userLng = (bookingData?.get("userLng") as? Number)?.toDouble() ?: 0.0
        if ((status == "Accepted" || status == "In Progress") && !hasStartedService && userLat != 0.0) {
            ServiceHelper.startLocationService(
                context = context,
                bookingId = bookingId,
                userLat = userLat,
                userLng = userLng
            )
            hasStartedService = true
        }
    }

    LaunchedEffect(showCompleteDialog, canResendOtp) {
        if (showCompleteDialog && !canResendOtp) {
            otpResendTimer = 60
            while (otpResendTimer > 0) {
                delay(1000L)
                otpResendTimer--
            }
            canResendOtp = true
        }
    }

    DisposableEffect(bookingId) {
        val registration = repository.monitorEngagement(bookingId) { data ->
            bookingData = data?.toPresentationMap()
            isLoading = false
            data?.let {
                val uLat = it.lat
                val uLng = it.lng
                val gLoc = it.gurujiLocation
                if (uLat != 0.0) {
                    val dest = LatLng(uLat, uLng)
                    if (gLoc != null) {
                        val gPos = LatLng(gLoc.latitude, gLoc.longitude)
                        if (routePoints.isEmpty()) {
                            fetchGoogleRoute(gPos, dest)
                            val bounds = LatLngBounds.builder().include(gPos).include(dest).build()
                            scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 150)) }
                        }
                    } else {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(dest, 15f)
                    }
                }
            }
        }
        onDispose { registration.remove() }
    }

    DivineScreen(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DivineTopBar(
                title = if (isMarathi) "सेवा तपशील" else "Booking Details",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = sacredCopper, modifier = Modifier.size(20.dp))
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = sacredCopper)
            }
        } else if (bookingData != null) {
            val booking = bookingData!!
            val status = booking["status"] as? String ?: ""
            val gurujiAction = booking["currentGurujiAction"] as? String ?: ""
            val completionOtpAvailable = booking["completionOtpAvailable"] as? Boolean ?: false
            val canReview = status == "Assigned" && (gurujiAction.isBlank() || gurujiAction == "REVIEW")
            val canStartSeva = status == "Accepted" && (gurujiAction.isBlank() || gurujiAction == "START_SEVA")
            val canRequestOtp = status == "In Progress" && (gurujiAction.isBlank() || gurujiAction == "REQUEST_OTP") && !completionOtpAvailable
            val canCompleteWithOtp = status == "In Progress" && (completionOtpAvailable || gurujiAction == "COMPLETE_WITH_OTP")
            val userLat = (booking["userLat"] as? Number)?.toDouble() ?: 0.0
            val userLng = (booking["userLng"] as? Number)?.toDouble() ?: 0.0
            val eta = booking["eta"] as? String ?: ""
            val hasArrived = booking["hasMarkedArrived"] as? Boolean ?: false
            val showArrived = hasArrived || eta.contains("Arrived", ignoreCase = true)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (status != "Assigned" && userLat != 0.0) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    ) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            uiSettings = MapUiSettings(zoomControlsEnabled = false)
                        ) {
                            Marker(
                                state = MarkerState(LatLng(userLat, userLng)),
                                title = if (isMarathi) "यजमानाचे ठिकाण" else "Destination"
                            )
                            booking["gurujiLocation"]?.let {
                                val gLoc = it as com.google.firebase.firestore.GeoPoint
                                Marker(
                                    state = MarkerState(LatLng(gLoc.latitude, gLoc.longitude)),
                                    title = if (isMarathi) "तुमचे स्थान" else "Your Location",
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                                )
                            }
                            if (routePoints.isNotEmpty()) {
                                Polyline(points = routePoints, color = Color(0xFF3498DB), width = 12f)
                            }
                        }
                        FloatingActionButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$userLat,$userLng")).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            containerColor = sacredCopper,
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Navigation, null)
                        }
                    }
                    if (routeDistanceText.isNotBlank() || routeDurationText.isNotBlank()) {
                        RouteSummaryCard(
                            distance = routeDistanceText,
                            duration = routeDurationText,
                            isMarathi = isMarathi,
                            color = sacredCopper,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = booking["poojaName"] as? String ?: "",
                        style = DivineTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, color = SocialUi.TitleColor)
                    )

                    GurujiBookingInfoCard(
                        booking = booking,
                        status = status,
                        eta = eta,
                        showArrived = showArrived,
                        isMarathi = isMarathi,
                        sacredCopper = sacredCopper
                    )

                    GurujiStatusActions(
                        status = status,
                        canReview = canReview,
                        canStartSeva = canStartSeva,
                        canRequestOtp = canRequestOtp,
                        canCompleteWithOtp = canCompleteWithOtp,
                        isStatusLoading = isStatusLoading,
                        isMarathi = isMarathi,
                        sacredCopper = sacredCopper,
                        onAccept = { updateStatus("Accepted") },
                        onReject = { updateStatus("Rejected") },
                        onRequestOtp = { requestCompletionOtpAndOpenDialog() },
                        onOpenCompletionDialog = { showCompleteDialog = true }
                    )
                }
            }
        }
    }

    GurujiCompletionDialog(
        show = showCompleteDialog,
        completionOtp = completionOtp,
        actualAmount = actualAmount,
        canResendOtp = canResendOtp,
        otpResendTimer = otpResendTimer,
        isStatusLoading = isStatusLoading,
        isMarathi = isMarathi,
        sacredCopper = sacredCopper,
        onOtpChange = { completionOtp = it },
        onAmountChange = { actualAmount = it },
        onResendOtp = {
            canResendOtp = false
            requestCompletionOtpAndOpenDialog()
        },
        onDismiss = { showCompleteDialog = false },
        onSubmit = { updateStatus("Completed", mapOf("otp" to completionOtp, "actualAmount" to actualAmount)) }
    )
}
