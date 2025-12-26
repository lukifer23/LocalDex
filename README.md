# DexLoop 🔄

**DexLoop** is an Android helper that streamlines the Samsung DeX configuration flow on compatible Samsung devices.

The app focuses on preparing the system for DeX activation through Shizuku-powered configuration, guiding you through prerequisites, display detection, and manual activation when necessary.

## 🚀 Features

### DeX Configuration & Management
*   **Prerequisite Checks**: Verifies device compatibility and Shizuku permission status before attempting configuration.
*   **System Configuration**: Applies DeX-friendly display settings using Shizuku when available.
*   **Display Detection**: Observes connected displays and surfaces connection guidance.
*   **Manual DeX Activation**: Provides a manual activation action when automatic prompts do not appear.

### System Integration
*   **Thread-Safe Architecture**: StrategyManager coordinates state updates with synchronization.
*   **State Persistence**: Display configuration and strategy status survive app restarts.
*   **Performance Optimization**: PerformanceOptimizer suggests quality levels for device conditions.
*   **Orientation Handling**: Adaptive configuration recommendations for portrait/landscape changes.

### Developer Tools
*   **Comprehensive Diagnostics**: System info, performance metrics, and troubleshooting guides.
*   **Structured Error Handling**: Error codes with actionable recovery suggestions.
*   **Dynamic DeX Detection**: Automatic discovery of DeX components across OneUI versions.
*   **Integration Checks**: Instrumentation coverage for the DeX configuration lifecycle.

## 🛠 Prerequisites

### Required
1.  **Samsung Galaxy Z Fold** series (Fold 2/3/4/5/6) or compatible Samsung device.
2.  **Shizuku** (Required): Provides elevated privileges for system operations.
    *   Install [Shizuku from Play Store](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api).
    *   Start via **Wireless Debugging** (recommended) or Root access.
3.  **System Overlay Permission**: Needed for showing manual activation prompts when requested by the app.

### Recommended
- Physical keyboard/mouse for optimal desktop experience.
- Android 11+ for best compatibility.
- OneUI 5.1+ for enhanced DeX support.

## 📖 User Guide

### Initial Setup
1. Install DexLoop APK.
2. Launch Shizuku service.
3. Open DexLoop and grant Shizuku permission.
4. Start DeX Configuration to prepare system settings.

### Using DeX Configuration
1. Tap "START" on the DeX Configuration strategy card.
2. The app will check prerequisites and configure system settings.
3. Connect an external display (HDMI, USB-C, or wireless).
4. Samsung DeX should automatically prompt for activation.
5. If no prompt appears, use "Attempt Manual DeX Activation".

### DeX Configuration Flow
1. Open **DexLoop Control Center** and confirm Shizuku is running and granted.
2. Grant **Display over other apps** if prompted (required for manual activation surfaces).
3. Tap **START** on the **DeX Configuration & Setup** card to begin checks and system configuration.
4. Connect an external display (HDMI, USB-C, or wireless) and watch for the detection status to update.
5. If Samsung DeX does not prompt automatically, use **Attempt Manual DeX Activation** from the configuration screen.

### Configuration
- **Display Settings**: Customize resolution, density, and orientation.
- **Performance**: Battery-aware operation modes.
- **Diagnostics**: Export logs and system information.
- **State Persistence**: Settings survive app restarts.

## ⚠️ Limitations & Troubleshooting

### Known Limitations
*   **Input Latency**: Some configuration commands rely on shell execution, which may introduce delays on certain devices.
*   **Samsung Restrictions**: OneUI updates can change DeX activation behavior; manual activation may still be required.
*   **Hardware Dependencies**: Optimized for Galaxy Z Fold series.

### Common Issues & Solutions

#### Shizuku Connection Failed
- Ensure Shizuku app is running
- Restart Shizuku service
- Check wireless debugging permissions

#### Overlay Permission Denied
- Grant "Display over other apps" in system settings
- Restart DexLoop after granting permission

#### DeX Not Activating
- Re-run DeX Configuration to ensure settings applied
- Use the manual activation button after connecting a display
- Check device compatibility and OneUI version
- Verify Developer Options are enabled

#### Performance Issues
- Close background applications
- Check battery optimization settings
- Monitor memory usage in diagnostics

## 🏗️ Architecture

### Core Components
- **StrategyManager**: Coordinates the single DeX configuration strategy and persists display settings.
- **DexConfigurationStrategy**: Performs prerequisite checks, system configuration, display detection, and manual DeX activation triggers.
- **PerformanceOptimizer**: Provides device-aware quality recommendations used by the UI.
- **DiagnosticsManager**: Collects logs and generates shareable diagnostic reports.
- **ShizukuHelper**: Handles permission checks and requests for privileged operations.

### Design Patterns
- **Strategy Pattern**: Encapsulates the DeX configuration routine behind a strategy interface.
- **Observer Pattern**: Uses Flow/StateFlow to drive UI state.
- **Singleton Pattern**: Central managers shared across the app lifecycle.

## 🧪 Testing

Current integration coverage focuses on the DeX configuration flow:

```bash
# Instrumentation tests
./gradlew connectedDebugAndroidTest

# Unit tests (none defined yet, command kept for consistency)
./gradlew testDebugUnitTest
```

### What’s Verified
- StrategyManager initialization and configuration persistence
- DexConfigurationStrategy start/stop lifecycle calls
- UI presence for the configuration controls
- Diagnostics report generation

## 👨‍💻 Development

Built with **Kotlin**, **Jetpack Compose**, and **Coroutines**.

### Build Commands
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install debug APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Run tests
./gradlew test

# Generate documentation
./gradlew dokkaHtml
```

### Project Structure
```
app/src/main/java/com/antigravity/dexloop/
├── DexLoopApp.kt               # Compose UI
├── MainActivity.kt             # Entry point and Shizuku wiring
├── diagnostics/DiagnosticsManager.kt
├── shizuku/ShizukuHelper.kt
├── strategies/
│   ├── DexConfigurationStrategy.kt
│   ├── PerformanceOptimizer.kt
│   ├── Strategy.kt
│   └── StrategyManager.kt
└── ui/SettingsUI.kt            # Resolution configuration dialog

app/src/androidTest/java/com/antigravity/dexloop/
└── DexLoopIntegrationTest.kt   # Integration tests for configuration flow
```

### Key Dependencies
- **Shizuku API**: Privileged system access
- **Jetpack Compose**: Modern UI framework
- **Coroutines**: Asynchronous operations
- **Flow**: Reactive state management

## 📊 Performance

### Optimization Features
- **Battery Monitoring**: Reduces performance on low battery
- **Memory Management**: Automatic cleanup and GC triggering
- **Thread Pool Optimization**: Adaptive thread allocation
- **Background Processing**: Low-priority task scheduling

### System Requirements
- **Minimum**: Android API 30 (Android 11)
- **Recommended**: Android API 33+ (Android 13)
- **RAM**: 4GB minimum, 8GB recommended
- **Storage**: 100MB free space

## 🤝 Contributing

### Development Setup
1. Clone repository
2. Import project in Android Studio
3. Install Shizuku for testing
4. Run `./gradlew test` to verify setup

### Code Standards
- Kotlin coding conventions
- Comprehensive error handling
- Unit test coverage for new features
- Documentation for public APIs

### Testing Guidelines
- Unit tests for business logic
- Integration tests for UI flows
- Performance tests for critical paths
- Compatibility tests across Android versions

## 📄 License

This project is developed for educational and research purposes. Commercial use requires permission.

---

**DexLoop** - Bringing Samsung DeX to internal displays.
