package app.lotus.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = LotusPinkDark,
    onPrimary = LotusCream,
    primaryContainer = LotusPinkLight,
    onPrimaryContainer = LotusDeep,
    secondary = LotusGreen,
    onSecondary = LotusCream,
    secondaryContainer = LotusGreenLight,
    onSecondaryContainer = LotusDeep,
    tertiary = LotusAccent,
    background = LotusIvory,
    onBackground = LotusDeep,
    surface = LotusSurface,
    onSurface = LotusDeep,
    surfaceVariant = LotusCream,
    onSurfaceVariant = LotusMuted,
    outline = LotusMuted.copy(alpha = 0.4f)
)

private val DarkColorScheme = darkColorScheme(
    primary = LotusPink,
    onPrimary = LotusDeep,
    primaryContainer = LotusPinkDark,
    onPrimaryContainer = LotusCream,
    secondary = LotusGreenLight,
    onSecondary = LotusDeep,
    secondaryContainer = LotusGreenDark,
    onSecondaryContainer = LotusCream,
    tertiary = LotusAccent,
    background = LotusSurfaceDark,
    onBackground = LotusCream,
    surface = LotusSurfaceDark,
    onSurface = LotusCream,
    surfaceVariant = LotusDeep,
    onSurfaceVariant = LotusPinkLight,
    outline = LotusMuted.copy(alpha = 0.5f)
)

@Composable
fun LotusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // пока отключаем dynamic для единого стиля
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
        typography = LotusTypography,
        content = content
    )
}
