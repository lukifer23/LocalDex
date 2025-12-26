package com.antigravity.dexloop.diagnostics

import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(val timestamp: Long, val tag: String, val message: String, val type: LogType, val errorCode: String? = null)
enum class LogType { INFO, ERROR, WARNING }

enum class DexLoopError(val code: String, val message: String, val recoverySuggestion: String) {
    SHIZUKU_NOT_AVAILABLE("SHIZUKU_001", "Shizuku service not available", "Install and start Shizuku service"),
    SHIZUKU_PERMISSION_DENIED("SHIZUKU_002", "Shizuku permission denied", "Grant permission in Shizuku app"),
    OVERLAY_PERMISSION_DENIED("OVERLAY_001", "System overlay permission denied", "Grant overlay permission in system settings"),
    VIRTUAL_DISPLAY_FAILED("VDISPLAY_001", "Virtual display creation failed", "Check system resources and try again"),
    DEX_LAUNCH_FAILED("DEX_001", "DeX launch failed", "Check if DeX is supported on this device"),
    STRATEGY_FAILED("STRATEGY_001", "Strategy execution failed", "Check strategy requirements and try again"),
    SURFACE_NOT_READY("SURFACE_001", "Display surface not ready", "Wait for UI to initialize and try again"),
    SERVICE_START_FAILED("SERVICE_001", "Foreground service failed to start", "Check system resources and permissions"),
    INPUT_INJECTION_FAILED("INPUT_001", "Input injection failed", "Try using physical keyboard/mouse"),
    UNKNOWN_ERROR("UNKNOWN_001", "Unknown error occurred", "Check logs and restart the app")
}

object DiagnosticsManager {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()
    
    fun log(message: String, tag: String = "DexLoop") {
        addLog(message, tag, LogType.INFO)
    }
    
    fun logError(message: String, tag: String = "DexLoop", e: Throwable? = null) {
        val msg = if (e != null) "$message\n${e.stackTraceToString()}" else message
        addLog(msg, tag, LogType.ERROR)
    }

    fun logError(error: DexLoopError, tag: String = "DexLoop", additionalInfo: String? = null) {
        val message = buildString {
            append(error.message)
            if (additionalInfo != null) {
                append(" - $additionalInfo")
            }
            append("\nRecovery: ${error.recoverySuggestion}")
        }
        addLog(message, tag, LogType.ERROR, error.code)
    }

    fun getErrorByCode(code: String): DexLoopError? {
        return DexLoopError.values().find { it.code == code }
    }

    fun getRecentErrors(limit: Int = 10): List<LogEntry> {
        return _logs.value.filter { it.type == LogType.ERROR }.takeLast(limit)
    }
    
    fun logWarning(message: String, tag: String = "DexLoop") {
        addLog(message, tag, LogType.WARNING)
    }

    private fun addLog(message: String, tag: String, type: LogType, errorCode: String? = null) {
        val entry = LogEntry(System.currentTimeMillis(), tag, message, type, errorCode)
        _logs.value = _logs.value + entry
    }
    
    fun getSystemInfo(context: android.content.Context): String {
        val metrics = context.resources.displayMetrics
        val activityManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        return """
            === Device Information ===
            Manufacturer: ${Build.MANUFACTURER}
            Model: ${Build.MODEL}
            Product: ${Build.PRODUCT}
            Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
            Security Patch: ${Build.VERSION.SECURITY_PATCH}
            Build ID: ${Build.ID}

            === Display Information ===
            Physical Size: ${metrics.widthPixels}x${metrics.heightPixels}
            Density: ${metrics.densityDpi}dpi (${metrics.density}x)
            Scaled Density: ${metrics.scaledDensity}

            === System Resources ===
            Total Memory: ${memoryInfo.totalMem / 1024 / 1024}MB
            Available Memory: ${memoryInfo.availMem / 1024 / 1024}MB
            Low Memory: ${memoryInfo.lowMemory}
            Memory Threshold: ${memoryInfo.threshold / 1024 / 1024}MB

            === Permissions ===
            Shizuku Available: ${com.antigravity.dexloop.shizuku.ShizukuHelper.isShizukuAvailable()}
            Overlay Permission: ${android.provider.Settings.canDrawOverlays(context)}

            === Audio Devices ===
            ${getAudioDeviceInfo(context)}
        """.trimIndent()
    }

    fun getSystemInfo(): String {
        // Fallback for calls without context - basic info only
        return """
            Device: ${Build.MANUFACTURER} ${Build.MODEL}
            Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
            Shizuku Available: ${com.antigravity.dexloop.shizuku.ShizukuHelper.isShizukuAvailable()}
        """.trimIndent()
    }

    fun getAudioDeviceInfo(context: android.content.Context): String {
        return try {
            val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            val devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS).toList()

            if (devices.isEmpty()) {
                "No audio devices detected"
            } else {
                devices.joinToString("\n") { device ->
                    "• ${device.productName} (Type: ${device.type})"
                }
            }
        } catch (e: Exception) {
            "Audio device info unavailable: ${e.message}"
        }
    }

    fun getPerformanceMetrics(): String {
        val runtime = Runtime.getRuntime()
        val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val freeMem = runtime.freeMemory() / 1024 / 1024
        val totalMem = runtime.totalMemory() / 1024 / 1024
        val maxMem = runtime.maxMemory() / 1024 / 1024

        return """
            === Performance Metrics ===
            App Memory Usage: ${usedMem}MB used, ${freeMem}MB free, ${totalMem}MB total, ${maxMem}MB max
            Thread Count: ${Thread.activeCount()}
            CPU Cores: ${Runtime.getRuntime().availableProcessors()}
            Uptime: ${android.os.SystemClock.uptimeMillis() / 1000}s
        """.trimIndent()
    }

    fun getTroubleshootingGuide(): String {
        val recentErrors = getRecentErrors(5)
        val hasErrors = recentErrors.isNotEmpty()

        return """
            === Troubleshooting Guide ===

            ${if (hasErrors) "RECENT ERRORS DETECTED:" else "No recent errors found."}

            ${if (hasErrors) recentErrors.joinToString("\n") { "• ${it.errorCode}: ${it.message}" } else ""}

            === Common Solutions ===

            1. SHIZUKU ISSUES:
               • Ensure Shizuku app is installed and running
               • Grant Shizuku permission when prompted
               • Try restarting Shizuku service

            2. OVERLAY PERMISSION:
               • Grant "Display over other apps" permission
               • Settings > Apps > DexLoop > Display over other apps

            3. DEX LAUNCH FAILURES:
               • Verify device supports DeX (Samsung Galaxy Z Fold series)
               • Try different strategies (Strategy 1 is most reliable)
               • Check if DeX is already running

            4. VIRTUAL DISPLAY ISSUES:
               • Ensure sufficient system resources
               • Close other memory-intensive apps
               • Try restarting the device

            5. INPUT PROBLEMS:
               • Use physical keyboard/mouse for best experience
               • Check USB debugging is enabled
               • Verify input permissions

            === Debug Information ===
            ${getSystemInfo()}

            ${getPerformanceMetrics()}
        """.trimIndent()
    }
    
    fun generateReport(context: android.content.Context? = null): String {
        val sb = StringBuilder()
        sb.append("=== DexLoop Diagnostic Report ===\n")
        sb.append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())).append("\n\n")

        if (context != null) {
        sb.append(getSystemInfo(context)).append("\n\n")
        sb.append(getPerformanceMetrics()).append("\n\n")
        sb.append(com.antigravity.dexloop.strategies.PerformanceOptimizer.getPerformanceStats()).append("\n\n")
        } else {
            sb.append(getSystemInfo()).append("\n\n")
        }

        sb.append("=== Recent Logs ===\n")
        _logs.value.takeLast(50).forEach {
            val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(it.timestamp))
            val errorCode = if (it.errorCode != null) " [${it.errorCode}]" else ""
            sb.append("[$time] [${it.type}] [${it.tag}]$errorCode ${it.message}\n")
        }

        sb.append("\n=== Troubleshooting Guide ===\n")
        sb.append(getTroubleshootingGuide())

        return sb.toString()
    }

    
    fun shareLogs(context: android.content.Context) {
         try {
            val report = generateReport(context)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "DexLoop Diagnostic Report")
                putExtra(android.content.Intent.EXTRA_TEXT, report)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share Diagnostic Report"))
         } catch (e: Exception) {
             logError("Share failed", "Diagnostics", e)
         }
    }
}
