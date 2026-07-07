package com.example.dindoripranityadnyiki

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dindoripranityadnyiki.feature.guruji.GurujiProfileScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for GurujiProfileScreen
 * Tests the guruji profile UI elements and user interactions
 */
@RunWith(AndroidJUnit4::class)
class GurujiProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun gurujiProfileScreen_displaysProfileHeader() {
        composeTestRule.setContent {
            GurujiProfileScreen(
                navController = androidx.navigation.compose.rememberNavController()
            )
        }

        // Verify profile section is displayed
        composeTestRule.onNodeWithText("Profile").assertExists()
    }

    @Test
    fun gurujiProfileScreen_displaysLogoutButton() {
        composeTestRule.setContent {
            GurujiProfileScreen(
                navController = androidx.navigation.compose.rememberNavController()
            )
        }

        // Verify logout button is displayed
        composeTestRule.onNodeWithText("Logout").assertIsDisplayed()
    }
}
