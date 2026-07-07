package com.example.dindoripranityadnyiki.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.network.ApiService
import com.example.dindoripranityadnyiki.core.network.SubmitServiceRequestRequest
import com.example.dindoripranityadnyiki.core.util.BookingAccess
import com.example.dindoripranityadnyiki.core.util.BookingAccessGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookingConfirmedUiState(
    val bookingData: Map<String, Any>? = null,
    val isLoading: Boolean = true,
    val isRequestLoading: Boolean = false,
    val accessDenied: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class BookingConfirmedViewModel @Inject constructor(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingConfirmedUiState())
    val uiState = _uiState.asStateFlow()

    private var pollJob: Job? = null

    private suspend fun getBearerToken(): String {
        val token = dataStoreManager.readString(PrefKeys.JWT_TOKEN).first()
        return "Bearer $token"
    }

    fun monitorBooking(bookingId: String) {
        if (bookingId.isBlank()) return

        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            while (isActive) {
                try {
                    val token = getBearerToken()
                    val response = apiService.getBookingDetails(token, bookingId)
                    if (response.isSuccessful && response.body()?.success == true) {
                        val bookingModel = response.body()?.data
                        if (bookingModel != null) {
                            val map = mapOf<String, Any>(
                                "id" to bookingModel.id,
                                "displayId" to bookingModel.displayId,
                                "userId" to bookingModel.userId,
                                "gurujiId" to (bookingModel.gurujiId ?: ""),
                                "poojaId" to bookingModel.poojaId,
                                "date" to (bookingModel.date ?: ""),
                                "status" to bookingModel.status,
                                "paymentStatus" to bookingModel.paymentStatus,
                                "amount" to bookingModel.amount,
                                "contactName" to (bookingModel.contactName ?: ""),
                                "contactPhone" to (bookingModel.contactPhone ?: ""),
                                "address" to (bookingModel.address ?: ""),
                                "district" to (bookingModel.district ?: ""),
                                "pincode" to (bookingModel.pincode ?: ""),
                                "userLat" to (bookingModel.userLat ?: 0.0),
                                "userLng" to (bookingModel.userLng ?: 0.0)
                            )
                            _uiState.value = _uiState.value.copy(
                                bookingData = map,
                                isLoading = false
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "बुकिंग सापडले नाही"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = response.errorBody()?.string() ?: "फेड अपयशी"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.localizedMessage
                    )
                }
                delay(15000) // Poll every 15 seconds
            }
        }
    }

    fun submitBookingRequest(bookingId: String, type: String, requestedDate: String, reason: String) {
        if (bookingId.isBlank()) return
        if (type == "Reschedule" && requestedDate.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please select a new date.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRequestLoading = true,
                error = null,
                message = null
            )
            try {
                val token = getBearerToken()
                val request = SubmitServiceRequestRequest(
                    type = type,
                    requestedDate = requestedDate.takeIf { it.isNotBlank() },
                    reason = reason
                )
                val response = apiService.submitBookingRequest(token, bookingId, request)
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(
                        isRequestLoading = false,
                        message = if (type == "Cancel") "Cancellation request sent." else "Reschedule request sent."
                    )
                } else {
                    throw Exception(response.errorBody()?.string() ?: "Request failed.")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRequestLoading = false,
                    error = e.localizedMessage ?: "Request failed."
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
