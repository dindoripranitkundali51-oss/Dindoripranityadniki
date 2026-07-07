package com.example.dindoripranityadnyiki.feature.guruji

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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.LocalAppLanguage
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.data.dataStore
import com.example.dindoripranityadnyiki.core.design.DivineTypography
import com.example.dindoripranityadnyiki.core.design.GurujiBottomTabs
import com.example.dindoripranityadnyiki.core.navigation.Routes
import com.example.dindoripranityadnyiki.core.service.ServiceHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GurujiDashboardScreen(
    navController: NavController,
    viewModel: GurujiDashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isMarathi = LocalAppLanguage.current == "Marathi"
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var hasStartedDashboardService by remember { mutableStateOf(false) }
    val sacredCopper = MaterialTheme.colorScheme.primary
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(uiState.assignedBookings, hasStartedDashboardService) {
        val activeBooking = uiState.assignedBookings.firstOrNull { booking ->
            val status = booking["status"] as? String ?: ""
            status == "Accepted" || status == "In Progress"
        }
        if (activeBooking != null && !hasStartedDashboardService) {
            val bookingId = activeBooking["id"] as? String ?: ""
            val userLat = (activeBooking["userLat"] as? Number)?.toDouble() ?: 0.0
            val userLng = (activeBooking["userLng"] as? Number)?.toDouble() ?: 0.0
            if (bookingId.isNotBlank() && userLat != 0.0 && userLng != 0.0) {
                ServiceHelper.startLocationService(
                    context = context,
                    bookingId = bookingId,
                    userLat = userLat,
                    userLng = userLng
                )
                hasStartedDashboardService = true
            }
        }
    }

    fun handleLogout() {
        viewModel.logout(context) {
            navController.navigate(Routes.LOGIN) { popUpTo(0) }
        }
    }

    LaunchedEffect(uiState.isBlocked) {
        if (uiState.isBlocked) handleLogout()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        bottomBar = { GurujiBottomTabs(navController, Routes.GURUJI_DASHBOARD) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = if (isMarathi) "मेनू" else "Menu",
                                    tint = sacredCopper
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isMarathi) "English मध्ये बदला" else "Switch to Marathi"
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        scope.launch {
                                            context.dataStore.edit {
                                                it[PrefKeys.LANGUAGE] = if (isMarathi) "en" else "mr"
                                            }
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Language, null, tint = sacredCopper)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(if (isMarathi) "मदत आणि सपोर्ट" else "Help & Support")
                                    },
                                    onClick = {
                                        showMenu = false
                                        navController.navigate(Routes.support("guruji"))
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.HeadsetMic, null, tint = sacredCopper)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isMarathi) "लॉगआउट" else "Logout") },
                                    onClick = {
                                        showMenu = false
                                        handleLogout()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Logout,
                                            null,
                                            tint = Color.Red
                                        )
                                    }
                                )
                            }
                        }
                        Text(
                            text = if (isMarathi) "दिंडोरी प्रणीत याज्ञिकी" else "Dindori Pranit Yadnyiki",
                            style = DivineTypography.headlineSmall.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Black,
                                fontSize = 19.sp
                            ),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        IconButton(onClick = { navController.navigate(Routes.GURUJI_PROFILE) }) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = if (isMarathi) "प्रोफाइल" else "Profile",
                                tint = sacredCopper,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = sacredCopper)
            }
            return@Scaffold
        }

        val currentActions = uiState.assignedBookings.sortedWith(
            compareBy<Map<String, Any>> {
                (it["currentGurujiActionPriority"] as? Number)?.toInt()
                    ?: gurujiActionPriority(it["status"] as? String ?: "")
            }.thenByDescending {
                (it["assignedAt"] as? com.google.firebase.Timestamp)?.seconds ?: 0L
            }
        )
        val hasAction = currentActions.any {
            ((it["currentGurujiActionPriority"] as? Number)?.toInt()
                ?: gurujiActionPriority(it["status"] as? String ?: "")) < 9
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            if (!uiState.isVerified) {
                item { ApprovalPendingCard(isMarathi) }
            }

            if (hasAction) {
                item {
                    Text(
                        text = if (isMarathi) "चालू कृती" else "Current Action",
                        style = DivineTypography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = sacredCopper
                        )
                    )
                }
                items(currentActions) { request ->
                    val bookingId = request["id"] as? String ?: ""
                    if (request["status"] == "Assigned") {
                        NewAssignmentCard(
                            request = request,
                            isMarathi = isMarathi,
                            isLoading = uiState.actionLoading == bookingId,
                            onAccept = { viewModel.acceptBooking(bookingId) },
                            onReject = { viewModel.rejectBooking(bookingId) },
                            onViewDetails = {
                                navController.navigate(Routes.gurujiBookingDetails(bookingId))
                            }
                        )
                    } else {
                        BookingItem(
                            poojaName = request["poojaName"] as? String ?: "Sacred Service",
                            yajmanName = request["contactName"] as? String ?: "Yajman",
                            status = request["currentGurujiActionTitle"] as? String
                                ?: request["status"] as? String
                                ?: "",
                            isMarathi = isMarathi
                        ) {
                            navController.navigate(Routes.gurujiBookingDetails(bookingId))
                        }
                    }
                }
            }

            if (!hasAction && uiState.isVerified) {
                item { EmptyGurujiBookingState(isMarathi) }
            }
            item { PlanningWidget(isMarathi, navController) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun gurujiActionPriority(status: String): Int {
    return when (status) {
        "Assigned" -> 1
        "In Progress" -> 2
        "Accepted" -> 3
        "Payment Pending" -> 4
        "Awaiting Verification" -> 5
        else -> 9
    }
}
