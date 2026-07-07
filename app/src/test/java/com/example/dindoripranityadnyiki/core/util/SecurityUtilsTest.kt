package com.example.dindoripranityadnyiki.core.util

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for SecurityUtils.
 * 
 * Note: Full encryption/decryption tests require Android Keystore access
 * and should be run as instrumented tests. These tests verify basic functionality
 * that can be tested without Android context.
 */
class SecurityUtilsTest {

    @Test
    fun `isKeyAvailable returns false in non-Android environment`() {
        // In pure JVM test environment, Android Keystore is not available
        assertFalse("Android Keystore should not be available in test environment", 
            SecurityUtils.isKeyAvailable())
    }

    @Test
    @Suppress("DEPRECATION")
    fun `sanitize trims whitespace`() {
        val input = "  hello world  "
        val result = SecurityUtils.sanitize(input)
        assertEquals("hello world", result)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `sanitize handles empty string`() {
        val input = ""
        val result = SecurityUtils.sanitize(input)
        assertEquals("", result)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `sanitize handles only whitespace`() {
        val input = "   "
        val result = SecurityUtils.sanitize(input)
        assertEquals("", result)
    }

    @Test(expected = Exception::class)
    fun `encrypt throws exception when key not available`() {
        // In test environment without Android Keystore, encrypt should throw
        SecurityUtils.encrypt("test data")
    }

    @Test(expected = Exception::class)
    fun `decrypt throws exception when key not available`() {
        // In test environment without Android Keystore, decrypt should throw
        SecurityUtils.decrypt("encrypted_data")
    }
}
