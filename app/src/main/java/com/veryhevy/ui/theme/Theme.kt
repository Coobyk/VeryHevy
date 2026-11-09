package com.veryhevy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkScheme = darkColorScheme(
    primary = Volt,
    onPrimary = IronBg,
    primaryContainer = VoltContainerDark,
    onPrimaryContainer = VoltOnContainerDark,
    secondary = IronInkMuted,
    onSecondary = IronBg,
    secondaryContainer = IronSurfaceVariant,
    onSecondaryContainer = IronInk,
    tertiary = TealDark,
    onTertiary = IronBg,
    background = IronBg,
    onBackground = IronInk,
    surface = IronSurface,
    onSurface = IronInk,
    surfaceVariant = IronSurfaceVariant,
    onSurfaceVariant = IronInkMuted,
    surfaceContainerLowest = IronBg,
    surfaceContainerLow = IronSurface,
    surfaceContainer = IronSurface,
    surfaceContainerHigh = IronSurfaceHigh,
    surfaceContainerHighest = IronSurfaceVariant,
    outline = IronOutline,
    outlineVariant = IronOutlineSoft,
    error = ErrorDark,
    onError = IronBg,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    inverseSurface = IronInk,
    inverseOnSurface = IronSurfaceHigh,
    inversePrimary = VoltDeep,
    surfaceTint = Volt,
)

private val LightScheme = lightColorScheme(
    primary = VoltDeep,
    onPrimary = PaperSurface,
    primaryContainer = VoltContainerLight,
    onPrimaryContainer = VoltOnContainerLight,
    secondary = PaperInkMuted,
    onSecondary = PaperSurface,
    secondaryContainer = PaperSurfaceVariant,
    onSecondaryContainer = PaperInk,
    tertiary = TealLight,
    onTertiary = PaperSurface,
    background = PaperBg,
    onBackground = PaperInk,
    surface = PaperSurface,
    onSurface = PaperInk,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = PaperInkMuted,
    surfaceContainerLowest = PaperBg,
    surfaceContainerLow = PaperBg,
    surfaceContainer = PaperSurface,
    surfaceContainerHigh = PaperSurfaceHigh,
    surfaceContainerHighest = PaperSurfaceVariant,
    outline = PaperOutline,
    outlineVariant = PaperOutlineSoft,
    error = ErrorLight,
    onError = PaperSurface,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    inverseSurface = PaperInk,
    inverseOnSurface = PaperBg,
    inversePrimary = Volt,
    surfaceTint = VoltDeep,
)

private val VhShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun VeryHevyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = VhTypography,
        shapes = VhShapes,
        content = content,
    )
}
