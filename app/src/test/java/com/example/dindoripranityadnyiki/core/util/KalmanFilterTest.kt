package com.example.dindoripranityadnyiki.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KalmanFilterTest {

    @Test
    fun `receiveLocation seeds first reading exactly`() {
        val filter = KalmanFilter(processNoise = 0.001)

        val result = filter.receiveLocation(
            newLat = 19.1234,
            newLng = 73.4321,
            accuracy = 8f,
            time = 1_000L
        )

        assertEquals(19.1234, result.latitude, 0.000001)
        assertEquals(73.4321, result.longitude, 0.000001)
    }

    @Test
    fun `receiveLocation smooths later noisy updates`() {
        val filter = KalmanFilter(processNoise = 0.001)

        filter.receiveLocation(
            newLat = 19.0000,
            newLng = 73.0000,
            accuracy = 5f,
            time = 1_000L
        )

        val result = filter.receiveLocation(
            newLat = 19.1000,
            newLng = 73.1000,
            accuracy = 5f,
            time = 2_000L
        )

        assertTrue(result.latitude > 19.0000)
        assertTrue(result.latitude < 19.1000)
        assertTrue(result.longitude > 73.0000)
        assertTrue(result.longitude < 73.1000)
    }
}
