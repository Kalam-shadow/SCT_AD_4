package com.example.qrnova.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.qrnova.ui.theme.ColorPalette.DarkColorScheme

object ColorPalette {
//    val Purple40 = Color(0xFF6650a4)
//    val PurpleGrey40 = Color(0xFF625b71)
//    val Pink40 = Color(0xFF7D5260)
//
//    val Purple80 = Color(0xFFD0BCFF)
//    val PurpleGrey80 = Color(0xFFCCC2DC)
//    val Pink80 = Color(0xFFEFB8C8)

    val Purple40 = Color(0xFF81D4FA)
    val PurpleGrey40 = Color(0xFF80DEEA)
    val Pink40 = Color(0xFF7D5260)

    val Purple80 = Color(0xFF03A9F4)
    val PurpleGrey80 = Color(0xFF00BCD4)
    val Pink80 = Color(0xFFEFB8C8)


//    val DarkBackground = Color(0xFF121212) // Dark gray background
//    val DarkSurface = Color(0xFF1E1E1E)
//    val LightBackground = Color(0xFFFFFFFF) // Light gray background
//    val LightSurface = Color(0xFFFAFAFA)
//    val LightPrimary = Color(0xFF4CAF50)
//    val DarkPrimary = Color(0xFFA5D6A7)
//    val OnLightPrimary = Color(0xFF000000)
//    val OnDarkPrimary = Color(0xFF000000)
//    val LightSecondary = Color(0xFF9575CD)
//    val DarkSecondary = Color(0xFFD1C4E9)

    val DarkBackground = Color(0xFF121212) // Same dark background
    val DarkSurface = Color(0xFF1E1E1E)     // Same dark surface

    val LightBackground = Color(0xFFFFFFFF) // Light background
    val LightSurface = Color(0xFFFAFAFA)    // Light surface

    val LightPrimary = Color(0xFF03A9F4)    // Sky Blue 500 (Light mode primary)
    val DarkPrimary = Color(0xFF81D4FA)     // Sky Blue 200 (Dark mode primary)

    val OnLightPrimary = Color(0xFFFFFFFF)  // White text/icons on blue
    val OnDarkPrimary = Color(0xFF000000)   // Black text/icons on lighter blue

    val LightSecondary = Color(0xFF00BCD4)  // Cyan 500 for accent
    val DarkSecondary = Color(0xFF80DEEA)   // Cyan 200 for dark mode accent


    val LightColorScheme = lightColorScheme(
        primary = LightPrimary,
        secondary = LightSecondary,
        tertiary = Pink40,
        background = LightBackground,
        surface = LightSurface,
        onPrimary = OnLightPrimary,
    )
    val DarkColorScheme = darkColorScheme(
        primary = DarkPrimary,
        secondary = DarkSecondary,
        tertiary = Pink80,
        background = DarkBackground,
        surface = DarkSurface,
        onPrimary = OnDarkPrimary,
    )
}

private val LocalCustomColors = staticCompositionLocalOf {
    ColorPalette.LightColorScheme
}

@Composable
fun QrnovaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = getColorScheme(darkTheme, dynamicColor)

    CompositionLocalProvider(LocalCustomColors provides colorScheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun getColorScheme(darkTheme: Boolean, dynamicColor: Boolean) : ColorScheme {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> ColorPalette.LightColorScheme
    }
    return colorScheme
}

object QrnovaTheme {
    val colors: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalCustomColors.current
}
