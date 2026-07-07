package com.example.dindoripranityadnyiki.feature.guruji

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.core.data.Constants
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.core.util.SessionManager
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class GurujiProfileUiState(
    val uid: String = "",
    val name: String = "",
    val mobile: String = "",
    val email: String = "",
    val district: String = "",
    val address: String = "",
    val pincode: String = "",
    val experience: String = "",
    val photoUrl: String = "",
    val upiId: String = "",
    val expertises: List<String> = emptyList(),
    val workDistricts: List<String> = emptyList(),
    val status: String = "",
    val joinedDate: String = "",
    val accountHolder: String = "",
    val accountNumber: String = "",
    val ifsc: String = "",
    val docs: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val message: String? = null
)

class GurujiProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(GurujiProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val mgr = DataStoreManager(getApplication())
                val mobile = mgr.readString(PrefKeys.USER_MOBILE).first()
                if (mobile.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "User not logged in"
                    )
                    return@launch
                }

                _uiState.value = GurujiProfileUiState(
                    uid = mobile,
                    name = mgr.readString(PrefKeys.USER_NAME).first(),
                    mobile = mobile,
                    email = mgr.readString(PrefKeys.USER_EMAIL).first(),
                    address = mgr.readString(PrefKeys.USER_ADDRESS).first(),
                    district = Constants.normalizeDistrict(mgr.readString(PrefKeys.USER_DISTRICT).first()),
                    pincode = mgr.readString(PrefKeys.USER_PINCODE).first(),
                    experience = "5", // Fallback experience
                    status = "Active",
                    joinedDate = "04 Jul 2026",
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Error: ${e.localizedMessage}"
                )
            }
        }
    }

    fun updateField(field: String, value: Any) {
        _uiState.value = when (field) {
            "photo" -> _uiState.value.copy(photoUrl = value as? String ?: "")
            else -> _uiState.value
        }
    }

    fun saveProfile(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                // In trial mode, changes to photo are kept in memory or we can save them
                onSuccess()
            } catch (_: Exception) {
            }
        }
    }

    fun logout(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            SessionManager.signOut(context)
            onComplete()
        }
    }
}
