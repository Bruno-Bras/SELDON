package com.seldon.fleet.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SeldonColorScheme = darkColorScheme(
    primary = SeldonColors.Healthy,
    onPrimary = SeldonColors.Void,
    primaryContainer = SeldonColors.Healthy.copy(alpha = 0.16f),
    onPrimaryContainer = SeldonColors.Healthy,
    secondary = SeldonColors.Warning,
    onSecondary = SeldonColors.Void,
    secondaryContainer = SeldonColors.Warning.copy(alpha = 0.16f),
    onSecondaryContainer = SeldonColors.Warning,
    tertiary = SeldonColors.Cyan,
    onTertiary = SeldonColors.Void,
    background = SeldonColors.Void,
    onBackground = SeldonColors.TextPrimary,
    surface = SeldonColors.Panel,
    onSurface = SeldonColors.TextPrimary,
    surfaceVariant = SeldonColors.PanelRaised,
    onSurfaceVariant = SeldonColors.TextSecondary,
    outline = SeldonColors.Line,
    outlineVariant = SeldonColors.Line,
    error = SeldonColors.Critical,
    onError = SeldonColors.Void,
    errorContainer = SeldonColors.Critical.copy(alpha = 0.14f),
    onErrorContainer = SeldonColors.Critical,
    scrim = SeldonColors.Void,
)

private val SeldonShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

@Composable
fun SeldonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SeldonColorScheme,
        typography = SeldonTypography,
        shapes = SeldonShapes,
        content = content,
    )
}
