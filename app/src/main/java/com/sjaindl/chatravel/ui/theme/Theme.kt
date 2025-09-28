package com.sjaindl.chatravel.ui.theme

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
    primary = Color(0xFF7AD0E3),
    onPrimary = Color(0xFF003641),
    primaryContainer = Color(0xFF074F5D),
    onPrimaryContainer = CT_PrimaryLight,
    secondary = Color(0xFFFFB4A4),
    onSecondary = Color(0xFF5A1406),
    secondaryContainer = Color(0xFF7A2B1C),
    onSecondaryContainer = CT_SecondaryLight,
    tertiary = Color(0xFFFFCA63),
    onTertiary = Color(0xFF3E2B00),
    tertiaryContainer = Color(0xFF5A4200),
    onTertiaryContainer = CT_TertiaryLight,
    background = CT_BackgroundDark,
    onBackground = CT_OnSurfaceDark,
    surface = CT_SurfaceDark,
    onSurface = CT_OnSurfaceDark,
    surfaceVariant = CT_SurfaceVariantD,
    onSurfaceVariant = CT_OnSurfVariantD,
    outline = Color(0xFF8E9199),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = CT_ErrorCont,
)

private val LightColorScheme = lightColorScheme(
    primary = CT_Primary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = CT_PrimaryLight,
    onPrimaryContainer = Color(0xFF001F27),
    secondary = CT_Secondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = CT_SecondaryLight,
    onSecondaryContainer = Color(0xFF360800),
    tertiary = CT_Tertiary,
    onTertiary = Color(0xFF311300),
    tertiaryContainer = CT_TertiaryLight,
    onTertiaryContainer = Color(0xFF231900),
    background = CT_BackgroundLight,
    onBackground = CT_OnSurfaceLight,
    surface = CT_SurfaceLight,
    onSurface = CT_OnSurfaceLight,
    surfaceVariant = CT_SurfaceVariantL,
    onSurfaceVariant = CT_OnSurfVariantL,
    outline = Color(0xFF73777F),
    error = CT_Error,
    onError = CT_OnError,
    errorContainer = CT_ErrorCont,
    onErrorContainer = CT_OnErrorCont,
)

@Composable
fun ChaTravelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
