package com.antigravity.dexloop.strategies

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.Settings
import android.view.Display
import com.antigravity.dexloop.diagnostics.DiagnosticsManager
import com.antigravity.dexloop.diagnostics.DexLoopError
import com.antigravity.dexloop.shizuku.ShizukuHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay

/**
 * Strategy for configuring Samsung DeX mode through proper system settings and display setup.
 * This strategy focuses on enabling real DeX functionality rather than simulating it.
 */
class DexConfigurationStrategy(private val context: Context) : Strategy {
    override val name = "DeX Configuration & Setup"
    override val description = "Configures system settings and detects displays for real Samsung DeX mode"

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _configurationStatus = MutableStateFlow<ConfigurationStatus>(ConfigurationStatus.Idle)
    val configurationStatus: StateFlow<ConfigurationStatus> = _configurationStatus.asStateFlow()

    sealed class ConfigurationStatus {
        object Idle : ConfigurationStatus()
        object CheckingPrerequisites : ConfigurationStatus()
        data class PrerequisitesFailed(val reason: String) : ConfigurationStatus()
        object ConfiguringSettings : ConfigurationStatus()
        data class SettingsFailed(val reason: String, val partialSuccess: Boolean = false) : ConfigurationStatus()
        object DetectingDisplays : ConfigurationStatus()
        data class DisplaysDetected(val displayInfo: String) : ConfigurationStatus()
        object ReadyForConnection : ConfigurationStatus()
        data class Configured(val displayInfo: String) : ConfigurationStatus()
        data class Failed(val reason: String, val canRetry: Boolean = true) : ConfigurationStatus()
    }

    override suspend fun start(): Result<Unit> {
        return try {
            DiagnosticsManager.log("=== STARTING DeX Configuration Strategy ===", "DexConfig")
            DiagnosticsManager.log("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}", "DexConfig")
            DiagnosticsManager.log("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})", "DexConfig")

            if (_isRunning.value) {
                DiagnosticsManager.log("Strategy already running, skipping start", "DexConfig")
                return Result.success(Unit)
            }

            _isRunning.value = true
            _configurationStatus.value = ConfigurationStatus.CheckingPrerequisites
            DiagnosticsManager.log("Step 1: Checking prerequisites...", "DexConfig")

            // Step 1: Check prerequisites
            val prereqResult = checkPrerequisites()
            if (prereqResult.isFailure) {
                val errorMsg = prereqResult.exceptionOrNull()?.message ?: "Unknown prerequisite failure"
                DiagnosticsManager.logError(DexLoopError.STRATEGY_FAILED, "DexConfig", "Prerequisites check failed: $errorMsg")
                _configurationStatus.value = ConfigurationStatus.PrerequisitesFailed(errorMsg)
                _isRunning.value = false
                StrategyManager.saveStrategyState()
                // Don't stop here - let the user see the error and retry
                return prereqResult
            }

            DiagnosticsManager.log("Prerequisites check passed", "DexConfig")
            _configurationStatus.value = ConfigurationStatus.ConfiguringSettings
            DiagnosticsManager.log("Step 2: Configuring system settings...", "DexConfig")

            // Step 2: Configure system settings for DeX
            val settingsResult = configureSystemSettings()
            if (settingsResult.isFailure) {
                val errorMsg = settingsResult.exceptionOrNull()?.message ?: "Unknown settings failure"
                DiagnosticsManager.logError(DexLoopError.STRATEGY_FAILED, "DexConfig", "Settings configuration failed: $errorMsg")
                _configurationStatus.value = ConfigurationStatus.SettingsFailed(errorMsg)
                _isRunning.value = false
                StrategyManager.saveStrategyState()
                // Don't stop here - let the user see the error but continue to display detection
                return settingsResult
            }

            DiagnosticsManager.log("System settings configured successfully", "DexConfig")
            _configurationStatus.value = ConfigurationStatus.DetectingDisplays
            DiagnosticsManager.log("Step 3: Detecting connected displays...", "DexConfig")

            // Step 3: Check for connected displays
            val displayResult = checkConnectedDisplays()
            if (displayResult.isFailure) {
                _configurationStatus.value = ConfigurationStatus.ReadyForConnection
                DiagnosticsManager.log("No external displays detected - system configured and ready for display connection", "DexConfig")
            } else {
                val displayInfo = displayResult.getOrNull() ?: "Unknown display"
                _configurationStatus.value = ConfigurationStatus.DisplaysDetected(displayInfo)
                DiagnosticsManager.log("External display detected: $displayInfo", "DexConfig")
            }

            StrategyManager.saveStrategyState()
            DiagnosticsManager.log("=== DeX Configuration Strategy completed successfully ===", "DexConfig")
            Result.success(Unit)

        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unknown error during configuration"
            DiagnosticsManager.logError(DexLoopError.STRATEGY_FAILED, "DexConfig", "Critical error: $errorMsg")
            DiagnosticsManager.logError(DexLoopError.STRATEGY_FAILED, "DexConfig", "Stack trace: ${e.stackTraceToString()}")
            _configurationStatus.value = ConfigurationStatus.Failed(errorMsg, true) // Allow retry
            _isRunning.value = false
            StrategyManager.saveStrategyState()
            // Don't call stop() here - let user see the error and retry
            Result.failure(e)
        }
    }

    override suspend fun stop() {
        DiagnosticsManager.log("Stopping DeX Configuration Strategy", "DexConfig")
        _isRunning.value = false
        _configurationStatus.value = ConfigurationStatus.Idle
        StrategyManager.saveStrategyState()
    }

    private suspend fun checkPrerequisites(): Result<Unit> {
        DiagnosticsManager.log("Checking DeX prerequisites", "DexConfig")

        // Check Shizuku availability
        val shizukuAvailable = ShizukuHelper.isShizukuAvailable()
        DiagnosticsManager.log("Shizuku available: $shizukuAvailable", "DexConfig")
        if (!shizukuAvailable) {
            return Result.failure(Exception("Shizuku not available - required for system configuration. Please install and start Shizuku."))
        }

        val shizukuPermissionGranted = ShizukuHelper.checkPermission()
        DiagnosticsManager.log("Shizuku permission granted: $shizukuPermissionGranted", "DexConfig")
        if (!shizukuPermissionGranted) {
            return Result.failure(Exception("Shizuku permission not granted. Please grant permission in Shizuku app."))
        }

        // Check if device supports DeX (Samsung device check)
        val isSamsungDevice = Build.MANUFACTURER.equals("Samsung", ignoreCase = true) ||
                             Build.BRAND.equals("Samsung", ignoreCase = true)
        DiagnosticsManager.log("Is Samsung device: $isSamsungDevice (${Build.MANUFACTURER} ${Build.MODEL})", "DexConfig")

        if (!isSamsungDevice) {
            DiagnosticsManager.logWarning("Device is not Samsung - DeX features may not be available or may not work properly", "DexConfig")
            // Don't fail here - some non-Samsung devices may still support DeX-like features
        }

        // Check Android version (DeX typically requires Android 7.0+)
        val androidVersionOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        DiagnosticsManager.log("Android version check: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) >= 24 (N): $androidVersionOk", "DexConfig")
        if (!androidVersionOk) {
            return Result.failure(Exception("Android ${Build.VERSION.RELEASE} not supported - DeX requires Android 7.0+ (API 24)"))
        }

        DiagnosticsManager.log("All prerequisites passed", "DexConfig")
        return Result.success(Unit)
    }

    private suspend fun configureSystemSettings(): Result<Unit> {
        DiagnosticsManager.log("Configuring system settings for DeX", "DexConfig")

        val settingsCommands = listOf(
            // Core DeX settings - these are the most important and most likely to work
            Triple("settings put global force_resizable_activities 1", "Force resizable activities", true),
            Triple("settings put global enable_freeform_support 1", "Enable freeform support", true),

            // Display-related settings (may not work on all devices)
            Triple("settings put global desktop_mode_enabled 1", "Enable desktop mode", false),
            Triple("settings put global hdmi_control_enabled 1", "Enable HDMI control", false),

            // Try to set overlay display devices (this is key for triggering display detection)
            Triple("settings put global overlay_display_devices \"1920x1080/240\"", "Configure overlay display devices", false)
        )

        var coreSuccessCount = 0
        var totalSuccessCount = 0
        val failedCommands = mutableListOf<String>()

        for ((command, description, isCore) in settingsCommands) {
            delay(200) // Small delay between commands
            val result = ShizukuHelper.runShellCommand(command)
            if (result.isSuccess) {
                totalSuccessCount++
                if (isCore) coreSuccessCount++
                DiagnosticsManager.log("✓ $description configured successfully", "DexConfig")
            } else {
                failedCommands.add(description)
                DiagnosticsManager.logWarning("✗ $description failed: ${result.stderr}", "DexConfig")
            }
        }

        val totalCommands = settingsCommands.size
        val coreCommands = settingsCommands.count { it.third }

        DiagnosticsManager.log("Settings configuration complete: $totalSuccessCount/$totalCommands successful, $coreSuccessCount/$coreCommands core settings", "DexConfig")

        if (coreSuccessCount == 0) {
            return Result.failure(Exception("Failed to configure any core DeX settings. Total attempts: $totalSuccessCount/$totalCommands"))
        }

        if (totalSuccessCount < totalCommands) {
            DiagnosticsManager.logWarning("Some settings failed: ${failedCommands.joinToString(", ")}", "DexConfig")
            // Don't fail completely if we got the core settings
        }

        return Result.success(Unit)
    }

    private fun checkConnectedDisplays(): Result<String> {
        DiagnosticsManager.log("Checking for connected displays", "DexConfig")

        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displays = displayManager.displays

        val externalDisplays = displays.filter { display ->
            display.displayId != Display.DEFAULT_DISPLAY &&
            display.isValid &&
            (display.flags and Display.FLAG_PRESENTATION) == 0 // Not a presentation display
        }

        if (externalDisplays.isEmpty()) {
            return Result.failure(Exception("No external displays detected"))
        }

        val displayInfo = externalDisplays.joinToString("; ") { display ->
            "Display ${display.displayId}: ${display.mode?.physicalWidth}x${display.mode?.physicalHeight} @${display.refreshRate}Hz"
        }

        DiagnosticsManager.log("Found ${externalDisplays.size} external display(s): $displayInfo", "DexConfig")
        return Result.success(displayInfo)
    }

    fun refreshDisplayDetection(): Result<String> {
        return checkConnectedDisplays()
    }

    fun resetForRetry() {
        DiagnosticsManager.log("Resetting strategy state for retry", "DexConfig")
        _configurationStatus.value = ConfigurationStatus.Idle
        // Don't change _isRunning.value here - let the start() method handle it
    }

    suspend fun attemptDexActivation(): Result<Unit> {
        return try {
            DiagnosticsManager.log("Attempting DeX activation", "DexConfig")

            // Try to trigger DeX mode through known intents
            val dexIntentCommands = listOf(
                "am broadcast -a com.samsung.android.desktopmode.action.ENTER_DESKTOP_MODE",
                "am broadcast -a android.intent.action.ENTER_DESKTOP_MODE",
                "am start -n com.sec.android.app.desktoplauncher/.Launcher"
            )

            var anySuccess = false
            for (command in dexIntentCommands) {
                delay(500)
                val result = ShizukuHelper.runShellCommand(command)
                if (result.isSuccess) {
                    anySuccess = true
                    DiagnosticsManager.log("DeX activation command succeeded: $command", "DexConfig")
                    break
                }
            }

            if (anySuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("No DeX activation method succeeded"))
            }
        } catch (e: Exception) {
            DiagnosticsManager.logError("DeX activation failed", "DexConfig", e)
            Result.failure(e)
        }
    }
}

