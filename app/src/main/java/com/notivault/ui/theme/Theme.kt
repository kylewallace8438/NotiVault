package com.notivault.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightFallback = lightColorScheme(
    primary = Color(0xFF0B5D52),
    secondaryContainer = Color(0xFFD3ECE6),
)
private val DarkFallback = darkColorScheme(
    primary = Color(0xFF7FD3C3),
)

/** Money colors, readable on both light and dark backgrounds. */
object MoneyColors {
    fun income(dark: Boolean) = if (dark) Color(0xFF7BD88F) else Color(0xFF1E7B3A)
    fun expense(dark: Boolean) = if (dark) Color(0xFFFF8A80) else Color(0xFFB3261E)
}

@Composable
fun NotiVaultTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    // Uses your wallpaper colors on Android 12+ (HyperOS supports this).
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkFallback
        else -> LightFallback
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
