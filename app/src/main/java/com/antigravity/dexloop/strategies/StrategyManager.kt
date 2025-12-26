package com.antigravity.dexloop.strategies

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import com.antigravity.dexloop.diagnostics.DiagnosticsManager

/**
 * Central manager for all display strategies and shared configuration.
 * Persists display configuration across app restarts using SharedPreferences.
 * Thread-safe singleton with proper lifecycle management.
 */
object StrategyManager {
    lateinit var dexConfigurationStrategy: DexConfigurationStrategy
        private set

    private lateinit var prefs: SharedPreferences
    private var isInitialized = false
    private val lock = Any() // Synchronization lock
    
    private const val PREFS_NAME = "dexloop_config"
    private const val KEY_WIDTH = "display_width"
    private const val KEY_HEIGHT = "display_height"
    private const val KEY_DENSITY = "display_density"
    private const val KEY_RUNNING_STRATEGY = "running_strategy"
    private const val KEY_STRATEGY_STATE = "strategy_state"

    data class DisplayConfig(
        val width: Int = 1920,
        val height: Int = 1080,
        val density: Int = 240
    ) {
        val isLandscape: Boolean get() = width > height
        val isPortrait: Boolean get() = height > width
        val aspectRatio: Float get() = width.toFloat() / height.toFloat()
    }

    private val _displayConfig = MutableStateFlow(DisplayConfig())
    val displayConfig = _displayConfig.asStateFlow()

    fun updateConfig(width: Int, height: Int, density: Int) {
        synchronized(lock) {
            val newConfig = DisplayConfig(width, height, density)
            _displayConfig.value = newConfig

            // Persist to SharedPreferences
            if (::prefs.isInitialized) {
                prefs.edit()
                    .putInt(KEY_WIDTH, width)
                    .putInt(KEY_HEIGHT, height)
                    .putInt(KEY_DENSITY, density)
                    .apply()
                DiagnosticsManager.log("Config saved: ${width}x${height}@${density}dpi", "StrategyManager")
            }
        }
    }

    fun saveStrategyState() {
        synchronized(lock) {
            if (!::prefs.isInitialized) return

            val runningStrategy = when {
                ::dexConfigurationStrategy.isInitialized && dexConfigurationStrategy.isRunning.value -> "dex_config"
                else -> "none"
            }

            prefs.edit()
                .putString(KEY_RUNNING_STRATEGY, runningStrategy)
                .putBoolean(KEY_STRATEGY_STATE, runningStrategy != "none")
                .apply()

            DiagnosticsManager.log("Strategy state saved: $runningStrategy", "StrategyManager")
        }
    }

    fun restoreStrategyState() {
        synchronized(lock) {
            if (!::prefs.isInitialized) return

            val runningStrategy = prefs.getString(KEY_RUNNING_STRATEGY, "none") ?: "none"
            val wasRunning = prefs.getBoolean(KEY_STRATEGY_STATE, false)

            if (wasRunning) {
                DiagnosticsManager.log("Restoring strategy state: $runningStrategy", "StrategyManager")

                // Note: We don't auto-restart strategies on app launch as it might be unexpected
                // Instead, we could show a dialog asking user if they want to resume
                DiagnosticsManager.log("Previous session had active strategy: $runningStrategy", "StrategyManager")
            }
        }
    }

    fun handleOrientationChange(newWidth: Int, newHeight: Int, newDensity: Int) {
        synchronized(lock) {
            val currentConfig = displayConfig.value

            // Only update if orientation actually changed
            val newIsLandscape = newWidth > newHeight
            val currentIsLandscape = currentConfig.isLandscape

            if (newIsLandscape != currentIsLandscape) {
                DiagnosticsManager.log("Orientation changed to ${if (newIsLandscape) "landscape" else "portrait"}", "StrategyManager")

                // Suggest adaptive configuration
                val suggestedConfig = getAdaptiveConfig(newWidth, newHeight, newDensity)
                DiagnosticsManager.log("Suggested adaptive config: ${suggestedConfig.width}x${suggestedConfig.height}@${suggestedConfig.density}dpi", "StrategyManager")

                // Auto-apply if no strategy is running
                val hasRunningStrategy = (::dexConfigurationStrategy.isInitialized && dexConfigurationStrategy.isRunning.value)

                if (!hasRunningStrategy) {
                    updateConfig(suggestedConfig.width, suggestedConfig.height, suggestedConfig.density)
                    DiagnosticsManager.log("Auto-applied adaptive configuration", "StrategyManager")
                } else {
                    DiagnosticsManager.log("Strategy running - manual config update required", "StrategyManager")
                }
            }
        }
    }

    private fun getAdaptiveConfig(deviceWidth: Int, deviceHeight: Int, deviceDensity: Int): DisplayConfig {
        // For foldable devices, optimize for landscape mode
        val isFoldable = deviceWidth > 2000 || deviceHeight > 2000 // Rough heuristic

        return if (isFoldable && deviceWidth > deviceHeight) {
            // Landscape foldable - use high res
            DisplayConfig(2560, 1440, 320)
        } else if (deviceWidth > deviceHeight) {
            // Landscape phone/tablet
            DisplayConfig(1920, 1080, 240)
        } else {
            // Portrait - suggest landscape-like config
            DisplayConfig(1080, 1920, 240)
        }
    }

    fun init(context: Context) {
        synchronized(lock) {
            if (isInitialized) {
                DiagnosticsManager.logWarning("StrategyManager already initialized", "StrategyManager")
                return
            }

            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // Load saved config or use defaults
            val savedWidth = prefs.getInt(KEY_WIDTH, 1920)
            val savedHeight = prefs.getInt(KEY_HEIGHT, 1080)
            val savedDensity = prefs.getInt(KEY_DENSITY, 240)
            _displayConfig.value = DisplayConfig(savedWidth, savedHeight, savedDensity)

            DiagnosticsManager.log(
                "Config loaded: ${savedWidth}x${savedHeight}@${savedDensity}dpi",
                "StrategyManager"
            )

            // Initialize strategies with proper error handling
            try {
                dexConfigurationStrategy = DexConfigurationStrategy(context)

                // Initialize performance optimization
                PerformanceOptimizer.init(context)

                // Restore previous state
                restoreStrategyState()

                isInitialized = true
                DiagnosticsManager.log("StrategyManager initialized successfully", "StrategyManager")
            } catch (e: Exception) {
                DiagnosticsManager.logError("Failed to initialize StrategyManager", "StrategyManager", e)
                throw e
            }
        }
    }

    fun shutdown() {
        synchronized(lock) {
            if (!isInitialized) {
                return
            }

            // Stop any running strategies
            runBlocking {
                try {
                    if (::dexConfigurationStrategy.isInitialized && dexConfigurationStrategy.isRunning.value) {
                        dexConfigurationStrategy.stop()
                    }
                } catch (e: Exception) {
                    DiagnosticsManager.logError("Error stopping DeX configuration strategy during shutdown", "StrategyManager", e)
                }
            }

            // Shutdown performance optimizer
            PerformanceOptimizer.shutdown()

            isInitialized = false
            DiagnosticsManager.log("StrategyManager shutdown complete", "StrategyManager")
        }
    }
}

