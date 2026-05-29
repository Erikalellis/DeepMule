package com.example.deepmule

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File

internal object DeviceTestDiagnostics {
    data class Snapshot(
        val screenshot: File,
        val hierarchy: File,
        val logcat: File,
        val resumedActivity: String
    )

    fun capture(prefix: String): Snapshot {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)

        val artifactsDir = File(context.cacheDir, "androidTest-artifacts").apply { mkdirs() }
        val safePrefix = prefix.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val timestamp = System.currentTimeMillis()

        val screenshot = File(artifactsDir, "${safePrefix}-${timestamp}-screen.png")
        val hierarchy = File(artifactsDir, "${safePrefix}-${timestamp}-hierarchy.xml")
        val logcat = File(artifactsDir, "${safePrefix}-${timestamp}-logcat.txt")

        runCatching { device.takeScreenshot(screenshot) }
        runCatching { device.dumpWindowHierarchy(hierarchy) }
        runCatching {
            logcat.writeText(runShell("logcat -d -t 400"))
        }

        return Snapshot(
            screenshot = screenshot,
            hierarchy = hierarchy,
            logcat = logcat,
            resumedActivity = runShell("dumpsys activity top")
        )
    }

    private fun runShell(command: String): String {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        descriptor.use { pfd ->
            ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().use { reader ->
                return reader.readText()
            }
        }
    }
}

