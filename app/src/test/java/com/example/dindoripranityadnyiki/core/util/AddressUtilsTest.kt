package com.example.dindoripranityadnyiki.core.util

import org.junit.Test
import org.junit.Assert.*

class AddressUtilsTest {

    @Test
    fun `extractPincodeFromText returns valid pincode from text`() {
        val text = "माझा पत्ता: शाहू चौक, नाशिक - 422001"
        val result = extractPincodeFromText(text)
        assertEquals("422001", result)
    }

    @Test
    fun `extractPincodeFromText returns empty for invalid pincode`() {
        val text = "माझा पत्ता: शाहू चौक, नाशिक"
        val result = extractPincodeFromText(text)
        assertEquals("", result)
    }

    @Test
    fun `extractPincodeFromText returns empty for pincode starting with 0`() {
        val text = "माझा पत्ता: शाहू चौक, नाशिक - 022001"
        val result = extractPincodeFromText(text)
        assertEquals("", result)
    }

    @Test
    fun `extractPincodeFromText returns first valid pincode`() {
        val text = "पत्ता 1: 422001, पत्ता 2: 400001"
        val result = extractPincodeFromText(text)
        assertEquals("422001", result)
    }
}
