// DashboardScreen.kt
package com.example.dindoripranityadnyiki.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dindoripranityadnyiki.R
import com.example.dindoripranityadnyiki.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Simple constant to avoid magic string scattered in file
private const val EMPTY_REQUEST = "—"
private const val LOG_TAG = "Dashboard"

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
    var activeBookingExists by remember { mutableStateOf(false) }
    var booking by remember { mutableStateOf(defaultBooking()) }

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

    // top-level scroll state so we can programmatically scroll
    val scrollState = rememberScrollState()

    // guard to prevent rapid UI updates flicker on many snapshot events
    var lastAppliedSnapshotAt by remember { mutableStateOf(0L) }
    val snapshotDebounceMs = 300L

    // Helper: safely apply booking map from a DocumentSnapshot-like accessor
    fun applyBookingFromMap(map: Map<String, String>, markActive: Boolean = true) {
        booking = map
        activeBookingExists = markActive && (map["id"] != null && map["id"] != EMPTY_REQUEST)
    }

    // -------- Deep-link fetch: if deepLinkBookingId provided, fetch that booking once from server
    LaunchedEffect(deepLinkBookingId) {
        if (!deepLinkBookingId.isNullOrBlank()) {
            isLoading = true
            try {
                db.collection("bookings").document(deepLinkBookingId).get(Source.SERVER)
                    .addOnSuccessListener { doc ->
                        if (doc != null && doc.exists()) {
                            val dateStr = doc.getTimestamp("date")?.toDate()?.let { sdf.format(it) }
                                ?: (doc.getString("date") ?: EMPTY_REQUEST)
                            applyBookingFromMap(
                                mapOf(
                                    "id" to doc.id,
                                    "poojaName" to (doc.getString("poojaName") ?: EMPTY_REQUEST),
                                    "date" to dateStr,
                                    "address" to (doc.getString("address") ?: EMPTY_REQUEST),
                                    "district" to (doc.getString("district") ?: EMPTY_REQUEST),
                                    "gurujiName" to (doc.getString("gurujiName") ?: EMPTY_REQUEST),
                                    "gurujiContact" to (doc.getString("gurujiContact") ?: EMPTY_REQUEST),
                                    "status" to (doc.getString("status") ?: "Pending")
                                ),
                                markActive = true
                            )
                        } else {
                            // fallback: try cache if server doesn't have it
                            db.collection("bookings").document(deepLinkBookingId).get()
                                .addOnSuccessListener { doc2 ->
                                    if (doc2 != null && doc2.exists()) {
                                        val dateStr = doc2.getTimestamp("date")?.toDate()?.let { sdf.format(it) }
                                            ?: (doc2.getString("date") ?: EMPTY_REQUEST)
                                        applyBookingFromMap(
                                            mapOf(
                                                "id" to doc2.id,
                                                "poojaName" to (doc2.getString("poojaName") ?: EMPTY_REQUEST),
                                                "date" to dateStr,
                                                "address" to (doc2.getString("address") ?: EMPTY_REQUEST),
                                                "district" to (doc2.getString("district") ?: EMPTY_REQUEST),
                                                "gurujiName" to (doc2.getString("gurujiName") ?: EMPTY_REQUEST),
                                                "gurujiContact" to (doc2.getString("gurujiContact") ?: EMPTY_REQUEST),
                                                "status" to (doc2.getString("status") ?: "Pending")
                                            ), markActive = true
                                        )
                                    } else {
                                        applyBookingFromMap(defaultBooking(), markActive = false)
                                    }
                                }
                                .addOnFailureListener { ex2 ->
                                    Log.w(LOG_TAG, "deepLink cache fetch failed", ex2)
                                    applyBookingFromMap(defaultBooking(), markActive = false)
                                }
                        }
                        isLoading = false
                    }
                    .addOnFailureListener { ex ->
                        Log.w(LOG_TAG, "deepLink server fetch failed, falling back to cache", ex)
                        db.collection("bookings").document(deepLinkBookingId).get()
                            .addOnSuccessListener { doc2 ->
                                if (doc2 != null && doc2.exists()) {
                                    val dateStr = doc2.getTimestamp("date")?.toDate()?.let { sdf.format(it) }
                                        ?: (doc2.getString("date") ?: EMPTY_REQUEST)
                                    applyBookingFromMap(
                                        mapOf(
                                            "id" to doc2.id,
                                            "poojaName" to (doc2.getString("poojaName") ?: EMPTY_REQUEST),
                                            "date" to dateStr,
                                            "address" to (doc2.getString("address") ?: EMPTY_REQUEST),
                                            "district" to (doc2.getString("district") ?: EMPTY_REQUEST),
                                            "gurujiName" to (doc2.getString("gurujiName") ?: EMPTY_REQUEST),
                                            "gurujiContact" to (doc2.getString("gurujiContact") ?: EMPTY_REQUEST),
                                            "status" to (doc2.getString("status") ?: "Pending")
                                        ), markActive = true
                                    )
                                } else {
                                    applyBookingFromMap(defaultBooking(), markActive = false)
                                }
                                isLoading = false
                            }
                            .addOnFailureListener { ex2 ->
                                Log.w(LOG_TAG, "deepLink cache fetch also failed", ex2)
                                applyBookingFromMap(defaultBooking(), markActive = false)
                                isLoading = false
                            }
                    }
            } catch (e: Exception) {
                Log.w(LOG_TAG, "deepLink fetch exception", e)
                applyBookingFromMap(defaultBooking(), markActive = false)
                isLoading = false
            }
        }
    }

    // Firestore realtime listener for active bookings (user's most recent active booking)
    DisposableEffect(key1 = auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            if (deepLinkBookingId.isNullOrBlank()) {
                applyBookingFromMap(defaultBooking(), markActive = false)
            }
            isLoading = false
            onDispose { }
        } else {
            val q: Query = db.collection("bookings")
                .whereEqualTo("userId", uid)
                .whereIn("status", listOf("Pending", "Approved"))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)

            val registration: ListenerRegistration = q.addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
                val now = System.currentTimeMillis()
                if (now - lastAppliedSnapshotAt < snapshotDebounceMs) {
                    Log.d(LOG_TAG, "snapshot ignored due debounce (${now - lastAppliedSnapshotAt}ms)")
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
                        applyBookingFromMap(defaultBooking(), markActive = false)
                    }
                    isLoading = false
                    lastAppliedSnapshotAt = System.currentTimeMillis()
                    return@addSnapshotListener
                }

                val first = docs[0]
                val snapshotId = first.id
                if (!deepLinkBookingId.isNullOrBlank() && deepLinkBookingId != snapshotId) {
                    Log.d(LOG_TAG, "deep-link active; skipping overwrite from listener (listener doc=$snapshotId)")
                    isLoading = false
                    lastAppliedSnapshotAt = System.currentTimeMillis()
                    return@addSnapshotListener
                }

                if (fromCache) {
                    val id = first.id
                    db.collection("bookings").document(id).get(Source.SERVER)
                        .addOnSuccessListener { serverDoc ->
                            if (!serverDoc.exists()) {
                                if (deepLinkBookingId.isNullOrBlank() || deepLinkBookingId != id) {
                                    applyBookingFromMap(defaultBooking(), markActive = false)
                                }
                                isLoading = false
                                lastAppliedSnapshotAt = System.currentTimeMillis()
                            } else {
                                val dateStr = serverDoc.getTimestamp("date")?.toDate()?.let { sdf.format(it) }
                                    ?: (serverDoc.getString("date") ?: EMPTY_REQUEST)
                                if (deepLinkBookingId.isNullOrBlank() || deepLinkBookingId != serverDoc.id) {
                                    applyBookingFromMap(
                                        mapOf(
                                            "id" to serverDoc.id,
                                            "poojaName" to (serverDoc.getString("poojaName") ?: EMPTY_REQUEST),
                                            "date" to dateStr,
                                            "address" to (serverDoc.getString("address") ?: EMPTY_REQUEST),
                                            "district" to (serverDoc.getString("district") ?: EMPTY_REQUEST),
                                            "gurujiName" to (serverDoc.getString("gurujiName") ?: EMPTY_REQUEST),
                                            "gurujiContact" to (serverDoc.getString("gurujiContact") ?: EMPTY_REQUEST),
                                            "status" to (serverDoc.getString("status") ?: "Pending")
                                        ), markActive = true
                                    )
                                }
                                isLoading = false
                                lastAppliedSnapshotAt = System.currentTimeMillis()
                            }
                        }
                        .addOnFailureListener { ex ->
                            Log.w(LOG_TAG, "server-get failed during reconcile", ex)
                            val dateStr = first.getTimestamp("date")?.toDate()?.let { sdf.format(it) }
                                ?: (first.getString("date") ?: EMPTY_REQUEST)
                            if (deepLinkBookingId.isNullOrBlank() || deepLinkBookingId != first.id) {
                                applyBookingFromMap(
                                    mapOf(
                                        "id" to first.id,
                                        "poojaName" to (first.getString("poojaName") ?: EMPTY_REQUEST),
                                        "date" to dateStr,
                                        "address" to (first.getString("address") ?: EMPTY_REQUEST),
                                        "district" to (first.getString("district") ?: EMPTY_REQUEST),
                                        "gurujiName" to (first.getString("gurujiName") ?: EMPTY_REQUEST),
                                        "gurujiContact" to (first.getString("gurujiContact") ?: EMPTY_REQUEST),
                                        "status" to (first.getString("status") ?: "Pending")
                                    ), markActive = true
                                )
                            }
                            isLoading = false
                            lastAppliedSnapshotAt = System.currentTimeMillis()
                        }
                } else {
                    val dateStr = first.getTimestamp("date")?.toDate()?.let { sdf.format(it) }
                        ?: (first.getString("date") ?: EMPTY_REQUEST)

                    if (deepLinkBookingId.isNullOrBlank() || deepLinkBookingId != first.id) {
                        applyBookingFromMap(
                            mapOf(
                                "id" to first.id,
                                "poojaName" to (first.getString("poojaName") ?: EMPTY_REQUEST),
                                "date" to dateStr,
                                "address" to (first.getString("address") ?: EMPTY_REQUEST),
                                "district" to (first.getString("district") ?: EMPTY_REQUEST),
                                "gurujiName" to (first.getString("gurujiName") ?: EMPTY_REQUEST),
                                "gurujiContact" to (first.getString("gurujiContact") ?: EMPTY_REQUEST),
                                "status" to (first.getString("status") ?: "Pending")
                            ), markActive = true
                        )
                    }
                    isLoading = false
                    lastAppliedSnapshotAt = System.currentTimeMillis()
                }
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
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        label: String,
        route: String,
        tint: Color = Color.Unspecified
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    drawerScope.launch {
                        drawerState.close()
                        delay(160)
                        navController.safeNavigate(route)
                    }
                }
                .padding(vertical = 12.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = if (tint == Color.Unspecified) LocalContentColor.current else tint)
            Spacer(modifier = Modifier.width(14.dp))
            Text(label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }
    }

    // Submit handler (Done / Not Done + feedback)
    fun submitPujaUpdate() {
        scope.launch {
            if (!activeBookingExists) {
                Toast.makeText(ctx, "No active booking to submit feedback", Toast.LENGTH_SHORT).show()
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
            val bookingId = booking["id"] ?: EMPTY_REQUEST
            if (bookingId.isBlank() || bookingId == EMPTY_REQUEST || uid == null) {
                Toast.makeText(ctx, "Unable to identify booking or user", Toast.LENGTH_SHORT).show()
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
            ModalDrawerSheet(modifier = Modifier.width(240.dp)) {
                Column(Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF1565C0), Color(0xFF0D47A1))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "App logo",
                            modifier = Modifier
                                .height(110.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

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

                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PowerSettingsNew,
                            contentDescription = "Logout",
                            tint = Color(0xFFB71C1C)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text("Logout", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
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
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
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
                            Icon(Icons.Default.Menu, contentDescription = "Open menu", tint = Color.White)
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
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .background(Color(0xFFF7F9FC))
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    // CTA
                    val ctaTitle = if (!activeBookingExists) {
                        "No active booking yet? Book your first puja."
                    } else {
                        "You have an active booking"
                    }

                    val ctaButtonLabel = if (!activeBookingExists) {
                        "Book your first puja"
                    } else {
                        "Book another puja"
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = ctaTitle,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color(0xFF212121),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        Button(
                            onClick = { navController.safeNavigate(Routes.POOJA_SELECTION) },
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                Icons.Default.EventAvailable,
                                contentDescription = "Book",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(ctaButtonLabel, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Booking card — show skeleton if loading
                    if (isLoading) {
                        BookingSkeleton()
                    } else {
                        TwoColumnBookingCard(
                            booking = booking,
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

                    Text(
                        text = "Is your puja completed? Update your Puja Status below.",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color(0xFF37474F),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 4.dp)
                    )

                    // Puja Updates
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
                        bookingDateStr = booking["date"] ?: EMPTY_REQUEST,
                        bookingId = booking["id"] ?: EMPTY_REQUEST,
                        ctx = ctx,
                        onNavigateToCancel = {
                            drawerScope.launch {
                                drawerState.close()
                                delay(160)
                                navController.safeNavigate(Routes.CANCEL_REQUEST)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // scroll to expanded area when status selected manually
            LaunchedEffect(pujaStatus) {
                if (pujaStatus.isNotBlank()) {
                    delay(180)
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }
        }
    }
}

// -----------------------------
// Small helper composables & helpers
// -----------------------------

private fun defaultBooking(): Map<String, String> {
    return mapOf(
        "id" to EMPTY_REQUEST,
        "poojaName" to "Not booked",
        "date" to EMPTY_REQUEST,
        "address" to EMPTY_REQUEST,
        "district" to EMPTY_REQUEST,
        "gurujiName" to EMPTY_REQUEST,
        "gurujiContact" to EMPTY_REQUEST,
        "status" to "Not booked"
    )
}

@Composable
fun BookingSkeleton() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                repeat(5) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .background(Color(0xFFECEFF1).copy(alpha = 0.9f))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun TwoColumnBookingCard(
    booking: Map<String, String>,
    onCopyRequestId: (String) -> Unit = {},
    onCallGuru: (String) -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(1.dp, Color(0xFFDFB86B)),
                    shape = RoundedCornerShape(10.dp)
                )
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(Color(0xFFB71C1C))
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Active Pujas",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            val leftWeight = 0.35f
            val rightWeight = 0.65f

            @Composable
            fun RowField(label: String, valueContent: @Composable () -> Unit) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(leftWeight),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            label,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF37474F)
                        )
                    }
                    Divider(
                        color = Color(0xFFDFB86B),
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(rightWeight)
                            .padding(start = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        valueContent()
                    }
                }
            }

            RowField("Request ID / No.") {
                val id = booking["id"] ?: EMPTY_REQUEST
                Column {
                    Text(
                        text = id,
                        color = Color(0xFF0D47A1),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { if (id != EMPTY_REQUEST) onCopyRequestId(id) }
                            .semantics { contentDescription = "Request ID $id (tap to copy)" }
                    )
                    if (id != EMPTY_REQUEST) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tap to copy",
                            fontSize = 11.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }
            }
            DividerThin()

            RowField("Puja Name") {
                Text(text = booking["poojaName"] ?: EMPTY_REQUEST, fontSize = 15.sp)
            }
            DividerThin()

            RowField("Puja Date") {
                Text(text = booking["date"] ?: EMPTY_REQUEST, fontSize = 15.sp)
            }
            DividerThin()

            RowField("Address") {
                val addrRaw = booking["address"] ?: EMPTY_REQUEST
                val distRaw = booking["district"] ?: EMPTY_REQUEST

                val addr = addrRaw.trim()
                val dist = distRaw.trim()

                val combined = when {
                    addr.isNotEmpty() && addr != EMPTY_REQUEST &&
                            dist.isNotEmpty() && dist != EMPTY_REQUEST -> "$addr • $dist"
                    addr.isNotEmpty() && addr != EMPTY_REQUEST -> addr
                    dist.isNotEmpty() && dist != EMPTY_REQUEST -> dist
                    else -> EMPTY_REQUEST
                }

                Text(text = combined, fontSize = 15.sp)
            }
            DividerThin()

            RowField("Guruji Name & Contact") {
                val status = booking["status"] ?: ""
                if (status.equals("Approved", ignoreCase = true)) {
                    val name = booking["gurujiName"] ?: EMPTY_REQUEST
                    val contact = booking["gurujiContact"] ?: EMPTY_REQUEST

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "$name · $contact", fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(10.dp))

                        IconButton(onClick = {
                            if (contact != EMPTY_REQUEST) onCallGuru(contact)
                        }) {
                            Icon(Icons.Default.Call, contentDescription = "Call guru")
                        }
                    }
                } else {
                    Text("Not assigned yet", fontSize = 15.sp, color = Color.Gray)
                }
            }
            DividerThin()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(0.35f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        "Request Status",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF37474F)
                    )
                }
                Divider(
                    color = Color(0xFFDFB86B),
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(0.65f)
                        .padding(start = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val status = booking["status"] ?: "Pending"
                    val statusKey = status.lowercase(Locale.getDefault())

                    val helperText = when (statusKey) {
                        "pending" -> "Guruji assignment is in progress."
                        "approved" -> "Guruji details are shared above."
                        "done" -> "Puja completed. You can share feedback below."
                        else -> ""
                    }

                    Column {
                        StatusChip(status = status)
                        if (helperText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = helperText,
                                fontSize = 12.sp,
                                color = Color(0xFF78909C)
                            )
                        }
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
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.background(Color.White)) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFB71C1C))
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Puja Updates",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            val leftWeight = 0.35f
            val rightWeight = 0.65f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Puja Status",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color(0xFF37474F),
                    modifier = Modifier.weight(leftWeight)
                )

                Divider(
                    color = Color(0xFFDFB86B),
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(rightWeight)
                        .padding(start = 12.dp)
                ) {

                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable { onToggleExpanded(!statusExpanded) }
                                .onGloballyPositioned { coords ->
                                    textFieldWidthPx.value = coords.size.width
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val display = if (pujaStatus.isBlank()) "Select Status" else pujaStatus

                            Text(
                                text = display,
                                fontSize = 15.sp,
                                color = if (pujaStatus.isBlank()) Color.Gray else Color.Black,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp)
                            )

                            IconButton(onClick = { onToggleExpanded(!statusExpanded) }) {
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }

                        val menuWidthDp = with(LocalDensity.current) { textFieldWidthPx.value.toDp() }

                        DropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { onToggleExpanded(false) },
                            modifier = Modifier.width(menuWidthDp)
                        ) {
                            statusOptions.forEachIndexed { index, option ->
                                DropdownMenuItem(
                                    text = { Text(option, fontSize = 15.sp) },
                                    onClick = {
                                        onToggleExpanded(false)

                                        when (option) {
                                            "Done" -> {
                                                // ✅ Date validation: future date असल्यास Done allow करू नये
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
                                                            // आज >= bookingDate असेल तर true
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
                                                    // toggle style: same option पुन्हा निवडल्यास clear
                                                    if (option == pujaStatus) {
                                                        onStatusSelected("")
                                                    } else {
                                                        onStatusSelected(option)
                                                    }
                                                }
                                            }

                                            "Not Done" -> {
                                                // प्रथम cancelRequested check करा
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
                                                                // Cancel request आधीच दिलेली → आता Not Done आणि feedback
                                                                if (option == pujaStatus) {
                                                                    onStatusSelected("")
                                                                } else {
                                                                    onStatusSelected(option)
                                                                }
                                                            } else {
                                                                // आधी Cancel Request स्क्रीनला ने
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

            // helper text under Puja Status
            Text(
                text = "Select 'Done' only after puja is completed.",
                fontSize = 12.sp,
                color = Color(0xFF78909C),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 4.dp)
            )

            DividerThin()

            AnimatedVisibility(
                visible = pujaStatus.isNotBlank(),
                enter = expandVertically(tween(300)) + fadeIn(),
                exit = shrinkVertically(tween(220)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { onFeedbackChange(it) },
                        label = { Text("Give Feedback (Reason)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Rating:", fontSize = 15.sp)
                                Spacer(Modifier.width(8.dp))
                                StarRating(rating) { onRatingChange(it) }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tap stars to rate your experience.",
                                fontSize = 12.sp,
                                color = Color(0xFF78909C)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = onSubmit,
                            enabled = activeBookingExists,
                            shape = RoundedCornerShape(28.dp)
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
        shape = RoundedCornerShape(20.dp),
        color = bg
    ) {
        Text(
            text = status,
            color = fg,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
                    // same star पुन्हा दाबला तर rating = 0 (clear)
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
