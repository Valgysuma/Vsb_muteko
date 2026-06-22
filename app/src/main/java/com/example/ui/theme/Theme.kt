package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF4DD99B),
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF112D21), // Specialized deep rich green
    onPrimaryContainer = Color(0xFFC0EEDB),
    secondary = Color(0xFF4DD99B),
    onSecondary = Color(0xFF003822),
    background = Color(0xFF161616), // Underlining background
    onBackground = Color(0xFFE2E3DD),
    surface = Color(0xFF1E1E1E), // Slightly lighter floating surface
    onSurface = Color(0xFFE2E3DD),
    surfaceVariant = Color(0xFF262629),
    onSurfaceVariant = Color(0xFFC4C8BB),
    outline = Color(0xFF2E2E2E), // Thin dark borders
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
  )

private val LightColorScheme = DarkColorScheme // Enforce the gorgeous premium dark mode always for this app identity

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force premium dark theme by default
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

