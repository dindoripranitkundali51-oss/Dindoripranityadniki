package com.example.dindoripranityadnyiki.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// -----------------------------------------------------------------------------
// DATA MODELS (ONE FILE)
// -----------------------------------------------------------------------------

enum class SupportCategory {
    BOOKING,
    PAYMENT,
    APP_ISSUE,
    FEEDBACK,
    OTHER
}

data class SupportUserProfile(
    val fullName: String? = null,
    val mobile: String? = null,
    val email: String? = null
)

data class HelpSupportUiState(
    val subject: String = "",
    val message: String = "",
    val category: SupportCategory = SupportCategory.BOOKING,
    val bookingId: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val profile: SupportUserProfile? = null
)

// -----------------------------------------------------------------------------
// REPOSITORY (ONE FILE)
// -----------------------------------------------------------------------------

class SupportRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    suspend fun getUserProfile(): SupportUserProfile? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snap = db.collection("users").document(uid).get().await()
            SupportUserProfile(
                fullName = snap.getString("fullName"),
                mobile = snap.getString("mobile"),
                email = snap.getString("email")
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun submitTicket(
        sub: String,
        msg: String,
        cat: SupportCategory,
        bookingId: String?,
        profile: SupportUserProfile?
    ): Result<String> {

        val uid = auth.currentUser?.uid ?: return Result.failure(
            IllegalStateException("User not logged in")
        )

        return try {
            val ref = db.collection("support_tickets").document()

            val data = hashMapOf(
                "ticketId" to ref.id,
                "userId" to uid,
                "subject" to sub,
                "message" to msg,
                "category" to cat.name,
                "bookingId" to (bookingId ?: ""),
                "userName" to (profile?.fullName ?: ""),
                "userMobile" to (profile?.mobile ?: ""),
                "userEmail" to (profile?.email ?: ""),
                "status" to "Open",
                "createdAt" to FieldValue.serverTimestamp()
            )

            ref.set(data).await()
            Result.success(ref.id)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// -----------------------------------------------------------------------------
// VIEWMODEL  (ONE FILE)
// -----------------------------------------------------------------------------

class HelpSupportViewModel(
    private val repo: SupportRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HelpSupportUiState())
    val state: StateFlow<HelpSupportUiState> = _state

    init {
        // load user profile initially
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val profile = repo.getUserProfile()
            _state.value = _state.value.copy(profile = profile)
        }
    }

    fun updateSubject(v: String) {
        _state.value = _state.value.copy(subject = v, error = null, success = null)
    }

    fun updateMessage(v: String) {
        _state.value = _state.value.copy(message = v, error = null, success = null)
    }

    fun updateBookingId(v: String) {
        _state.value = _state.value.copy(bookingId = v, error = null, success = null)
    }

    fun updateCategory(cat: SupportCategory) {
        _state.value = _state.value.copy(category = cat, error = null, success = null)
    }

    fun submit() {
        val s = _state.value

        val subject = s.subject.trim()
        val msg = s.message.trim()

        if (subject.length < 4 || msg.length < 10) {
            _state.value = s.copy(error = "Please enter a valid subject & message.")
            return
        }

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            _state.value = s.copy(isSubmitting = true)

            val res = repo.submitTicket(
                sub = subject,
                msg = msg,
                cat = s.category,
                bookingId = if (s.bookingId.isBlank()) null else s.bookingId,
                profile = s.profile
            )

            if (res.isSuccess) {
                _state.value = HelpSupportUiState(
                    success = "Ticket submitted ✔️  ID: ${res.getOrNull()}",
                    profile = s.profile
                )
            } else {
                _state.value = s.copy(
                    isSubmitting = false,
                    error = res.exceptionOrNull()?.localizedMessage ?: "Failed"
                )
            }
        }
    }
}

class HelpSupportVMFactory(
    private val repo: SupportRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HelpSupportViewModel(repo) as T
    }
}

// -----------------------------------------------------------------------------
// UI SCREEN (ONE FILE)
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(navController: NavController) {

    val ctx = LocalContext.current

    val vm: HelpSupportViewModel = viewModel(
        factory = HelpSupportVMFactory(
            SupportRepository(
                FirebaseFirestore.getInstance(),
                FirebaseAuth.getInstance()
            )
        )
    )

    val st by vm.state.collectAsState()
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(st.error, st.success) {
        st.error?.let { snack.showSnackbar(it) }
        st.success?.let { snack.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Help & Support", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFB71C1C)
                )
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { inner ->

        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFF5F7FA), Color.White, Color(0xFFF5F7FA))
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Quick Contact Buttons
            Text("Quick Contact", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                Button(
                    onClick = {
                        val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:0000000000"))
                        ctx.startActivity(i)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(Color.White)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF0D47A1))
                    Spacer(Modifier.width(6.dp))
                    Text("Call", color = Color(0xFF0D47A1))
                }

                Button(
                    onClick = {
                        val url = "https://wa.me/0000000000"
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(Color.White)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF0D47A1))
                    Spacer(Modifier.width(6.dp))
                    Text("WhatsApp", color = Color(0xFF0D47A1))
                }

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@example.com")
                        }
                        ctx.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(Color.White)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF0D47A1))
                    Spacer(Modifier.width(6.dp))
                    Text("Email", color = Color(0xFF0D47A1))
                }
            }

            // FAQ
            Text("FAQ", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            SupportFaq("How do I get my Booking ID?", "You can find the booking ID on Dashboard or Instant Receipt screen.")
            SupportFaq("How to Cancel Request?", "Menu → Cancel Request → Select Booking → Submit.")
            SupportFaq("Payment deducted?", "Wait 5 minutes. If not updated, raise support ticket.")

            // Ticket Form
            Text("Raise a Ticket", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            OutlinedTextField(
                value = st.subject,
                onValueChange = vm::updateSubject,
                label = { Text("Subject") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = st.message,
                onValueChange = vm::updateMessage,
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )

            OutlinedTextField(
                value = st.bookingId,
                onValueChange = vm::updateBookingId,
                label = { Text("Booking ID (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Category dropdown
            var expand by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(expanded = expand, onExpandedChange = { expand = !expand }) {
                OutlinedTextField(
                    value = st.category.name.replace("_", " "),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expand) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )

                ExposedDropdownMenu(expanded = expand, onDismissRequest = { expand = false }) {
                    SupportCategory.values().forEach {
                        DropdownMenuItem(
                            text = { Text(it.name.replace("_", " ")) },
                            onClick = {
                                vm.updateCategory(it)
                                expand = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = { vm.submit() },
                enabled = !st.isSubmitting,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                if (st.isSubmitting) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Submit Ticket")
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// FAQ Component (ONE FILE)
// -----------------------------------------------------------------------------

@Composable
fun SupportFaq(q: String, a: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(q, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(a, fontSize = 13.sp)
        }
    }
}
