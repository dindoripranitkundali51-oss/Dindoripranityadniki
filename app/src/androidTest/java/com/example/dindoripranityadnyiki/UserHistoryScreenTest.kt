package com.example.dindoripranityadnyiki

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dindoripranityadnyiki.feature.user.UserHistoryScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for UserHistoryScreen
 * Tests the user booking history UI elements and user interactions
 */
@RunWith(AndroidJUnit4::class)
class UserHistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun userHistoryScreen_displaysHeader() {
        composeTestRule.setContent {
            UserHistoryScreen(
                navController = androidx.navigation.compose.rememberNavController()
            )
        }

        // Verify history header is displayed
        composeTestRule.onNodeWithText("History").assertExists()
    }

    @Test
    fun userHistoryScreen_displaysBookingList() {
        composeTestRule.setContent {
            UserHistoryScreen(
                navController = androidx.navigation.compose.rememberNavController()
            )
        }

        // Verify booking list section exists
        composeTestRule.onNode(hasScrollAction()).assertExists()
    }
}
