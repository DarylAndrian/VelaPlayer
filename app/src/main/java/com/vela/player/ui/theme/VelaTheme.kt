package com.vela.player.ui.theme

import android.app.Activity
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light");

    companion object {
        fun fromStored(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: DARK
    }
}

val LocalVelaColors = staticCompositionLocalOf { VelaDarkColors }
val LocalVelaTypography = staticCompositionLocalOf { VelaTypography() }
val LocalReducedMotion = staticCompositionLocalOf { false }

object VelaTheme {
    val colors: VelaColors
        @Composable get() = LocalVelaColors.current

    val typography: VelaTypography
        @Composable get() = LocalVelaTypography.current

    val reducedMotion: Boolean
        @Composable get() = LocalReducedMotion.current
}

@Composable
fun VelaTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    forceSystemBarContrast: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val velaColors = if (useDark) VelaDarkColors else VelaLightColors
    val velaTypography = remember { VelaTypography() }

    val context = LocalContext.current
    val reducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }

    val view = LocalView.current
    if (forceSystemBarContrast && !view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !useDark
                    isAppearanceLightNavigationBars = !useDark
                }
            }
        }
    }

    val materialScheme = if (useDark) {
        darkColorScheme(
            primary = velaColors.actionPrimaryBg,
            onPrimary = velaColors.actionPrimaryFg,
            secondary = velaColors.brandHover,
            onSecondary = velaColors.actionPrimaryFg,
            background = velaColors.bgCanvas,
            onBackground = velaColors.textPrimary,
            surface = velaColors.bgSurface,
            onSurface = velaColors.textPrimary,
            surfaceVariant = velaColors.bgSubtle,
            onSurfaceVariant = velaColors.textSecondary,
            outline = velaColors.borderInput,
            error = velaColors.statusDanger.fg
        )
    } else {
        lightColorScheme(
            primary = velaColors.actionPrimaryBg,
            onPrimary = velaColors.actionPrimaryFg,
            secondary = velaColors.brandPressed,
            onSecondary = velaColors.actionPrimaryFg,
            background = velaColors.bgCanvas,
            onBackground = velaColors.textPrimary,
            surface = velaColors.bgSurface,
            onSurface = velaColors.textPrimary,
            surfaceVariant = velaColors.bgBase,
            onSurfaceVariant = velaColors.textSecondary,
            outline = velaColors.borderInput,
            error = velaColors.statusDanger.fg
        )
    }

    val materialTypography = Typography(
        displaySmall = velaTypography.display,
        headlineMedium = velaTypography.page,
        titleLarge = velaTypography.section,
        titleMedium = velaTypography.cardTitle,
        bodyLarge = velaTypography.body,
        bodyMedium = velaTypography.bodySmall,
        labelLarge = velaTypography.label,
        labelSmall = velaTypography.caption
    )

    CompositionLocalProvider(
        LocalVelaColors provides velaColors,
        LocalVelaTypography provides velaTypography,
        LocalReducedMotion provides reducedMotion
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = materialTypography,
            content = content
        )
    }
}
