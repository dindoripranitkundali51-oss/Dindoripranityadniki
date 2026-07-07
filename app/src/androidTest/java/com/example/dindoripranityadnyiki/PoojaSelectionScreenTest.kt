package com.example.dindoripranityadnyiki

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dindoripranityadnyiki.feature.user.PoojaSelectionScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for PoojaSelectionScreen
 * Tests the pooja selection flow UI elements and user interactions
 */
@RunWith(AndroidJUnit4::class)
class PoojaSelectionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun poojaSelectionScreen_displaysSearchBar() {
        composeTestRule.setContent {
            PoojaSelectionScreen(
                navController = androidx.navigation.compose.rememberNavController(),
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            )
        }

        // Verify search bar is displayed
        composeTestRule.onNodeWithContentDescription("Search").assertIsDisplayed()
    }

    @Test
    fun poojaSelectionScreen_displaysPoojaList() {
        composeTestRule.setContent {
            PoojaSelectionScreen(
                navController = androidx.navigation.compose.rememberNavController(),
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            )
        }

        // Verify pooja list section exists
        composeTestRule.onNode(hasScrollAction()).assertExists()
    }
}
