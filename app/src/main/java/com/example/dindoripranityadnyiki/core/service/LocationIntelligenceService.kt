package com.example.dindoripranityadnyiki.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

import com.google.android.gms.location.*
import com.example.dindoripranityadnyiki.R
import com.example.dindoripranityadnyiki.core.util.KalmanFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.example.dindoripranityadnyiki.core.network.ApiService
import com.example.dindoripranityadnyiki.core.network.UpdateLocationRequest
import com.example.dindoripranityadnyiki.core.network.UpdateStatusRequest
import com.example.dindoripranityadnyiki.core.network.BookingDto
import com.example.dindoripranityadnyiki.core.data.DataStoreManager
import com.example.dindoripranityadnyiki.core.data.PrefKeys

@AndroidEntryPoint
class LocationIntelligenceService : Service() {

    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentBookingId: String? = null
    private var targetLat: Double = 0.0
    private var targetLng: Double = 0.0
    private var gurujiHomeLat: Double = 0.0
    private var gurujiHomeLng: Double = 0.0
    private var hasMarkedDeparted = false
    private var hasMarkedArrived = false
    private var isBookingDateNearby = false
    private var locationUpdatesStarted = false
    private var bookingPollJob: kotlinx.coroutines.Job? = null
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val kalmanFilter = KalmanFilter()
    
    private var lastUploadedLocation: Location? = null
    private var lastUpdateTime: Long = 0
    private val UPLOAD_INTERVAL_MS = 30000L
    private val IDLE_INTERVAL_MS = 120000L
    private val MIN_DISPLACEMENT = 20f

    companion object {
        const val CHANNEL_ID = "SevaLocationChannel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentBookingId = intent?.getStringExtra("bookingId")?.takeIf { it.isNotBlank() }
        targetLat = intent?.getDoubleExtra("lat", 0.0) ?: 0.0
        targetLng = intent?.getDoubleExtra("lng", 0.0) ?: 0.0

        startForeground(NOTIFICATION_ID, createNotification("सेवा लोकेशन अपडेट सुरू आहे..."))
        
        serviceScope.launch {
            try {
                val mobile = dataStoreManager.readString(PrefKeys.USER_MOBILE).first()
                if (mobile.isBlank()) {
                    stopSelf(startId)
                    return@launch
                }

                gurujiHomeLat = dataStoreManager.readString(PrefKeys.USER_LAT).first().toDoubleOrNull() ?: 0.0
                gurujiHomeLng = dataStoreManager.readString(PrefKeys.USER_LNG).first().toDoubleOrNull() ?: 0.0

                val bookingDoc = resolveTrackableBooking(mobile, currentBookingId)
                if (bookingDoc == null || !configureActiveBooking(bookingDoc)) {
                    stopSelf(startId)
                    return@launch
                }
                listenToCurrentBooking()
                startLocationUpdates()
            } catch (e: Exception) {
                stopSelf(startId)
            }
        }
        
        return START_STICKY
    }

    private suspend fun resolveTrackableBooking(gurujiMobile: String, requestedBookingId: String?): BookingDto? {
        val token = dataStoreManager.readString(PrefKeys.JWT_TOKEN).first()
        if (token.isBlank()) return null

        if (!requestedBookingId.isNullOrBlank()) {
            val response = apiService.getBookingDetails("Bearer $token", requestedBookingId)
            if (response.isSuccessful && response.body()?.success == true) {
                val booking = response.body()?.data
                if (booking != null && (booking.gurujiId == gurujiMobile || booking.contactPhone == gurujiMobile)) return booking
            }
            return null
        }

        val response = apiService.getExpertBookings("Bearer $token")
        if (response.isSuccessful && response.body()?.success == true) {
            return response.body()?.data.orEmpty()
                .filter { isTrackableStatus(it.status) }
                .filter { parseBookingDate(it.date)?.let(::isDateNearby) == true }
                .sortedBy { it.date.orEmpty() }
                .firstOrNull()
        }

        return null
    }

    private fun configureActiveBooking(bookingDoc: BookingDto): Boolean {
        val status = bookingDoc.status
        val bookingDate = parseBookingDate(bookingDoc.date)
        if (!isTrackableStatus(status) || bookingDate?.let(::isDateNearby) != true) return false

        currentBookingId = bookingDoc.id
        isBookingDateNearby = true
        if (targetLat == 0.0 || targetLng == 0.0) {
            targetLat = bookingDoc.userLat ?: bookingDoc.lat ?: 0.0
            targetLng = bookingDoc.userLng ?: bookingDoc.lng ?: 0.0
        }
        hasMarkedArrived = bookingDoc.hasMarkedArrived ?: false
        return true
    }

    private fun listenToCurrentBooking() {
        bookingPollJob?.cancel()
        val bookingId = currentBookingId ?: return
        bookingPollJob = serviceScope.launch {
            while (isActive) {
                delay(20000)
                try {
                    val token = dataStoreManager.readString(PrefKeys.JWT_TOKEN).first()
                    if (token.isNotBlank()) {
                        val response = apiService.getBookingDetails("Bearer $token", bookingId)
                        if (response.isSuccessful && response.body()?.success == true) {
                            val booking = response.body()?.data
                            if (booking == null || !isTrackableStatus(booking.status)) {
                                stopSelf()
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LocationService", "Polling active booking failed: ${e.message}")
                }
            }
        }
    }

    private fun isTrackableStatus(status: String): Boolean =
        status in setOf("Assigned", "Accepted", "InProgress")

    private fun startLocationUpdates() {
        if (locationUpdatesStarted) return
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .setWaitForAccurateLocation(true)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
            locationUpdatesStarted = true
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return
            
            val smoothedLocation = kalmanFilter.receiveLocation(location.latitude, location.longitude, location.accuracy.toFloat(), System.currentTimeMillis())
            val currentTime = System.currentTimeMillis()
            val distanceMoved = lastUploadedLocation?.distanceTo(location) ?: Float.MAX_VALUE
            val isMoving = location.speed > 0.5f

            val shouldUpdate = (distanceMoved >= MIN_DISPLACEMENT && (currentTime - lastUpdateTime) >= UPLOAD_INTERVAL_MS) ||
                               ((currentTime - lastUpdateTime) >= IDLE_INTERVAL_MS)

            if (shouldUpdate) {
                lastUploadedLocation = location
                lastUpdateTime = currentTime
                
                val uploadLocation = Location(location).apply {
                    latitude = smoothedLocation.latitude
                    longitude = smoothedLocation.longitude
                }
                
                updateRestApi(uploadLocation)
                calculateETA(uploadLocation)
                
                if (isBookingDateNearby && gurujiHomeLat != 0.0 && gurujiHomeLng != 0.0 && !hasMarkedDeparted && currentBookingId != null) {
                    val distFromHome = haversineDistance(
                        uploadLocation.latitude, uploadLocation.longitude,
                        gurujiHomeLat, gurujiHomeLng
                    ) * 1000
                    if (distFromHome > 150) {
                        hasMarkedDeparted = true
                        currentBookingId?.let { markBookingInProgress(it) }
                    }
                }
                
                val distRemaining = haversineDistance(uploadLocation.latitude, uploadLocation.longitude, targetLat, targetLng) * 1000
                val statusText = if (isMoving) "मार्गक्रमण सुरू..." else "गुरुजी थांबलेले आहेत"
                updateNotification("$statusText (अंतर: ${formatDistance(distRemaining)})")
            }
        }
    }

    private fun formatDistance(dist: Double): String {
        return if (dist > 1000) "${String.format("%.1f", dist / 1000)} km" else "${dist.toInt()} m"
    }

    private fun parseBookingDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        val trimmed = value.trim()
        return runCatching { LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
            ?: runCatching { LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrNull()
    }

    private fun isDateNearby(bookingDate: LocalDate): Boolean {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        return bookingDate == today || bookingDate == tomorrow
    }

    private fun updateRestApi(loc: Location) {
        serviceScope.launch {
            try {
                val token = dataStoreManager.readString(PrefKeys.JWT_TOKEN).first()
                if (token.isNotBlank()) {
                    apiService.updateLocation(
                        token = "Bearer $token",
                        request = UpdateLocationRequest(loc.latitude, loc.longitude)
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("LocationService", "Error updating location: ${e.message}")
            }
        }
    }

    private fun markBookingInProgress(bookingId: String) {
        serviceScope.launch {
            try {
                val token = dataStoreManager.readString(PrefKeys.JWT_TOKEN).first()
                if (token.isNotBlank()) {
                    apiService.updateBookingStatus(
                        token = "Bearer $token",
                        bookingId = bookingId,
                        request = UpdateStatusRequest("InProgress")
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("LocationService", "Error updating status: ${e.message}")
            }
        }
    }

    private fun calculateETA(currentLoc: Location) {
        if (targetLat == 0.0 || targetLng == 0.0) return
        
        serviceScope.launch {
            try {
                val etaFromDirections = withContext(Dispatchers.IO) {
                    getGoogleDirectionsETA(currentLoc.latitude, currentLoc.longitude, targetLat, targetLng)
                }
                if (etaFromDirections != null) {
                    val distKm = haversineDistance(currentLoc.latitude, currentLoc.longitude, targetLat, targetLng)
                    if (distKm < 0.15 && !hasMarkedArrived) {
                        hasMarkedArrived = true
                    }
                    return@launch
                }
            } catch (e: Exception) {
                // Fallback to local haversine calculation
            }
            
            val distKm = haversineDistance(currentLoc.latitude, currentLoc.longitude, targetLat, targetLng)
            val minutes = ((distKm * 1.3) / 25.0 * 60).toInt() + 2
            val etaText = when {
                distKm < 0.15 -> "पोहोचले आहेत / Arrived"
                minutes < 2 -> "1-2 मिनिटे"
                else -> "$minutes मिनिटे"
            }
            if (distKm < 0.15 && !hasMarkedArrived) {
                hasMarkedArrived = true
            }
        }
    }
    
    private suspend fun getGoogleDirectionsETA(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): String? {
        return try {
            val url = "https://maps.googleapis.com/maps/api/directions/json"
            val apiKey = getString(R.string.runtime_maps_key)
            if (apiKey.isBlank() || apiKey == "YOUR_MAPS_KEY") return null

            val connection = URL("$url?origin=$fromLat,$fromLng&destination=$toLat,$toLng&key=$apiKey")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"
            val response = try {
                if (connection.responseCode !in 200..299) return null
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
            val json = org.json.JSONObject(response)
            val route = json.getJSONArray("routes").optJSONObject(0) ?: return null
            val leg = route.getJSONArray("legs").optJSONObject(0) ?: return null
            val duration = leg.getJSONObject("duration")
            val durationText = duration.getString("text")
            val durationSeconds = duration.getInt("value")

            when {
                durationSeconds < 120 -> "1-2 मिनिटे"
                durationSeconds < 300 -> "5 मिनिटे"
                durationSeconds < 600 -> "10 मिनिटे"
                durationSeconds < 1800 -> durationText
                else -> "${durationSeconds / 60} मिनिटे"
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("यज्ञिकी सेवा ट्रॅकिंग")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(content))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Location Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        locationUpdatesStarted = false
        bookingPollJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
