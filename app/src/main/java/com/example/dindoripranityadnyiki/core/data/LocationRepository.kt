package com.example.dindoripranityadnyiki.core.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

data class GeoResult(
    val lat: Double,
    val lng: Double,
    val displayName: String,
    val confidence: Double // 0.0 to 1.0
)

/**
 * 🛰️ AUTONOMOUS PRECISION LOCATION ENGINE (v5)
 */
@Singleton
class LocationRepository @Inject constructor() {
    private val USER_AGENT = "DindoriPranit_AutoPrecision_AI"

    suspend fun getAutomaticLocation(address: String, district: String, pincode: String): GeoResult? {
        return withContext(Dispatchers.IO) {
            val cleanAddr = address.lowercase()
                .replace(Regex("(near|behind|opposite|opp|at post|taluka|dist|district|flat|no|room|house)\\s*\\d*"), "")
                .trim()

            val strategies = listOf(
                "$cleanAddr, $district, $pincode, Maharashtra, India",
                "$cleanAddr, $pincode, India",
                "$cleanAddr, $district, Maharashtra",
                "$pincode, India"
            )

            for (query in strategies) {
                val results = executeSearch(query)
                val bestMatch = results.filter { 
                    it.displayName.contains("Maharashtra", ignoreCase = true) &&
                    (it.displayName.contains(district, ignoreCase = true) || it.displayName.contains(pincode))
                }.maxByOrNull { calculateScore(it.displayName, address, district) }

                if (bestMatch != null && bestMatch.confidence > 0.7) {
                    return@withContext bestMatch
                }
            }
            null
        }
    }

    suspend fun reverseGeocode(lat: Double, lng: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lng&addressdetails=1")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.connectTimeout = 3000
                conn.readTimeout = 3000

                val response = try {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    conn.disconnect()
                }
                val json = JSONObject(response)
                json.optString("display_name")
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun calculateScore(foundName: String, inputAddr: String, district: String): Double {
        var score = 0.0
        val foundLower = foundName.lowercase()
        if (foundLower.contains(district.lowercase())) score += 0.4
        if (foundLower.contains("maharashtra")) score += 0.2
        
        val inputWords = inputAddr.lowercase().split(" ")
        var matchedWords = 0
        for (word in inputWords) {
            if (word.length > 3 && foundLower.contains(word)) matchedWords++
        }
        score += (matchedWords.toDouble() / inputWords.size) * 0.4
        return score
    }

    private fun executeSearch(query: String): List<GeoResult> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=3&addressdetails=1&countrycodes=in")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            
            val response = try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
            val jsonArray = JSONArray(response)
            val results = mutableListOf<GeoResult>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                results.add(GeoResult(
                    lat = obj.getDouble("lat"),
                    lng = obj.getDouble("lon"),
                    displayName = obj.getString("display_name"),
                    confidence = 0.5 // Initial
                ))
            }
            results
        } catch (e: Exception) { emptyList() }
    }
}
