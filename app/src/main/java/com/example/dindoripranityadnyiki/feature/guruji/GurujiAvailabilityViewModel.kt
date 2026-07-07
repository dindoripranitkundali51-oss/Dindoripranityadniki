package com.example.dindoripranityadnyiki.feature.guruji

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.core.data.GurujiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AvailabilityUiState(
    val selectedDates: List<String> = emptyList(),
    val bookedDates: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val message: String? = null
)

class GurujiAvailabilityViewModel(
    private val repository: GurujiRepository = GurujiRepository.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AvailabilityUiState())
    val uiState = _uiState.asStateFlow()

    private var bookingsListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        loadAvailability()
        listenToBookings()
    }

    private fun listenToBookings() {
        bookingsListener = repository.listenToAssignedBookings { bookings ->
            val bookedDates = bookings.map { it.date }.filter { it.isNotBlank() }.distinct()
            _uiState.value = _uiState.value.copy(bookedDates = bookedDates)
        }
    }

    override fun onCleared() {
        super.onCleared()
        bookingsListener?.remove()
    }

    private fun loadAvailability() {
        viewModelScope.launch {
            try {
                val profile = repository.getGurujiProfile()
                if (profile != null) {
                    val dates = profile.availableDates
                        .distinct()
                        .filter { runCatching { !LocalDate.parse(it).isBefore(LocalDate.now()) }.getOrDefault(false) }
                        .sorted()
                    _uiState.value = _uiState.value.copy(selectedDates = dates, isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "\u092a\u094d\u0930\u094b\u092b\u093e\u0907\u0932 \u0938\u093e\u092a\u0921\u0932\u0947 \u0928\u093e\u0939\u0940"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, message = e.localizedMessage)
            }
        }
    }

    fun toggleDate(date: String) {
        val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return
        if (parsedDate.isBefore(LocalDate.now())) {
            _uiState.value = _uiState.value.copy(
                message = "\u092e\u093e\u0917\u0940\u0932 \u0924\u093e\u0930\u0940\u0916 \u0928\u093f\u0935\u0921\u0924\u093e \u092f\u0947\u0923\u093e\u0930 \u0928\u093e\u0939\u0940"
            )
            return
        }
        val currentDates = _uiState.value.selectedDates.toMutableList()
        if (currentDates.contains(date)) currentDates.remove(date) else currentDates.add(date)
        _uiState.value = _uiState.value.copy(selectedDates = currentDates)
    }

    fun saveAvailability() {
        val dates = _uiState.value.selectedDates
            .filter { runCatching { !LocalDate.parse(it).isBefore(LocalDate.now()) }.getOrDefault(false) }
            .distinct()
            .sorted()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true)

            repository.updateAvailability(dates)
                .onSuccess { msg ->
                    _uiState.value = _uiState.value.copy(
                        selectedDates = dates,
                        isUpdating = false,
                        message = msg
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        message = "\u0924\u094d\u0930\u0941\u091f\u0940: ${e.localizedMessage}"
                    )
                }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
