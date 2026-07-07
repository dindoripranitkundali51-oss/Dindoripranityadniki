package com.example.dindoripranityadnyiki.feature.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.network.ApiService
import com.example.dindoripranityadnyiki.core.network.ChatMessageDto
import com.example.dindoripranityadnyiki.core.network.SendMessageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessageDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LiveSupportChatViewModel @Inject constructor(
    private val apiService: ApiService,
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private var isPolling = false

    fun startChatPolling() {
        if (isPolling) return
        isPolling = true
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            while (isPolling) {
                fetchMessages()
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    fun stopChatPolling() {
        isPolling = false
    }

    private suspend fun fetchMessages() {
        try {
            val mgr = DataStoreManager(getApplication())
            val token = mgr.readString(PrefKeys.JWT_TOKEN).first()
            if (token.isBlank()) return

            val response = apiService.getChatHistory("Bearer $token", "Admin")
            if (response.isSuccessful && response.body()?.success == true) {
                _uiState.value = _uiState.value.copy(
                    messages = response.body()?.data ?: emptyList(),
                    isLoading = false,
                    error = null
                )
            }
        } catch (e: Exception) {
            // Silence connection exceptions during active background polling to prevent crash
        }
    }

    fun sendMessage(text: String) {
        if (text.trim().isBlank()) return
        _uiState.value = _uiState.value.copy(isSending = true)

        viewModelScope.launch {
            try {
                val mgr = DataStoreManager(getApplication())
                val token = mgr.readString(PrefKeys.JWT_TOKEN).first()

                val response = apiService.sendChatMessage(
                    token = "Bearer $token",
                    request = SendMessageRequest(receiverId = "Admin", message = text.trim())
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    fetchMessages() // Refresh immediately
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.localizedMessage)
            } finally {
                _uiState.value = _uiState.value.copy(isSending = false)
            }
        }
    }
}
