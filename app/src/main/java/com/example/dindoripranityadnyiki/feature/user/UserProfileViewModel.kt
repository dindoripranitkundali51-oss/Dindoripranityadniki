package com.example.dindoripranityadnyiki.feature.user

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

data class UserProfileUiState(
    val fullName: String = "",
    val mobile: String = "",
    val email: String = "",
    val address: String = "",
    val district: String = "",
    val pincode: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val cached = readCachedProfile()
                _uiState.value = cached.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage
                )
            }
        }
    }

    fun saveProfile(onSuccess: () -> Unit) {
        val state = _uiState.value
        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch {
            try {
                cacheProfile(state.copy(isLoading = false, isSaving = false))
                _uiState.value = _uiState.value.copy(isSaving = false)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = e.localizedMessage)
            }
        }
    }

    fun logout(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            SessionManager.signOut(context)
            onComplete()
        }
    }

    private suspend fun readCachedProfile(): UserProfileUiState {
        val mgr = DataStoreManager(getApplication())
        return UserProfileUiState(
            fullName = mgr.readString(PrefKeys.USER_NAME).first(),
            mobile = mgr.readString(PrefKeys.USER_MOBILE).first(),
            email = mgr.readString(PrefKeys.USER_EMAIL).first(),
            address = mgr.readString(PrefKeys.USER_ADDRESS).first(),
            district = Constants.normalizeDistrict(mgr.readString(PrefKeys.USER_DISTRICT).first()),
            pincode = mgr.readString(PrefKeys.USER_PINCODE).first(),
            isLoading = false
        )
    }

    private suspend fun cacheProfile(profile: UserProfileUiState) {
        val mgr = DataStoreManager(getApplication())
        mgr.saveStringPreference(PrefKeys.USER_NAME, profile.fullName)
        mgr.saveStringPreference(PrefKeys.USER_MOBILE, profile.mobile)
        mgr.saveStringPreference(PrefKeys.USER_EMAIL, profile.email)
        mgr.saveStringPreference(PrefKeys.USER_ADDRESS, profile.address)
        mgr.saveStringPreference(PrefKeys.USER_DISTRICT, Constants.normalizeDistrict(profile.district))
        mgr.saveStringPreference(PrefKeys.USER_PINCODE, profile.pincode)
    }
}
