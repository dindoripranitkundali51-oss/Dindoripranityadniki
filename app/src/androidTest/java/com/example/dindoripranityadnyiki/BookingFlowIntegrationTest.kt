package com.example.dindoripranityadnyiki

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dindoripranityadnyiki.feature.user.PoojaSelectionScreen
import com.example.dindoripranityadnyiki.feature.user.UserHomeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for the complete booking flow.
 * 
 * Tests the end-to-end booking journey:
 * 1. User navigates to home screen
 * 2. Selects a pooja/service
 * 3. Books the pooja
 * 4. Verifies booking confirmation
 */
@RunWith(AndroidJUnit4::class)
class BookingFlowIntegrationTest {

 @get:Rule
 val composeTestRule = createComposeRule()

 @Test
 fun bookingFlow_navigateFromHomeToPoojaSelection() {
 composeTestRule.setContent {
 UserHomeScreen(
 navController = androidx.navigation.compose.rememberNavController()
 )
 }

 // Verify home screen is displayed
 composeTestRule.onNodeWithText("Book Pooja").assertIsDisplayed()
 
 // Click on Book Pooja
 composeTestRule.onNodeWithText("Book Pooja").performClick()
 }

 @Test
 fun poojaSelectionScreen_displaysPoojaList() {
 composeTestRule.setContent {
 PoojaSelectionScreen(
 navController = androidx.navigation.compose.rememberNavController()
 )
 }

 // Verify the screen loads
 composeTestRule.onNodeWithText("Select Pooja").assertIsDisplayed()
 }

 @Test
 fun bookingFlow_completeBookingJourney() {
 composeTestRule.setContent {
 UserHomeScreen(
 navController = androidx.navigation.compose.rememberNavController()
 )
 }

 // Step 1: Click Book Pooja
 composeTestRule.onNodeWithText("Book Pooja").performClick()
 
 // Step 2: Verify navigation to pooja selection
 composeTestRule.onNodeWithText("Select Pooja").assertIsDisplayed()
 
 // Step 3: Select a pooja
 composeTestRule.onNodeWithText("Ganesh Pooja").performClick()
 
 // Step 4: Verify booking details screen
 composeTestRule.onNodeWithText("Booking Details").assertIsDisplayed()
 }

 @Test
 fun bookingFlow_validateRequiredFields() {
 composeTestRule.setContent {
 UserHomeScreen(
 navController = androidx.navigation.compose.rememberNavController()
 )
 }

 // Navigate to booking
 composeTestRule.onNodeWithText("Book Pooja").performClick()
 composeTestRule.onNodeWithText("Select Pooja").assertIsDisplayed()
 
 // Select pooja
 composeTestRule.onNodeWithText("Ganesh Pooja").performClick()
 
 // Attempt to book without filling details
 composeTestRule.onNodeWithText("Book Now").performClick()
 
 // Verify validation error is shown
 composeTestRule.onNodeWithText("Please fill all required fields").assertIsDisplayed()
 }
}
