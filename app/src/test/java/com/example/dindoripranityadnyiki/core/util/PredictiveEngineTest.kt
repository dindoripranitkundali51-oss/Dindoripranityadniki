package com.example.dindoripranityadnyiki.core.util

import org.junit.Test
import org.junit.Assert.*

class PredictiveEngineTest {

    private val predictiveEngine = PredictiveEngine()

    @Test
    fun `suggestNextSeva returns most booked seva when frequency is above two`() {
        val pastBookings = listOf(
            mapOf("poojaName" to "सत्यनारायण"),
            mapOf("poojaName" to "सत्यनारायण"),
            mapOf("poojaName" to "सत्यनारायण")
        )
        val result = predictiveEngine.suggestNextSeva(pastBookings, null)
        assertNotNull(result)
        assertEquals("REBOOK", result?.get("action"))
        assertEquals("सत्यनारायण", result?.get("poojaName"))
    }

    @Test
    fun `suggestNextSeva returns null when no conditions met`() {
        val pastBookings = listOf(mapOf("poojaName" to "सत्यनारायण"))
        val result = predictiveEngine.suggestNextSeva(pastBookings, null)
        assertNull(result)
    }

    @Test
    fun `suggestNextSeva returns chaturthi suggestion when tithi has chaturthi`() {
        val pastBookings = emptyList<Map<String, Any>>()
        val panchang = mapOf("tithi" to "संकष्टी चतुर्थी")
        val result = predictiveEngine.suggestNextSeva(pastBookings, panchang)
        assertNotNull(result)
        assertEquals("BOOK_NOW", result?.get("action"))
    }
}
