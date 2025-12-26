package com.antigravity.dexloop.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Simplified theme - using system defaults for now
@Composable
fun DexLoopTheme(
    content: @Composable () -> Unit
) {
    // For now, just use the system theme without custom overrides
    content()
}

// Status colors for consistent status indication
object DexLoopColors {
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFF9800)
    val Error = Color(0xFFF44336)
    val Info = Color(0xFF2196F3)

    val Running = Success
    val Stopped = Color(0xFF9E9E9E)
    val Loading = Info
    val Disabled = Color(0xFFBDBDBD)

    // Strategy-specific colors - hardcoded for compatibility
    val Strategy1 = Color(0xFF8B5CF6) // Purple - Overlay
    val Strategy2 = Color(0xFF03DAC6) // Teal - Virtual Display
    val Strategy3 = Color(0xFFFFB74D) // Orange - Desktop Shell
}

// Spacing system
object DexLoopSpacing {
    const val ExtraSmall = 4
    const val Small = 8
    const val Medium = 16
    const val Large = 24
    const val ExtraLarge = 32
}

// Border radius system
object DexLoopShapes {
    const val Small = 8
    const val Medium = 12
    const val Large = 16
    const val ExtraLarge = 24
}

// Common modifiers for consistency
object DexLoopModifiers {
    val CardPadding = Modifier.padding(16.dp)
    val ScreenPadding = Modifier.padding(16.dp)
    val ItemSpacing = Modifier.padding(vertical = 8.dp)
}
