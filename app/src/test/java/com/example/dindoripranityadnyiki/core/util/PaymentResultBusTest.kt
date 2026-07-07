package com.example.dindoripranityadnyiki.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentResultBusTest {

    @Test
    fun `publish stores latest success event and consume clears it`() {
        val event = PaymentResultEvent.Success(paymentId = "pay_123", data = null)

        PaymentResultBus.publish(event)
        assertEquals(event, PaymentResultBus.events.value)

        PaymentResultBus.consume()
        assertNull(PaymentResultBus.events.value)
    }

    @Test
    fun `publish stores latest error event`() {
        val event = PaymentResultEvent.Error(code = 400, message = "failed", data = null)

        PaymentResultBus.publish(event)
        assertEquals(event, PaymentResultBus.events.value)

        PaymentResultBus.consume()
    }
}
