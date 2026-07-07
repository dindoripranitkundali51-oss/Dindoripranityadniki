package com.example.dindoripranityadnyiki

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dindoripranityadnyiki.feature.guruji.GurujiWalletScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for GurujiWalletScreen
 * Tests the guruji wallet UI elements and user interactions
 */
@RunWith(AndroidJUnit4::class)
class GurujiWalletScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun gurujiWalletScreen_displaysWalletHeader() {
        composeTestRule.setContent {
            GurujiWalletScreen(
                navController = androidx.navigation.compose.rememberNavController()
            )
        }

        // Verify wallet header is displayed
        composeTestRule.onNodeWithText("Wallet").assertExists()
    }

    @Test
    fun gurujiWalletScreen_displaysBalance() {
        composeTestRule.setContent {
            GurujiWalletScreen(
                navController = androidx.navigation.compose.rememberNavController()
            )
        }

        // Verify balance section is displayed
        composeTestRule.onNodeWithText("Balance").assertExists()
    }

    @Test
    fun gurujiWalletScreen_displaysWithdrawalButton() {
        composeTestRule.setContent {
            GurujiWalletScreen(
                navController = androidx.navigation.compose.rememberNavController()
            )
        }

        // Verify withdrawal request button is displayed
        composeTestRule.onNodeWithText("Request Withdrawal").assertExists()
    }
}
