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
    val sacredCopper = MaterialTheme.colorScheme.primary
    val snackbarHostState = remember { SnackbarHostState() }

    var bookingModel by mutableStateOf<com.example.dindoripranityadnyiki.core.data.BookingModel?>(null)
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

    fun updateStatus(status: String, otp: String = "", amount: Double = 0.0) {
        isStatusLoading = true
        scope.launch {
            val result = if (status == "Completed") {
                repository.verifyCompletionOtp(bookingId, otp, amount)
            } else {
                repository.updateBookingStatus(bookingId, status)
            }

            isStatusLoading = false
            if (result.isSuccess) {
                if (status == "Completed") showCompleteDialog = false
            } else {
                snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "Update Failed")
            }
        }
    }

    fun requestOtp() {
        isStatusLoading = true
        scope.launch {
            val result = repository.requestCompletionOtp(bookingId)
            isStatusLoading = false
            if (result.isSuccess) {
                showCompleteDialog = true
            } else {
                snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "OTP request failed")
            }
        }
    }

    LaunchedEffect(bookingModel, hasStartedService) {
        val status = bookingModel?.status ?: ""
        val userLat = bookingModel?.lat ?: 0.0
        val userLng = bookingModel?.lng ?: 0.0
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
        val subscription = repository.monitorEngagement(bookingId) { data ->
            bookingModel = data
            isLoading = false
            data?.let {
                val uLat = it.lat
                val uLng = it.lng
                val gLat = it.gurujiLat
                val gLng = it.gurujiLng

                if (uLat != 0.0) {
                    val dest = LatLng(uLat, uLng)
                    if (gLat != null && gLng != null) {
                        val gPos = LatLng(gLat, gLng)
                        if (routePoints.isEmpty()) {
                            // fetchGoogleRoute implementation removed or move to repo if needed
                            val bounds = LatLngBounds.builder().include(gPos).include(dest).build()
                            scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 150)) }
                        }
                    } else {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(dest, 15f)
                    }
                }
            }
        }
        onDispose { subscription.remove() }
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
        } else if (bookingModel != null) {
            val booking = bookingModel!!
            val status = booking.status
            val gurujiAction = booking.currentGurujiAction
            val completionOtpAvailable = booking.completionOtpAvailable
            val canReview = status == "Assigned" && (gurujiAction.isBlank() || gurujiAction == "REVIEW")
            val canStartSeva = status == "Accepted" && (gurujiAction.isBlank() || gurujiAction == "START_SEVA")
            val canRequestOtp = status == "In Progress" && (gurujiAction.isBlank() || gurujiAction == "REQUEST_OTP") && !completionOtpAvailable
            val canCompleteWithOtp = status == "In Progress" && (completionOtpAvailable || gurujiAction == "COMPLETE_WITH_OTP")
            val userLat = booking.lat
            val userLng = booking.lng
            val eta = booking.eta
            val hasArrived = booking.hasMarkedArrived
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
                            booking.gurujiLat?.let { gLat ->
                                booking.gurujiLng?.let { gLng ->
                                    Marker(
                                        state = MarkerState(LatLng(gLat, gLng)),
                                        title = if (isMarathi) "तुमचे स्थान" else "Your Location",
                                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                                    )
                                }
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
                }

                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = booking.poojaName,
                        style = DivineTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, color = SocialUi.TitleColor)
                    )

                    // Components like GurujiBookingInfoCard, GurujiStatusActions, GurujiCompletionDialog
                    // should be updated to take BookingModel instead of Map if they aren't already.
                    // Assuming they are standard components that can be adapted.
                }
            }
        }
    }
}
