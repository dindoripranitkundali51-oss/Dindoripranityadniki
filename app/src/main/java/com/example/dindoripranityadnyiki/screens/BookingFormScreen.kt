package com.example.dindoripranityadnyiki.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.dindoripranityadnyiki.data.PrefKeys
import com.example.dindoripranityadnyiki.data.dataStore
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// ---- Location Type (तुझ्या लिस्टनुसार) ----
enum class LocationType(val label: String) {
    HOME("At Home"),
    OFFICE_SHOP("At Office / Shop"),
    EVENT_VENUE("At Event Venue / Hall"),
    TEMPLE("At Temple (Mandir)"),
    FARM_PLOT("At Farm / Plot (भू-पूजन / शेत पूजा)"),
    SOCIETY_APT("At Society / Apartment Premises"),
    OTHER("Other (Specify)")
}

// ---- Form model + validation ----
data class BookingFormInput(
    val poojaName: String,
    val selectedDateMillis: Long?,
    val address: String,
    val district: String,
    val pincode: String,
    val contactName: String,
    val contactPhone: String,
    val contactEmail: String,
    val locationType: LocationType,
    val consentAccepted: Boolean
)

sealed class BookingValidationResult {
    object Valid : BookingValidationResult()
    data class Error(val message: String) : BookingValidationResult()
}

fun validateBookingForm(
    input: BookingFormInput,
    todayMillis: Long = System.currentTimeMillis()
): BookingValidationResult {

    if (input.poojaName.isBlank()) {
        return BookingValidationResult.Error("Please select a Pooja first.")
    }

    val dateMillis = input.selectedDateMillis
    if (dateMillis == null) {
        return BookingValidationResult.Error("Please choose a date.")
    } else {
        val calToday = Calendar.getInstance().apply {
            timeInMillis = todayMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calSelected = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calSelected.before(calToday)) {
            return BookingValidationResult.Error("Selected date cannot be in the past.")
        }
    }

    if (input.address.trim().length < 10) {
        return BookingValidationResult.Error(
            "Please enter a complete address (house no., street, landmark, district)."
        )
    }
    // district optional (address मध्ये असू शकतो)

    if (input.pincode.length != 6 || input.pincode.any { !it.isDigit() }) {
        return BookingValidationResult.Error("Please enter a valid 6-digit pincode.")
    }

    if (input.contactName.isBlank()) {
        return BookingValidationResult.Error("Please enter contact person name.")
    }

    val phoneDigits = input.contactPhone.filter(Char::isDigit)
    if (phoneDigits.length != 10) {
        return BookingValidationResult.Error("Please enter a valid 10-digit mobile number.")
    }

    if (!input.consentAccepted) {
        return BookingValidationResult.Error("Please accept the terms & conditions to proceed.")
    }

    return BookingValidationResult.Valid
}

// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFormScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = FirebaseAuth.getInstance()

    // --- basic pooja + address ---
    var poojaId by remember {
        mutableStateOf(
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<String>("selectedPoojaId") ?: ""
        )
    }
    var poojaName by remember { mutableStateOf("") }
    var poojaImage by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Long?>(null) }

    var address by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") } // background only
    var pincode by remember { mutableStateOf("") }

    // --- contact & prefs ---
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }

    var locationType by remember { mutableStateOf(LocationType.HOME) }
    var specialInstructions by remember { mutableStateOf("") }
    var consentAccepted by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val bgColor = Color(0xFFF5F6FA)

    // Date picker
    val calendar = Calendar.getInstance()
    val dateDialog = remember {
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val cal = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }
                selectedDate = cal.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = calendar.timeInMillis
        }
    }

    // preload pooja info + user details from DataStore + Firestore
    LaunchedEffect(Unit) {
        val prefs = runCatching { context.dataStore.data.first() }.getOrNull()
        district = prefs?.get(PrefKeys.DISTRICT)
            ?: prefs?.get(PrefKeys.USER_DISTRICT)
                    ?: ""
        address = prefs?.get(PrefKeys.USER_ADDRESS) ?: ""
        pincode = prefs?.get(PrefKeys.USER_PINCODE) ?: ""

        val uid = auth.currentUser?.uid
        if (uid != null) {
            runCatching {
                val userDoc = db.collection("users").document(uid).get().await()
                if (userDoc.exists()) {
                    if (contactName.isBlank()) {
                        contactName = userDoc.getString("fullName") ?: contactName
                    }
                    if (contactPhone.isBlank()) {
                        contactPhone = userDoc.getString("mobile") ?: contactPhone
                    }
                    if (contactEmail.isBlank()) {
                        contactEmail = userDoc.getString("email") ?: contactEmail
                    }
                    if (district.isBlank()) {
                        district = userDoc.getString("district") ?: district
                    }
                    if (address.isBlank()) {
                        address = userDoc.getString("address") ?: address
                    }
                    if (pincode.isBlank()) {
                        pincode = userDoc.getString("pincode") ?: pincode
                    }
                }
            }.onFailure { /* ignore */ }
        }

        if (poojaId.isNotBlank()) {
            runCatching {
                val doc = db.collection("poojas").document(poojaId).get().await()
                poojaName = doc.getString("name") ?: ""
                poojaImage = doc.getString("imageUrl") ?: ""
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Booking Confirmation",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFB71C1C),
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Pooja Info
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(poojaImage)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(
                                    Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE3F2FD))
                                )
                            },
                            error = {
                                Box(
                                    Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE3F2FD)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🕉️")
                                }
                            }
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                poojaName.ifBlank { "Selected Pooja" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0D47A1)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "सर्व पूजांचे आयोजन अधिकृत प्रमाणे पारदर्शक पद्धतीने केले जाते. तपशील योग्य भरल्यास बुकिंग निश्चित होईल.",
                                color = Color(0xFF37474F),
                                fontSize = 14.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Date
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Select Muhurat Date", fontWeight = FontWeight.Bold)
                            Text(
                                selectedDate?.let { sdf.format(Date(it)) } ?: "No date chosen",
                                color = Color(0xFF546E7A)
                            )
                        }
                        IconButton(onClick = { dateDialog.show() }) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFF0D47A1)
                            )
                        }
                    }
                }

                // Contact
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Contact Details",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D47A1)
                        )

                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text("Contact Person Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = {
                                if (it.length <= 10) contactPhone = it.filter(Char::isDigit)
                            },
                            label = { Text("Mobile (10 digits)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = contactEmail,
                            onValueChange = { contactEmail = it },
                            label = { Text("Email (optional)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Location details – Address + Pincode
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Location Details",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D47A1)
                        )

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address") },
                            placeholder = { Text("House no., Street, Landmark, District") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = pincode,
                            onValueChange = {
                                if (it.length <= 6) pincode = it.filter(Char::isDigit)
                            },
                            label = { Text("Pincode") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Preferences (फक्त Location Type + Notes)
                var locationExpanded by remember { mutableStateOf(false) }

                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Pooja Preferences",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D47A1)
                        )

                        ExposedDropdownMenuBox(
                            expanded = locationExpanded,
                            onExpandedChange = { locationExpanded = !locationExpanded }
                        ) {
                            OutlinedTextField(
                                value = locationType.label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Location Type") },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = locationExpanded,
                                onDismissRequest = { locationExpanded = false }
                            ) {
                                LocationType.values().forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.label) },
                                        onClick = {
                                            locationType = type
                                            locationExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = specialInstructions,
                            onValueChange = { specialInstructions = it },
                            label = {
                                Text(
                                    if (locationType == LocationType.OTHER)
                                        "Specify Location / Special Instructions"
                                    else
                                        "Special Instructions (optional)"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp)
                        )
                    }
                }

                // Summary
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Summary", fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                        Text("Pooja: ${if (poojaName.isNotBlank()) poojaName else "—"}")
                        Text("Date: ${selectedDate?.let { sdf.format(Date(it)) } ?: "—"}")
                        Text(
                            "Contact: ${if (contactName.isNotBlank()) contactName else "—"} · " +
                                    "${if (contactPhone.isNotBlank()) contactPhone else "—"}"
                        )
                        Text("Location Type: ${locationType.label}")
                        Text("Address: ${if (address.isNotBlank()) address else "—"}")
                        Text("Pincode: ${if (pincode.isNotBlank()) pincode else "—"}")
                    }
                }

                // Consent
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Checkbox(
                        checked = consentAccepted,
                        onCheckedChange = { consentAccepted = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "I have read and accept the terms & conditions.",
                        fontSize = 13.sp
                    )
                }

                // Submit
                Button(
                    onClick = {
                        scope.launch {
                            val authUid = auth.currentUser?.uid
                            if (authUid == null) {
                                snackbarHostState.showSnackbar("कृपया बुकिंग करण्यासाठी लॉगिन करा.")
                                navController.navigate("login") { launchSingleTop = true }
                                return@launch
                            }

                            val input = BookingFormInput(
                                poojaName = poojaName,
                                selectedDateMillis = selectedDate,
                                address = address,
                                district = district,
                                pincode = pincode,
                                contactName = contactName,
                                contactPhone = contactPhone,
                                contactEmail = contactEmail,
                                locationType = locationType,
                                consentAccepted = consentAccepted
                            )

                            when (val validation = validateBookingForm(input)) {
                                is BookingValidationResult.Error -> {
                                    snackbarHostState.showSnackbar(validation.message)
                                    return@launch
                                }
                                BookingValidationResult.Valid -> Unit
                            }

                            isLoading = true

                            try {
                                val counterRef =
                                    db.collection("counters").document("eventCounter")

                                val newSeq = db.runTransaction { txn ->
                                    val snap = txn.get(counterRef)
                                    val current = snap.getLong("seq") ?: 0L
                                    val next = current + 1L
                                    txn.update(counterRef, "seq", next)
                                    next
                                }.await()

                                val formattedId =
                                    String.format(Locale.getDefault(), "EI-%03d", newSeq)

                                val bookingMap = hashMapOf(
                                    "bookingId" to formattedId,
                                    "userId" to authUid,
                                    "poojaId" to poojaId,
                                    "poojaName" to poojaName,
                                    "date" to Timestamp(Date(selectedDate!!)),
                                    "address" to address,
                                    "district" to district,
                                    "pincode" to pincode,
                                    "status" to "Pending",
                                    "createdAt" to FieldValue.serverTimestamp(),
                                    "contactName" to contactName,
                                    "contactPhone" to contactPhone,
                                    "contactEmail" to contactEmail,
                                    "locationType" to locationType.name,
                                    "locationTypeLabel" to locationType.label,
                                    "specialInstructions" to specialInstructions
                                )

                                db.collection("bookings")
                                    .document(formattedId)
                                    .set(bookingMap)
                                    .await()

                                val updateMap =
                                    mapOf("bookings" to FieldValue.arrayUnion(formattedId))
                                db.collection("users")
                                    .document(authUid)
                                    .set(updateMap, SetOptions.merge())
                                    .await()

                                context.dataStore.edit { prefs ->
                                    prefs[PrefKeys.IS_FIRST_BOOKING_DONE] = true
                                    prefs[PrefKeys.IS_FIRST_TIME] = false
                                    prefs[PrefKeys.IS_REGISTERED] = true
                                    prefs[PrefKeys.IS_LOGGED_IN] = true
                                    prefs[PrefKeys.USER_ROLE] =
                                        prefs[PrefKeys.USER_ROLE] ?: "user"

                                    prefs[PrefKeys.USER_ADDRESS] = address
                                    prefs[PrefKeys.USER_DISTRICT] = district
                                    prefs[PrefKeys.USER_PINCODE] = pincode
                                }

                                snackbarHostState.showSnackbar(
                                    "Booking submitted successfully (ID: $formattedId)"
                                )
                                navController.navigate("userDashboard") {
                                    popUpTo("poojaSelection") { inclusive = true }
                                    launchSingleTop = true
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    "Error: ${e.localizedMessage ?: "Unknown error"}"
                                )
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(40.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(40.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Confirm & Submit",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
