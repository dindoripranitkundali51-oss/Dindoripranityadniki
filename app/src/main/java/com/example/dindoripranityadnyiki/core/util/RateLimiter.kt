package com.example.dindoripranityadnyiki.core.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

/**
 * Rate limiter to prevent API quota exhaustion.
 * 
 * Uses token bucket algorithm to limit the rate of API calls.
 * 
 * @param maxRequests Maximum number of requests allowed in the time window
 * @param timeWindow Time window in milliseconds
 */
class RateLimiter(
    private val maxRequests: Int,
    private val timeWindow: Long
) {
    private val mutex = Mutex()
    private val requestTimestamps = mutableListOf<Long>()

    /**
     * Checks if a request is allowed and records the attempt.
     * 
     * @return true if request is allowed, false if rate limit exceeded
     */
    suspend fun tryAcquire(): Boolean {
        val now = System.currentTimeMillis()
        
        return mutex.withLock {
            // Remove timestamps outside the time window
            requestTimestamps.removeIf { timestamp ->
                now - timestamp > timeWindow
            }

            // Check if we can make a request
            if (requestTimestamps.size < maxRequests) {
                requestTimestamps.add(now)
                true
            } else {
                false
            }
        }
    }

    /**
     * Gets the time until the next request is allowed.
     * 
     * @return Time in milliseconds until next request, or 0 if request is allowed now
     */
    suspend fun getTimeUntilNextRequest(): Long {
        val now = System.currentTimeMillis()
        
        return mutex.withLock {
            if (requestTimestamps.size < maxRequests) {
                0
            } else {
                val oldestTimestamp = requestTimestamps.firstOrNull() ?: 0
                val timeUntilOldestExpires = (oldestTimestamp + timeWindow) - now
                maxOf(0, timeUntilOldestExpires)
            }
        }
    }

    /**
     * Resets the rate limiter (for testing or manual reset).
     */
    suspend fun reset() {
        mutex.withLock {
            requestTimestamps.clear()
        }
    }
}

/**
 * Pre-configured rate limiters for different API endpoints.
 */
object RateLimiters {
    /**
     * Google Places Autocomplete API rate limiter.
     * Limits to 10 requests per second (Google's free tier limit).
     */
    val placesAutocomplete = RateLimiter(
        maxRequests = 10,
        timeWindow = TimeUnit.SECONDS.toMillis(1)
    )

    /**
     * Google Places Fetch Place API rate limiter.
     * Limits to 50 requests per second.
     */
    val placesFetchPlace = RateLimiter(
        maxRequests = 50,
        timeWindow = TimeUnit.SECONDS.toMillis(1)
    )

    /**
     * Geocoding API rate limiter.
     * Limits to 50 requests per second.
     */
    val geocoding = RateLimiter(
        maxRequests = 50,
        timeWindow = TimeUnit.SECONDS.toMillis(1)
    )
}
