package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val TrueCheckLightColorScheme = lightColorScheme(
    primary = DeepTeal,
    onPrimary = PureWhite,
    primaryContainer = SoftMint,
    onPrimaryContainer = DarkGreen,
    secondary = EmeraldGreen,
    onSecondary = PureWhite,
    secondaryContainer = LightMintBackground,
    onSecondaryContainer = MainText,
    tertiary = DarkGreen,
    onTertiary = PureWhite,
    background = PureWhite,
    onBackground = MainText,
    surface = PureWhite,
    onSurface = MainText,
    surfaceVariant = LightMintBackground,
    onSurfaceVariant = SecondaryText,
    outline = DividerMint,
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = ErrorRedSoft,
    onErrorContainer = ErrorRed
)

val TrueCheckShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TrueCheckTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TrueCheckLightColorScheme,
        typography = TrueCheckTypography,
        shapes = TrueCheckShapes,
        content = content
    )
}
