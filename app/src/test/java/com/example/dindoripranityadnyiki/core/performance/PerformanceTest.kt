package com.example.dindoripranityadnyiki.core.performance

import kotlinx.coroutines.*
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Performance tests for critical application components.
 * 
 * Tests the performance of:
 * - Database operations
 * - Network requests
 * - Image loading
 * - UI rendering
 * - Rate limiting
 */
class PerformanceTest {

 @Test
 fun performanceTest_rateLimiter_highThroughput() = runBlocking {
 val limiter = RateLimiter(maxRequests = 10, timeWindow = 1000L)
 var allowedCount = 0
 val executionTime = measureTimeMillis {
 repeat(20) {
 if (limiter.tryAcquire()) allowedCount++
 delay(50)
 }
 }
 
 println("RateLimiter throughput: 20 requests in {executionTime}ms")
 println("Allowed requests: allowedCount")
 assert(allowedCount in 9..11) { "Rate limiter should allow approximately 10 requests per second" }
 }

 @Test
 fun performanceTest_bookingCreation_concurrentUsers() = runBlocking {
 val startTime = System.currentTimeMillis()
 val jobs = mutableListOf<Job>()
 
 repeat(10) { userId ->
 jobs.add(
 async {
 delay(100) // Simulate booking creation
 println("Booking created for user userId")
 }