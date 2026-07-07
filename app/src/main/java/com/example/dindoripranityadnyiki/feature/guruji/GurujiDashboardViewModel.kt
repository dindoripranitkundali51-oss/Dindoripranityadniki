package com.example.dindoripranityadnyiki.feature.guruji

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.core.data.GurujiRepository
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.data.toPresentationMap
import com.example.dindoripranityadnyiki.core.network.ApiService
import com.example.dindoripranityadnyiki.core.network.UpdateFcmTokenRequest
import com.example.dindoripranityadnyiki.core.util.Resource
import com.example.dindoripranityadnyiki.core.util.SessionManager
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class GurujiDashboardUiState(
    val gurujiProfile: Map<String, Any>? = null,
    val assignedBookings: List<Map<String, Any>> = emptyList(),
    val isVerified: Boolean = false,
    val isAvailable: Boolean = false,
    val approvalStatus: String = "PENDING",
    val walletBalance: Double = 0.0,
    val isLoading: Boolean = true,
    val isBlocked: Boolean = false,
    val errorMessage: String? = null,
    val actionLoading: String? = null
)

@HiltViewModel
class GurujiDashboardViewModel @Inject constructor(
    private val repository: GurujiRepository,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GurujiDashboardUiState())
    val uiState = _uiState.asStateFlow()

    private var bookingsListener: ListenerRegistration? = null

    init {
        monitorGurujiData()
    }

    private fun monitorGurujiData() {
        FirebaseMessaging.getInstance().subscribeToTopic("providers")
        refreshFcmToken()

        repository.getGurujiProfileFlow().onEach { resource ->
            when (resource) {
                is Resource.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
                is Resource.Success -> {
                    val data = resource.data
                    if (data == null) {
                        _uiState.value = _uiState.value.copy(isBlocked = true, isLoading = false)
                        return@onEach
                    }

                    val status = data.status
                    val available = data.isAvailable
                    val wallet = data.walletBalance
                    val isBlocked = status == "Blocked"

                    _uiState.value = _uiState.value.copy(
                        gurujiProfile = data.toPresentationMap(),
                        approvalStatus = status,
                        isVerified = (status == "Approved" || status == "Active"),
                        isAvailable = available,
                        walletBalance = wallet,
                        isBlocked = isBlocked,
                        isLoading = false,
                        errorMessage = null
                    )

                    if (_uiState.value.isVerified && !isBlocked) {
                        loadBookings()
                    } else {
                        bookingsListener?.remove()
                        bookingsListener = null
                        _uiState.value = _uiState.value.copy(assignedBookings = emptyList())
                    }
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = resource.message
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun refreshFcmToken() {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                if (token.isNotBlank()) {
                    val jwt = dataStoreManager.readString(PrefKeys.JWT_TOKEN).first()
                    if (jwt.isNotBlank()) {
                        apiService.updateFcmToken("Bearer $jwt", UpdateFcmTokenRequest(token))
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun loadBookings() {
        if (bookingsListener != null) return

        try {
            bookingsListener = repository.listenToAssignedBookings { list ->
                _uiState.value = _uiState.value.copy(assignedBookings = list.map { it.toPresentationMap() })
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(errorMessage = "Failed to load bookings")
            cleanupListeners()
        }
    }

    fun acceptBooking(bookingId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = bookingId)
            val result = repository.updateBookingStatus(bookingId, "Accepted")
            if (result is Resource.Error) {
                _uiState.value = _uiState.value.copy(errorMessage = result.message)
            }
            _uiState.value = _uiState.value.copy(actionLoading = null)
        }
    }

    fun rejectBooking(bookingId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionLoading = bookingId)
            val result = repository.updateBookingStatus(bookingId, "Rejected")
            if (result is Resource.Error) {
                _uiState.value = _uiState.value.copy(errorMessage = result.message)
            }
            _uiState.value = _uiState.value.copy(actionLoading = null)
        }
    }

    fun logout(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            cleanupListeners()
            SessionManager.signOut(context)
            onComplete()
        }
    }

    private fun cleanupListeners() {
        bookingsListener?.remove()
    }

    override fun onCleared() {
        cleanupListeners()
        super.onCleared()
    }
}
