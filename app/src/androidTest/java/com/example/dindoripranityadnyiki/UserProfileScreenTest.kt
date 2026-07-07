package com.example.dindoripranityadnyiki

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dindoripranityadnyiki.feature.user.UserProfileScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for UserProfileScreen
 * Tests the user profile UI elements and user interactions
 */
@RunWith(AndroidJUnit4::class)
class UserProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun userProfileScreen_displaysProfileHeader() {
        composeTestRule.setContent {
            UserProfileScreen(
                navController = androidx.navigation.compose.rememberNavController()
            )
        }

        // Verify profile section is displayed
        composeTestRule.onNodeWithText("Profile").assertExists()
    }

    @Test
    fun userProfileScreen_displaysLogoutButton() {
        composeTestRule.setContent {
            UserProfileScreen(
                navController = androidx.navigation.compose.rememberNavController()
            )
        }

        // Verify logout button is displayed
        composeTestRule.onNodeWithText("Logout").assertIsDisplayed()
    }
}
