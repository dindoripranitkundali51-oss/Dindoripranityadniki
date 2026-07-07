package com.example.dindoripranityadnyiki

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dindoripranityadnyiki.feature.guruji.GurujiDashboardScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for GurujiDashboardScreen
 * Tests the guruji dashboard UI elements and user interactions
 */
@RunWith(AndroidJUnit4::class)
class GurujiDashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun gurujiDashboardScreen_displaysHeader() {
        composeTestRule.setContent {
            GurujiDashboardScreen(
                navController = androidx.navigation.compose.rememberNavController()
            )
        }

        // Verify menu button is displayed
        composeTestRule.onNodeWithContentDescription("Menu").assertIsDisplayed()
        
        // Verify profile button is displayed
        composeTestRule.onNodeWithContentDescription("Profile").assertIsDisplayed()
    }

    @Test
    fun gurujiDashboardScreen_displaysWalletSection() {
        composeTestRule.setContent {
            GurujiDashboardScreen(
                navController = androidx.navigation.compose.rememberNavController()
            )
        }

        // Verify wallet section is displayed
        composeTestRule.onNodeWithText("Wallet").assertExists()
    }

    @Test
    fun gurujiDashboardScreen_displaysBottomNavigation() {
        composeTestRule.setContent {
            GurujiDashboardScreen(
                navController = androidx.navigation.compose.rememberNavController()
            )
        }

        // Verify bottom navigation is displayed
        composeTestRule.onNode(hasClickAction()).assertExists()
    }
}
