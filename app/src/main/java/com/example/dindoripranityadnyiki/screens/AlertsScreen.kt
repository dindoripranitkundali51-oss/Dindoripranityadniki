package com.example.dindoripranityadnyiki.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.dindoripranityadnyiki.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// ======================================================================
//   MODEL + REPOSITORY + VIEWMODEL + UI  — ALL IN ONE FILE
// ======================================================================

// -----------------------------
// MODEL
// -----------------------------
data class UserNotification(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val bookingId: String?,
    val createdAt: Date?,
    val read: Boolean
)

// -----------------------------
// REPOSITORY
// -----------------------------
class NotificationRepository(
    private val firestore: FirebaseFirestore
) {
    private val col get() = firestore.collection("user_notifications")

    suspend fun fetchNotifications(userId: String): List<UserNotification> {
        if (userId.isBlank()) return emptyList()

        return try {
            val snap = col.whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get().await()

            snap.documents.mapNotNull { d ->
                val title = d.getString("title") ?: return@mapNotNull null
                UserNotification(
                    id = d.id,
                    title = title,
                    body = d.getString("body") ?: "",
                    type = d.getString("type") ?: "general",
                    bookingId = d.getString("bookingId"),
                    createdAt = d.getTimestamp("createdAt")?.toDate(),
                    read = d.getBoolean("read") ?: false
                )
            }
        } catch (e: Exception) {
            Log.e("AlertsRepo", "fetch failed", e)
            emptyList()
        }
    }

    suspend fun markAsRead(id: String) {
        if (id.isBlank()) return
        runCatching { col.document(id).update("read", true).await() }
    }

    suspend fun markAllAsRead(userId: String) {
        if (userId.isBlank()) return

        val snap = runCatching {
            col.whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .limit(100).get().await()
        }.getOrNull() ?: return

        firestore.runBatch { b ->
            snap.documents.forEach { d ->
                b.update(d.reference, "read", true)
            }
        }.await()
    }
}

// -----------------------------
// VIEWMODEL
// -----------------------------
data class AlertsUiState(
    val isLoading: Boolean = false,
    val list: List<UserNotification> = emptyList(),
    val error: String? = null
)

class AlertsViewModel : ViewModel() {

    private val repo = NotificationRepository(FirebaseFirestore.getInstance())
    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow(AlertsUiState(isLoading = true))
    val state: StateFlow<AlertsUiState> = _state

    private val df = SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault())

    init {
        refresh()
    }

    fun refresh() {
        val uid = auth.currentUser?.uid ?: run {
            _state.value = AlertsUiState(false, emptyList(), "Login required")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = repo.fetchNotifications(uid)

            _state.update {
                it.copy(
                    isLoading = false,
                    list = result,
                    error = null
                )
            }
        }
    }

    fun formattedDate(n: UserNotification): String =
        n.createdAt?.let { df.format(it) } ?: ""

    fun markRead(n: UserNotification) {
        viewModelScope.launch {
            _state.update {
                it.copy(list = it.list.map { x ->
                    if (x.id == n.id) x.copy(read = true) else x
                })
            }
            repo.markAsRead(n.id)
        }
    }

    fun markAll() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _state.update { it.copy(list = it.list.map { n -> n.copy(read = true) }) }
            repo.markAllAsRead(uid)
        }
    }
}

// -----------------------------
// UI SCREEN
// -----------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(navController: NavController) {

    val vm = remember { AlertsViewModel() }
    val ui by vm.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Alerts", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { vm.markAll() },
                        enabled = ui.list.any { !it.read }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Mark All", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFFB71C1C))
            )
        }
    ) { pad ->
        Box(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFF5F8FB), Color.White)))
        ) {

            when {
                ui.isLoading -> AlertsLoading()
                ui.list.isEmpty() -> AlertsEmpty { vm.refresh() }

                else -> NotificationList(
                    list = ui.list,
                    onClick = { n ->
                        vm.markRead(n)
                        handleNotificationClick(navController, n)
                    },
                    dateFormat = { n -> vm.formattedDate(n) }
                )
            }
        }
    }
}

// -----------------------------
// SUB UI
// -----------------------------
@Composable
fun AlertsLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(6.dp))
            Text("Loading...", color = Color.Gray)
        }
    }
}

@Composable
fun AlertsEmpty(onRefresh: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Notifications, null, tint = Color.Gray, modifier = Modifier.size(50.dp))
            Spacer(Modifier.height(10.dp))
            Text("No alerts available", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("तुमच्या सूचनांसाठी इथे तपासा.", fontSize = 13.sp, color = Color.DarkGray)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onRefresh) { Text("Refresh") }
        }
    }
}

@Composable
fun NotificationList(
    list: List<UserNotification>,
    onClick: (UserNotification) -> Unit,
    dateFormat: (UserNotification) -> String
) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(list, key = { it.id }) { n ->
            NotificationCard(
                n = n,
                onClick = { onClick(n) },
                dateLabel = dateFormat(n)
            )
        }
    }
}

@Composable
fun NotificationCard(
    n: UserNotification,
    onClick: () -> Unit,
    dateLabel: String
) {
    val bg = if (n.read) Color.White else Color(0xFFFFF6D5)
    val border = if (n.read) Color(0xFFE0E0E0) else Color(0xFFFFB300)

    // 🔁 type-wise body override (तू सांगितलेले चारही cases)
    val normalizedType = n.type.lowercase(Locale.getDefault())
    val resolvedBody = when (normalizedType) {
        // booking_status_change + booking_update — दोन्ही एकच
        "booking_status_change", "booking_update" ->
            if (n.body.isBlank())
                "Your puja booking has been approved / Guruji has been assigned for your booking."
            else n.body

        "booking_reminder" ->
            if (n.body.isBlank())
                "Reminder: Your puja is scheduled for tomorrow."
            else n.body

        "puja_status_update_reminder" ->
            "Reminder: If your puja is complete, please update the puja status."

        "instant_receipt", "instantreceipt" ->
            if (n.body.isBlank())
                "Your payment receipt has been generated. Please check."
            else n.body

        else -> n.body
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Column(Modifier.padding(12.dp)) {

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(n.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                if (!n.read) {
                    Box(
                        Modifier
                            .background(Color(0xFFFFD54F), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("New", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(resolvedBody, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(dateLabel, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

// -----------------------------
// NAVIGATION LOGIC
// -----------------------------
private fun handleNotificationClick(navController: NavController, n: UserNotification) {

    val typeKey = n.type.lowercase(Locale.getDefault())

    when {
        // booking_status_change / booking_update / booking_reminder → booking details
        n.bookingId != null &&
                (typeKey == "booking_status_change" ||
                        typeKey == "booking_update" ||
                        typeKey == "booking_reminder") -> {
            navController.navigate("bookingDetails/${n.bookingId}")
        }

        // puja status update reminder → dashboard + puja update section
        typeKey == "puja_status_update_reminder" -> {
            navController.navigate("${Routes.USER_DASHBOARD}?openPujaUpdate=true")
        }

        // instant receipt → InstantReceipt screen
        typeKey == "instant_receipt" || typeKey == "instantreceipt" -> {
            navController.navigate(Routes.INSTANT_RECEIPT)
        }

        // generic open dashboard
        typeKey == "open_dashboard" -> {
            navController.navigate(Routes.USER_DASHBOARD)
        }
    }
}
