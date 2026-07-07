package com.example.dindoripranityadnyiki

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dindoripranityadnyiki.feature.guruji.GurujiBookingDetailsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for GurujiBookingDetailsScreen
 * Tests the guruji booking details UI elements and user interactions
 */
@RunWith(AndroidJUnit4::class)
class GurujiBookingDetailsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun gurujiBookingDetailsScreen_displaysBookingInfo() {
        composeTestRule.setContent {
            GurujiBookingDetailsScreen(
                navController = androidx.navigation.compose.rememberNavController(),
                bookingId = "test_booking_id"
            )
        }

        // Verify booking details section is displayed
        composeTestRule.onNode(hasScrollAction()).assertExists()
    }

    @Test
    fun gurujiBookingDetailsScreen_displaysActionButtons() {
        composeTestRule.setContent {
            GurujiBookingDetailsScreen(
                navController = androidx.navigation.compose.rememberNavController(),
                bookingId = "test_booking_id"
            )
        }

        // Verify action buttons exist
        composeTestRule.onNode(hasClickAction()).assertExists()
    }
}
