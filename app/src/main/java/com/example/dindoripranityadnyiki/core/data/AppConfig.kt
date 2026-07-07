package com.example.dindoripranityadnyiki.core.data

import android.content.Context
import com.example.dindoripranityadnyiki.R

/**
 * Runtime configuration that is safe to ship to the client.
 * Secret server credentials must stay in Cloud Functions config, not the app.
 */
object AppConfig {
    fun razorpayKeyId(context: Context): String = context.getString(R.string.runtime_razorpay_key)
    fun googleMapsKey(context: Context): String = context.getString(R.string.runtime_maps_key)
}
