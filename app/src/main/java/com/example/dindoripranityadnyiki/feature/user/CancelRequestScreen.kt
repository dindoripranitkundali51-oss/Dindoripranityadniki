package com.example.dindoripranityadnyiki.feature.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BookingSummary(
    val id: String,
    val poojaName: String,
    val date: Date?,
    val address: String,
    val status: String,
    val cancelRequested: Boolean
)

interface BookingRepository {
    suspend fun getCancelableBookingsForUser(userId: String): List<BookingSummary>

    /**
     * फक्त cancelRequested flag सेट करायचा – reason नाही.
     */
    suspend fun submitCancelRequest(
        bookingId: String,
        userId: String
    )
}

class FirebaseBookingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : BookingRepository {

    override suspend fun getCancelableBookingsForUser(userId: String): List<BookingSummary> {
        val snap = firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .whereIn("status", listOf("Pending", "Approved"))
            // 🔁 DESCENDING order so latest booking वर येईल (Dashboard सारखं)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snap.documents.mapNotNull { doc ->
            BookingSummary(
                id = doc.id,
                poojaName = doc.getString("poojaName") ?: return@mapNotNull null,
                date = doc.getTimestamp("date")?.toDate(),
                address = doc.getString("address") ?: "",
                status = doc.getString("status") ?: "Pending",
                cancelRequested = doc.getBoolean("cancelRequested") == true
            )
        }.filter { !it.cancelRequested } // ज्या booking साठी cancelRequested=false तेवढेच
    }

    override suspend fun submitCancelRequest(
        bookingId: String,
        userId: String
    ) {
        val docRef = firestore.collection("bookings").document(bookingId)

        firestore.runTransaction { txn ->
            val snap = txn.get(docRef)

            if (snap.getBoolean("cancelRequested") == true) {
                throw IllegalStateException("Cancel request already sent.")
            }

            val currentStatus = snap.getString("status") ?: "Pending"
            if (currentStatus.equals("Done", ignoreCase = true)) {
                throw IllegalStateException("Puja already completed.")
            }

            val updates = mapOf(
                "cancelRequested" to true,
                "cancelRequestedAt" to FieldValue.serverTimestamp(),
                "cancelRequestedBy" to userId,
                "status" to currentStatus
            )

            txn.update(docRef, updates)
        }.await()

        // Optional: generic admin notification (reason नाही)
        firestore.collection("admin_notifications").add(
            mapOf(
                "type" to "cancel_request",
                "bookingId" to bookingId,
                "userId" to userId,
                "note" to "User requested cancellation.",
                "createdAt" to FieldValue.serverTimestamp(),
                "handled" to false
            )
        )
    }
}

data class CancelRequestUiState(
    val isLoading: Boolean = false,
    val bookings: List<BookingSummary> = emptyList(),
    val selectedBookingId: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val navigateToPujaUpdate: Boolean = false
) {
    val hasSelection: Boolean get() = !selectedBookingId.isNullOrBlank()
}

class CancelRequestViewModel(
    private val repo: BookingRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow(CancelRequestUiState(isLoading = true))
    val state: StateFlow<CancelRequestUiState> = _state

    init {
        loadBookings()
    }

    fun loadBookings() {
        val uid = auth.currentUser?.uid ?: run {
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "कृपया आधी लॉगिन करा."
                )
            }
            return
        }

        _state.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            try {
                val list = repo.getCancelableBookingsForUser(uid)
                _state.update {
                    it.copy(
                        isLoading = false,
                        bookings = list
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unable to load bookings."
                    )
                }
            }
        }
    }

    fun selectBooking(id: String) {
        val isSame = _state.value.selectedBookingId == id
        _state.update {
            it.copy(
                selectedBookingId = if (isSame) null else id,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun submitCancelRequest() {
        val uid = auth.currentUser?.uid ?: run {
            _state.update { it.copy(errorMessage = "कृपया आधी लॉगिन करा.") }
            return
        }

        val cur = _state.value
        val bookingId = cur.selectedBookingId

        if (bookingId.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "कृपया आधी एक बुकिंग निवडा.") }
            return
        }

        _state.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            try {
                repo.submitCancelRequest(bookingId, uid)

                // पुन्हा list refresh (हा booking आता list मधून गायब होईल)
                val updated = repo.getCancelableBookingsForUser(uid)

                _state.update {
                    it.copy(
                        isLoading = false,
                        bookings = updated,
                        selectedBookingId = null,
                        successMessage = "Cancel request यशस्वीपणे पाठवली आहे.",
                        navigateToPujaUpdate = true  // 👉 Dashboard → Puja Status कडे नेण्यासाठी flag
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Cancel request पाठवताना समस्या आली."
                    )
                }
            }
        }
    }

    fun onNavigationHandled() {
        _state.update { it.copy(navigateToPujaUpdate = false) }
    }

    class Factory(
        private val repo: BookingRepository = FirebaseBookingRepository()
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CancelRequestViewModel(repo) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CancelRequestScreen(
    navController: NavController,
    vm: CancelRequestViewModel = viewModel(factory = CancelRequestViewModel.Factory())
) {
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // errors / success snackbar
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    // 🔁 Cancel request नंतर → Dashboard वर Puja Update सेक्शन open करणे
    LaunchedEffect(state.navigateToPujaUpdate) {
        if (state.navigateToPujaUpdate) {
            // AppNavGraph मध्ये route: "userDashboard?openPujaUpdate={openPujaUpdate}"
            navController.navigate("userDashboard?openPujaUpdate=true") {
                popUpTo("cancelRequest") { inclusive = true }
                launchSingleTop = true
            }
            vm.onNavigationHandled()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Cancel Request",
                        color = Color.Companion.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Companion.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Companion.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFB71C1C)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        Box(
            modifier = Modifier.Companion
                .padding(innerPadding)
                .fillMaxSize()
                .background(
                    Brush.Companion.verticalGradient(
                        listOf(Color(0xFFE3F2FD), Color.Companion.White, Color(0xFFBBDEFB))
                    )
                )
        ) {

            Column(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Text(
                    text = "रद्द करायची पूजा निवडा. Cancel Request सबमिट केल्यानंतर तुम्हाला Puja Status मध्ये 'Not Done' व Feedback भरायला नेण्यात येईल.",
                    color = Color(0xFF37474F),
                    fontSize = 14.sp
                )

                if (state.isLoading && state.bookings.isEmpty()) {
                    Box(
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        contentAlignment = Alignment.Companion.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (state.bookings.isEmpty()) {
                    EmptyCancelState()
                } else {
                    BookingListUI(
                        state = state,
                        onSelect = { vm.selectBooking(it) }
                    )
                }

                Spacer(modifier = Modifier.Companion.height(12.dp))

                Button(
                    onClick = { vm.submitCancelRequest() },
                    enabled = state.hasSelection && !state.isLoading,
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.hasSelection) Color(0xFFB71C1C) else Color(
                            0xFFCFD8DC
                        )
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = Color.Companion.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.Companion.size(20.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.EventBusy,
                            contentDescription = null,
                            modifier = Modifier.Companion.size(18.dp)
                        )
                        Spacer(modifier = Modifier.Companion.width(8.dp))
                        Text(
                            text = "Send Cancel Request",
                            color = Color.Companion.White,
                            fontWeight = FontWeight.Companion.Bold
                        )
                    }
                }

                Text(
                    text = "टीप: Cancel Request नंतरच Dashboard → Puja Status मध्ये 'Not Done' + Feedback भरून पूजा स्थिती पूर्ण अपडेट करा.",
                    fontSize = 12.sp,
                    color = Color(0xFF546E7A)
                )
            }
        }
    }
}

@Composable
fun BookingListUI(
    state: CancelRequestUiState,
    onSelect: (String) -> Unit
) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(Color.Companion.White)
    ) {
        LazyColumn(
            modifier = Modifier.Companion.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(state.bookings) { b ->
                Row(
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .clickable { onSelect(b.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.Companion.CenterVertically
                ) {
                    RadioButton(
                        selected = state.selectedBookingId == b.id,
                        onClick = { onSelect(b.id) }
                    )
                    Spacer(Modifier.Companion.width(10.dp))
                    Column {
                        Text(b.poojaName, fontWeight = FontWeight.Companion.Bold)
                        Text(
                            "Date: ${formatDate(b.date)}",
                            fontSize = 12.sp,
                            color = Color(0xFF546E7A)
                        )
                        if (b.address.isNotBlank()) {
                            Text(
                                b.address,
                                fontSize = 12.sp,
                                color = Color(0xFF78909C)
                            )
                        }
                        Text(
                            "Status: ${b.status}",
                            fontSize = 12.sp,
                            color = Color(0xFF6D4C41)
                        )
                    }
                }
                Divider()
            }
        }
    }
}

@Composable
fun EmptyCancelState() {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(Color.Companion.White)
    ) {
        Column(
            modifier = Modifier.Companion.padding(20.dp),
            horizontalAlignment = Alignment.Companion.CenterHorizontally
        ) {
            Icon(
                Icons.Default.WarningAmber,
                contentDescription = null,
                tint = Color(0xFFFFA000),
                modifier = Modifier.Companion.size(40.dp)
            )
            Text(
                "सध्या रद्द करण्यासाठी कोणतेही Pending / Approved बुकिंग उपलब्ध नाहीत.",
                color = Color(0xFF455A64),
                modifier = Modifier.Companion.padding(top = 8.dp)
            )
        }
    }
}

private fun formatDate(date: Date?): String {
    if (date == null) return "—"
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(date)
}