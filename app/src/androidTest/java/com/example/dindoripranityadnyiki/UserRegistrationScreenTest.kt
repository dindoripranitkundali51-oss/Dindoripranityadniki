package com.example.dindoripranityadnyiki

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dindoripranityadnyiki.feature.user.UserRegistrationScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for UserRegistrationScreen
 * Tests the registration flow UI elements and user interactions
 */
@RunWith(AndroidJUnit4::class)
class UserRegistrationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun registrationScreen_displaysAllFields() {
        composeTestRule.setContent {
            UserRegistrationScreen(
                navController = androidx.navigation.compose.rememberNavController(),
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            )
        }

        // Verify full name field is displayed
        composeTestRule.onNodeWithText("Full Name").assertIsDisplayed()
        
        // Verify mobile field is displayed
        composeTestRule.onNodeWithText("Mobile Number").assertIsDisplayed()
        
        // Verify email field is displayed
        composeTestRule.onNodeWithText("Email (Optional)").assertIsDisplayed()
        
        // Verify address field is displayed
        composeTestRule.onNodeWithText("Full Address").assertIsDisplayed()
        
        // Verify district field is displayed
        composeTestRule.onNodeWithText("District").assertIsDisplayed()
        
        // Verify pincode field is displayed
        composeTestRule.onNodeWithText("Pincode").assertIsDisplayed()
        
        // Verify password field is displayed
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        
        // Verify confirm password field is displayed
        composeTestRule.onNodeWithText("Confirm Password").assertIsDisplayed()
    }

    @Test
    fun registrationScreen_displaysRegisterButton() {
        composeTestRule.setContent {
            UserRegistrationScreen(
                navController = androidx.navigation.compose.rememberNavController(),
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            )
        }

        // Verify register button is displayed
        composeTestRule.onNodeWithText("Register").assertIsDisplayed()
    }

    @Test
    fun registrationScreen_displaysLoginLink() {
        composeTestRule.setContent {
            UserRegistrationScreen(
                navController = androidx.navigation.compose.rememberNavController(),
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            )
        }

        // Verify login link is displayed
        composeTestRule.onNodeWithText("Already have an account? Login").assertIsDisplayed()
    }
}
