package com.example.dindoripranityadnyiki.core.util

import com.google.android.gms.maps.model.LatLng

/**
 * 🛰️ KALMAN FILTER FOR SMOOTH LOCATION TRACKING
 * Removes GPS noise and predicts the next position for a "cinematic" movement effect.
 */
class KalmanFilter(private val processNoise: Double = 0.00001) {
    private var variance: Double = -1.0
    private var lat: Double = 0.0
    private var lng: Double = 0.0
    private var timestamp: Long = 0

    /**
     * Receives raw GPS data and returns a filtered, smooth LatLng.
     */
    fun receiveLocation(newLat: Double, newLng: Double, accuracy: Float, time: Long): LatLng {
        if (variance < 0) {
            // Initial state
            lat = newLat
            lng = newLng
            variance = (accuracy * accuracy).toDouble()
            timestamp = time
        } else {
            val duration = time - timestamp
            if (duration > 0) {
                // Prediction step: Increase variance based on time passed
                variance += duration * processNoise * processNoise / 1000
                timestamp = time
            }

            // Measurement update step (Kalman Gain)
            val k = variance / (variance + accuracy * accuracy)
            lat += k * (newLat - lat)
            lng += k * (newLng - lng)
            variance *= (1 - k)
        }
        return LatLng(lat, lng)
    }

    fun reset() {
        variance = -1.0
    }
}
