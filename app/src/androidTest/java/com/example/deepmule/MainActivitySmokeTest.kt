package com.example.deepmule

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.isRoot
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

class MainActivitySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launch_showsInitialLibraryScreen() {
        val appName = composeRule.activity.getString(R.string.app_name)
        val scanDescription = composeRule.activity.getString(R.string.scan_button)
        val onboardingTitle = composeRule.activity.getString(R.string.onboarding_title)
        val onboardingSkip = composeRule.activity.getString(R.string.onboarding_skip)

        withDiagnostics("main-activity-startup") {
            relaunchMainActivity()

            composeRule.waitUntil(timeoutMillis = 15_000) {
                nodeWithTextExists(onboardingTitle) ||
                    (nodeWithTextExists(appName) && nodeWithContentDescriptionExists(scanDescription))
            }

            if (nodeWithTextExists(onboardingTitle)) {
                composeRule.onNodeWithText(onboardingSkip).performClick()
                composeRule.waitForIdle()
            }

            composeRule.waitUntil(timeoutMillis = 15_000) {
                nodeWithTextExists(appName) && nodeWithContentDescriptionExists(scanDescription)
            }

            composeRule.onNodeWithText(appName).assertIsDisplayed()
            composeRule.onNodeWithContentDescription(scanDescription).assertIsDisplayed()
        }
    }


    private fun relaunchMainActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        val activityClass = MainActivity::class.java.name
        val device = UiDevice.getInstance(instrumentation)
        device.pressHome()
        runShellCommand("am start -W -n $packageName/$activityClass")
        val launched = device.wait(Until.hasObject(By.pkg(packageName).depth(0)), 10_000)
        check(launched) {
            "MainActivity did not reach foreground. dumpsys=\n${runShellCommand("dumpsys activity top")}"}
        composeRule.waitForIdle()
    }

    private fun nodeWithTextExists(text: String): Boolean {
        return composeRule
            .onAllNodesWithText(text, useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isNotEmpty()
    }

    private fun nodeWithContentDescriptionExists(contentDescription: String): Boolean {
        return composeRule
            .onAllNodesWithContentDescription(contentDescription, useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isNotEmpty()
    }

    private fun runShellCommand(command: String): String {
        val descriptor = InstrumentationRegistry
            .getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
        return descriptor.use { pfd ->
            ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().use { it.readText() }
        }
    }

    private fun withDiagnostics(prefix: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            val snapshot = DeviceTestDiagnostics.capture(prefix)
            throw AssertionError(
                "Smoke test failed. Artifacts: " +
                    "screenshot=${snapshot.screenshot.absolutePath}, " +
                    "hierarchy=${snapshot.hierarchy.absolutePath}, " +
                    "logcat=${snapshot.logcat.absolutePath}\n\n" +
                    "Activity dump:\n${snapshot.resumedActivity}",
                t
            )
        }
    }
}

