package com.example.dindoripranityadnyiki

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dindoripranityadnyiki.feature.user.UserLoginScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for UserLoginScreen
 * Tests the login flow UI elements and user interactions
 */
@RunWith(AndroidJUnit4::class)
class UserLoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_displaysAllElements() {
        composeTestRule.setContent {
            UserLoginScreen(
                navController = androidx.navigation.compose.rememberNavController(),
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            )
        }

        // Verify mobile input field is displayed
        composeTestRule.onNodeWithText("Mobile Number").assertIsDisplayed()
        
        // Verify password input field is displayed
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        
        // Verify login button is displayed
        composeTestRule.onNodeWithText("Login").assertIsDisplayed()
    }

    @Test
    fun loginScreen_displaysForgotPasswordLink() {
        composeTestRule.setContent {
            UserLoginScreen(
                navController = androidx.navigation.compose.rememberNavController(),
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            )
        }

        // Verify forgot password link is displayed
        composeTestRule.onNodeWithText("Forgot Password?").assertIsDisplayed()
    }

    @Test
    fun loginScreen_displaysRegistrationLink() {
        composeTestRule.setContent {
            UserLoginScreen(
                navController = androidx.navigation.compose.rememberNavController(),
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            )
        }

        // Verify registration link is displayed
        composeTestRule.onNodeWithText("Register as User").assertIsDisplayed()
    }
}
