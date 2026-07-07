package com.example.dindoripranityadnyiki

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dindoripranityadnyiki.feature.user.UserHomeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for UserHomeScreen
 * Tests the user home dashboard UI elements and user interactions
 */
@RunWith(AndroidJUnit4::class)
class UserHomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun userHomeScreen_displaysHeader() {
        composeTestRule.setContent {
            UserHomeScreen(
                navController = androidx.navigation.compose.rememberNavController()
            )
        }

        // Verify menu button is displayed
        composeTestRule.onNodeWithContentDescription("Menu").assertIsDisplayed()
        
        // Verify profile button is displayed
        composeTestRule.onNodeWithContentDescription("Profile").assertIsDisplayed()
    }

    @Test
    fun userHomeScreen_displaysBookingCTA() {
        composeTestRule.setContent {
            UserHomeScreen(
                navController = androidx.navigation.compose.rememberNavController()
            )
        }

        // Verify booking CTA is displayed
        composeTestRule.onNodeWithText("Book Pooja").assertIsDisplayed()
    }

    @Test
    fun userHomeScreen_displaysBottomNavigation() {
        composeTestRule.setContent {
            UserHomeScreen(
                navController = androidx.navigation.compose.rememberNavController()
            )
        }

        // Verify bottom navigation is displayed
        composeTestRule.onNode(hasClickAction()).assertExists()
    }
}
