package com.example.dindoripranityadnyiki.core.data

import android.util.Log
import com.example.dindoripranityadnyiki.core.network.ApiService
import com.example.dindoripranityadnyiki.core.network.CreateBookingRequest
import com.example.dindoripranityadnyiki.core.network.toBookingModel
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

// Mock ListenerRegistration cancel wrapper
class CoroutineListenerRegistration(private val job: Job) : ListenerRegistration {
    override fun remove() {
        job.cancel()
    }
}

@Singleton
class SacredSevaRepository @Inject constructor(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) {
    private var cachedServices: List<PoojaService>? = null

    companion object {
        @Volatile private var instance: SacredSevaRepository? = null
        fun getInstance(): SacredSevaRepository {
            return instance ?: synchronized(this) {
                instance ?: error("Use Dependency Injection via Hilt instead of getInstance()")
            }
        }
        val ACTIVE_STATUSES = listOf("Pending", "Assigned", "Accepted", "In Progress", "Payment Pending", "Awaiting Verification", "Completed", "Paid")
    }

    suspend fun getSacredServices(forceRefresh: Boolean = false): List<PoojaService> {
        if (!forceRefresh) cachedServices?.let { return it }
        cachedServices = fallbackCatalog()
        return fallbackCatalog()
    }

    suspend fun getPoojaDetails(poojaId: String): PoojaService? {
        return fallbackCatalog().firstOrNull { it.id == poojaId }
    }

    private fun fallbackCatalog(): List<PoojaService> =
        (Constants.MAIN_POOJA_LIST + Constants.OTHER_POOJA_LIST)
            .filter { it != Constants.POOJA_SEPARATOR }.distinct()
            .mapIndexed { index, name -> PoojaService(id = "fallback_pooja_$index", name = name, nameEn = name) }

    private fun getBearerToken(): String {
        return runBlocking {
            val token = dataStoreManager.readString(PrefKeys.JWT_TOKEN).first()
            "Bearer $token"
        }
    }

    fun monitorEngagement(bookingId: String, onUpdate: (BookingModel?) -> Unit): ListenerRegistration {
        val job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val response = apiService.getBookingDetails(getBearerToken(), bookingId)
                    if (response.isSuccessful && response.body()?.success == true) {
                        val booking = response.body()?.data
                        withContext(Dispatchers.Main) {
                            onUpdate(booking?.toBookingModel())
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SacredSevaRepo", "monitorEngagement failed", e)
                }
                delay(15000) // Poll every 15 seconds
            }
        }
        return CoroutineListenerRegistration(job)
    }

    fun listenToActiveBooking(userId: String, onUpdate: (BookingModel?) -> Unit): ListenerRegistration {
        val job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val response = apiService.getUserBookings(getBearerToken())
                    if (response.isSuccessful && response.body()?.success == true) {
                        val list = response.body()?.data.orEmpty()
                        val active = list
                            .filter { it.status in ACTIVE_STATUSES }
                            .filter { !(it.status in setOf("Completed", "Paid") && (it.currentUserAction ?: "").isBlank() && it.currentUserActionPriority == null) }
                            .map { it.toBookingModel() }
                            .minByOrNull(::activePriority)
                        withContext(Dispatchers.Main) {
                            onUpdate(active)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SacredSevaRepo", "listenToActiveBooking failed", e)
                }
                delay(15000) // Poll every 15 seconds
            }
        }
        return CoroutineListenerRegistration(job)
    }

    fun listenToBookingHistory(userId: String, onUpdate: (List<BookingModel>) -> Unit): ListenerRegistration {
        val job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val response = apiService.getUserBookings(getBearerToken())
                    if (response.isSuccessful && response.body()?.success == true) {
                        val list = response.body()?.data.orEmpty()
                        val sorted = list.map { it.toBookingModel() }
                            .sortedWith(compareBy<BookingModel>(::historyPriority).thenByDescending { it.id })
                        withContext(Dispatchers.Main) {
                            onUpdate(sorted)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SacredSevaRepo", "listenToBookingHistory failed", e)
                }
                delay(20000) // Poll every 20 seconds
            }
        }
        return CoroutineListenerRegistration(job)
    }

    private fun activePriority(booking: BookingModel): Int = booking.currentUserActionPriority ?: when (booking.currentUserAction) {
        "PAY_NOW" -> 1; "CHECK_OTP_NOTIFICATION" -> 2; "WAIT_PAYMENT_VERIFICATION" -> 3; "RATE_SEVA" -> 4; "VIEW_RECEIPT" -> 8
        else -> when (booking.status) { "Payment Pending" -> 1; "Awaiting Verification" -> 2; "In Progress" -> 3; "Accepted" -> 4; "Assigned" -> 5; "Pending" -> 6; else -> 9 }
    }

    private fun historyPriority(booking: BookingModel): Int = when {
        booking.status in setOf("Completed", "Paid") && booking.feedbackSubmittedAt == null && booking.rating == null -> 1
        booking.status in setOf("Payment Pending", "Awaiting Verification") -> 2
        booking.status == "Paid" || booking.paymentStatus == "Paid" -> 3
        booking.status == "Cancelled" -> 5
        else -> 4
    }

    suspend fun createBookingWithCounter(request: BookingCreateRequest): Result<String> = runCatching {
        val createRequest = CreateBookingRequest(
            clientRequestId = request.clientRequestId,
            poojaId = request.poojaId,
            date = request.date,
            contactName = request.contactName,
            contactPhone = request.contactPhone,
            address = request.address,
            district = request.district,
            pincode = request.pincode,
            userLat = request.lat,
            userLng = request.lng
        )
        val response = apiService.createBooking(getBearerToken(), createRequest)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()?.assignedId ?: ""
        } else {
            throw Exception(response.errorBody()?.string() ?: "Failed to create booking")
        }
    }

    suspend fun getAvailableDates(): List<String> = runCatching {
        val response = apiService.getAvailableDates(getBearerToken())
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()?.data.orEmpty()
        } else {
            emptyList()
        }
    }.getOrElse { emptyList() }
}
