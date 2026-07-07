package com.example.dindoripranityadnyiki.core.util

import com.razorpay.PaymentData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class PaymentResultEvent {
    data class Success(val paymentId: String?, val data: PaymentData?) : PaymentResultEvent()
    data class Error(val code: Int, val message: String?, val data: PaymentData?) : PaymentResultEvent()
}

object PaymentResultBus {
    private val _events = MutableStateFlow<PaymentResultEvent?>(null)
    val events: StateFlow<PaymentResultEvent?> = _events.asStateFlow()

    fun publish(event: PaymentResultEvent) {
        _events.value = event
    }

    fun consume() {
        _events.value = null
    }
}
