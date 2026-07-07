package com.example.dindoripranityadnyiki

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dindoripranityadnyiki.feature.user.BookingDetailsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for BookingDetailsScreen
 * Tests the booking details form UI elements and user interactions
 */
@RunWith(AndroidJUnit4::class)
class BookingDetailsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bookingDetailsScreen_displaysDevoteeFields() {
        composeTestRule.setContent {
            BookingDetailsScreen(
                navController = androidx.navigation.compose.rememberNavController(),
                poojaId = "test_pooja_id",
                poojaName = "Satyanarayan Pooja"
            )
        }

        // Verify devotee name field is displayed
        composeTestRule.onNodeWithText("Devotee Name").assertIsDisplayed()
        
        // Verify mobile field is displayed
        composeTestRule.onNodeWithText("Mobile Number").assertIsDisplayed()
    }

    @Test
    fun bookingDetailsScreen_displaysLocationFields() {
        composeTestRule.setContent {
            BookingDetailsScreen(
                navController = androidx.navigation.compose.rememberNavController(),
                poojaId = "test_pooja_id",
                poojaName = "Satyanarayan Pooja"
            )
        }

        // Verify address field is displayed
        composeTestRule.onNodeWithText("Full Address").assertIsDisplayed()
        
        // Verify district field is displayed
        composeTestRule.onNodeWithText("District").assertIsDisplayed()
        
        // Verify pincode field is displayed
        composeTestRule.onNodeWithText("Pincode").assertIsDisplayed()
    }

    @Test
    fun bookingDetailsScreen_displaysDateField() {
        composeTestRule.setContent {
            BookingDetailsScreen(
                navController = androidx.navigation.compose.rememberNavController(),
                poojaId = "test_pooja_id",
                poojaName = "Satyanarayan Pooja"
            )
        }

        // Verify date field is displayed
        composeTestRule.onNodeWithText("Select Date").assertExists()
    }

    @Test
    fun bookingDetailsScreen_displaysConfirmButton() {
        composeTestRule.setContent {
            BookingDetailsScreen(
                navController = androidx.navigation.compose.rememberNavController(),
                poojaId = "test_pooja_id",
                poojaName = "Satyanarayan Pooja"
            )
        }

        // Verify confirm button is displayed
        composeTestRule.onNodeWithText("Confirm Booking").assertIsDisplayed()
    }
}
