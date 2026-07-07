package com.example.dindoripranityadnyiki.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DigitalReceiptViewModel @Inject constructor(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _booking = MutableStateFlow<Map<String, Any>?>(null)
    val booking = _booking.asStateFlow()

    private val _receiptSnapshot = MutableStateFlow<Map<String, Any>?>(null)
    val receiptSnapshot = _receiptSnapshot.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun loadReceiptDetails(bookingId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val token = dataStoreManager.readString(PrefKeys.JWT_TOKEN).first()
                if (token.isBlank()) {
                    _errorMessage.value = "Please login again."
                    return@launch
                }

                val bookingRes = apiService.getBookingDetails("Bearer $token", bookingId)
                if (bookingRes.isSuccessful && bookingRes.body()?.success == true) {
                    val bookingModel = bookingRes.body()!!.data
                    val map = mapOf<String, Any>(
                        "id" to bookingModel.id,
                        "displayId" to bookingModel.displayId,
                        "userId" to bookingModel.userId,
                        "gurujiId" to (bookingModel.gurujiId ?: ""),
                        "gurujiName" to (bookingModel.gurujiName ?: ""),
                        "poojaId" to bookingModel.poojaId,
                        "poojaName" to (bookingModel.poojaName ?: ""),
                        "contactName" to (bookingModel.contactName ?: ""),
                        "contactPhone" to (bookingModel.contactPhone ?: ""),
                        "address" to (bookingModel.address ?: ""),
                        "district" to (bookingModel.district ?: ""),
                        "pincode" to (bookingModel.pincode ?: ""),
                        "date" to (bookingModel.date ?: ""),
                        "status" to bookingModel.status,
                        "paymentStatus" to bookingModel.paymentStatus,
                        "actualAmount" to (bookingModel.actualAmount ?: 0.0),
                        "gatewayPaymentId" to (bookingModel.razorpayPaymentId ?: "")
                    )
                    _booking.value = map

                    val receiptRes = apiService.getPaymentReceipt("Bearer $token", bookingId)
                    if (receiptRes.isSuccessful && receiptRes.body()?.success == true) {
                        val receiptDto = receiptRes.body()!!.data
                        val receiptMap = mapOf<String, Any>(
                            "bookingId" to receiptDto.bookingId,
                            "receiptNo" to receiptDto.receiptNo,
                            "amount" to receiptDto.amount,
                            "status" to receiptDto.paymentStatus,
                            "date" to receiptDto.createdAt
                        )
                        _receiptSnapshot.value = receiptMap
                    }
                } else {
                    _errorMessage.value = bookingRes.errorBody()?.string() ?: "Failed to load booking details."
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }
}
