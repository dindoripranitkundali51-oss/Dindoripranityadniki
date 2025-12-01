package com.example.dindoripranityadnyiki.feature.user

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dindoripranityadnyiki.R
import com.example.dindoripranityadnyiki.core.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Simple constant to avoid magic string scattered in file
private const val EMPTY_REQUEST = "—"
private const val LOG_TAG = "Dashboard"

// UI model for dashboard bookings (multiple active bookings)
data class DashboardBookingUi(
    val id: String = EMPTY_REQUEST,
    val poojaName: String = "Not booked",
    val date: String = EMPTY_REQUEST,
    val address: String = EMPTY_REQUEST,
    val district: String = EMPTY_REQUEST,
    val gurujiName: String = EMPTY_REQUEST,
    val gurujiContact: String = EMPTY_REQUEST,
    val status: String = "Not booked"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    deepLinkBookingId: String? = null,                 // optional id from deep link/notification
    openPujaUpdateFromDeepLink: Boolean = false       // if true -> expand & scroll to Puja update UI
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // UI / data state
    var isLoading by remember { mutableStateOf(true) }

    // सर्व active bookings (Pending/Approved)
    var bookings by remember { mutableStateOf<List<DashboardBookingUi>>(emptyList()) }

    // सध्या कोणता booking card visible आहे
    var selectedBookingIndex by rememberSaveable { mutableStateOf(0) }

    // derived state: active booking आहे का?
    val activeBookingExists by derivedStateOf { bookings.isNotEmpty() }

    // सध्या दिसत असलेला booking (null जर काही नसेल)
    val currentBooking: DashboardBookingUi? = bookings.getOrNull(selectedBookingIndex)

    // local UI state (persist across recompositions)
    var pujaStatus by rememberSaveable { mutableStateOf("") }
    var feedbackText by rememberSaveable { mutableStateOf("") }
    var rating by rememberSaveable { mutableStateOf(0) }

    val statusOptions = listOf("Done", "Not Done")
    var statusExpanded by remember { mutableStateOf(false) }

    val textFieldWidthPx = remember { mutableStateOf(0) }

    // Drawer
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()

    // current route for bottom nav selection
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // snackbar + clipboard
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current

    // top-level scroll state (feedback expand झाल्यावर वापरणार)
    val scrollState = rememberScrollState()

    // guard to prevent rapid UI updates flicker on many snapshot events
    var lastAppliedSnapshotAt by remember { mutableStateOf(0L) }
    val snapshotDebounceMs = 300L

    // Helper: सर्व bookings apply करण्यासाठी
    fun applyBookingsFromList(list: List<DashboardBookingUi>) {
        bookings = list
        selectedBookingIndex = when {
            list.isEmpty() -> 0
            deepLinkBookingId.isNullOrBlank() -> 0
            else -> {
                val idx = list.indexOfFirst { it.id == deepLinkBookingId }
                if (idx >= 0) idx else 0
            }
        }
    }

    // -------- Deep-link fetch: जर deepLinkBookingId दिलं असेल तर त्या booking ला load करा
    LaunchedEffect(deepLinkBookingId) {
        if (!deepLinkBookingId.isNullOrBlank()) {
            isLoading = true
            try {
                // Try server first
                val docServer = db.collection("bookings")
                    .document(deepLinkBookingId)
                    .get(Source.SERVER)
                    .await()

                val finalDoc = if (docServer.exists()) {
                    docServer
                } else {
                    db.collection("bookings")
                        .document(deepLinkBookingId)
                        .get()
                        .await()
                }

                if (finalDoc.exists()) {
                    val dateStr = finalDoc.getTimestamp("date")
                        ?.toDate()
                        ?.let { sdf.format(it) }
                        ?: (finalDoc.getString("date") ?: EMPTY_REQUEST)

                    val ui = DashboardBookingUi(
                        id = finalDoc.id,
                        poojaName = finalDoc.getString("poojaName") ?: EMPTY_REQUEST,
                        date = dateStr,
                        address = finalDoc.getString("address") ?: EMPTY_REQUEST,
                        district = finalDoc.getString("district") ?: EMPTY_REQUEST,
                        gurujiName = finalDoc.getString("gurujiName") ?: EMPTY_REQUEST,
                        gurujiContact = finalDoc.getString("gurujiContact") ?: EMPTY_REQUEST,
                        status = finalDoc.getString("status") ?: "Pending"
                    )
                    applyBookingsFromList(listOf(ui))
                } else {
                    applyBookingsFromList(emptyList())
                }
            } catch (e: Exception) {
                Log.w(LOG_TAG, "deepLink fetch exception", e)
                applyBookingsFromList(emptyList())
            } finally {
                isLoading = false
            }
        }
    }

    // Firestore realtime listener for active bookings (multiple, latest first)
    DisposableEffect(key1 = auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            if (deepLinkBookingId.isNullOrBlank()) {
                applyBookingsFromList(emptyList())
            }
            isLoading = false
            onDispose { }
        } else {
            val q: Query = db.collection("bookings")
                .whereEqualTo("userId", uid)
                .whereIn("status", listOf("Pending", "Approved"))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10) // max 10 active bookings in carousel

            val registration: ListenerRegistration =
                q.addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
                    val now = System.currentTimeMillis()
                    if (now - lastAppliedSnapshotAt < snapshotDebounceMs) {
                        Log.d(
                            LOG_TAG,
                            "snapshot ignored due debounce (${now - lastAppliedSnapshotAt}ms)"
                        )
                        return@addSnapshotListener
                    }

                    if (err != null) {
                        Log.w(LOG_TAG, "listen:error", err)
                        scope.launch {
                            snackbarHostState.showSnackbar("Failed to listen for bookings")
                        }
                        isLoading = false
                        lastAppliedSnapshotAt = System.currentTimeMillis()
                        return@addSnapshotListener
                    }

                    val docs = snap?.documents ?: emptyList()
                    val fromCache = snap?.metadata?.isFromCache ?: false
                    Log.d(LOG_TAG, "snapshot size=${docs.size} fromCache=$fromCache")

                    if (docs.isEmpty()) {
                        if (deepLinkBookingId.isNullOrBlank()) {
                            applyBookingsFromList(emptyList())
                        }
                        isLoading = false
                        lastAppliedSnapshotAt = System.currentTimeMillis()
                        return@addSnapshotListener
                    }

                    // Map सर्व documents → UI list
                    val uiList = docs.map { doc ->
                        val dateStr = doc.getTimestamp("date")
                            ?.toDate()
                            ?.let { sdf.format(it) }
                            ?: (doc.getString("date") ?: EMPTY_REQUEST)

                        DashboardBookingUi(
                            id = doc.id,
                            poojaName = doc.getString("poojaName") ?: EMPTY_REQUEST,
                            date = dateStr,
                            address = doc.getString("address") ?: EMPTY_REQUEST,
                            district = doc.getString("district") ?: EMPTY_REQUEST,
                            gurujiName = doc.getString("gurujiName") ?: EMPTY_REQUEST,
                            gurujiContact = doc.getString("gurujiContact") ?: EMPTY_REQUEST,
                            status = doc.getString("status") ?: "Pending"
                        )
                    }

                    applyBookingsFromList(uiList)
                    isLoading = false
                    lastAppliedSnapshotAt = System.currentTimeMillis()
                }

            onDispose {
                try {
                    registration.remove()
                } catch (e: Exception) {
                    Log.w(LOG_TAG, "Failed to remove listener: ${e.localizedMessage}")
                }
            }
        }
    }

    // If deep link requested to open PujaUpdate, expand and scroll to bottom
    LaunchedEffect(openPujaUpdateFromDeepLink) {
        if (openPujaUpdateFromDeepLink) {
            delay(300)
            statusExpanded = true
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    // Safe navigation helper
    fun NavController.safeNavigate(route: String) {
        try {
            val cur = this.currentBackStackEntry?.destination?.route
            if (cur == route) return
            this.navigate(route)
        } catch (e: Exception) {
            Log.e("Nav", "safeNavigate failed", e)
        }
    }

    // Drawer item composable
    @Composable
    fun DrawerNavItem(
        icon: ImageVector,
        label: String,
        route: String,
        tint: Color = Color.Companion.Unspecified
    ) {
        Row(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .clickable {
                    drawerScope.launch {
                        drawerState.close()
                        delay(160)
                        navController.safeNavigate(route)
                    }
                }
                .padding(vertical = 12.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (tint == Color.Companion.Unspecified) LocalContentColor.current else tint
            )
            Spacer(modifier = Modifier.Companion.width(14.dp))
            Text(label, fontWeight = FontWeight.Companion.Medium, fontSize = 15.sp)
        }
    }

    // Submit handler (Done / Not Done + feedback) — नेहमी currentBooking वर काम करेल
    fun submitPujaUpdate() {
        scope.launch {
            if (!activeBookingExists) {
                Toast.makeText(ctx, "No active booking to submit feedback", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val bookingForUpdate = currentBooking
            if (bookingForUpdate == null || bookingForUpdate.id.isBlank() || bookingForUpdate.id == EMPTY_REQUEST) {
                Toast.makeText(ctx, "Unable to identify booking", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (pujaStatus.isBlank()) {
                Toast.makeText(ctx, "Please select status", Toast.LENGTH_SHORT).show()
                return@launch
            }
            // Not Done -> reason (feedback) compulsory
            if (pujaStatus == "Not Done" && feedbackText.isBlank()) {
                Toast.makeText(ctx, "कृपया कारण लिहा", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val uid = auth.currentUser?.uid
            val bookingId = bookingForUpdate.id
            if (uid == null) {
                Toast.makeText(ctx, "Unable to identify user", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val bookingRef = db.collection("bookings").document(bookingId)

            if (pujaStatus == "Done") {
                val updates = hashMapOf<String, Any>(
                    "status" to pujaStatus,
                    "statusUpdatedAt" to FieldValue.serverTimestamp(),
                    "statusUpdatedBy" to uid,
                    "lastFeedback" to (if (feedbackText.isBlank()) "" else feedbackText),
                    "rating" to rating,
                    "feedbackGiven" to (if (feedbackText.isBlank()) "" else feedbackText)
                )
                bookingRef.update(updates as Map<String, Any>)
                    .addOnSuccessListener {
                        val notif = hashMapOf(
                            "type" to "booking_status_change",
                            "bookingId" to bookingId,
                            "userId" to uid,
                            "newStatus" to pujaStatus,
                            "summary" to "User marked Done (rating: $rating)",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "handled" to false
                        )
                        db.collection("admin_notifications").add(notif).addOnCompleteListener {
                            scope.launch {
                                snackbarHostState.showSnackbar("Feedback submitted", "OK")
                            }
                            Toast.makeText(ctx, "Feedback submitted", Toast.LENGTH_SHORT).show()
                            feedbackText = ""
                            rating = 4
                            pujaStatus = ""
                            statusExpanded = false
                        }
                    }
                    .addOnFailureListener { ex ->
                        Log.w(LOG_TAG, "update failed", ex)
                        Toast.makeText(ctx, "Failed to submit. Please try again.", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Not Done — double safety: verify cancelRequested is true
                bookingRef.get().addOnSuccessListener { snapshot ->
                    val cancelRequested = snapshot?.getBoolean("cancelRequested") ?: false
                    if (!cancelRequested) {
                        Toast.makeText(ctx, "कृपया प्रथम Cancel Request पाठवा.", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }
                    val updates = hashMapOf<String, Any>(
                        "status" to pujaStatus,
                        "statusUpdatedAt" to FieldValue.serverTimestamp(),
                        "statusUpdatedBy" to uid,
                        "lastFeedback" to (if (feedbackText.isBlank()) "" else feedbackText),
                        "failureReason" to (if (feedbackText.isBlank()) "" else feedbackText)
                    )
                    bookingRef.update(updates as Map<String, Any>)
                        .addOnSuccessListener {
                            val notif = hashMapOf(
                                "type" to "booking_status_change",
                                "bookingId" to bookingId,
                                "userId" to uid,
                                "newStatus" to pujaStatus,
                                "summary" to "User reported Not Done: ${feedbackText.take(120)}",
                                "createdAt" to FieldValue.serverTimestamp(),
                                "handled" to false
                            )
                            db.collection("admin_notifications").add(notif).addOnCompleteListener {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Feedback submitted", "OK")
                                }
                                Toast.makeText(ctx, "Feedback submitted", Toast.LENGTH_SHORT).show()
                                feedbackText = ""
                                rating = 4
                                pujaStatus = ""
                                statusExpanded = false
                            }
                        }
                        .addOnFailureListener { ex ->
                            Log.w(LOG_TAG, "update failed", ex)
                            Toast.makeText(ctx, "Failed to submit. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                }.addOnFailureListener { ex ->
                    Log.w(LOG_TAG, "fetch booking failed", ex)
                    Toast.makeText(ctx, "Unable to verify cancel request. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // UI
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.Companion.width(240.dp)) {
                Column(Modifier.Companion.fillMaxSize()) {
                    Spacer(modifier = Modifier.Companion.height(18.dp))
                    Box(
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(
                                brush = Brush.Companion.verticalGradient(
                                    colors = listOf(Color(0xFF1565C0), Color(0xFF0D47A1))
                                )
                            ),
                        contentAlignment = Alignment.Companion.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "App logo",
                            modifier = Modifier.Companion
                                .height(110.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                    Spacer(modifier = Modifier.Companion.height(12.dp))

                    DrawerNavItem(
                        icon = Icons.Default.ReceiptLong,
                        label = "Instant Receipt",
                        route = Routes.INSTANT_RECEIPT
                    )
                    DrawerNavItem(
                        icon = Icons.Default.Cancel,
                        label = "Cancel Request",
                        route = Routes.CANCEL_REQUEST
                    )
                    DrawerNavItem(
                        icon = Icons.Default.SupportAgent,
                        label = "Help & Support",
                        route = Routes.HELP_SUPPORT
                    )
                    DrawerNavItem(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        route = Routes.SETTINGS
                    )
                    DrawerNavItem(
                        icon = Icons.Default.PrivacyTip,
                        label = "Privacy Policy",
                        route = Routes.PRIVACY
                    )
                    DrawerNavItem(
                        icon = Icons.Default.Description,
                        label = "Terms & Conditions",
                        route = Routes.TERMS
                    )

                    Spacer(modifier = Modifier.Companion.weight(1f))
                    Row(
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .clickable {
                                drawerScope.launch {
                                    drawerState.close()
                                    delay(120)
                                    FirebaseAuth.getInstance().signOut()
                                    navController.safeNavigate(Routes.LOGIN)
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 14.dp),
                        verticalAlignment = Alignment.Companion.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PowerSettingsNew,
                            contentDescription = "Logout",
                            tint = Color(0xFFB71C1C)
                        )
                        Spacer(modifier = Modifier.Companion.width(14.dp))
                        Text("Logout", fontWeight = FontWeight.Companion.Medium, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.Companion.height(12.dp))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Dindori Pranit Yadniki",
                            fontWeight = FontWeight.Companion.Bold,
                            color = Color.Companion.White,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                drawerScope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Open menu",
                                tint = Color.Companion.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFFB71C1C)
                    )
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.USER_DASHBOARD || currentRoute == null,
                        onClick = { navController.safeNavigate(Routes.USER_DASHBOARD) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.HISTORY,
                        onClick = { navController.safeNavigate(Routes.HISTORY) },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.ALERTS,
                        onClick = { navController.safeNavigate(Routes.ALERTS) },
                        icon = { Icon(Icons.Default.Notifications, contentDescription = "Alerts") },
                        label = { Text("Alerts") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.PROFILE,
                        onClick = { navController.safeNavigate(Routes.PROFILE) },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") }
                    )
                }
            }
        ) { inner ->
            Box(
                modifier = Modifier.Companion
                    .padding(inner)
                    .fillMaxSize()
                    .background(Color(0xFFF7F9FC))
                    .verticalScroll(scrollState)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    // 🔹 CTA
                    val ctaTitle = if (!activeBookingExists) {
                        "No active booking yet? Book your first puja."
                    } else {
                        "You have active puja bookings"
                    }

                    val ctaButtonLabel = if (!activeBookingExists) {
                        "Book your first puja"
                    } else {
                        "Book another puja"
                    }

                    Column(
                        modifier = Modifier.Companion.fillMaxWidth(),
                        horizontalAlignment = Alignment.Companion.CenterHorizontally
                    ) {
                        Text(
                            text = ctaTitle,
                            fontWeight = FontWeight.Companion.SemiBold,
                            fontSize = 15.sp,
                            color = Color(0xFF212121),
                            textAlign = TextAlign.Companion.Center,
                            modifier = Modifier.Companion
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )

                        Button(
                            onClick = { navController.safeNavigate(Routes.POOJA_SELECTION) },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                            modifier = Modifier.Companion
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(
                                Icons.Default.EventAvailable,
                                contentDescription = "Book",
                                modifier = Modifier.Companion.size(18.dp)
                            )
                            Spacer(modifier = Modifier.Companion.width(6.dp))
                            Text(
                                ctaButtonLabel,
                                color = Color.Companion.White,
                                fontWeight = FontWeight.Companion.SemiBold
                            )
                        }
                    }

                    // 🔹 Home Quick Actions (Instant Receipt / Cancel / Payments)
                    HomeQuickActionsRow(
                        onInstantReceipt = { navController.safeNavigate(Routes.INSTANT_RECEIPT) },
                        onCancelRequest = { navController.safeNavigate(Routes.CANCEL_REQUEST) },
                        onPayment = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Payments feature coming soon.")
                            }
                        }
                    )

                    // 🔹 Booking card — skeleton / no active / carousel
                    when {
                        isLoading -> {
                            BookingSkeleton()
                        }

                        bookings.isEmpty() -> {
                            NoActiveBookingCard()
                        }

                        else -> {
                            BookingCarousel(
                                bookings = bookings,
                                selectedIndex = selectedBookingIndex,
                                onSelectedIndexChange = { selectedBookingIndex = it },
                                onCopyRequestId = { id ->
                                    if (id != EMPTY_REQUEST) {
                                        clipboard.setText(AnnotatedString(id))
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Request ID copied")
                                        }
                                    }
                                },
                                onCallGuru = { number ->
                                    if (number.isNotBlank() && number != EMPTY_REQUEST) {
                                        try {
                                            val intent =
                                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            ctx.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                ctx,
                                                "Unable to start dialer",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        Toast.makeText(
                                            ctx,
                                            "No contact number available",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                    }

                    Text(
                        text = "After your puja, update your Puja Status below.",
                        fontWeight = FontWeight.Companion.Medium,
                        fontSize = 12.sp,
                        color = Color(0xFF37474F),
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 4.dp, top = 4.dp)
                    )

                    // 🔹 Puja Updates — नेहमी सध्या selected booking साठी
                    PujaUpdateCard(
                        pujaStatus = pujaStatus,
                        onStatusSelected = { pujaStatus = it },
                        statusExpanded = statusExpanded,
                        onToggleExpanded = { statusExpanded = it },
                        statusOptions = statusOptions,
                        textFieldWidthPx = textFieldWidthPx,
                        activeBookingExists = activeBookingExists,
                        feedbackText = feedbackText,
                        onFeedbackChange = { feedbackText = it },
                        rating = rating,
                        onRatingChange = { rating = it },
                        onSubmit = { submitPujaUpdate() },
                        bookingDateStr = currentBooking?.date ?: EMPTY_REQUEST,
                        bookingId = currentBooking?.id ?: EMPTY_REQUEST,
                        ctx = ctx,
                        onNavigateToCancel = {
                            drawerScope.launch {
                                drawerState.close()
                                delay(160)
                                navController.safeNavigate(Routes.CANCEL_REQUEST)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.Companion.height(6.dp))
                }
            }

            // ✅ फक्त जेव्हा status निवडला जातो (feedback भाग उघडतो)
            LaunchedEffect(pujaStatus) {
                if (pujaStatus.isNotBlank()) {
                    delay(180)
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }
        }
    }
}

@Composable
fun BookingSkeleton() {
    Column(modifier = Modifier.Companion.fillMaxWidth()) {
        Card(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.Companion.fillMaxWidth()
        ) {
            Column(modifier = Modifier.Companion.padding(12.dp)) {
                repeat(4) {
                    Box(
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(Color(0xFFECEFF1).copy(alpha = 0.9f))
                    )
                    Spacer(modifier = Modifier.Companion.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun NoActiveBookingCard() {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.Companion.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Companion.White)
    ) {
        Column(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.Companion.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EventBusy,
                contentDescription = null,
                tint = Color(0xFFB0BEC5),
                modifier = Modifier.Companion.size(34.dp)
            )
            Spacer(Modifier.Companion.height(6.dp))
            Text(
                text = "No active puja bookings.",
                fontWeight = FontWeight.Companion.SemiBold,
                color = Color(0xFF455A64),
                fontSize = 13.sp
            )
            Text(
                text = "Book your first puja using the button above.",
                fontSize = 12.sp,
                color = Color(0xFF78909C),
                textAlign = TextAlign.Companion.Center
            )
        }
    }
}

@Composable
fun BookingCarousel(
    bookings: List<DashboardBookingUi>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    onCopyRequestId: (String) -> Unit,
    onCallGuru: (String) -> Unit
) {
    Column {
        LazyRow(
            modifier = Modifier.Companion.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(bookings) { index, booking ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier.Companion
                        .fillParentMaxWidth()
                        .padding(horizontal = 2.dp)
                        .clickable { onSelectedIndexChange(index) }
                ) {
                    TwoColumnBookingCard(
                        booking = booking,
                        isSelected = isSelected,
                        onCopyRequestId = onCopyRequestId,
                        onCallGuru = onCallGuru
                    )
                }
            }
        }

        if (bookings.size > 1) {
            Spacer(Modifier.Companion.height(6.dp))
            Row(
                modifier = Modifier.Companion.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                bookings.forEachIndexed { index, _ ->
                    val size = if (index == selectedIndex) 10.dp else 6.dp
                    val color =
                        if (index == selectedIndex) Color(0xFFB71C1C) else Color(0xFFCFD8DC)
                    Box(
                        modifier = Modifier.Companion
                            .size(size)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                            .background(color)
                    )
                    Spacer(Modifier.Companion.width(4.dp))
                }
            }
        }
    }
}

@Composable
fun TwoColumnBookingCard(
    booking: DashboardBookingUi,
    isSelected: Boolean,
    onCopyRequestId: (String) -> Unit = {},
    onCallGuru: (String) -> Unit = {}
) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 10.dp else 6.dp),
        modifier = Modifier.Companion.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Companion.White)
    ) {
        Column(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .border(
                    BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) Color(0xFFB71C1C) else Color(0xFFDFB86B)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                )
        ) {

            Box(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(Color(0xFFB71C1C))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Companion.Center
            ) {
                Text(
                    "Active Pujas",
                    color = Color.Companion.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Companion.Bold
                )
            }

            val leftWeight = 0.35f
            val rightWeight = 0.65f

            @Composable
            fun RowField(label: String, valueContent: @Composable () -> Unit) {
                Row(
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Companion.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.Companion.weight(leftWeight),
                        contentAlignment = Alignment.Companion.CenterStart
                    ) {
                        Text(
                            label,
                            fontWeight = FontWeight.Companion.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF37474F)
                        )
                    }
                    Divider(
                        color = Color(0xFFDFB86B),
                        modifier = Modifier.Companion
                            .width(1.dp)
                            .height(32.dp)
                    )

                    Box(
                        modifier = Modifier.Companion
                            .weight(rightWeight)
                            .padding(start = 10.dp),
                        contentAlignment = Alignment.Companion.CenterStart
                    ) {
                        valueContent()
                    }
                }
            }

            RowField("Request ID / No.") {
                val id = booking.id
                Column {
                    Text(
                        text = id,
                        color = Color(0xFF0D47A1),
                        fontWeight = FontWeight.Companion.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.Companion
                            .clickable { if (id != EMPTY_REQUEST) onCopyRequestId(id) }
                            .semantics { contentDescription = "Request ID $id (tap to copy)" }
                    )
                }
            }

            DividerThin()

            RowField("Puja Name") {
                Text(text = booking.poojaName, fontSize = 14.sp)
            }
            DividerThin()

            RowField("Puja Date") {
                Text(text = booking.date, fontSize = 14.sp)
            }
            DividerThin()

            RowField("Address") {
                val addrRaw = booking.address
                val distRaw = booking.district

                val addr = addrRaw.trim()
                val dist = distRaw.trim()

                val combined = when {
                    addr.isNotEmpty() && addr != EMPTY_REQUEST &&
                            dist.isNotEmpty() && dist != EMPTY_REQUEST -> "$addr • $dist"

                    addr.isNotEmpty() && addr != EMPTY_REQUEST -> addr
                    dist.isNotEmpty() && dist != EMPTY_REQUEST -> dist
                    else -> EMPTY_REQUEST
                }

                Text(text = combined, fontSize = 14.sp)
            }
            DividerThin()

            RowField("Guruji Name & Contact") {
                val status = booking.status
                if (status.equals("Approved", ignoreCase = true)) {
                    val name = booking.gurujiName
                    val contact = booking.gurujiContact

                    Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                        Text(text = "$name · $contact", fontSize = 14.sp)
                        Spacer(modifier = Modifier.Companion.width(6.dp))

                        IconButton(onClick = {
                            if (contact != EMPTY_REQUEST) onCallGuru(contact)
                        }) {
                            Icon(Icons.Default.Call, contentDescription = "Call guru")
                        }
                    }
                } else {
                    Text("Not assigned yet", fontSize = 14.sp, color = Color.Companion.Gray)
                }
            }
            DividerThin()

            Row(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                Box(
                    modifier = Modifier.Companion.weight(0.35f),
                    contentAlignment = Alignment.Companion.CenterStart
                ) {
                    Text(
                        "Request Status",
                        fontWeight = FontWeight.Companion.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF37474F)
                    )
                }
                Divider(
                    color = Color(0xFFDFB86B),
                    modifier = Modifier.Companion
                        .width(1.dp)
                        .height(32.dp)
                )
                Box(
                    modifier = Modifier.Companion
                        .weight(0.65f)
                        .padding(start = 10.dp),
                    contentAlignment = Alignment.Companion.CenterStart
                ) {
                    val status = booking.status
                    Column {
                        StatusChip(status = status)
                    }
                }
            }
        }
    }
}

@Composable
fun PujaUpdateCard(
    pujaStatus: String,
    onStatusSelected: (String) -> Unit,
    statusExpanded: Boolean,
    onToggleExpanded: (Boolean) -> Unit,
    statusOptions: List<String>,
    textFieldWidthPx: MutableState<Int>,
    activeBookingExists: Boolean,
    feedbackText: String,
    onFeedbackChange: (String) -> Unit,
    rating: Int,
    onRatingChange: (Int) -> Unit,
    onSubmit: () -> Unit,
    bookingDateStr: String,
    bookingId: String,
    ctx: Context,
    onNavigateToCancel: () -> Unit
) {
    val firestore = remember { FirebaseFirestore.getInstance() }

    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.Companion.fillMaxWidth()
    ) {
        Column(modifier = Modifier.Companion.background(Color.Companion.White)) {

            Box(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .background(Color(0xFFB71C1C))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Companion.Center
            ) {
                Text(
                    "Puja Updates",
                    color = Color.Companion.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Companion.Bold
                )
            }

            val leftWeight = 0.35f
            val rightWeight = 0.65f

            Row(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                Text(
                    "Puja Status",
                    fontWeight = FontWeight.Companion.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF37474F),
                    modifier = Modifier.Companion.weight(leftWeight)
                )

                Divider(
                    color = Color(0xFFDFB86B),
                    modifier = Modifier.Companion
                        .width(1.dp)
                        .height(36.dp)
                )

                Box(
                    modifier = Modifier.Companion
                        .weight(rightWeight)
                        .padding(start = 10.dp)
                ) {

                    Box {
                        Row(
                            modifier = Modifier.Companion
                                .fillMaxWidth()
                                .height(44.dp)
                                .clickable { onToggleExpanded(!statusExpanded) }
                                .onGloballyPositioned { coords ->
                                    textFieldWidthPx.value = coords.size.width
                                },
                            verticalAlignment = Alignment.Companion.CenterVertically
                        ) {
                            val display = if (pujaStatus.isBlank()) "Select Status" else pujaStatus

                            Text(
                                text = display,
                                fontSize = 14.sp,
                                color = if (pujaStatus.isBlank()) Color.Companion.Gray else Color.Companion.Black,
                                modifier = Modifier.Companion
                                    .weight(1f)
                                    .padding(start = 4.dp)
                            )

                            IconButton(onClick = { onToggleExpanded(!statusExpanded) }) {
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }

                        val menuWidthDp =
                            with(LocalDensity.current) { textFieldWidthPx.value.toDp() }

                        DropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { onToggleExpanded(false) },
                            modifier = Modifier.Companion.width(menuWidthDp)
                        ) {
                            statusOptions.forEachIndexed { index, option ->
                                DropdownMenuItem(
                                    text = { Text(option, fontSize = 14.sp) },
                                    onClick = {
                                        onToggleExpanded(false)

                                        when (option) {
                                            "Done" -> {
                                                // Date validation: future date असल्यास Done allow करू नये
                                                val ok = try {
                                                    if (bookingDateStr.isBlank() || bookingDateStr == EMPTY_REQUEST) {
                                                        false
                                                    } else {
                                                        val sdfLocal = SimpleDateFormat(
                                                            "dd/MM/yyyy",
                                                            Locale.getDefault()
                                                        )
                                                        val bookingDate =
                                                            sdfLocal.parse(bookingDateStr)
                                                        if (bookingDate == null) {
                                                            false
                                                        } else {
                                                            val calBooking =
                                                                Calendar.getInstance().apply {
                                                                    time = bookingDate
                                                                    set(Calendar.HOUR_OF_DAY, 0)
                                                                    set(Calendar.MINUTE, 0)
                                                                    set(Calendar.SECOND, 0)
                                                                    set(Calendar.MILLISECOND, 0)
                                                                }
                                                            val calToday =
                                                                Calendar.getInstance().apply {
                                                                    set(Calendar.HOUR_OF_DAY, 0)
                                                                    set(Calendar.MINUTE, 0)
                                                                    set(Calendar.SECOND, 0)
                                                                    set(Calendar.MILLISECOND, 0)
                                                                }
                                                            !calToday.time.before(calBooking.time)
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    false
                                                }

                                                if (!ok) {
                                                    Toast
                                                        .makeText(
                                                            ctx,
                                                            "कृपया पुजा पूर्ण झाल्यानंतरच 'Done' निवडा. पूजेची तारीख: $bookingDateStr",
                                                            Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                                } else {
                                                    if (option == pujaStatus) {
                                                        onStatusSelected("")
                                                    } else {
                                                        onStatusSelected(option)
                                                    }
                                                }
                                            }

                                            "Not Done" -> {
                                                if (bookingId.isBlank() || bookingId == EMPTY_REQUEST) {
                                                    Toast
                                                        .makeText(
                                                            ctx,
                                                            "Unable to verify booking. कृपया नंतर पुन्हा प्रयत्न करा.",
                                                            Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                                } else {
                                                    firestore.collection("bookings")
                                                        .document(bookingId)
                                                        .get()
                                                        .addOnSuccessListener { snap ->
                                                            val cancelRequested =
                                                                snap?.getBoolean("cancelRequested")
                                                                    ?: false
                                                            if (cancelRequested) {
                                                                if (option == pujaStatus) {
                                                                    onStatusSelected("")
                                                                } else {
                                                                    onStatusSelected(option)
                                                                }
                                                            } else {
                                                                Toast
                                                                    .makeText(
                                                                        ctx,
                                                                        "कृपया प्रथम Cancel Request पाठवा.",
                                                                        Toast.LENGTH_LONG
                                                                    )
                                                                    .show()
                                                                onNavigateToCancel()
                                                            }
                                                        }
                                                        .addOnFailureListener {
                                                            Toast
                                                                .makeText(
                                                                    ctx,
                                                                    "Unable to verify cancel request. कृपया नंतर पुन्हा प्रयत्न करा.",
                                                                    Toast.LENGTH_SHORT
                                                                )
                                                                .show()
                                                        }
                                                }
                                            }

                                            else -> {
                                                if (option == pujaStatus) {
                                                    onStatusSelected("")
                                                } else {
                                                    onStatusSelected(option)
                                                }
                                            }
                                        }
                                    }
                                )
                                if (index < statusOptions.lastIndex) {
                                    Divider()
                                }
                            }
                        }
                    }
                }
            }

            DividerThin()

            AnimatedVisibility(
                visible = pujaStatus.isNotBlank(),
                enter = expandVertically(tween(300)) + fadeIn(),
                exit = shrinkVertically(tween(220)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { onFeedbackChange(it) },
                        label = { Text("Give Feedback (Reason)") },
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .height(100.dp),
                        enabled = activeBookingExists,
                        placeholder = {
                            Text(
                                if (!activeBookingExists) "No active booking"
                                else "Write about your experience / reason..."
                            )
                        }
                    )

                    AnimatedVisibility(visible = pujaStatus == "Done") {
                        Column {
                            Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                                Text("Rating:", fontSize = 14.sp)
                                Spacer(Modifier.Companion.width(8.dp))
                                StarRating(rating) { onRatingChange(it) }
                            }
                            Spacer(modifier = Modifier.Companion.height(2.dp))
                            Text(
                                text = "Tap stars to rate your experience.",
                                fontSize = 11.sp,
                                color = Color(0xFF78909C)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.Companion.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = onSubmit,
                            enabled = activeBookingExists,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        ) {
                            Text("Submit")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DividerThin() {
    Divider(color = Color(0xFFDFB86B), thickness = 1.dp)
}

@Composable
fun StatusChip(status: String) {
    val key = status.replace(" ", "").lowercase(Locale.getDefault())

    val (bg, fg) = when (key) {
        "pending" -> Pair(Color(0xFFFFF3CD), Color(0xFF856404))
        "approved" -> Pair(Color(0xFFD4EDDA), Color(0xFF155724))
        "done" -> Pair(Color(0xFFD4EDDA), Color(0xFF155724))
        "notdone" -> Pair(Color(0xFFFFD6D6), Color(0xFF8B0000))
        "notbooked" -> Pair(Color(0xFFF1F5F9), Color(0xFF90A4AE))
        else -> Pair(Color(0xFFF1F5F9), Color(0xFF607D8B))
    }

    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = bg
    ) {
        Text(
            text = status,
            color = fg,
            fontSize = 13.sp,
            modifier = Modifier.Companion.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StarRating(
    rating: Int,
    onRatingChanged: (Int) -> Unit
) {
    Row {
        (1..5).forEach { i ->
            IconButton(
                onClick = {
                    val newRating = if (rating == i) 0 else i
                    onRatingChanged(newRating)
                }
            ) {
                Icon(
                    imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "star $i",
                    tint = if (i <= rating) Color(0xFFFFC107) else Color(0xFFB0BEC5)
                )
            }
        }
    }
}

// 🔹 Home Quick Actions Row (Instant Receipt / Cancel Request / Payments)
@Composable
fun HomeQuickActionsRow(
    onInstantReceipt: () -> Unit,
    onCancelRequest: () -> Unit,
    onPayment: () -> Unit
) {
    Row(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HomeQuickActionButton(
            modifier = Modifier.Companion.weight(1f),
            icon = Icons.Default.ReceiptLong,
            title = "Instant Receipt",
            subtitle = "Download & share",
            onClick = onInstantReceipt
        )
        HomeQuickActionButton(
            modifier = Modifier.Companion.weight(1f),
            icon = Icons.Default.Cancel,
            title = "Cancel Request",
            subtitle = "Manage cancellations",
            onClick = onCancelRequest
        )
        HomeQuickActionButton(
            modifier = Modifier.Companion.weight(1f),
            icon = Icons.Default.AccountBalanceWallet,
            title = "Payments",
            subtitle = "Coming soon",
            onClick = onPayment
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeQuickActionButton(
    modifier: Modifier = Modifier.Companion,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Companion.White
        )
    ) {
        Box(
            modifier = Modifier.Companion
                .fillMaxSize()
                .background(
                    Brush.Companion.verticalGradient(
                        listOf(
                            Color(0xFFFFF8E1),
                            Color(0xFFFFF3E0)
                        )
                    )
                )
                .border(
                    BorderStroke(1.dp, Color(0xFFFFCA28).copy(alpha = 0.5f)),
                    androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier.Companion.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFFF57C00),
                    modifier = Modifier.Companion.size(20.dp)
                )
                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Companion.SemiBold,
                        color = Color(0xFF0D47A1)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}