package com.example.dindoripranityadnyiki.feature.user

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.R
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.network.ApiService
import com.example.dindoripranityadnyiki.core.network.LoginRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false
)

@HiltViewModel
class UserLoginViewModel @Inject constructor(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun login(context: Context, emailOrMobile: String, password: String, onRoleFound: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = apiService.login(LoginRequest(emailOrMobile))
                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    
                    // Save login state and token
                    dataStoreManager.saveStringPreference(PrefKeys.JWT_TOKEN, body.token)
                    dataStoreManager.savePreference(PrefKeys.IS_LOGGED_IN, true)
                    dataStoreManager.saveStringPreference(PrefKeys.USER_TYPE, body.role.lowercase())
                    dataStoreManager.saveStringPreference(PrefKeys.USER_ROLE, body.role)
                    
                    // Cache user profile details returned in LoginResponse profile payload
                    val profile = body.profile as? Map<*, *>
                    profile?.let {
                        dataStoreManager.saveStringPreference(PrefKeys.USER_NAME, it.firstString("fullName", "name"))
                        dataStoreManager.saveStringPreference(PrefKeys.USER_MOBILE, it.firstString("mobile", "phone"))
                        dataStoreManager.saveStringPreference(PrefKeys.USER_EMAIL, it.firstString("email"))
                        dataStoreManager.saveStringPreference(PrefKeys.USER_ADDRESS, it.firstString("address"))
                        dataStoreManager.saveStringPreference(PrefKeys.USER_DISTRICT, it.firstString("district"))
                        dataStoreManager.saveStringPreference(PrefKeys.USER_PINCODE, it.firstString("pincode"))
                        
                        // Save coordinates
                        val latVal = (it["lat"] ?: it["latitude"])?.toString() ?: "0.0"
                        val lngVal = (it["lng"] ?: it["longitude"])?.toString() ?: "0.0"
                        dataStoreManager.saveStringPreference(PrefKeys.USER_LAT, latVal)
                        dataStoreManager.saveStringPreference(PrefKeys.USER_LNG, lngVal)
                    }
                    
                    onRoleFound(body.role.lowercase())
                    _uiState.value = _uiState.value.copy(isLoading = false, loginSuccess = true)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Login failed"
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = context.getString(R.string.error_login_failed, e.localizedMessage ?: "")
                )
            }
        }
    }

    fun resetPassword(context: Context, input: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            // Password reset is handled as a placeholder for .NET REST migration
            onComplete("संकेतशब्द पुनर्संचयित करण्याची लिंक तुमच्या ईमेलवर पाठवली आहे (Trial Mode).")
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun Map<*, *>.firstString(vararg keys: String): String {
        return keys.firstNotNullOfOrNull { key -> this[key] as? String }?.trim().orEmpty()
    }
}
