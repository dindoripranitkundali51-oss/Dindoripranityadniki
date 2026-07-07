package com.example.dindoripranityadnyiki.feature.guruji

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.network.ApiService
import com.example.dindoripranityadnyiki.core.network.SubmitKycRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KycUiState(
    val panNumber: String = "",
    val aadharNumber: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val panError: String? = null,
    val aadharError: String? = null
)

@HiltViewModel
class KycUploadViewModel @Inject constructor(
    private val apiService: ApiService,
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(KycUiState())
    val uiState = _uiState.asStateFlow()

    fun updatePan(value: String) {
        _uiState.value = _uiState.value.copy(
            panNumber = value,
            panError = if (value.trim().length != 10) "PAN card must be exactly 10 characters." else null
        )
    }

    fun updateAadhar(value: String) {
        _uiState.value = _uiState.value.copy(
            aadharNumber = value,
            aadharError = if (value.trim().length != 12) "Aadhar must be exactly 12 digits." else null
        )
    }

    fun submitKyc(onSuccess: () -> Unit) {
        val pan = _uiState.value.panNumber.trim()
        val aadhar = _uiState.value.aadharNumber.trim()

        if (pan.length != 10) {
            _uiState.value = _uiState.value.copy(panError = "PAN must be 10 characters.")
            return
        }
        if (aadhar.length != 12) {
            _uiState.value = _uiState.value.copy(aadharError = "Aadhar must be 12 digits.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val mgr = DataStoreManager(getApplication())
                val token = mgr.readString(PrefKeys.JWT_TOKEN).first()

                val response = apiService.submitKyc(
                    token = "Bearer $token",
                    request = SubmitKycRequest(panNumber = pan, aadharNumber = aadhar)
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                    onSuccess()
                } else {
                    val errMsg = response.errorBody()?.string() ?: response.body()?.message ?: "KYC OCR validation failed."
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = errMsg)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Unknown connection error")
            }
        }
    }
}
