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

private val LightColorScheme = lightColorScheme(
  primary = MedicalTealPrimary,
  onPrimary = MedicalTealOnPrimary,
  primaryContainer = MedicalTealPrimaryContainer,
  onPrimaryContainer = MedicalTealOnPrimaryContainer,
  secondary = MedicalSecondary,
  onSecondary = MedicalOnSecondary,
  secondaryContainer = MedicalSecondaryContainer,
  onSecondaryContainer = MedicalOnSecondaryContainer,
  tertiary = MedicalTertiary,
  onTertiary = MedicalOnTertiary,
  tertiaryContainer = MedicalTertiaryContainer,
  onTertiaryContainer = MedicalOnTertiaryContainer,
  background = MedicalBackground,
  onBackground = MedicalOnBackground,
  surface = MedicalSurface,
  onSurface = MedicalOnSurface,
  surfaceVariant = MedicalSurfaceVariant,
  onSurfaceVariant = MedicalOnSurfaceVariant,
  error = StatusMissed,
  onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
  primary = MedicalTealDarkPrimary,
  onPrimary = MedicalTealDarkOnPrimary,
  primaryContainer = MedicalTealDarkPrimaryContainer,
  onPrimaryContainer = MedicalTealDarkOnPrimaryContainer,
  secondary = Color(0xFFB1CBD0),
  onSecondary = Color(0xFF1C3438),
  secondaryContainer = Color(0xFF334B4F),
  onSecondaryContainer = Color(0xFFCDE7EC),
  tertiary = Color(0xFFBAC6EA),
  onTertiary = Color(0xFF24304D),
  tertiaryContainer = Color(0xFF3B4664),
  onTertiaryContainer = Color(0xFFDAE2FF),
  background = Color(0xFF0E1415),
  onBackground = Color(0xFFE0E3E3),
  surface = Color(0xFF121A1B),
  onSurface = Color(0xFFE0E3E3),
  surfaceVariant = Color(0xFF3F484A),
  onSurfaceVariant = Color(0xFFBFC8CA),
  error = Color(0xFFFFB4AB),
  onError = Color(0xFF690005)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
