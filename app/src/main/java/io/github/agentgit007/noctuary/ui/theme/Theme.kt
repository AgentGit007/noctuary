/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package io.github.agentgit007.noctuary.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.agentgit007.noctuary.data.Palette
import io.github.agentgit007.noctuary.data.ThemeMode

val LocalAccents = staticCompositionLocalOf { AuroraNight.accents }
val LocalPaletteColors = staticCompositionLocalOf { AuroraNight }
val LocalIsNight = staticCompositionLocalOf { true }

private fun colorsFor(palette: Palette, night: Boolean): PaletteColors = when (palette) {
    Palette.AURORA -> if (night) AuroraNight else AuroraDay
    Palette.SUNSET -> if (night) SunsetNight else SunsetDay
    Palette.CASSETTE -> if (night) CassetteNight else CassetteDay
    Palette.KOSMOS -> if (night) KosmosNight else KosmosDay
}

/** Resolves whether the app is in night mode for a given setting. */
@Composable
fun isNight(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.NIGHT -> true
    ThemeMode.DAY -> false
}

@Composable
fun NoctuaryTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    palette: Palette = Palette.AURORA,
    content: @Composable () -> Unit,
) {
    val night = isNight(themeMode)
    val p = colorsFor(palette, night)

    val scheme = if (night) {
        darkColorScheme(
            primary = p.primary,
            onPrimary = p.onPrimary,
            primaryContainer = p.surfaceHigh,
            onPrimaryContainer = p.onBackground,
            secondary = p.accents.mint,
            onSecondary = p.onPrimary,
            tertiary = p.accents.blossom,
            onTertiary = p.onPrimary,
            background = p.background,
            onBackground = p.onBackground,
            surface = p.surface,
            onSurface = p.onBackground,
            surfaceVariant = p.surfaceHigh,
            onSurfaceVariant = p.onSurfaceMuted,
            outline = p.outline,
            outlineVariant = p.outline,
            error = p.accents.blossom,
            onError = p.onPrimary,
        )
    } else {
        lightColorScheme(
            primary = p.primary,
            onPrimary = p.onPrimary,
            primaryContainer = p.surfaceHigh,
            onPrimaryContainer = p.onBackground,
            secondary = p.accents.mint,
            onSecondary = p.onPrimary,
            tertiary = p.accents.blossom,
            onTertiary = p.onPrimary,
            background = p.background,
            onBackground = p.onBackground,
            surface = p.surface,
            onSurface = p.onBackground,
            surfaceVariant = p.surfaceHigh,
            onSurfaceVariant = p.onSurfaceMuted,
            outline = p.outline,
            outlineVariant = p.outline,
            error = p.accents.blossom,
            onError = p.onPrimary,
        )
    }

    CompositionLocalProvider(
        LocalAccents provides p.accents,
        LocalPaletteColors provides p,
        LocalIsNight provides night,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = if (p.poster) KosmosTypography else NoctuaryTypography,
            shapes = if (p.poster) PosterShapes else NoctuaryShapes,
            content = content,
        )
    }
}
