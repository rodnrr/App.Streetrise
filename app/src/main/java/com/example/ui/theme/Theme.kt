package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val HighDensityLightColorScheme = lightColorScheme(
  primary = HighDensityPrimary,
  onPrimary = HighDensityOnPrimary,
  primaryContainer = HighDensityPrimaryContainer,
  onPrimaryContainer = HighDensityOnPrimaryContainer,
  secondary = HighDensitySecondary,
  secondaryContainer = HighDensitySecondaryContainer,
  onSecondaryContainer = HighDensityOnSecondaryContainer,
  background = HighDensityBackground,
  onBackground = HighDensityOnBackground,
  surface = HighDensitySurface,
  onSurface = HighDensityOnSurface,
  surfaceVariant = HighDensitySurfaceVariant,
  onSurfaceVariant = HighDensityOnSurfaceVariant,
  outline = HighDensityOutline,
  error = HighDensityError
)

private val HighDensityDarkColorScheme = darkColorScheme(
  primary = HighDensityPrimaryDark,
  onPrimary = HighDensityOnPrimaryDark,
  primaryContainer = HighDensityPrimaryContainerDark,
  onPrimaryContainer = HighDensityOnPrimaryContainerDark,
  secondary = HighDensitySecondaryDark,
  secondaryContainer = HighDensitySecondaryContainerDark,
  onSecondaryContainer = HighDensityOnSecondaryContainerDark,
  background = HighDensityBackgroundDark,
  onBackground = HighDensityOnBackgroundDark,
  surface = HighDensitySurfaceDark,
  onSurface = HighDensityOnSurfaceDark,
  outline = HighDensityOutline,
  error = HighDensityError
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Set to false to prioritize our custom authentic "High Density" StreetRise colors
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) {
    HighDensityDarkColorScheme
  } else {
    HighDensityLightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
