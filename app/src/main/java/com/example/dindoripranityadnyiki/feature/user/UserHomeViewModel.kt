package com.example.dindoripranityadnyiki.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.data.SacredSevaRepository
import com.example.dindoripranityadnyiki.core.data.UserRepository
import com.example.dindoripranityadnyiki.core.data.RepositorySubscription
import com.example.dindoripranityadnyiki.core.network.ApiService
import com.example.dindoripranityadnyiki.core.network.UpdateFcmTokenRequest
import com.example.dindoripranityadnyiki.core.util.PredictiveEngine
import com.example.dindoripranityadnyiki.core.util.Resource
import com.example.dindoripranityadnyiki.core.util.SessionManager
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

    private var activeBookingSubscription: RepositorySubscription? = null
    private var historySubscription: RepositorySubscription? = null

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

        // 1. Load Profile from Local DataStore/Cache (Production Flow)
        userRepository.getUserProfileFlow().onEach { resource ->
            when (resource) {
                is Resource.Success -> {
                    val profile = resource.data
                    // In Production, we'll map fields from Profile to UI
                    val data = mapOf(
                        "fullName" to (profile?.fullName ?: ""),
                        "mobile" to (profile?.mobile ?: ""),
                        "email" to (profile?.email ?: ""),
                        "address" to (profile?.address ?: ""),
                        "district" to (profile?.district ?: ""),
                        "status" to (profile?.status ?: "Active")
                    )
                    _uiState.value = _uiState.value.copy(
                        userProfile = data,
                        isBlocked = profile?.status == "Blocked" || profile?.status == "Deleted",
                        isLoading = false
                    )
                }
                is Resource.Error -> { _uiState.value = _uiState.value.copy(isLoading = false) }
                else -> {}
            }
        }.launchIn(viewModelScope)

        // 2. Load Available Poojas from REST API
        viewModelScope.launch {
            try {
                val poojas = sevaRepository.getSacredServices()
                _uiState.value = _uiState.value.copy(poojaList = poojas.map {
                    mapOf("id" to it.id, "name" to it.name, "nameEn" to it.nameEn)
                })
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Failed to load poojas")
            }
        }

        // 3. Setup REST Polling Subscriptions (Replaces Firestore Realtime)
        try {
            activeBookingSubscription = sevaRepository.listenToActiveBooking(userId) { active ->
                _uiState.value = _uiState.value.copy(upcomingBooking = if (active != null) {
                    mapOf("id" to active.id, "poojaName" to active.poojaName, "status" to active.status)
                } else null)
            }

            // History polling also simplified for production
            historySubscription = sevaRepository.listenToBookingHistory(userId) { list ->
                _uiState.value = _uiState.value.copy(pastBookings = list.map {
                    mapOf("id" to it.id, "poojaName" to it.poojaName, "status" to it.status)
                })
                updatePredictiveSuggestion()
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Failed to setup booking listeners")
            cleanupSubscriptions()
        }
    }

    private fun updatePredictiveSuggestion() {
        val pastBookings = _uiState.value.pastBookings
        val panchang = _uiState.value.todayPanchang
        val suggestion = predictiveEngine.suggestNextSeva(pastBookings, panchang)
        _uiState.value = _uiState.value.copy(predictiveSuggestion = suggestion)
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
            cleanupSubscriptions()
            SessionManager.signOut(context)
            onComplete()
        }
    }

    private fun cleanupSubscriptions() {
        activeBookingSubscription?.remove()
        historySubscription?.remove()
    }

    override fun onCleared() {
        cleanupSubscriptions()
        super.onCleared()
    }
}
