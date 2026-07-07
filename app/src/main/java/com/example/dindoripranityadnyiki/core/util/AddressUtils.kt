@file:Suppress("DEPRECATION")

package com.example.dindoripranityadnyiki.core.util

import com.google.android.libraries.places.api.model.Place

private val INDIAN_PINCODE_REGEX = Regex("\\b[1-9][0-9]{5}\\b")

fun extractPincodeFromText(text: String?): String {
    return INDIAN_PINCODE_REGEX.find(text.orEmpty())?.value.orEmpty()
}

fun extractPincodeFromPlace(place: Place): String {
    place.addressComponents?.asList()?.forEach { component ->
        if (component.types.contains("postal_code")) {
            return component.name.filter { it.isDigit() }.take(6)
        }
    }
    return extractPincodeFromText(place.address ?: place.name)
}

fun extractDistrictFromPlace(place: Place): String {
    place.addressComponents?.asList()?.forEach { component ->
        if (component.types.contains("administrative_area_level_2")) {
            return component.name
        }
    }
    return ""
}
