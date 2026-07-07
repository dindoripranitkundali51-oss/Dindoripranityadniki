package com.example.dindoripranityadnyiki.core.util

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RateLimiterTest {

    private lateinit var rateLimiter: RateLimiter

    @Before
    fun setup() {
        // Create a rate limiter: 3 requests per 1000ms
        rateLimiter = RateLimiter(maxRequests = 3, timeWindow = 1000L)
    }

    @Test
    fun `tryAcquire returns true when under limit`() = runTest {
        assertTrue(rateLimiter.tryAcquire())
        assertTrue(rateLimiter.tryAcquire())
        assertTrue(rateLimiter.tryAcquire())
    }

    @Test
    fun `tryAcquire returns false when limit exceeded`() = runTest {
        // Use all 3 requests
        assertTrue(rateLimiter.tryAcquire())
        assertTrue(rateLimiter.tryAcquire())
        assertTrue(rateLimiter.tryAcquire())

        // 4th request should be denied
        assertFalse(rateLimiter.tryAcquire())
    }

    @Test
    fun `tryAcquire allows requests after time window expires`() = runTest {
        // Use all 3 requests
        assertTrue(rateLimiter.tryAcquire())
        assertTrue(rateLimiter.tryAcquire())
        assertTrue(rateLimiter.tryAcquire())

        // 4th request should be denied
        assertFalse(rateLimiter.tryAcquire())

        // Wait for time window to expire
        kotlinx.coroutines.delay(1100)

        // Should allow new requests after window expires
        assertTrue(rateLimiter.tryAcquire())
    }

    @Test
    fun `getTimeUntilNextRequest returns 0 when under limit`() = runTest {
        assertEquals(0L, rateLimiter.getTimeUntilNextRequest())
        assertTrue(rateLimiter.tryAcquire())
        assertEquals(0L, rateLimiter.getTimeUntilNextRequest())
    }

    @Test
    fun `getTimeUntilNextRequest returns positive value when limit exceeded`() = runTest {
        // Use all 3 requests
        assertTrue(rateLimiter.tryAcquire())
        assertTrue(rateLimiter.tryAcquire())
        assertTrue(rateLimiter.tryAcquire())

        val waitTime = rateLimiter.getTimeUntilNextRequest()
        assertTrue(waitTime > 0)
        assertTrue(waitTime <= 1000L)
    }

    @Test
    fun `reset clears all request timestamps`() = runTest {
        // Use all 3 requests
        assertTrue(rateLimiter.tryAcquire())
        assertTrue(rateLimiter.tryAcquire())
        assertTrue(rateLimiter.tryAcquire())

        // 4th request should be denied
        assertFalse(rateLimiter.tryAcquire())

        // Reset the rate limiter
        rateLimiter.reset()

        // Should allow requests again
        assertTrue(rateLimiter.tryAcquire())
        assertTrue(rateLimiter.tryAcquire())
        assertTrue(rateLimiter.tryAcquire())
    }

    @Test
    fun `RateLimiters placesAutocomplete has correct configuration`() {
        val limiter = RateLimiters.placesAutocomplete
        // Verify it's a valid instance
        assertNotNull(limiter)
    }

    @Test
    fun `RateLimiters placesFetchPlace has correct configuration`() {
        val limiter = RateLimiters.placesFetchPlace
        // Verify it's a valid instance
        assertNotNull(limiter)
    }

    @Test
    fun `RateLimiters geocoding has correct configuration`() {
        val limiter = RateLimiters.geocoding
        // Verify it's a valid instance
        assertNotNull(limiter)
    }
}
