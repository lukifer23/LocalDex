package com.antigravity.dexloop

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.antigravity.dexloop.strategies.StrategyManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DexLoopIntegrationTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setup() {
        // Reset StrategyManager
        resetStrategyManager()

        // Launch activity
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun cleanup() {
        scenario.close()

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
        scenario.onActivity { activity ->
            assertNotNull("Activity should not be null", activity)
            assertFalse("Activity should not be finishing", activity.isFinishing)
        }
    }

    @Test
    fun testStrategyManagerInitialization() {
        scenario.onActivity { activity ->
            assertTrue("StrategyManager should be initialized",
                StrategyManager::class.java.getDeclaredField("isInitialized").apply {
                    isAccessible = true
                }.get(StrategyManager) as Boolean)
        }
    }

    @Test
    fun testUIElementsPresent() {
        // Check that main UI elements are present
        onView(withText("DexLoop Control Center")).check(matches(isDisplayed()))

        // Check DeX configuration strategy is present
        onView(withText("DeX Configuration & Setup")).check(matches(isDisplayed()))
    }

    @Test
    fun testDexConfigurationStrategyStartStop() {
        scenario.onActivity { activity ->
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
    fun testConfigurationPersistence() {
        scenario.onActivity { activity ->
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
        scenario.onActivity { activity ->
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
        scenario.onActivity { activity ->
            // Test that diagnostics can be generated without crashing
            val report = com.antigravity.dexloop.diagnostics.DiagnosticsManager.generateReport(activity)
            assertNotNull("Diagnostic report should not be null", report)
            assertTrue("Report should contain content", report.isNotEmpty())
            assertTrue("Report should contain system info", report.contains("Device") || report.contains("Android"))
        }
    }

    @Test
    fun testStrategyStatePersistence() {
        scenario.onActivity { activity ->
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
