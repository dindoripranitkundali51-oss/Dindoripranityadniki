@file:Suppress("DEPRECATION")

package com.example.dindoripranityadnyiki.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.core.data.Constants
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.data.UserRepository
import com.example.dindoripranityadnyiki.core.data.UserProfile
import com.example.dindoripranityadnyiki.core.util.extractDistrictFromPlace
import com.example.dindoripranityadnyiki.core.util.extractPincodeFromPlace
import com.example.dindoripranityadnyiki.feature.common.SmartAddressResult
import com.google.android.libraries.places.api.model.Place
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegistrationUiState(
    val fullName: String = "",
    val mobile: String = "",
    val email: String = "",
    val address: String = "",
    val selectedDistrict: String = "",
    val pincode: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registrationSuccess: Boolean = false
)

@HiltViewModel
class UserRegistrationViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState = _uiState.asStateFlow()

    fun updateField(field: String, value: String) {
        _uiState.value = when (field) {
            "fullName" -> _uiState.value.copy(fullName = value)
            "mobile" -> _uiState.value.copy(mobile = value.filter { it.isDigit() }.take(10))
            "email" -> _uiState.value.copy(email = value.trim())
            "address" -> _uiState.value.copy(address = value)
            "district" -> _uiState.value.copy(selectedDistrict = Constants.normalizeDistrict(value))
            "pincode" -> _uiState.value.copy(pincode = value.filter { it.isDigit() }.take(6))
            else -> _uiState.value
        }
    }

    fun onPlaceSelected(place: Place) {
        _uiState.value = _uiState.value.copy(
            address = place.address ?: place.name ?: "",
            pincode = extractPincodeFromPlace(place),
            selectedDistrict = Constants.normalizeDistrict(extractDistrictFromPlace(place)),
            lat = place.latLng?.latitude ?: 0.0,
            lng = place.latLng?.longitude ?: 0.0
        )
    }

    fun onSmartAddressSelected(result: SmartAddressResult) {
        _uiState.value = _uiState.value.copy(
            address = result.address,
            pincode = result.pincode,
            selectedDistrict = Constants.normalizeDistrict(result.district),
            lat = result.lat,
            lng = result.lng
        )
    }

    fun registerUser(password: String) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val profile = UserProfile(
                    uid = "",
                    fullName = state.fullName.trim(),
                    mobile = state.mobile,
                    email = state.email.trim(),
                    authEmail = "",
                    address = state.address.trim(),
                    district = Constants.normalizeDistrict(state.selectedDistrict),
                    pincode = state.pincode,
                    lat = state.lat,
                    lng = state.lng,
                    status = "Active",
                    fcmToken = ""
                )

                userRepository.registerNewUser("", profile)
                    .onSuccess {
                        dataStoreManager.savePreference(PrefKeys.IS_LOGGED_IN, true)
                        dataStoreManager.saveStringPreference(PrefKeys.USER_TYPE, "user")
                        dataStoreManager.saveStringPreference(PrefKeys.USER_ROLE, "Yajman")
                        _uiState.value = _uiState.value.copy(isLoading = false, registrationSuccess = true)
                    }
                    .onFailure { throw it }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.localizedMessage)
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
