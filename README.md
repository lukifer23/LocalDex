# DexLoop 🔄

**DexLoop** is a production-ready Android application designed to **configure and manage Samsung DeX** mode on compatible Samsung devices.

Unlike traditional approaches that simulate DeX, DexLoop properly configures the Android system to enable genuine Samsung DeX functionality, ensuring compatibility and reliability.

## 🚀 Features

### DeX Configuration & Management
*   **System Configuration**: Properly configures Android system settings for DeX compatibility
*   **Display Detection**: Automatically detects and validates connected external displays
*   **Prerequisites Checking**: Validates device compatibility and required permissions
*   **Manual DeX Activation**: Provides fallback activation methods when automatic detection fails

### System Integration
*   **Thread-Safe Architecture**: Concurrent operation support with proper synchronization.
*   **State Persistence**: Configuration and strategy states survive app restarts.
*   **Performance Optimization**: Battery-aware operations with memory management.
*   **Orientation Handling**: Automatic adaptive configurations for device rotation.
*   **Background Services**: Foreground service support for continuous display management.

### Developer Tools
*   **Comprehensive Diagnostics**: System info, performance metrics, and troubleshooting guides.
*   **Structured Error Handling**: Error codes with actionable recovery suggestions.
*   **Dynamic DeX Detection**: Automatic discovery of DeX components across OneUI versions.
*   **Testing Framework**: Unit and integration tests for all core components.

## 🛠 Prerequisites

### Required
1.  **Samsung Galaxy Z Fold** series (Fold 2/3/4/5/6) or compatible Samsung device.
2.  **Shizuku** (Required): Provides elevated privileges for system operations.
    *   Install [Shizuku from Play Store](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api).
    *   Start via **Wireless Debugging** (recommended) or Root access.
3.  **System Overlay Permission**: Required for Strategy 1 overlay display simulation.

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

### Strategy Selection

#### Strategy 1: Overlay Display (Recommended)
**Best for native DeX experience**
1. Select "Strategy 1: Overlay Display".
2. Grant overlay permission if prompted.
3. Screen may flicker during activation.
4. DeX should activate automatically on OneUI 5.1+.
5. Use Quick Settings DeX toggle if needed.

#### Strategy 2: Virtual Display
**Best for contained DeX experience**
1. Select "Strategy 2: App VirtualDisplay".
2. App creates virtual display environment.
3. DeX launches within the virtual display.
4. Full input support via touch/keyboard.
5. Audio routes to virtual display.

#### Strategy 3: Desktop Shell
**Fallback when DeX unavailable**
1. Select "Strategy 3: Desktop Shell".
2. Custom desktop environment launches.
3. Use taskbar to access applications.
4. Apps launch in freeform windows.
5. Full window management support.

### Configuration
- **Display Settings**: Customize resolution, density, and orientation.
- **Performance**: Battery-aware operation modes.
- **Diagnostics**: Export logs and system information.
- **State Persistence**: Settings survive app restarts.

## ⚠️ Limitations & Troubleshooting

### Known Limitations
*   **Input Latency**: Shell-based input injection may have slight delays (improved with direct injection where available).
*   **Samsung Restrictions**: Newer OneUI versions may block DeX on virtual displays (mitigated by multiple strategies).
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
- Try different strategies in order
- Check device compatibility
- Update to latest OneUI version
- Verify Developer Options are enabled

#### Performance Issues
- Close background applications
- Check battery optimization settings
- Monitor memory usage in diagnostics

## 🏗️ Architecture

### Core Components
- **StrategyManager**: Thread-safe strategy orchestration and state management.
- **VirtualDisplayStrategy**: Virtual display creation with surface management.
- **InputInjector**: Multi-modal input handling with reflection and shell fallbacks.
- **AudioRouter**: Intelligent audio channel management.
- **PerformanceOptimizer**: Battery and resource-aware operation management.
- **DiagnosticsManager**: Comprehensive logging and error reporting.

### Design Patterns
- **Strategy Pattern**: Pluggable DeX activation methods.
- **Observer Pattern**: Reactive state management with Flow.
- **Factory Pattern**: Dynamic component instantiation.
- **Singleton Pattern**: Thread-safe shared resources.

## 🧪 Testing

Comprehensive test suite included:

```bash
# Unit tests
./gradlew testDebugUnitTest

# Integration tests
./gradlew connectedDebugAndroidTest

# All tests
./gradlew test
```

### Test Coverage
- Strategy lifecycle management
- Input injection mechanisms
- Diagnostic functionality
- Performance optimization
- UI component interaction

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
├── strategies/          # Core strategy implementations
│   ├── StrategyManager.kt
│   ├── VirtualDisplayStrategy.kt
│   ├── OverlayDisplayStrategy.kt
│   ├── DesktopShellStrategy.kt
│   ├── InputInjector.kt
│   ├── AudioRouter.kt
│   └── PerformanceOptimizer.kt
├── diagnostics/         # Logging and error handling
├── shizuku/            # Privileged operation wrappers
├── services/           # Background services
└── ui/                 # Compose UI components
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
