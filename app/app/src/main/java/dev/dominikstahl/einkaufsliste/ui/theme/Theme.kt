package dev.dominikstahl.einkaufsliste.ui.theme

import android.app.Activity
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

private val DarkColorScheme = darkColorScheme(
    primary = Zinc100,
    onPrimary = Zinc900,
    primaryContainer = Zinc800,
    onPrimaryContainer = Zinc100,
    secondary = Zinc200,
    onSecondary = Zinc900,
    background = Zinc950,
    onBackground = Zinc100,
    surface = Zinc900,
    onSurface = Zinc100,
    surfaceVariant = Zinc800,
    onSurfaceVariant = Zinc400,
    outline = Zinc800,
    outlineVariant = Zinc900,
    error = Red500,
    errorContainer = Red950,
    onErrorContainer = Red300
)

private val LightColorScheme = lightColorScheme(
    primary = Zinc900,
    onPrimary = Color.White,
    primaryContainer = Zinc100,
    onPrimaryContainer = Zinc900,
    secondary = Zinc800,
    onSecondary = Color.White,
    background = Zinc50,
    onBackground = Zinc900,
    surface = Color.White,
    onSurface = Zinc900,
    surfaceVariant = Zinc100,
    onSurfaceVariant = Zinc500,
    outline = Zinc200,
    outlineVariant = Zinc100,
    error = Red500,
    errorContainer = Red50,
    onErrorContainer = Red500
)

@Composable
fun EinkaufslisteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled by default to keep the custom brand colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}