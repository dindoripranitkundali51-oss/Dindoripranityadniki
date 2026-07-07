package com.example.dindoripranityadnyiki.feature.common

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.core.data.PrefKeys
import com.example.dindoripranityadnyiki.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationInboxViewModel @Inject constructor(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private var pollJob: Job? = null

    fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val token = dataStoreManager.readString(PrefKeys.JWT_TOKEN).first()
                    if (token.isNotBlank()) {
                        val response = apiService.getUserNotifications("Bearer $token")
                        if (response.isSuccessful && response.body()?.success == true) {
                            val list = response.body()?.data.orEmpty()
                            _notifications.value = list.map { item ->
                                mapOf<String, Any>(
                                    "id" to item.id,
                                    "userId" to item.userId,
                                    "title" to item.title,
                                    "body" to item.body,
                                    "action" to (item.action ?: ""),
                                    "bookingId" to (item.bookingId ?: ""),
                                    "deepLink" to (item.deepLink ?: ""),
                                    "priority" to item.priority,
                                    "isRead" to item.isRead,
                                    "createdAt" to item.createdAt
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NotificationInboxVM", "Failed to fetch notifications: ${e.message}")
                } finally {
                    _isLoading.value = false
                }
                delay(15000) // Poll every 15 seconds
            }
        }
    }

    fun markAsRead(id: Int) {
        viewModelScope.launch {
            try {
                val token = dataStoreManager.readString(PrefKeys.JWT_TOKEN).first()
                if (token.isNotBlank()) {
                    apiService.markNotificationRead("Bearer $token", id)
                    // Optimistic update
                    _notifications.value = _notifications.value.map {
                        if (it["id"] == id) it.plus("isRead" to true) else it
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationInboxVM", "Failed to mark read: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
