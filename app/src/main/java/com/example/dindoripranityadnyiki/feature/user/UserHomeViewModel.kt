package com.example.dindoripranityadnyiki.feature.user

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.data.SacredSevaRepository
import com.example.dindoripranityadnyiki.core.data.UserRepository
import com.example.dindoripranityadnyiki.core.data.toPresentationMap
import com.example.dindoripranityadnyiki.core.network.ApiService
import com.example.dindoripranityadnyiki.core.network.UpdateFcmTokenRequest
import com.example.dindoripranityadnyiki.core.util.PredictiveEngine
import com.example.dindoripranityadnyiki.core.util.Resource
import com.example.dindoripranityadnyiki.core.util.SessionManager
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class UserHomeUiState(
    val userProfile: Map<String, Any>? = null,
    val upcomingBooking: Map<String, Any>? = null,
    val pastBookings: List<Map<String, Any>> = emptyList(),
    val poojaList: List<Map<String, Any>> = emptyList(),
    val todayPanchang: Map<String, Any>? = null,
    val predictiveSuggestion: Map<String, String>? = null,
    val isLoading: Boolean = true,
    val isBlocked: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class UserHomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sevaRepository: SacredSevaRepository,
    private val apiService: ApiService,
    private val messaging: FirebaseMessaging,
    private val dataStoreManager: DataStoreManager,
    private val predictiveEngine: PredictiveEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserHomeUiState())
    val uiState: StateFlow<UserHomeUiState> = _uiState.asStateFlow()

    private var activeBookingListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null
    private var panchangListener: ListenerRegistration? = null

    init {
        viewModelScope.launch {
            val mobile = dataStoreManager.readString(PrefKeys.USER_MOBILE).first()
            if (mobile.isNotEmpty()) {
                loadDashboard(mobile)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "User not logged in")
            }
        }
    }

    private fun loadDashboard(userId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        messaging.subscribeToTopic("all_users")
        refreshFcmToken()

        // 1. Load Profile
        userRepository.getUserProfileFlow().onEach { resource ->
            when (resource) {
                is Resource.Success -> {
                    val profile = resource.data
                    val data = profile?.toPresentationMap()
                    val status = profile?.status ?: "Active"
                    data?.let { cacheUserProfile(it) }
                    _uiState.value = _uiState.value.copy(
                        userProfile = data,
                        isBlocked = status == "Blocked" || status == "Deleted",
                        isLoading = false
                    )
                }
                is Resource.Error -> { _uiState.value = _uiState.value.copy(isLoading = false) }
                else -> {}
            }
        }.launchIn(viewModelScope)

        // 2. Load Available Poojas
        viewModelScope.launch {
            try {
                val poojas = sevaRepository.getSacredServices()
                _uiState.value = _uiState.value.copy(poojaList = poojas.map { it.toPresentationMap() })
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Failed to load poojas")
            }
        }

        // 3. Setup listeners with proper error handling
        try {
            activeBookingListener = sevaRepository.listenToActiveBooking(userId) { active ->
                _uiState.value = _uiState.value.copy(upcomingBooking = active?.toPresentationMap())
            }

            historyListener = sevaRepository.listenToBookingHistory(userId) { list ->
                _uiState.value = _uiState.value.copy(pastBookings = list.map { it.toPresentationMap() })
                // Update predictive suggestion when we have new history
                updatePredictiveSuggestion()
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Failed to setup booking listeners")
            cleanupListeners()
        }
    }

    private fun updatePredictiveSuggestion() {
        val pastBookings = _uiState.value.pastBookings
        val panchang = _uiState.value.todayPanchang
        val suggestion = predictiveEngine.suggestNextSeva(pastBookings, panchang)
        _uiState.value = _uiState.value.copy(predictiveSuggestion = suggestion)
    }

    private fun cacheUserProfile(profile: Map<String, Any>) {
        viewModelScope.launch {
            dataStoreManager.saveStringPreference(PrefKeys.USER_NAME, profile.firstString("fullName", "name", "displayName"))
            dataStoreManager.saveStringPreference(PrefKeys.USER_MOBILE, profile.firstString("mobile", "phone", "phoneNumber", "contactPhone"))
            dataStoreManager.saveStringPreference(PrefKeys.USER_EMAIL, profile.firstString("email"))
            dataStoreManager.saveStringPreference(PrefKeys.USER_ADDRESS, profile.firstString("address", "fullAddress", "homeAddress", "full_address"))
            dataStoreManager.saveStringPreference(PrefKeys.USER_DISTRICT, profile.firstString("district", "selectedDistrict", "city"))
            dataStoreManager.saveStringPreference(PrefKeys.USER_PINCODE, profile.firstString("pincode", "pinCode", "postalCode"))
            
            // Save coordinates
            val latVal = (profile["lat"] ?: profile["latitude"])?.toString() ?: "0.0"
            val lngVal = (profile["lng"] ?: profile["longitude"])?.toString() ?: "0.0"
            dataStoreManager.saveStringPreference(PrefKeys.USER_LAT, latVal)
            dataStoreManager.saveStringPreference(PrefKeys.USER_LNG, lngVal)
        }
    }

    private fun Map<String, Any>.firstString(vararg keys: String): String {
        return keys.firstNotNullOfOrNull { key -> this[key] as? String }?.trim().orEmpty()
    }

    private fun refreshFcmToken() {
        viewModelScope.launch {
            try {
                val token = messaging.token.await()
                if (token.isNotBlank()) {
                    val jwt = dataStoreManager.readString(PrefKeys.JWT_TOKEN).first()
                    if (jwt.isNotBlank()) {
                        apiService.updateFcmToken("Bearer $jwt", UpdateFcmTokenRequest(token))
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun logout(context: android.content.Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            cleanupListeners()
            SessionManager.signOut(context)
            onComplete()
        }
    }

    private fun cleanupListeners() {
        activeBookingListener?.remove()
        historyListener?.remove()
        panchangListener?.remove()
    }

    override fun onCleared() {
        cleanupListeners()
        super.onCleared()
    }
}
