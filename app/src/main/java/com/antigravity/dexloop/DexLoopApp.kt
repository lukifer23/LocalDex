package com.antigravity.dexloop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.antigravity.dexloop.strategies.StrategyManager
import kotlinx.coroutines.launch

// Simple constants for UI
object DexLoopColors {
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFF9800)
    val Error = Color(0xFFF44336)
    val Info = Color(0xFF2196F3)
    val Running = Success
    val Stopped = Color(0xFF9E9E9E)
    val Loading = Info
    val Disabled = Color(0xFFBDBDBD)
    val Strategy1 = Color(0xFF8B5CF6)
    val Strategy2 = Color(0xFF03DAC6)
    val Strategy3 = Color(0xFFFFB74D)
}

object DexLoopSpacing {
    val ExtraSmall = 4
    val Small = 8
    val Medium = 16
    val Large = 24
    val ExtraLarge = 32
}

enum class StatusType {
    Success, Warning, Error
}

@Composable
fun DexLoopApp(
    onCheckShizuku: () -> Unit,
    shizukuGranted: Boolean
) {
    // Check Strategy states
    val dexConfigRunning by StrategyManager.dexConfigurationStrategy.isRunning.collectAsState()
    val dexConfigStatus by StrategyManager.dexConfigurationStrategy.configurationStatus.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Handle system UI visibility
    androidx.compose.runtime.LaunchedEffect(dexConfigRunning) {
        val activity = context as? android.app.Activity
        activity?.let {
            val window = it.window
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)

            if (dexConfigRunning) {
                // Show bars in configuration mode
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            } else {
                // Show bars in menu
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    when {
        dexConfigRunning -> {
            // DeX Configuration Mode
            DexConfigurationUI(dexConfigStatus)
        }
        else -> {
            // Control Panel Mode
            DexLoopControlPanel(onCheckShizuku, shizukuGranted)
        }
    }
}

@Composable
fun DexLoopControlPanel(
    onCheckShizuku: () -> Unit,
    shizukuGranted: Boolean
) {
    var showSettings by remember { mutableStateOf(false) }
    val currentConfig by StrategyManager.displayConfig.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = androidx.compose.ui.graphics.Color.White
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "DexLoop Control Center",
                style = MaterialTheme.typography.h4
            )

            // Status Overview
            Card(elevation = 2.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("System Status", style = androidx.compose.material.MaterialTheme.typography.h6)

                    // Shizuku Status
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("Shizuku: ", style = androidx.compose.material.MaterialTheme.typography.body1)
                        Text(
                            text = if (shizukuGranted) "Granted" else "Not Granted",
                            color = if (shizukuGranted) DexLoopColors.Success else DexLoopColors.Error
                        )
                    }

                    // Overlay Permission
                    val hasOverlayPermission = androidx.compose.runtime.remember {
                        android.provider.Settings.canDrawOverlays(context)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("Overlay: ", style = androidx.compose.material.MaterialTheme.typography.body1)
                        Text(
                            text = if (hasOverlayPermission) "Granted" else "Not Granted",
                            color = if (hasOverlayPermission) DexLoopColors.Success else DexLoopColors.Warning
                        )
                    }
                }
            }

            // Display Configuration
            Card(elevation = 2.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Display Settings", style = androidx.compose.material.MaterialTheme.typography.h6)
                    Text("${currentConfig.width}x${currentConfig.height} @ ${currentConfig.density}dpi")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showSettings = true }) {
                        Text("Configure Resolution")
                    }
                }
            }

            // Permissions
            Card(elevation = 2.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Configuration", style = androidx.compose.material.MaterialTheme.typography.h6)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (!shizukuGranted) {
                        Button(onClick = onCheckShizuku) {
                            Text("Check/Request Shizuku")
                        }
                    }

                    val hasOverlayPermission = androidx.compose.runtime.remember {
                        android.provider.Settings.canDrawOverlays(context)
                    }
                    if (!hasOverlayPermission) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }) {
                            Text("Grant Overlay Permission")
                        }
                    }
                }
            }

            Text("Strategies", style = androidx.compose.material.MaterialTheme.typography.h5)

            // DeX Configuration Strategy
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = StrategyManager.dexConfigurationStrategy.name,
                        style = MaterialTheme.typography.h6,
                        color = DexLoopColors.Strategy1
                    )
                    Text(
                        text = StrategyManager.dexConfigurationStrategy.description,
                        style = MaterialTheme.typography.body2
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val dexConfigRunning by StrategyManager.dexConfigurationStrategy.isRunning.collectAsState()
                    val dexConfigStatus by StrategyManager.dexConfigurationStrategy.configurationStatus.collectAsState()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                dexConfigRunning -> "Running"
                                dexConfigStatus is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.Failed -> "Failed"
                                dexConfigStatus is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.Configured -> "Configured"
                                else -> "Ready"
                            },
                            color = when {
                                dexConfigRunning -> DexLoopColors.Running
                                dexConfigStatus is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.Failed -> DexLoopColors.Error
                                dexConfigStatus is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.Configured -> DexLoopColors.Success
                                else -> DexLoopColors.Stopped
                            }
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (dexConfigRunning) {
                                OutlinedButton(onClick = {
                                    scope.launch { StrategyManager.dexConfigurationStrategy.stop() }
                                }) {
                                    Text("STOP")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        scope.launch { StrategyManager.dexConfigurationStrategy.start() }
                                    },
                                    enabled = shizukuGranted
                                ) {
                                    Text("START")
                                }
                            }
                        }
                    }
                }
            }

            // Diagnostics
            Card(elevation = 2.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Diagnostics", style = MaterialTheme.typography.h6)
                    Button(onClick = {
                        val diagnosticsManager = com.antigravity.dexloop.diagnostics.DiagnosticsManager
                        diagnosticsManager.shareLogs(context)
                    }) {
                        Text("Export Logs")
                    }
                }
            }
        }

        // Settings Dialog
        if (showSettings) {
            com.antigravity.dexloop.ui.SettingsDialog(
                currentConfig = currentConfig,
                onDismiss = { showSettings = false },
                onSave = { w, h, d ->
                    StrategyManager.updateConfig(w, h, d)
                    showSettings = false
                }
            )
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.body1)
        Text(
            text = value,
            color = color
        )
    }
}

@Composable
private fun SimpleStrategyCard(
    strategy: com.antigravity.dexloop.strategies.Strategy,
    enabled: Boolean,
    strategyColor: androidx.compose.ui.graphics.Color
) {
    val isRunning by strategy.isRunning.collectAsState()
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = strategy.name, style = MaterialTheme.typography.h6, color = strategyColor)
                    Text(text = strategy.description, style = MaterialTheme.typography.body2)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRunning) "RUNNING" else "STOPPED",
                    color = if (isRunning) DexLoopColors.Success else DexLoopColors.Stopped
                )

                androidx.compose.material.Button(
                    onClick = {
                        scope.launch {
                            if (isRunning) {
                                strategy.stop()
                            } else {
                                strategy.start()
                            }
                        }
                    },
                    enabled = enabled,
                    colors = androidx.compose.material.ButtonDefaults.buttonColors(
                        backgroundColor = if (isRunning) androidx.compose.ui.graphics.Color.Red else strategyColor
                    )
                ) {
                    Text(if (isRunning) "STOP" else "START")
                }
            }
        }
    }
}

@Composable
private fun StatusOverviewCard(shizukuGranted: Boolean, context: android.content.Context) {
    val hasOverlayPermission = androidx.compose.runtime.remember {
        android.provider.Settings.canDrawOverlays(context)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(DexLoopSpacing.Medium.dp),
            verticalArrangement = Arrangement.spacedBy(DexLoopSpacing.Medium.dp)
        ) {
            Text(
                text = "System Status",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface
            )

            // Shizuku Status
            StatusRow(
                label = "Shizuku",
                value = if (shizukuGranted) "Granted" else "Not Granted",
                color = if (shizukuGranted) DexLoopColors.Success else DexLoopColors.Error
            )

            // Overlay Permission Status
            StatusRow(
                label = "Overlay Permission",
                value = if (hasOverlayPermission) "Granted" else "Not Granted",
                color = if (hasOverlayPermission) DexLoopColors.Success else DexLoopColors.Warning
            )

            // Performance Status
            val performanceLevel = com.antigravity.dexloop.strategies.PerformanceOptimizer.getRecommendedQuality()
            StatusRow(
                label = "Performance",
                value = performanceLevel.name,
                color = when (performanceLevel) {
                    com.antigravity.dexloop.strategies.PerformanceOptimizer.QualityLevel.HIGH -> DexLoopColors.Success
                    com.antigravity.dexloop.strategies.PerformanceOptimizer.QualityLevel.MEDIUM -> DexLoopColors.Warning
                    com.antigravity.dexloop.strategies.PerformanceOptimizer.QualityLevel.LOW -> DexLoopColors.Error
                }
            )
        }
    }
}

@Composable
fun DexConfigurationUI(status: com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "DeX Configuration",
            style = MaterialTheme.typography.h4,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                    Text("Configuration Status", style = MaterialTheme.typography.h6)

                when (status) {
                    is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.Idle -> {
                        Column {
                            Text("Ready to configure", color = DexLoopColors.Info)
                            Text("Click Start to begin DeX configuration", style = MaterialTheme.typography.body2)
                        }
                    }
                    is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.CheckingPrerequisites -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Checking prerequisites...")
                        }
                    }
                    is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.PrerequisitesFailed -> {
                        Column {
                            Text("Prerequisites Check Failed", color = DexLoopColors.Error, style = MaterialTheme.typography.h6)
                            Text(status.reason, color = DexLoopColors.Error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Please ensure:", style = MaterialTheme.typography.body2)
                            Text("• Shizuku app is installed and running", style = MaterialTheme.typography.body2)
                            Text("• Shizuku permission is granted", style = MaterialTheme.typography.body2)
                            Text("• Device is running Android 7.0+", style = MaterialTheme.typography.body2)
                        }
                    }
                    is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.ConfiguringSettings -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Configuring system settings...")
                        }
                    }
                    is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.SettingsFailed -> {
                        Column {
                            Text("Settings Configuration Failed", color = DexLoopColors.Error, style = MaterialTheme.typography.h6)
                            Text(status.reason, color = DexLoopColors.Error)
                            if (status.partialSuccess) {
                                Text("Some settings were configured successfully. You can try again or proceed with manual DeX activation.", color = DexLoopColors.Warning)
                            }
                        }
                    }
                    is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.DetectingDisplays -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Detecting connected displays...")
                        }
                    }
                    is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.DisplaysDetected -> {
                        Column {
                            Text("External Display Detected!", color = DexLoopColors.Success, style = MaterialTheme.typography.h6)
                            Text("Display: ${status.displayInfo}", color = DexLoopColors.Success)
                            Text("Samsung DeX should prompt for activation. If not, use manual activation below.")
                        }
                    }
                    is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.ReadyForConnection -> {
                        Column {
                            Text("System Configured Successfully!", color = DexLoopColors.Success, style = MaterialTheme.typography.h6)
                            Text("No external displays detected yet.", color = DexLoopColors.Info)
                            Text("Connect HDMI, USB-C, or wireless display to trigger DeX mode.")
                        }
                    }
                    is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.Configured -> {
                        Column {
                            Text("Configuration Complete!", color = DexLoopColors.Success, style = MaterialTheme.typography.h6)
                            Text("Display detected: ${status.displayInfo}", color = DexLoopColors.Success)
                            Text("Samsung DeX should prompt for activation when ready.")
                        }
                    }
                    is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.Failed -> {
                        Column {
                            Text("Configuration Failed", color = DexLoopColors.Error, style = MaterialTheme.typography.h6)
                            Text(status.reason, color = DexLoopColors.Error)
                            if (status.canRetry) {
                                Text("You can try again or check the troubleshooting guide.", style = MaterialTheme.typography.body2)
                            }
                        }
                    }
                }
            }
        }

        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Setup Instructions", style = MaterialTheme.typography.h6)
                Spacer(modifier = Modifier.height(8.dp))

                // Dynamic instructions based on current status
                when (status) {
                    is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.Idle -> {
                        Text("1. Click 'Start Configuration' to begin setup")
                        Text("2. The app will check prerequisites and configure your device")
                        Text("3. Connect external display when prompted")
                    }
                    is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.PrerequisitesFailed -> {
                        Text("Troubleshooting:", style = MaterialTheme.typography.h6, color = DexLoopColors.Warning)
                        Text("• Install and start Shizuku app")
                        Text("• Grant Shizuku permission when prompted")
                        Text("• Ensure device is Samsung with Android 7.0+")
                        Text("• Restart the app after fixing issues")
                    }
                    is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.ReadyForConnection -> {
                        Text("1. Connect external display (HDMI, USB-C, wireless)")
                        Text("2. Samsung should prompt to enter DeX mode")
                        Text("3. If no prompt appears, try manual activation below")
                        Text("4. DeX will launch automatically when display is detected")
                    }
                    is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.DisplaysDetected,
                    is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.Configured -> {
                        Text("Display detected! DeX should prompt for activation.")
                        Text("If DeX doesn't start automatically:")
                        Text("• Check if Samsung DeX is enabled in Settings")
                        Text("• Try the manual activation button below")
                        Text("• Some devices may need a restart after configuration")
                    }
                    else -> {
                        Text("Configuration in progress...")
                        Text("Please wait while settings are applied.")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch {
                            StrategyManager.dexConfigurationStrategy.attemptDexActivation()
                        }
                    },
                    enabled = status is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.ReadyForConnection ||
                             status is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.DisplaysDetected ||
                             status is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.Configured
                ) {
                    Text("Attempt Manual DeX Activation")
                }
            }
        }

        // Error-specific actions
        if (status is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.PrerequisitesFailed ||
            status is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.SettingsFailed ||
            status is com.antigravity.dexloop.strategies.DexConfigurationStrategy.ConfigurationStatus.Failed) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            StrategyManager.dexConfigurationStrategy.resetForRetry()
                            StrategyManager.dexConfigurationStrategy.start()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Retry Configuration")
                }

                Button(
                    onClick = {
                        scope.launch {
                            StrategyManager.dexConfigurationStrategy.stop()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Return to Menu")
                }
            }
        }

        // Standard action buttons (when not in error state)
        else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            StrategyManager.dexConfigurationStrategy.refreshDisplayDetection()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Refresh Displays")
                }

                Button(
                    onClick = {
                        scope.launch {
                            StrategyManager.dexConfigurationStrategy.stop()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop Configuration")
                }
            }
        }
    }
}

