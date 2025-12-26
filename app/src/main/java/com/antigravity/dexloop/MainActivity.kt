package com.antigravity.dexloop

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.antigravity.dexloop.shizuku.ShizukuHelper
import rikka.shizuku.Shizuku
import com.antigravity.dexloop.strategies.StrategyManager

class MainActivity : ComponentActivity(), Shizuku.OnRequestPermissionResultListener {

    private var shizukuGranted by mutableStateOf(false)
    private val SHIZUKU_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StrategyManager.init(this)

        Shizuku.addRequestPermissionResultListener(this)
        checkShizuku()

        // Keep screen on
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Logical Option: Enable Edge-to-Edge so content draws behind bars (smoother immersive toggle)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            DexLoopApp(
                onCheckShizuku = { checkShizukuRequest() },
                shizukuGranted = shizukuGranted
            )
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)

        // Handle orientation changes
        val metrics = resources.displayMetrics
        StrategyManager.handleOrientationChange(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(this)
    }

    private fun checkShizuku() {
        if (ShizukuHelper.isShizukuAvailable()) {
            shizukuGranted = ShizukuHelper.checkPermission()
        }
    }

    private fun checkShizukuRequest() {
        if (ShizukuHelper.isShizukuAvailable()) {
            if (ShizukuHelper.checkPermission()) {
                shizukuGranted = true
            } else {
                ShizukuHelper.requestPermission(SHIZUKU_CODE)
            }
        }
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode == SHIZUKU_CODE) {
            shizukuGranted = grantResult == PackageManager.PERMISSION_GRANTED
        }
    }
}
