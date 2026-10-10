package app.lotus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LotusGreenDark,
    onPrimary = LotusCream,
    primaryContainer = LotusGreenLight,
    onPrimaryContainer = LotusDeep,
    secondary = LotusPinkDark,
    onSecondary = LotusCream,
    secondaryContainer = LotusPinkLight,
    onSecondaryContainer = LotusDeep,
    tertiary = LotusAccent,
    background = ColorLightBackground,
    onBackground = ColorLightText,
    surface = ColorLightSurface,
    onSurface = ColorLightText,
    surfaceVariant = ColorLightVariant,
    onSurfaceVariant = ColorLightMuted,
    outline = ColorLightOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = LotusGreen,
    onPrimary = LotusDeep,
    primaryContainer = LotusGreenDark,
    onPrimaryContainer = LotusCream,
    secondary = LotusPink,
    onSecondary = LotusDeep,
    secondaryContainer = Color(0xFF39262D),
    onSecondaryContainer = LotusPinkLight,
    tertiary = LotusAccent,
    background = LotusSurfaceDark,
    onBackground = LotusCream,
    surface = LotusSurface,
    onSurface = LotusCream,
    surfaceVariant = LotusSurfaceRaised,
    onSurfaceVariant = LotusTextSecondary,
    outline = LotusDivider
)

private val ColorLightBackground = Color(0xFFF4F6F2)
private val ColorLightSurface = Color(0xFFFFFFFF)
private val ColorLightVariant = Color(0xFFE9EEE9)
private val ColorLightText = Color(0xFF18221B)
private val ColorLightMuted = Color(0xFF657168)
private val ColorLightOutline = Color(0xFFD4DDD5)

@Composable
fun LotusTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LotusTypography,
        content = content
    )
}
