package com.example.dindoripranityadnyiki.core.service

import android.content.Context
import android.content.Intent

object ServiceHelper {
    fun startLocationService(
        context: Context,
        bookingId: String,
        userLat: Double,
        userLng: Double
    ) {
        val serviceIntent = Intent(context, LocationIntelligenceService::class.java).apply {
            putExtra("bookingId", bookingId)
            putExtra("lat", userLat)
            putExtra("lng", userLng)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
