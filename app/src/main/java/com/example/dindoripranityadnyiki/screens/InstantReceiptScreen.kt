package com.example.dindoripranityadnyiki.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* -------------------------------------------------------------------
   DATA MODEL + MAPPER
------------------------------------------------------------------- */

data class InstantReceiptData(
    val bookingId: String,
    val poojaName: String,
    val fullName: String,
    val mobile: String,
    val address: String,
    val amount: Long,
    val paymentMode: String,
    val bankName: String?,
    val chequeNo: String?,
    val createdAt: Date?
) {
    fun dateStr(): String {
        return createdAt?.let {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
        } ?: "—"
    }

    fun amountInWords(): String = numberToIndianRupeesWords(amount)
}

fun mapToInstantReceiptData(id: String, data: Map<String, Any?>): InstantReceiptData {
    return InstantReceiptData(
        bookingId = id,
        poojaName = (data["poojaName"] as? String).orEmpty(),
        fullName = (data["fullName"] as? String)
            ?: (data["userName"] as? String).orEmpty(),
        mobile = (data["mobile"] as? String).orEmpty(),
        address = (data["address"] as? String).orEmpty(),
        amount = (data["amount"] as? Number)?.toLong() ?: 0L,
        paymentMode = (data["paymentMode"] as? String).orEmpty(),
        bankName = data["bankName"] as? String,
        chequeNo = data["chequeNo"] as? String,
        createdAt = when (val ts = data["createdAt"]) {
            is Timestamp -> ts.toDate()
            is Date -> ts
            else -> null
        }
    )
}

/* Amount → Words (Indian system) */
fun numberToIndianRupeesWords(amount: Long): String {
    if (amount == 0L) return "Zero Rupees"

    val units = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven",
        "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen",
        "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    )
    val tens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty",
        "Sixty", "Seventy", "Eighty", "Ninety"
    )

    fun two(n: Int): String {
        return if (n < 20) units[n]
        else tens[n / 10] + (if (n % 10 > 0) " ${units[n % 10]}" else "")
    }

    fun three(n: Int): String {
        return when {
            n < 100 -> two(n)
            else -> units[n / 100] + " Hundred" +
                    (if (n % 100 != 0) " and ${two(n % 100)}" else "")
        }
    }

    var n = amount
    val parts = mutableListOf<String>()

    val crore = (n / 10000000).toInt()
    if (crore > 0) {
        parts.add("${two(crore)} Crore")
        n %= 10000000
    }

    val lakh = (n / 100000).toInt()
    if (lakh > 0) {
        parts.add("${two(lakh)} Lakh")
        n %= 100000
    }

    val thousand = (n / 1000).toInt()
    if (thousand > 0) {
        parts.add("${two(thousand)} Thousand")
        n %= 1000
    }

    if (n > 0) parts.add(three(n.toInt()))

    return parts.joinToString(" ") + " Rupees"
}

/* -------------------------------------------------------------------
   MAIN SCREEN
------------------------------------------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstantReceiptScreen(navController: NavController) {
    val ctx = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var bookingId by rememberSaveable { mutableStateOf("") }
    var receipt by remember { mutableStateOf<InstantReceiptData?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val snackbarHost = remember { SnackbarHostState() }

    /* Auto–load user’s latest booking */
    LaunchedEffect(Unit) {
        try {
            val uid = auth.currentUser?.uid ?: return@LaunchedEffect
            loading = true

            val snap = db.collection("bookings")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val doc = snap.documents.firstOrNull()
            if (doc != null) {
                val data = doc.data ?: emptyMap()
                receipt = mapToInstantReceiptData(doc.id, data)
                bookingId = doc.id
            } else {
                error = "No bookings found."
            }

        } catch (e: Exception) {
            error = e.localizedMessage
        } finally {
            loading = false
        }
    }

    suspend fun fetchById(id: String) {
        loading = true
        error = null
        receipt = null

        try {
            val doc = db.collection("bookings").document(id).get().await()
            if (!doc.exists()) {
                error = "No booking found with ID $id"
                return
            }
            val uid = auth.currentUser?.uid
            if (doc.getString("userId") != uid) {
                error = "This receipt does not belong to you."
                return
            }
            receipt = mapToInstantReceiptData(id, doc.data ?: emptyMap())

        } catch (e: Exception) {
            error = e.localizedMessage
        } finally {
            loading = false
        }
    }

    fun shareReceipt(r: InstantReceiptData) {
        val text = """
            Dindori Pranit Yadnyiki Receipt
            -------------------------------
            Request ID: ${r.bookingId}
            Puja: ${r.poojaName}
            Name: ${r.fullName}
            Mobile: ${r.mobile}
            Address: ${r.address}
            Amount: ₹${r.amount} (${r.amountInWords()})
            Payment Mode: ${r.paymentMode}
            Date: ${r.dateStr()}
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        ctx.startActivity(Intent.createChooser(intent, "Share Receipt"))
    }

    /* Background gradient */
    val bg = Brush.verticalGradient(
        listOf(Color(0xFFE3F2FD), Color.White, Color(0xFFBBDEFB))
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Instant Receipt", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFB71C1C)
                )
            )
        }
    ) { inner ->

        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .background(bg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            /* Search Card */
            Card(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Enter Request ID:", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = bookingId,
                        onValueChange = { bookingId = it.trim() },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Eg. EI-001") }
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                if (bookingId.isBlank()) {
                                    snackbarHost.showSnackbar("Enter request ID")
                                } else {
                                    fetchById(bookingId)
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .height(44.dp),
                        shape = RoundedCornerShape(100)
                    ) {
                        Icon(Icons.Default.Search, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Get Receipt")
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            when {
                loading -> {
                    CircularProgressIndicator()
                }

                error != null -> {
                    Text(
                        error ?: "",
                        color = Color.Red,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                receipt != null -> {
                    ReceiptCard(
                        r = receipt!!,
                        onShare = { shareReceipt(receipt!!) }
                    )
                }

                else -> {
                    Text(
                        "Enter ID or wait for auto-fetch of last booking",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/* -------------------------------------------------------------------
   UI CARD FOR RECEIPT
------------------------------------------------------------------- */

@Composable
fun ReceiptCard(r: InstantReceiptData, onShare: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {

            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF4CAF50))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "श्री स्वामी समर्थ कृषी विकास व संशोधन ट्रस्ट",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "श्री स्वामी समर्थ सेवा व आध्यात्मिक विकास मार्ग (दिंडोरी प्रणित)",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("पावती नं.: ${r.bookingId}", fontWeight = FontWeight.Bold)
            Text("दिनांक: ${r.dateStr()}")

            Spacer(Modifier.height(10.dp))

            InfoRow("श्री./श्रीमती :", r.fullName)
            InfoRow("मोबाईल :", r.mobile)
            InfoRow("पत्ता :", r.address)
            InfoRow("पूजा :", r.poojaName)
            InfoRow("देणगी :", "₹${r.amount} (${r.amountInWords()})")
            InfoRow("भरणा पद्धत :", r.paymentMode)
            InfoRow("बँक :", r.bankName ?: "—")
            if (!r.chequeNo.isNullOrBlank())
                InfoRow("Cheque/Ref No.:", r.chequeNo)

            Spacer(Modifier.height(14.dp))

            OutlinedButton(
                onClick = onShare,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(6.dp))
                Text("Share Receipt")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            value,
            modifier = Modifier.weight(0.55f)
        )
    }
}
