package com.antigravity.dexloop.shizuku

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.antigravity.dexloop.diagnostics.DexLoopError
import com.antigravity.dexloop.diagnostics.DiagnosticsManager

object ShizukuHelper {

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun checkPermission(): Boolean {
        if (!isShizukuAvailable()) {
            DiagnosticsManager.logError(DexLoopError.SHIZUKU_NOT_AVAILABLE, "ShizukuHelper")
            return false
        }
        return if (Shizuku.isPreV11()) {
            DiagnosticsManager.logError(
                DexLoopError.SHIZUKU_PERMISSION_DENIED,
                "ShizukuHelper",
                "Pre-v11 version not supported"
            )
            false
        } else {
            val permission = Shizuku.checkSelfPermission()
            val granted = permission == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                DiagnosticsManager.logError(DexLoopError.SHIZUKU_PERMISSION_DENIED, "ShizukuHelper")
            }
            DiagnosticsManager.log("Shizuku permission check: $granted (code: $permission)", "ShizukuHelper")
            granted
        }
    }

    fun requestPermission(code: Int) {
        if (isShizukuAvailable() && !Shizuku.isPreV11()) {
            Shizuku.requestPermission(code)
        }
    }

    suspend fun runShellCommand(command: String): ShellResult = withContext(Dispatchers.IO) {
        if (!isShizukuAvailable()) {
            return@withContext ShellResult(-1, "", "Shizuku not available")
        }

        try {
            // Shizuku.newProcess is hidden/private in newer versions, use reflection
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess", 
                Array<String>::class.java, 
                Array<String>::class.java, 
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            
            val exitCode = process.waitFor()
            
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val error = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }

            ShellResult(exitCode, output, error)
        } catch (e: Exception) {
            e.printStackTrace()
            ShellResult(-1, "", e.message ?: "Unknown error")
        }
    }

    data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String) {
        val isSuccess: Boolean get() = exitCode == 0
    }
}
