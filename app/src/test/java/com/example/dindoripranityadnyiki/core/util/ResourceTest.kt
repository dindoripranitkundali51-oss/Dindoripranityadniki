package com.example.dindoripranityadnyiki.core.util

import org.junit.Assert.*
import org.junit.Test

class ResourceTest {

    @Test
    fun `Resource Loading state works correctly`() {
        val loading = Resource.Loading<String>()
        assertNull(loading.data)
        assertNull(loading.message)
    }

    @Test
    fun `Resource Success state holds data correctly`() {
        val testData = "Test Data"
        val success = Resource.Success(testData)
        assertEquals(testData, success.data)
        assertNull(success.message)
    }

    @Test
    fun `Resource Error state holds message correctly`() {
        val testError = "Test Error Message"
        val error = Resource.Error<String>(testError)
        assertNull(error.data)
        assertEquals(testError, error.message)
    }
}
