package com.example.dindoripranityadnyiki.feature.guruji

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.network.ApiService
import com.example.dindoripranityadnyiki.core.network.LoginRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GurujiLoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false
)

@HiltViewModel
class GurujiLoginViewModel @Inject constructor(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GurujiLoginUiState())
    val uiState = _uiState.asStateFlow()

    fun login(context: Context, emailOrMobile: String, password: String, onComplete: () -> Unit) {
        val input = emailOrMobile.trim()
        if (input.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "कृपया सर्व माहिती भरा")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = apiService.login(LoginRequest(input))
                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    
                    if (body.role.lowercase() != "guruji" && body.role.lowercase() != "vastuexpert") {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "दिलेला ईमेल किंवा मोबाईल गुरुजी खाते म्हणून नोंदणीकृत नाही."
                        )
                        return@launch
                    }

                    // Save login state and token
                    dataStoreManager.saveStringPreference(PrefKeys.JWT_TOKEN, body.token)
                    dataStoreManager.savePreference(PrefKeys.IS_LOGGED_IN, true)
                    dataStoreManager.saveStringPreference(PrefKeys.USER_TYPE, body.role.lowercase())
                    dataStoreManager.saveStringPreference(PrefKeys.USER_ROLE, body.role)
                    
                    // Cache expert profile details
                    val profile = body.profile as? Map<*, *>
                    profile?.let {
                        dataStoreManager.saveStringPreference(PrefKeys.USER_NAME, it.firstString("fullName", "name"))
                        dataStoreManager.saveStringPreference(PrefKeys.USER_MOBILE, it.firstString("mobile", "phone"))
                        dataStoreManager.saveStringPreference(PrefKeys.USER_EMAIL, it.firstString("email"))
                        dataStoreManager.saveStringPreference(PrefKeys.USER_ADDRESS, it.firstString("address"))
                        dataStoreManager.saveStringPreference(PrefKeys.USER_DISTRICT, it.firstString("district"))
                        dataStoreManager.saveStringPreference(PrefKeys.USER_PINCODE, it.firstString("pincode"))
                    }

                    _uiState.value = _uiState.value.copy(isLoading = false, loginSuccess = true)
                    onComplete()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Login failed"
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "लॉगिन यशस्वी झाले नाही: ${e.localizedMessage}"
                )
            }
        }
    }

    fun resetPassword(input: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            onComplete("पासवर्ड रिसेट लिंक पाठवली आहे (Trial Mode).")
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun Map<*, *>.firstString(vararg keys: String): String {
        return keys.firstNotNullOfOrNull { key -> this[key] as? String }?.trim().orEmpty()
    }
}
