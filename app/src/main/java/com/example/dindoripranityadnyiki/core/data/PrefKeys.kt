package com.example.dindoripranityadnyiki.core.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * ✅ PrefKeys.kt
 * इथे सगळे DataStore keys एकाच ठिकाणी define केले जातील.
 * यामुळे naming consistent राहते आणि debugging सोपं होतं.
 */

object PrefKeys {

    // 🧠 App flow related flags
    val IS_FIRST_TIME =
        booleanPreferencesKey("is_first_time_launch")           // App पहिल्यांदा चालतंय का?
    val IS_ONBOARDING_DONE =
        booleanPreferencesKey("is_onboarding_completed")   // Onboarding पूर्ण झाली का?
    val IS_REGISTERED =
        booleanPreferencesKey("is_registration_done")           // Registration पूर्ण झाली का?
    val IS_LOGGED_IN =
        booleanPreferencesKey("is_logged_in")                    // User logged in आहे का?

    val IS_FIRST_BOOKING_DONE = booleanPreferencesKey("is_first_booking_done")

    // 👥 Role (User / Guruji)
    val USER_ROLE = stringPreferencesKey("user_role")

    // 📍 User location / address info
    val DISTRICT = stringPreferencesKey("district")
    val USER_DISTRICT = stringPreferencesKey("user_district")// Registration वेळी घेतलेला जिल्हा
    val USER_ADDRESS =
        stringPreferencesKey("user_address")                     // Optional: user चा पत्ता
    val USER_PINCODE =
        stringPreferencesKey("user_pincode")                     // Optional: user चा पिनकोड

    // 🌐 App preferences / settings
    val LANGUAGE = stringPreferencesKey("app_language")                         // Marathi / English
    val THEME_MODE = stringPreferencesKey("theme_mode")                         // Light / Dark
    val NOTIFICATION_ENABLED =
        booleanPreferencesKey("notifications_enabled")   // Push notifications on/off
}