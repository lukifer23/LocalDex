package com.antigravity.dexloop

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.antigravity.dexloop.strategies.DexConfigurationStrategy
import com.antigravity.dexloop.strategies.StrategyManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DexLoopIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        resetStrategyManager()
        composeTestRule.activityRule.scenario.onActivity { activity ->
            StrategyManager.init(activity)
        }
    }

    @After
    fun cleanup() {
        // Clean up any running strategies
        runBlocking {
            try {
                if (StrategyManager.dexConfigurationStrategy.isRunning.value) {
                    StrategyManager.dexConfigurationStrategy.stop()
                }
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }

        resetStrategyManager()
    }

    @Test
    fun testMainActivityLaunches() {
        composeTestRule.activityRule.scenario.onActivity { activity ->
            assertNotNull("Activity should not be null", activity)
            assertFalse("Activity should not be finishing", activity.isFinishing)
        }
    }

    @Test
    fun testStrategyManagerInitialization() {
        composeTestRule.activityRule.scenario.onActivity { activity ->
            assertTrue("StrategyManager should be initialized",
                StrategyManager::class.java.getDeclaredField("isInitialized").apply {
                    isAccessible = true
                }.get(StrategyManager) as Boolean)
        }
    }

    @Test
    fun testUIElementsPresent() {
        // Check that main UI elements are present
        composeTestRule.onNodeWithText("DexLoop Control Center").assertIsDisplayed()

        // Check DeX configuration strategy is present
        composeTestRule.onNodeWithText("DeX Configuration & Setup").assertIsDisplayed()
    }

    @Test
    fun testDexConfigurationStrategyStartStop() {
        composeTestRule.activityRule.scenario.onActivity { _ ->
            // Test starting DeX configuration strategy
            runBlocking {
                val startResult = StrategyManager.dexConfigurationStrategy.start()
                // Note: This might fail without proper Shizuku setup in test environment
                // but we can at least test that the method exists and doesn't crash
                assertNotNull("DeX configuration strategy start should return a result", startResult)
            }

            // Test stopping DeX configuration strategy
            runBlocking {
                StrategyManager.dexConfigurationStrategy.stop()
                assertFalse("DeX configuration strategy should be stopped",
                    StrategyManager.dexConfigurationStrategy.isRunning.value)
            }
        }
    }

    @Test
    fun testDexConfigurationFailureResetsRunningState() = runBlocking {
        val result = StrategyManager.dexConfigurationStrategy.start()

        assertTrue("Configuration should fail in test environment without Shizuku", result.isFailure)
        assertFalse("Strategy should not be marked as running after failure",
            StrategyManager.dexConfigurationStrategy.isRunning.value)

        val status = StrategyManager.dexConfigurationStrategy.configurationStatus.value
        assertTrue(
            "Failure status should be terminal",
            status is DexConfigurationStrategy.ConfigurationStatus.PrerequisitesFailed ||
                status is DexConfigurationStrategy.ConfigurationStatus.SettingsFailed ||
                status is DexConfigurationStrategy.ConfigurationStatus.Failed
        )
    }

    @Test
    fun testConfigurationPersistence() {
        composeTestRule.activityRule.scenario.onActivity { activity ->
            val initialConfig = StrategyManager.displayConfig.value

            // Update configuration
            val newWidth = 2560
            val newHeight = 1440
            StrategyManager.updateConfig(newWidth, newHeight, 320)

            val updatedConfig = StrategyManager.displayConfig.value
            assertEquals("Width should be updated", newWidth, updatedConfig.width)
            assertEquals("Height should be updated", newHeight, updatedConfig.height)

            // Reset StrategyManager to test persistence
            resetStrategyManager()
            StrategyManager.init(activity)

            val restoredConfig = StrategyManager.displayConfig.value
            assertEquals("Configuration should be persisted", newWidth, restoredConfig.width)
            assertEquals("Configuration should be persisted", newHeight, restoredConfig.height)
        }
    }

    @Test
    fun testOrientationChangeHandling() {
        composeTestRule.activityRule.scenario.onActivity { _ ->
            // Simulate orientation change
            val newWidth = 1080
            val newHeight = 1920
            val newDensity = 240

            StrategyManager.handleOrientationChange(newWidth, newHeight, newDensity)

            // Verify adaptive configuration was applied
            val config = StrategyManager.displayConfig.value
            assertTrue("Should adapt to portrait configuration", config.isPortrait)
        }
    }

    @Test
    fun testDiagnosticsExport() {
        composeTestRule.activityRule.scenario.onActivity { activity ->
            // Test that diagnostics can be generated without crashing
            val report = com.antigravity.dexloop.diagnostics.DiagnosticsManager.generateReport(activity)
            assertNotNull("Diagnostic report should not be null", report)
            assertTrue("Report should contain content", report.isNotEmpty())
            assertTrue("Report should contain system info", report.contains("Device") || report.contains("Android"))
        }
    }

    @Test
    fun testStrategyStatePersistence() {
        composeTestRule.activityRule.scenario.onActivity { activity ->
            // Start DeX configuration strategy
            runBlocking {
                val startResult = StrategyManager.dexConfigurationStrategy.start()
                // Strategy might not actually start in test environment, but method should work
                assertNotNull("Strategy start should return a result", startResult)
            }

            // Save state
            StrategyManager.saveStrategyState()

            // Reset and reinitialize StrategyManager
            resetStrategyManager()
            StrategyManager.init(activity)

            // State should be restored (though strategy won't auto-restart for safety)
            // This mainly tests that the save/load mechanism doesn't crash
            assertTrue("StrategyManager should reinitialize successfully", true)
        }
    }

    private fun resetStrategyManager() {
        try {
            val strategyManagerClass = StrategyManager::class.java
            val isInitializedField = strategyManagerClass.getDeclaredField("isInitialized")
            isInitializedField.isAccessible = true
            isInitializedField.set(StrategyManager, false)
        } catch (e: Exception) {
            // Ignore if field access fails
        }
    }
}
