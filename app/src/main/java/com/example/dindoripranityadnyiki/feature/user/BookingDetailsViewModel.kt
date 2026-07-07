@file:Suppress("DEPRECATION")

package com.example.dindoripranityadnyiki.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.core.data.Constants
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.data.SacredSevaRepository
import com.example.dindoripranityadnyiki.core.data.UserRepository
import com.example.dindoripranityadnyiki.core.data.toPresentationMap
import com.example.dindoripranityadnyiki.core.data.BookingCreateRequest
import com.example.dindoripranityadnyiki.core.util.extractDistrictFromPlace
import com.example.dindoripranityadnyiki.core.util.extractPincodeFromPlace
import com.example.dindoripranityadnyiki.feature.common.SmartAddressResult
import com.google.android.libraries.places.api.model.Place
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class BookingDetailsUiState(
    val fullName: String = "",
    val nameError: String? = null,
    val address: String = "",
    val addressError: String? = null,
    val mobile: String = "",
    val mobileError: String? = null,
    val email: String = "",
    val district: String = "",
    val pincode: String = "",
    val districtError: String? = null,
    val pincodeError: String? = null,
    val specialInstructions: String = "",
    val poojaImageUrl: String? = null,
    val isPoojaDetailsLoaded: Boolean = false,
    val selectedLocationType: String = "Home",
    val selectedDate: LocalDate? = null,
    val dateError: String? = null,
    val availableDates: List<String> = emptyList(), 
    val userLat: Double = 0.0,
    val userLng: Double = 0.0,
    val isLocationFetched: Boolean = false,
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val clientRequestId: String = "",
    val bookingId: String? = null,
    val error: String? = null
)

@HiltViewModel
class BookingDetailsViewModel @Inject constructor(
    private val sevaRepository: SacredSevaRepository,
    private val userRepository: UserRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingDetailsUiState())
    val uiState = _uiState.asStateFlow()
    
    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                fetchGlobalMuhurts()
                refreshProfileDefaults()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            try {
                refreshProfileDefaults()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.localizedMessage)
            }
        }
    }

    private suspend fun refreshProfileDefaults() {
        val profile = loadProfileLikeProfileScreen()
            .ifEmpty { userRepository.getUserProfile()?.toPresentationMap().orEmpty() }
        applyProfileDefaults(profile, readCachedProfile())
    }

    private suspend fun loadProfileLikeProfileScreen(): Map<String, Any> {
        val profile = userRepository.getUserProfile() ?: return emptyMap()
        return profile.toPresentationMap()
    }

    private suspend fun readCachedProfile(): Map<String, Any> = mapOf(
        "fullName" to dataStoreManager.readString(PrefKeys.USER_NAME).first(),
        "mobile" to dataStoreManager.readString(PrefKeys.USER_MOBILE).first(),
        "email" to dataStoreManager.readString(PrefKeys.USER_EMAIL).first(),
        "address" to dataStoreManager.readString(PrefKeys.USER_ADDRESS).first(),
        "district" to dataStoreManager.readString(PrefKeys.USER_DISTRICT).first(),
        "pincode" to dataStoreManager.readString(PrefKeys.USER_PINCODE).first()
    )

    private fun applyProfileDefaults(user: Map<String, Any>, cached: Map<String, Any>) {
        val lat = user.firstNumber("lat", "latitude") ?: 0.0
        val lng = user.firstNumber("lng", "longitude") ?: 0.0

        _uiState.value = _uiState.value.copy(
            fullName = user.firstString("fullName", "name", "displayName").ifBlank { cached.firstString("fullName") },
            address = user.firstString("address", "fullAddress").ifBlank { cached.firstString("address") },
            mobile = user.firstString("mobile", "phone").ifBlank { cached.firstString("mobile") },
            email = user.firstString("email").ifBlank { cached.firstString("email") },
            district = Constants.normalizeDistrict(user.firstString("district").ifBlank { cached.firstString("district") }),
            pincode = user.firstString("pincode").ifBlank { cached.firstString("pincode") },
            userLat = lat,
            userLng = lng,
            isLocationFetched = lat != 0.0 && lng != 0.0
        )
    }

    private fun fetchGlobalMuhurts() {
        viewModelScope.launch {
            try {
                val dates = sevaRepository.getAvailableDates()
                _uiState.value = _uiState.value.copy(
                    availableDates = dates,
                    dateError = if (dates.isEmpty()) "सध्या कोणतेही मुहूर्त उपलब्ध नाहीत." else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    availableDates = emptyList(),
                    dateError = "मुहूर्त लोड करताना त्रुटी आली. कृपया पुन्हा प्रयत्न करा."
                )
            }
        }
    }

    fun loadPoojaImage(poojaId: String) {
        viewModelScope.launch {
            val details = sevaRepository.getPoojaDetails(poojaId)
            _uiState.value = _uiState.value.copy(
                poojaImageUrl = details?.imageUrl.orEmpty(),
                isPoojaDetailsLoaded = details != null
            )
        }
    }

    fun updateName(name: String) { _uiState.value = _uiState.value.copy(fullName = name, nameError = null) }
    
    fun updateMobile(mob: String) { 
        val cleaned = mob.filter { it.isDigit() }.take(10)
        _uiState.value = _uiState.value.copy(mobile = cleaned, mobileError = null) 
    }
    
    fun updateEmail(email: String) { _uiState.value = _uiState.value.copy(email = email) }
    
    fun updateAddress(addr: String) { 
        val current = _uiState.value
        if (addr != current.address) {
            _uiState.value = current.copy(
                address = addr, 
                addressError = null,
                district = "", 
                pincode = "",  
                userLat = 0.0,
                userLng = 0.0,
                isLocationFetched = false
            ) 
        }
    }

    fun onPlaceSelected(place: Place) {
        _uiState.value = _uiState.value.copy(
            address = place.address ?: place.name ?: "",
            district = Constants.normalizeDistrict(extractDistrictFromPlace(place)),
            pincode = extractPincodeFromPlace(place),
            userLat = place.latLng?.latitude ?: 0.0,
            userLng = place.latLng?.longitude ?: 0.0,
            isLocationFetched = true,
            addressError = null,
            districtError = null,
            pincodeError = null
        )
    }

    fun onSmartAddressSelected(result: SmartAddressResult) {
        _uiState.value = _uiState.value.copy(
            address = result.address,
            pincode = result.pincode,
            district = Constants.normalizeDistrict(result.district),
            userLat = result.lat,
            userLng = result.lng,
            isLocationFetched = true,
            addressError = null,
            districtError = null,
            pincodeError = null
        )
    }

    fun onDateSelected(date: LocalDate) { _uiState.value = _uiState.value.copy(selectedDate = date, dateError = null) }
    fun updateLocationType(type: String) { _uiState.value = _uiState.value.copy(selectedLocationType = type) }
    fun updateSpecialInstructions(instr: String) { _uiState.value = _uiState.value.copy(specialInstructions = instr) }

    private fun validateAll(): Boolean {
        val state = _uiState.value
        val nameErr = if (state.fullName.isBlank()) "नाव आवश्यक आहे" else null
        val mobileErr = if (!state.mobile.matches(Regex("^[6-9]\\d{9}$"))) "योग्य मोबाईल नंबर टाका" else null
        val addrErr = if (state.address.isBlank()) "पत्ता आवश्यक आहे" else null
        
        val isValidDistrict = Constants.DISTRICTS.any { it.first == state.district }
        val distErr = if (state.district.isBlank() || !isValidDistrict) "कृपया लिस्ट मधून वैध जिल्हा निवडा" else null
        
        val pinErr = if (!state.pincode.matches(Regex("^[1-9][0-9]{5}$"))) "योग्य ६ अंकी पिनकोड टाका" else null
        
        val dateErr = if (state.selectedDate == null) "तारीख निवडा" else null
        val locErr = if (!state.isLocationFetched || state.userLat == 0.0) "कृपया लिस्ट मधून अचूक पत्ता निवडा" else null

        _uiState.value = _uiState.value.copy(
            nameError = nameErr, 
            mobileError = mobileErr, 
            addressError = addrErr,
            districtError = distErr, 
            pincodeError = pinErr, 
            dateError = dateErr, 
            error = locErr
        )
        return nameErr == null && mobileErr == null && addrErr == null && distErr == null && pinErr == null && dateErr == null && locErr == null
    }

    fun confirmBooking(poojaId: String, poojaName: String) {
        if (!validateAll()) return
        val state = _uiState.value
        if (state.isProcessing || !state.isPoojaDetailsLoaded) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            val bookingData = BookingCreateRequest(
                clientRequestId = UUID.randomUUID().toString(),
                poojaId = poojaId,
                poojaName = poojaName,
                date = state.selectedDate.toString(),
                time = "",
                contactName = state.fullName,
                contactPhone = state.mobile,
                address = state.address,
                district = state.district,
                pincode = state.pincode,
                lat = state.userLat,
                lng = state.userLng,
                instructions = state.specialInstructions
            )
            val result = sevaRepository.createBookingWithCounter(bookingData)
            _uiState.value = _uiState.value.copy(
                isProcessing = false, 
                bookingId = result.getOrNull(), 
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    private fun Map<String, Any>.firstString(vararg keys: String): String = keys.firstNotNullOfOrNull { this[it] as? String }?.trim().orEmpty()
    private fun Map<String, Any>.firstNumber(vararg keys: String): Double? = keys.firstNotNullOfOrNull { (this[it] as? Number)?.toDouble() }
}
