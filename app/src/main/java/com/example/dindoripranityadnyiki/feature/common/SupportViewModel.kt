package com.example.dindoripranityadnyiki.feature.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.network.ApiService
import com.example.dindoripranityadnyiki.core.network.CreateTicketRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _tickets = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val tickets = _tickets.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _createSuccess = MutableStateFlow(false)
    val createSuccess = _createSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun loadTickets() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val token = dataStoreManager.readString(PrefKeys.JWT_TOKEN).first()
                if (token.isNotBlank()) {
                    val res = apiService.getSupportTickets("Bearer $token")
                    if (res.isSuccessful && res.body()?.success == true) {
                        val list = res.body()?.data.orEmpty()
                        _tickets.value = list.map { item ->
                            mapOf<String, Any>(
                                "id" to item.id,
                                "userId" to item.userId,
                                "subject" to item.subject,
                                "message" to item.description,
                                "status" to item.status,
                                "priority" to item.priority,
                                "createdAt" to item.createdAt
                            )
                        }
                    } else {
                        _errorMessage.value = res.errorBody()?.string() ?: "Failed to load tickets"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitTicket(subject: String, description: String, category: String, language: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _createSuccess.value = false
            try {
                val token = dataStoreManager.readString(PrefKeys.JWT_TOKEN).first()
                if (token.isNotBlank()) {
                    val request = CreateTicketRequest(
                        subject = subject,
                        description = description,
                        category = category,
                        language = language
                    )
                    val res = apiService.createSupportTicket("Bearer $token", request)
                    if (res.isSuccessful && res.body()?.success == true) {
                        _createSuccess.value = true
                    } else {
                        _errorMessage.value = res.errorBody()?.string() ?: "Failed to submit ticket"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun requestAccountDeletion() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _createSuccess.value = false
            try {
                val token = dataStoreManager.readString(PrefKeys.JWT_TOKEN).first()
                if (token.isNotBlank()) {
                    val request = CreateTicketRequest(
                        subject = "Account Deletion Request",
                        description = "User requested account deletion.",
                        category = "Deletion",
                        language = "mr"
                    )
                    val res = apiService.createSupportTicket("Bearer $token", request)
                    if (res.isSuccessful && res.body()?.success == true) {
                        _createSuccess.value = true
                    } else {
                        _errorMessage.value = res.errorBody()?.string() ?: "Deletion request failed"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetSuccess() {
        _createSuccess.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
