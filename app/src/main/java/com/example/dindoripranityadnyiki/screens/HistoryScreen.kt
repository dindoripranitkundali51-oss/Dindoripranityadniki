// HistoryScreen.kt — FIXED to use status + createdAt (matches existing index)
package com.example.dindoripranityadnyiki.screens

import android.widget.Toast
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.text.SimpleDateFormat
import java.util.*

// ------------------------------
// Model
// ------------------------------
data class Booking(
    val id: String,
    val poojaName: String,
    val dateStr: String,
    val address: String,
    val gurujiName: String?,
    val gurujiContact: String?,
    val status: String
)

// ------------------------------
// Helpers (kept testable)
// ------------------------------
private fun mergeUnique(existing: List<Booking>, newItems: List<Booking>): List<Booking> {
    val ids = existing.map { it.id }.toMutableSet()
    val filtered = newItems.filter { ids.add(it.id) }
    return existing + filtered
}

private fun parseBookingDoc(doc: DocumentSnapshot, sdf: SimpleDateFormat): Booking? {
    return try {
        // Prefer dateTs (Timestamp) -> then date (Timestamp) -> then date (String)
        val tsField = when {
            doc.contains("dateTs") -> doc.get("dateTs") as? Timestamp
            doc.get("date") is Timestamp -> doc.get("date") as? Timestamp
            else -> null
        }
        val dateStr = tsField?.toDate()?.let { sdf.format(it) }
            ?: doc.getString("date")
            ?: "—"

        Booking(
            id = doc.id,
            poojaName = doc.getString("poojaName") ?: "—",
            dateStr = dateStr,
            address = doc.getString("address") ?: "—",
            gurujiName = doc.getString("gurujiName"),
            gurujiContact = doc.getString("gurujiContact"),
            status = doc.getString("status") ?: doc.getString("statusKey") ?: "Unknown"
        )
    } catch (e: Exception) {
        Log.w("HistoryParse", "Parse error for ${doc.id}", e)
        null
    }
}

// Chunking utility (Firestore whereIn limit=10)
private fun <T> chunked(list: List<T>, size: Int = 10): List<List<T>> = list.chunked(size)

// Detect index-required error
private fun isIndexError(e: Exception): Boolean {
    val msg = e.localizedMessage ?: ""
    return msg.contains("requires an index", ignoreCase = true) ||
            msg.contains("no matching index found", ignoreCase = true)
}

// ------------------------------
// UI: HistoryScreen
// ------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val ctx = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // UI state
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }

    // pagination
    val pageSize = 20L
    var canonicalCursor by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var legacyCursor by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMoreCanonical by remember { mutableStateOf(true) }
    var hasMoreLegacy by remember { mutableStateOf(true) }

    // आता दोन्ही ठिकाणी status field वापरतो
    val canonicalStatusValues = listOf("Done", "NotDone", "Not Done", "notdone", "done", "not_done")
    val legacyStatusValues = listOf("Done", "done", "NotDone", "Not Done", "notdone")

    fun <T> buildBatches(list: List<T>) = chunked(list, 10)

    // Error handler
    fun handleFirestoreFailure(tag: String, e: Exception) {
        val msg = e.localizedMessage ?: e.toString()
        Log.e("HistoryScreen", "$tag failed: $msg")
        errorMsg = if (isIndexError(e)) {
            "Query requires a Firestore index. कृपया Firebase Console मध्ये आवश्यक index तयार करा."
        } else {
            "Unable to load history. कृपया नंतर प्रयत्न करा."
        }
    }

    // Generic: try cache then server pattern
    fun Query.getCacheThenServer(
        onCacheSuccess: (QuerySnapshot) -> Unit,
        onServerSuccess: (QuerySnapshot) -> Unit,
        onFail: (Exception) -> Unit
    ) {
        this.get(Source.CACHE)
            .addOnSuccessListener { snap ->
                onCacheSuccess(snap)
                this.get(Source.SERVER)
                    .addOnSuccessListener { srv -> onServerSuccess(srv) }
                    .addOnFailureListener { onFail(it) }
            }
            .addOnFailureListener {
                this.get(Source.SERVER)
                    .addOnSuccessListener { srv -> onServerSuccess(srv) }
                    .addOnFailureListener { err -> onFail(err) }
            }
    }

    // Canonical-first page (userId + status + createdAt) – matches existing index
    fun fetchCanonicalFirstPage(
        uid: String,
        onSuccess: (List<DocumentSnapshot>) -> Unit,
        onFail: (Exception) -> Unit
    ) {
        val batches = buildBatches(canonicalStatusValues)
        fun runBatch(i: Int) {
            if (i >= batches.size) {
                onSuccess(emptyList())
                return
            }

            var q: Query = db.collection("bookings")
                .whereEqualTo("userId", uid)
                .whereIn("status", batches[i])                  // statusKey -> status
                .orderBy("createdAt", Query.Direction.DESCENDING) // dateTs -> createdAt
                .limit(pageSize)

            try {
                q.getCacheThenServer(
                    onCacheSuccess = { snap ->
                        if (snap != null && snap.documents.isNotEmpty()) {
                            onSuccess(snap.documents)
                        }
                    },
                    onServerSuccess = { snap ->
                        if (snap != null && snap.documents.isNotEmpty()) onSuccess(snap.documents)
                        else runBatch(i + 1)
                    },
                    onFail = { err ->
                        onFail(err)
                    }
                )
            } catch (e: Exception) {
                onFail(e)
            }
        }

        runBatch(0)
    }

    // Canonical next page
    fun fetchCanonicalPage(
        uid: String,
        start: DocumentSnapshot?,
        onSuccess: (List<DocumentSnapshot>) -> Unit,
        onFail: (Exception) -> Unit
    ) {
        val batches = buildBatches(canonicalStatusValues)
        fun runBatch(i: Int) {
            if (i >= batches.size) {
                onSuccess(emptyList()); return
            }
            var q: Query = db.collection("bookings")
                .whereEqualTo("userId", uid)
                .whereIn("status", batches[i])                  // statusKey -> status
                .orderBy("createdAt", Query.Direction.DESCENDING) // dateTs -> createdAt
                .limit(pageSize)
            if (start != null) q = q.startAfter(start)

            try {
                q.getCacheThenServer(
                    onCacheSuccess = { snap ->
                        if (snap != null && snap.documents.isNotEmpty()) onSuccess(snap.documents)
                    },
                    onServerSuccess = { snap ->
                        if (snap != null && snap.documents.isNotEmpty()) onSuccess(snap.documents) else runBatch(i + 1)
                    },
                    onFail = { err ->
                        onFail(err)
                    }
                )
            } catch (e: Exception) {
                onFail(e)
            }
        }
        runBatch(0)
    }

    // Legacy page (for जुन्या status values; createdAt वापरतो)
    fun fetchLegacyPage(
        uid: String,
        start: DocumentSnapshot?,
        onSuccess: (List<DocumentSnapshot>) -> Unit,
        onFail: (Exception) -> Unit
    ) {
        val batches = buildBatches(legacyStatusValues)
        fun runBatch(i: Int) {
            if (i >= batches.size) { onSuccess(emptyList()); return }
            var q: Query = db.collection("bookings")
                .whereEqualTo("userId", uid)
                .whereIn("status", batches[i])
                .orderBy("createdAt", Query.Direction.DESCENDING) // date -> createdAt
                .limit(pageSize)
            if (start != null) q = q.startAfter(start)

            q.get(Source.CACHE)
                .addOnSuccessListener { snap ->
                    if (snap.documents.isNotEmpty()) onSuccess(snap.documents)
                    q.get(Source.SERVER)
                        .addOnSuccessListener { srv ->
                            if (srv.documents.isNotEmpty()) onSuccess(srv.documents)
                        }
                        .addOnFailureListener { onFail(it) }
                }
                .addOnFailureListener {
                    q.get(Source.SERVER).addOnSuccessListener { srv ->
                        if (srv.documents.isNotEmpty()) onSuccess(srv.documents) else runBatch(i + 1)
                    }.addOnFailureListener { onFail(it) }
                }
        }
        runBatch(0)
    }

    // Fetch first page (canonical → legacy)
    fun fetchFirst() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            isLoading = false
            errorMsg = "User not logged in."
            bookings = emptyList()
            return
        }

        isLoading = true
        errorMsg = null
        bookings = emptyList()
        canonicalCursor = null
        legacyCursor = null
        hasMoreCanonical = true
        hasMoreLegacy = true

        fetchCanonicalFirstPage(uid,
            onSuccess = { docs ->
                if (docs.isNotEmpty()) {
                    val parsed = docs.mapNotNull { parseBookingDoc(it, sdf) }
                    bookings = parsed
                    canonicalCursor = docs.lastOrNull()
                    hasMoreCanonical = docs.size >= pageSize
                    isLoading = false
                } else {
                    // fallback legacy
                    fetchLegacyPage(uid, null,
                        onSuccess = { legacy ->
                            val parsed = legacy.mapNotNull { parseBookingDoc(it, sdf) }
                            bookings = parsed
                            legacyCursor = legacy.lastOrNull()
                            hasMoreLegacy = legacy.size >= pageSize
                            isLoading = false
                        },
                        onFail = {
                            handleFirestoreFailure("legacy-first", it)
                            isLoading = false
                        }
                    )
                }
            },
            onFail = { e ->
                handleFirestoreFailure("canonical-first", e)
                fetchLegacyPage(uid, null,
                    onSuccess = { legacy ->
                        val parsed = legacy.mapNotNull { parseBookingDoc(it, sdf) }
                        bookings = parsed
                        legacyCursor = legacy.lastOrNull()
                        hasMoreLegacy = legacy.size >= pageSize
                        isLoading = false
                    },
                    onFail = {
                        handleFirestoreFailure("legacy-first-fallback", it)
                        isLoading = false
                    }
                )
            }
        )
    }

    // Load more (canonical → legacy)
    fun loadMore() {
        val uid = auth.currentUser?.uid ?: return
        if (isLoadingMore) return
        isLoadingMore = true
        errorMsg = null

        if (hasMoreCanonical) {
            fetchCanonicalPage(uid, canonicalCursor,
                onSuccess = { docs ->
                    if (docs.isEmpty()) {
                        hasMoreCanonical = false
                    } else {
                        val parsed = docs.mapNotNull { parseBookingDoc(it, sdf) }
                        bookings = mergeUnique(bookings, parsed)
                        canonicalCursor = docs.lastOrNull()
                        hasMoreCanonical = docs.size >= pageSize
                    }
                    isLoadingMore = false
                },
                onFail = {
                    handleFirestoreFailure("canonical-next", it)
                    hasMoreCanonical = false
                    isLoadingMore = false
                }
            )
            return
        }

        if (hasMoreLegacy) {
            fetchLegacyPage(uid, legacyCursor,
                onSuccess = { docs ->
                    if (docs.isEmpty()) {
                        hasMoreLegacy = false
                    } else {
                        val parsed = docs.mapNotNull { parseBookingDoc(it, sdf) }
                        bookings = mergeUnique(bookings, parsed)
                        legacyCursor = docs.lastOrNull()
                        hasMoreLegacy = docs.size >= pageSize
                    }
                    isLoadingMore = false
                },
                onFail = {
                    handleFirestoreFailure("legacy-next", it)
                    hasMoreLegacy = false
                    isLoadingMore = false
                }
            )
        } else {
            isLoadingMore = false
        }
    }

    // initial
    LaunchedEffect(Unit) { fetchFirst() }

    // UI
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("History") },
                actions = {
                    IconButton(onClick = { fetchFirst() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(14.dp)
                .fillMaxSize()
        ) {
            if (isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())

            errorMsg?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { fetchFirst() }) { Text("Retry") }
                return@Column
            }

            if (!isLoading && bookings.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("No completed / Not Done bookings found.")
                Spacer(Modifier.height(10.dp))
                Button(onClick = { navController.navigate("poojaSelection") }) {
                    Text("Book a Pooja")
                }
                return@Column
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(bookings, key = { it.id }) { bk ->
                    BookingHistoryCard(
                        requestId = bk.id,
                        poojaName = bk.poojaName,
                        dateStr = bk.dateStr,
                        address = bk.address,
                        guru = bk.gurujiName ?: "—",
                        status = bk.status,
                        onClick = {
                            try { navController.navigate("bookingDetails/${bk.id}") }
                            catch (e: Exception) { Toast.makeText(ctx, "Cannot open details", Toast.LENGTH_SHORT).show() }
                        }
                    )
                }

                if (hasMoreCanonical || hasMoreLegacy) {
                    item {
                        Spacer(Modifier.height(10.dp))
                        if (isLoadingMore) LinearProgressIndicator(Modifier.fillMaxWidth())
                        else Button(modifier = Modifier.fillMaxWidth(), onClick = { loadMore() }) { Text("Load more") }
                    }
                }
            }
        }
    }
}

// BookingHistoryCard & HistoryStatusChip unchanged
@Composable
fun BookingHistoryCard(
    requestId: String,
    poojaName: String,
    dateStr: String,
    address: String,
    guru: String,
    status: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Request: $requestId", fontWeight = FontWeight.SemiBold)
                HistoryStatusChip(status)
            }
            Spacer(Modifier.height(6.dp))
            Text(poojaName, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Date: $dateStr")
            Spacer(Modifier.height(4.dp))
            Text("Address: $address")
            Spacer(Modifier.height(4.dp))
            Text("Guruji: ${if (guru.isBlank()) "Not assigned" else guru}")
        }
    }
}

@Composable
fun HistoryStatusChip(status: String) {
    val key = status.replace(" ", "").lowercase(Locale.getDefault())
    val (bg, fg) = when (key) {
        "pending" -> Color(0xFFFFF3CD) to Color(0xFF856404)
        "approved", "done" -> Color(0xFFD4EDDA) to Color(0xFF155724)
        "notdone", "not_done" -> Color(0xFFFFD6D6) to Color(0xFF8B0000)
        else -> Color.LightGray to Color.DarkGray
    }
    Surface(shape = RoundedCornerShape(20.dp), color = bg) {
        Text(status, color = fg, modifier = Modifier.padding(10.dp, 6.dp))
    }
}
