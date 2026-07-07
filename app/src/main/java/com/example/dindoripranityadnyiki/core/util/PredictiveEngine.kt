package com.example.dindoripranityadnyiki.core.util

import com.example.dindoripranityadnyiki.core.data.Constants
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🧠 DIVINE PREDICTIVE ENGINE (AI/Psychology Layer)
 * Analyzes user patterns, Panchang data, and seasonal trends to "read" user needs.
 */
@Singleton
class PredictiveEngine @Inject constructor() {

    fun suggestNextSeva(pastBookings: List<Map<String, Any>>, todayPanchang: Map<String, Any>?): Map<String, String>? {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH)
        
        // 1. Seasonal Psychology (e.g., Shravan, Ganpati, Diwali)
        if (month == Calendar.AUGUST || month == Calendar.SEPTEMBER) {
            return mapOf(
                "title" to "गणेशोत्सव विशेष",
                "message" to "तुमच्या घरी गणपती बाप्पांच्या आगमनासाठी 'प्राणप्रतिष्ठा' पूजेचे नियोजन करा.",
                "action" to "BOOK_NOW",
                "poojaName" to "मूर्ती प्राणप्रतिष्ठा"
            )
        }

        // 2. Behavioral Retargeting (Mind Reading based on frequency)
        val frequencyMap = pastBookings.groupBy { it["poojaName"] as? String ?: "" }.mapValues { it.value.size }
        val mostBooked = frequencyMap.maxByOrNull { it.value }?.key
        
        if (mostBooked != null && (frequencyMap[mostBooked] ?: 0) > 2) {
            return mapOf(
                "title" to "नियमित सेवा",
                "message" to "तुम्ही नेहमी $mostBooked करता, पुढील मुहूर्तावर ही सेवा पुन्हा बुक करायची आहे का?",
                "action" to "REBOOK",
                "poojaName" to mostBooked
            )
        }

        // 3. Panchang Intelligence (Contextual suggestions)
        val tithi = todayPanchang?.get("tithi") as? String ?: ""
        if (tithi.contains("चतुर्थी")) {
            return mapOf(
                "title" to "संकष्टी चतुर्थी विशेष",
                "message" to "आज चतुर्थी निमित्त विशेष बाप्पांची पूजा आयोजित करा.",
                "action" to "BOOK_NOW",
                "poojaName" to "सत्यनारायण"
            )
        }

        return null
    }
}
