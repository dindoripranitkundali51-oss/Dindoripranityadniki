package com.example.dindoripranityadnyiki.core.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PrefKeys {
    val IS_LOGGED_IN = booleanPreferencesKey("is_authenticated")
    val LANGUAGE = stringPreferencesKey("app_locale")
    val USER_TYPE = stringPreferencesKey("user_type")
    val USER_ROLE = stringPreferencesKey("identity_role")
    val USER_NAME = stringPreferencesKey("user_name")
    val USER_MOBILE = stringPreferencesKey("user_mobile")
    val USER_EMAIL = stringPreferencesKey("user_email")
    
    val IS_REGISTERED = booleanPreferencesKey("is_registered")
    val IS_ONBOARDING_DONE = booleanPreferencesKey("onboarding_status")
    val IS_FIRST_TIME = booleanPreferencesKey("is_first_time")
    val IS_FIRST_BOOKING_DONE = booleanPreferencesKey("is_first_booking_done")
    
    val USER_ADDRESS = stringPreferencesKey("user_address")
    val USER_PINCODE = stringPreferencesKey("user_pincode")
    val USER_DISTRICT = stringPreferencesKey("user_district")
    val DISTRICT = stringPreferencesKey("district")

    val USER_LAT = stringPreferencesKey("user_lat")
    val USER_LNG = stringPreferencesKey("user_lng")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val NOTIFICATION_ENABLED = booleanPreferencesKey("notifications_enabled")
    val JWT_TOKEN = stringPreferencesKey("jwt_auth_token")
}
