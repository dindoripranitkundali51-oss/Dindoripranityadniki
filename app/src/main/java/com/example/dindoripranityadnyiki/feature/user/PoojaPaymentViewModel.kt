package com.example.dindoripranityadnyiki.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.network.ApiService
import com.example.dindoripranityadnyiki.core.network.CreateOrderRequest
import com.example.dindoripranityadnyiki.core.network.VerifyPaymentRequest
import com.example.dindoripranityadnyiki.core.util.BookingAccess
import com.example.dindoripranityadnyiki.core.util.BookingAccessGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentUiState(
    val amount: Double = 0.0,
    val poojaName: String = "",
    val userPhone: String = "",
    val userEmail: String = "",
    val razorpayOrderId: String = "",
    val razorpayKeyId: String = "",
    val status: String = "",
    val isLoading: Boolean = true,
    val paymentProcessed: Boolean = false,
    val isCreatingOrder: Boolean = false,
    val isVerifying: Boolean = false,
    val accessDenied: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PoojaPaymentViewModel @Inject constructor(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState = _uiState.asStateFlow()

    private suspend fun getBearerToken(): String {
        val token = dataStoreManager.readString(PrefKeys.JWT_TOKEN).first()
        return "Bearer $token"
    }

    fun loadPaymentDetails(bookingId: String) {
        viewModelScope.launch {
            try {
                val token = getBearerToken()
                val response = apiService.getBookingDetails(token, bookingId)
                
                if (!response.isSuccessful || response.body()?.success != true) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "बुकिंग सापडले नाही.")
                    return@launch
                }

                val bookingModel = response.body()!!.data
                val cachedMobile = dataStoreManager.readString(PrefKeys.USER_MOBILE).first()
                val cachedEmail = dataStoreManager.readString(PrefKeys.USER_EMAIL).first()

                // Client-side ownership check (additional security layer)
                if (bookingModel.userId != cachedMobile && bookingModel.contactPhone != cachedMobile) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        accessDenied = true,
                        error = "या बुकिंगसाठी पेमेंट करण्याची परवानगी नाही."
                    )
                    return@launch
                }

                val amt = bookingModel.amount.toDouble()
                
                _uiState.value = _uiState.value.copy(
                    amount = amt,
                    poojaName = bookingModel.poojaId,
                    userPhone = bookingModel.contactPhone.takeIf { !it.isNullOrBlank() } ?: cachedMobile,
                    userEmail = cachedEmail,
                    status = bookingModel.status,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = paymentErrorMessage(e))
            }
        }
    }

    fun createRazorpayOrder(bookingId: String, onOrderReady: (String, String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingOrder = true, error = null)
            try {
                val token = getBearerToken()
                val res = apiService.createRazorpayOrder(token, CreateOrderRequest(bookingId))
                if (res.isSuccessful && res.body()?.success == true) {
                    val body = res.body()!!
                    _uiState.value = _uiState.value.copy(
                        isCreatingOrder = false,
                        razorpayOrderId = body.orderId,
                        razorpayKeyId = body.keyId
                    )
                    onOrderReady(body.orderId, body.keyId)
                } else {
                    throw Exception(res.errorBody()?.string() ?: "Order generation failed")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isCreatingOrder = false, error = paymentErrorMessage(e))
            }
        }
    }

    fun verifyRazorpayPayment(bookingId: String, paymentId: String, orderId: String?, signature: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVerifying = true)
            try {
                val token = getBearerToken()
                val request = VerifyPaymentRequest(
                    bookingId = bookingId,
                    razorpayPaymentId = paymentId,
                    razorpaySignature = signature ?: ""
                )
                val res = apiService.verifyPayment(token, request)
                if (res.isSuccessful && res.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(isVerifying = false, paymentProcessed = true)
                } else {
                    _uiState.value = _uiState.value.copy(isVerifying = false, error = "पेमेंट व्हेरिफिकेशन अयशस्वी.")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isVerifying = false, error = paymentErrorMessage(e))
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    private fun paymentErrorMessage(error: Exception): String {
        val raw = error.localizedMessage.orEmpty()
        return when {
            raw.contains("network", ignoreCase = true) ||
                raw.contains("timeout", ignoreCase = true) ||
                raw.contains("unavailable", ignoreCase = true) ->
                "Internet connection is unstable. Please wait a moment and try again."
            raw.contains("paymentId, orderId and signature", ignoreCase = true) ||
                raw.contains("signature", ignoreCase = true) ->
                "Payment confirmation was incomplete. Please retry from the payment screen."
            raw.contains("order", ignoreCase = true) ->
                "Payment order could not be prepared. Please try again."
            raw.isBlank() -> "Payment could not be completed. Please try again."
            else -> raw
        }
    }
}
