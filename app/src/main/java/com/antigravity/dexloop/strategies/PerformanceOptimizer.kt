package com.antigravity.dexloop.strategies

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import android.os.Process
import com.antigravity.dexloop.diagnostics.DiagnosticsManager
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

/**
 * Performance optimizer for DexLoop operations.
 * Manages CPU usage, memory, and battery considerations.
 */
object PerformanceOptimizer {

    private lateinit var context: Context
    private lateinit var powerManager: PowerManager
    private lateinit var batteryManager: BatteryManager

    private var isInitialized = false
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Performance monitoring
    private var monitoringJob: Job? = null
    private var lastMemoryWarning = 0L

    // CPU optimization
    private var cpuCores = Runtime.getRuntime().availableProcessors()
    private var optimalThreadPoolSize = maxOf(2, cpuCores / 2) // Use half available cores

    // Battery optimization
    private var isBatteryLow = false
    private var isCharging = false

    // Memory optimization
    private val memoryThreshold = 50 * 1024 * 1024L // 50MB threshold

    fun init(context: Context) {
        if (isInitialized) return

        this.context = context.applicationContext
        powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        isInitialized = true
        startPerformanceMonitoring()

        DiagnosticsManager.log("PerformanceOptimizer initialized", "Performance")
    }

    fun shutdown() {
        monitoringJob?.cancel()
        scope.cancel()
        isInitialized = false
        DiagnosticsManager.log("PerformanceOptimizer shutdown", "Performance")
    }

    private fun startPerformanceMonitoring() {
        monitoringJob = scope.launch {
            while (isActive) {
                updateBatteryStatus()
                checkMemoryUsage()
                optimizeThreadPool()

                delay(30000) // Check every 30 seconds
            }
        }
    }

    private fun updateBatteryStatus() {
        try {
            val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            isBatteryLow = batteryLevel < 20

            val chargingStatus = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            isCharging = chargingStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                        chargingStatus == BatteryManager.BATTERY_STATUS_FULL

            if (isBatteryLow && !isCharging) {
                DiagnosticsManager.logWarning("Battery low (${batteryLevel}%), performance optimizations active", "Performance")
            }
        } catch (e: Exception) {
            DiagnosticsManager.logError("Failed to check battery status", "Performance", e)
        }
    }

    private fun checkMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryUsagePercent = (usedMemory.toFloat() / maxMemory.toFloat()) * 100

        if (usedMemory > maxMemory - memoryThreshold) {
            val now = System.currentTimeMillis()
            if (now - lastMemoryWarning > 60000) { // Warn at most once per minute
                DiagnosticsManager.logWarning("High memory usage: ${usedMemory / 1024 / 1024}MB used, triggering cleanup", "Performance")
                triggerMemoryCleanup()
                lastMemoryWarning = now
            }
        }

        // Log memory stats periodically
        if (memoryUsagePercent > 80) {
            DiagnosticsManager.log("Memory usage: ${String.format("%.1f", memoryUsagePercent)}%", "Performance")
        }
    }

    private fun triggerMemoryCleanup() {
        scope.launch {
            try {
                // Force garbage collection (use with caution)
                System.gc()
                System.runFinalization()

                // Clean up any cached resources in strategies
                cleanupStrategyResources()

                val runtime = Runtime.getRuntime()
                val freedMemory = runtime.freeMemory()
                DiagnosticsManager.log("Memory cleanup completed, ${freedMemory / 1024 / 1024}MB free", "Performance")
            } catch (e: Exception) {
                DiagnosticsManager.logError("Memory cleanup failed", "Performance", e)
            }
        }
    }

    private fun cleanupStrategyResources() {
        // Note: Strategy cleanup is handled by StrategyManager
        // This method can be extended to clean up other cached resources
        try {
            // Force garbage collection for memory cleanup
            System.gc()
            System.runFinalization()
            DiagnosticsManager.log("Performed system resource cleanup", "Performance")
        } catch (e: Exception) {
            DiagnosticsManager.logError("Resource cleanup failed", "Performance", e)
        }
    }

    private fun optimizeThreadPool() {
        val activeThreads = Thread.activeCount()
        val optimalSize = if (isBatteryLow && !isCharging) {
            maxOf(1, optimalThreadPoolSize / 2) // Reduce threads on low battery
        } else {
            optimalThreadPoolSize
        }

        if (activeThreads > optimalSize * 2) {
            DiagnosticsManager.logWarning("High thread count: $activeThreads, consider optimization", "Performance")
        }
    }

    fun shouldThrottleOperations(): Boolean {
        return isBatteryLow && !isCharging
    }

    fun getRecommendedQuality(): QualityLevel {
        return when {
            isBatteryLow && !isCharging -> QualityLevel.LOW
            Runtime.getRuntime().freeMemory() < memoryThreshold -> QualityLevel.MEDIUM
            else -> QualityLevel.HIGH
        }
    }

    fun scheduleLowPriorityTask(task: suspend () -> Unit) {
        scope.launch(Dispatchers.IO) {
            if (shouldThrottleOperations()) {
                delay(1000) // Throttle on low battery
            }
            try {
                task()
            } catch (e: Exception) {
                DiagnosticsManager.logError("Low priority task failed", "Performance", e)
            }
        }
    }

    fun getPerformanceStats(): String {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val freeMemory = runtime.freeMemory() / 1024 / 1024
        val totalMemory = runtime.totalMemory() / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024

        return """
            === Performance Stats ===
            Memory: ${usedMemory}MB used, ${freeMemory}MB free, ${totalMemory}MB total, ${maxMemory}MB max
            Threads: ${Thread.activeCount()} active, ${cpuCores} CPU cores
            Battery: ${if (isBatteryLow) "LOW" else "OK"}, ${if (isCharging) "CHARGING" else "NOT CHARGING"}
            Quality Level: ${getRecommendedQuality()}
        """.trimIndent()
    }

    enum class QualityLevel {
        LOW, MEDIUM, HIGH
    }

    // Utility function to run operations with performance monitoring
    suspend fun <T> runWithPerformanceMonitoring(
        operation: suspend () -> T,
        operationName: String
    ): T {
        val startTime = System.currentTimeMillis()
        val startMemory = Runtime.getRuntime().freeMemory()

        return try {
            val result = operation()
            val endTime = System.currentTimeMillis()
            val endMemory = Runtime.getRuntime().freeMemory()

            val duration = endTime - startTime
            val memoryDelta = startMemory - endMemory

            if (duration > 1000 || memoryDelta > 1024 * 1024) { // Log if > 1s or > 1MB
                DiagnosticsManager.log("$operationName completed in ${duration}ms, memory delta: ${memoryDelta / 1024}KB", "Performance")
            }

            result
        } catch (e: Exception) {
            DiagnosticsManager.logError("$operationName failed", "Performance", e)
            throw e
        }
    }
}
