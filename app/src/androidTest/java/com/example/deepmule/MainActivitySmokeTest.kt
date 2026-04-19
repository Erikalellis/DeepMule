package com.example.deepmule

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class MainActivitySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launch_showsInitialLibraryScreen() {
        val appName = composeRule.activity.getString(R.string.app_name)
        val scanDescription = composeRule.activity.getString(R.string.scan_button)

        composeRule.onNodeWithText(appName).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(scanDescription).assertIsDisplayed()
    }
}

