package com.example.dindoripranityadnyiki.core.data

import android.util.Log
import com.example.dindoripranityadnyiki.core.network.*
import com.example.dindoripranityadnyiki.core.util.Resource
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GurujiRepository @Inject constructor(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) {
    companion object {
        @Volatile private var instance: GurujiRepository? = null
        fun getInstance(): GurujiRepository {
            return instance ?: synchronized(this) {
                instance ?: error("Use Dependency Injection via Hilt instead of getInstance()")
            }
        }
    }

    private fun getUid(): String {
        return runBlocking {
            dataStoreManager.readString(PrefKeys.USER_MOBILE).first()
        }
    }

    private fun getBearerToken(): String {
        return runBlocking {
            val token = dataStoreManager.readString(PrefKeys.JWT_TOKEN).first()
            "Bearer $token"
        }
    }

    suspend fun getGurujiProfile(): GurujiProfile? {
        val mobile = getUid()
        if (mobile.isBlank()) return null
        return try {
            val name = dataStoreManager.readString(PrefKeys.USER_NAME).first()
            val email = dataStoreManager.readString(PrefKeys.USER_EMAIL).first()
            val address = dataStoreManager.readString(PrefKeys.USER_ADDRESS).first()
            val district = dataStoreManager.readString(PrefKeys.USER_DISTRICT).first()

            val token = getBearerToken()
            val availResponse = apiService.getAvailability(token)
            val dates = if (availResponse.isSuccessful && availResponse.body()?.success == true) {
                availResponse.body()?.data.orEmpty()
            } else {
                emptyList()
            }

            GurujiProfile(
                fullName = name,
                name = name,
                mobile = mobile,
                email = email,
                address = address,
                district = district,
                uid = mobile,
                id = mobile,
                status = "Active",
                isAvailable = true,
                availableDates = dates
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getGurujiProfileFlow(): Flow<Resource<GurujiProfile>> = callbackFlow {
        trySend(Resource.Loading())
        val job = CoroutineScope(Dispatchers.IO).launch {
            val profile = getGurujiProfile()
            if (profile != null) {
                trySend(Resource.Success(profile))
            } else {
                trySend(Resource.Error("Guruji profile not found"))
            }
        }
        awaitClose { job.cancel() }
    }

    fun listenToAssignedBookings(onUpdate: (List<BookingModel>) -> Unit): RepositorySubscription {
        val job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val response = apiService.getExpertBookings(getBearerToken())
                    if (response.isSuccessful && response.body()?.success == true) {
                        val list = response.body()?.data.orEmpty()
                        val filtered = list.filter {
                            it.status in listOf("Assigned", "Accepted", "InProgress", "PaymentPending", "Completed")
                        }.map { it.toBookingModel() }
                        withContext(Dispatchers.Main) {
                            onUpdate(filtered)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GurujiRepository", "listenToAssignedBookings failed", e)
                }
                delay(15000) // Poll every 15 seconds
            }
        }
        return CoroutineSubscription(job)
    }

    fun listenToProfile(onUpdate: (GurujiProfile?) -> Unit): RepositorySubscription {
        val job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val profile = getGurujiProfile()
                withContext(Dispatchers.Main) {
                    onUpdate(profile)
                }
                delay(20000) // Poll every 20 seconds
            }
        }
        return CoroutineSubscription(job)
    }

    fun listenToTransactions(onUpdate: (List<PaymentTransaction>) -> Unit): RepositorySubscription {
        val job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val response = apiService.getTransactions(getBearerToken())
                    if (response.isSuccessful && response.body()?.success == true) {
                        val list = response.body()?.data.orEmpty().map { it.toDomain() }
                        withContext(Dispatchers.Main) {
                            onUpdate(list)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GurujiRepository", "listenToTransactions failed", e)
                }
                delay(20000) // Poll every 20 seconds
            }
        }
        return CoroutineSubscription(job)
    }

    suspend fun updateBookingStatus(
        bookingId: String,
        status: String,
        completionOtp: String? = null,
        actualAmount: Double? = null
    ): Resource<Boolean> = runCatching {
        if (!completionOtp.isNullOrBlank()) {
            // Verify OTP
            val verifyResponse = apiService.verifyCompletionOtp(
                getBearerToken(),
                bookingId,
                VerifyOtpRequest(completionOtp, actualAmount ?: 0.0)
            )
            if (verifyResponse.isSuccessful && verifyResponse.body()?.success == true) {
                Resource.Success(true)
            } else {
                Resource.Error(verifyResponse.body()?.message ?: "OTP Verification failed")
            }
        } else {
            // Regular status update
            val statusResponse = apiService.updateBookingStatus(
                getBearerToken(),
                bookingId,
                UpdateStatusRequest(status)
            )
            if (statusResponse.isSuccessful && statusResponse.body()?.success == true) {
                Resource.Success(true)
            } else {
                Resource.Error(statusResponse.body()?.message ?: "Status update failed")
            }
        }
    }.getOrElse { Resource.Error(it.message ?: "Update failed") }

    suspend fun updateAvailability(dates: List<String>): Result<String> = runCatching {
        val token = getBearerToken()
        val response = apiService.saveAvailability(token, GurujiAvailabilityRequest(dates))
        if (response.isSuccessful && response.body()?.success == true) {
            "उपलब्धता अपडेट झाली"
        } else {
            throw Exception(response.body()?.message ?: "Failed to update availability")
        }
    }

    suspend fun requestWithdrawal(
        amount: Double,
        accountHolder: String,
        accountNumber: String,
        ifsc: String,
        upiId: String
    ): Result<String> = runCatching {
        val response = apiService.requestWithdrawal(
            getBearerToken(),
            WithdrawalRequest(amount = amount, bankAccount = accountNumber, ifsc = ifsc, upi = upiId)
        )
        if (response.isSuccessful && response.body()?.success == true) {
            "विड्रॉवल विनंती यशस्वीरित्या पाठवली गेली"
        } else {
            throw Exception(response.errorBody()?.string() ?: "Withdrawal request failed")
        }
    }
}
