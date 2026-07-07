@file:Suppress("DEPRECATION")

package com.example.dindoripranityadnyiki.feature.guruji

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.core.data.Constants
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.data.SacredSevaRepository
import com.example.dindoripranityadnyiki.core.network.ApiService
import com.example.dindoripranityadnyiki.core.network.ExpertRegisterRequest
import com.example.dindoripranityadnyiki.feature.common.SmartAddressResult
import com.google.android.libraries.places.api.model.Place
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GurujiRegistrationUiState(
    val fullName: String = "",
    val mobile: String = "",
    val email: String = "",
    val district: String = "",
    val pincode: String = "",
    val address: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val experience: String = "",
    val aadhaarNumber: String = "",
    val panNumber: String = "",
    val isLoading: Boolean = false,
    val loadingMessage: String = "", 
    val isPoojaLoading: Boolean = true,
    val error: String? = null,
    val success: Boolean = false,
    val poojaList: List<String> = emptyList()
)

@HiltViewModel
class GurujiRegistrationViewModel @Inject constructor(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val sevaRepository: SacredSevaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GurujiRegistrationUiState())
    val uiState = _uiState.asStateFlow()

    val selectedDistricts = mutableStateListOf<String>()
    val selectedExpertises = mutableStateListOf<String>()
    val selectedDocs = mutableStateMapOf<String, Uri>()

    init {
        loadPoojas()
    }

    fun updateField(field: String, value: String) {
        _uiState.value = when (field) {
            "fullName" -> _uiState.value.copy(fullName = value)
            "mobile" -> _uiState.value.copy(mobile = value.filter { it.isDigit() }.take(10))
            "email" -> _uiState.value.copy(email = value.trim())
            "district" -> _uiState.value.copy(district = Constants.normalizeDistrict(value))
            "pincode" -> _uiState.value.copy(pincode = value.filter { it.isDigit() }.take(6))
            "address" -> _uiState.value.copy(address = value)
            "experience" -> _uiState.value.copy(experience = value.filter { it.isDigit() }.take(2))
            "aadhaar" -> _uiState.value.copy(aadhaarNumber = value.filter { it.isDigit() }.take(12))
            "pan" -> _uiState.value.copy(panNumber = value.uppercase().filter { it.isLetterOrDigit() }.take(10))
            else -> _uiState.value
        }
    }

    fun onPlaceSelected(place: Place) {
        var detectedPincode = ""
        var detectedDistrict = ""
        place.addressComponents?.asList()?.forEach { component ->
            when {
                component.types.contains("postal_code") -> detectedPincode = component.name
                component.types.contains("administrative_area_level_2") -> detectedDistrict = component.name
            }
        }
        _uiState.value = _uiState.value.copy(
            address = place.address ?: place.name ?: "",
            district = Constants.normalizeDistrict(detectedDistrict),
            pincode = detectedPincode,
            lat = place.latLng?.latitude ?: 0.0,
            lng = place.latLng?.longitude ?: 0.0
        )
    }

    fun onSmartAddressSelected(result: SmartAddressResult) {
        _uiState.value = _uiState.value.copy(
            address = result.address,
            district = Constants.normalizeDistrict(result.district),
            pincode = result.pincode,
            lat = result.lat,
            lng = result.lng
        )
    }

    fun loadPoojas() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPoojaLoading = true)
            try {
                val services = sevaRepository.getSacredServices(forceRefresh = false)
                val servicePoojas = services.mapNotNull { service ->
                    service.nameMr.ifBlank { service.name.ifBlank { service.nameEn } }.trim().takeIf { it.isNotBlank() }
                }.distinct()
                val finalPoojas = if (servicePoojas.isNotEmpty()) {
                    servicePoojas
                } else {
                    buildList {
                        addAll(Constants.MAIN_POOJA_LIST)
                        add(Constants.POOJA_SEPARATOR)
                        addAll(Constants.OTHER_POOJA_LIST)
                    }
                }
                _uiState.value = _uiState.value.copy(poojaList = finalPoojas, isPoojaLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isPoojaLoading = false)
            }
        }
    }

    fun registerGuruji(context: Context, password: String, onSuccess: () -> Unit) {
        val state = _uiState.value
        val actualExpertises = selectedExpertises.filter { it != Constants.POOJA_SEPARATOR }
        val expertiseStr = actualExpertises.joinToString(",")

        if (state.fullName.trim().length < 3 || !state.mobile.matches(Regex("^[6-9]\\d{9}$")) ||
            state.lat == 0.0 || state.district.isBlank() || !state.pincode.matches(Regex("^\\d{6}$")) ||
            !state.aadhaarNumber.matches(Regex("^\\d{12}$")) || !state.panNumber.matches(Regex("^[A-Z]{5}\\d{4}[A-Z]$")) ||
            password.length < 6
        ) {
            _uiState.value = _uiState.value.copy(error = "कृपया सर्व माहिती अचूक भरा.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, loadingMessage = "खाते तयार होत आहे...")
            try {
                val request = ExpertRegisterRequest(
                    fullName = state.fullName.trim(),
                    mobile = state.mobile,
                    email = state.email.trim(),
                    address = state.address.trim(),
                    district = state.district,
                    pincode = state.pincode,
                    lat = state.lat,
                    lng = state.lng,
                    expertises = expertiseStr,
                    expertType = "Guruji",
                    panNumber = state.panNumber,
                    aadharNumber = state.aadhaarNumber
                )

                val response = apiService.registerExpert(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    
                    // Save login state
                    body.token?.let { dataStoreManager.saveStringPreference(PrefKeys.JWT_TOKEN, it) }
                    dataStoreManager.savePreference(PrefKeys.IS_LOGGED_IN, true)
                    dataStoreManager.saveStringPreference(PrefKeys.USER_TYPE, "guruji")
                    dataStoreManager.saveStringPreference(PrefKeys.USER_ROLE, "Guruji")
                    
                    dataStoreManager.saveStringPreference(PrefKeys.USER_NAME, state.fullName.trim())
                    dataStoreManager.saveStringPreference(PrefKeys.USER_MOBILE, state.mobile)
                    dataStoreManager.saveStringPreference(PrefKeys.USER_EMAIL, state.email.trim())
                    dataStoreManager.saveStringPreference(PrefKeys.USER_ADDRESS, state.address.trim())
                    dataStoreManager.saveStringPreference(PrefKeys.USER_DISTRICT, state.district)
                    dataStoreManager.saveStringPreference(PrefKeys.USER_PINCODE, state.pincode)

                    _uiState.value = _uiState.value.copy(isLoading = false, success = true)
                    onSuccess()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }
}
