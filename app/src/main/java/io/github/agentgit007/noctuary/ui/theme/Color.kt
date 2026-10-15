/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package io.github.agentgit007.noctuary.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The accent set a palette contributes on top of the Material scheme.
 * Charts and the retro background read from here, so a palette switch
 * repaints the whole app including the statistics.
 */
@Immutable
data class Accents(
    val mint: Color,
    val lilac: Color,
    val blossom: Color,
    val amber: Color,
    val sky: Color,
    /** Horizon grid + sun used by the retro backdrop. */
    val horizon: Color,
    val sunTop: Color,
    val sunBottom: Color,
    val haze: Color,
) {
    /** Stable ordering used wherever a series needs N distinct colours. */
    val series: List<Color> get() = listOf(mint, lilac, blossom, amber, sky)
}

@Immutable
data class PaletteColors(
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val onBackground: Color,
    val onSurfaceMuted: Color,
    val outline: Color,
    val primary: Color,
    val onPrimary: Color,
    val accents: Accents,
    /**
     * Poster palettes (Kosmos) swap the glass-and-glow look for printed
     * paper: opaque cards, thick [ink] outlines, hard offset shadows and
     * pastel fills with dark text. Components branch on this flag.
     */
    val poster: Boolean = false,
    /** Outline / rule colour for poster palettes. */
    val ink: Color = outline,
    /** Hard offset shadow under poster cards. */
    val shadow: Color = Color.Transparent,
    /** Text on an [ink]-filled surface (the active nav cell). */
    val onInk: Color = background,
    /** Pastel fill per accent; null means "tint the accent". */
    val fills: Accents? = null,
    /** Text colour on a pastel fill. */
    val onFill: Color = onBackground,
) {
    /**
     * Background for something tagged with [accent]. Glass palettes tint the
     * accent itself; poster palettes look up the matching flat pastel, so
     * text on it can stay dark ink and remain readable.
     */
    fun fillFor(accent: Color, glassAlpha: Float = 0.18f): Color {
        val f = fills ?: return accent.copy(alpha = glassAlpha)
        return when (accent) {
            accents.mint -> f.mint
            accents.lilac -> f.lilac
            accents.blossom -> f.blossom
            accents.amber -> f.amber
            accents.sky -> f.sky
            else -> accent.copy(alpha = 0.3f)
        }
    }

    /** Text colour to use on top of [fillFor]. */
    fun onFillFor(accent: Color): Color = if (fills != null) onFill else accent
}

// ---------------------------------------------------------------- AURORA ----

val AuroraNight = PaletteColors(
    background = Color(0xFF0E0A1F),
    surface = Color(0xFF171233),
    surfaceHigh = Color(0xFF201A42),
    onBackground = Color(0xFFF2ECFF),
    onSurfaceMuted = Color(0xFFA79BD0),
    outline = Color(0xFF3A3166),
    primary = Color(0xFFC9B6FF),
    onPrimary = Color(0xFF221A3D),
    accents = Accents(
        mint = Color(0xFF9BF0E1),
        lilac = Color(0xFFC9B6FF),
        blossom = Color(0xFFFFB3D1),
        amber = Color(0xFFFFD9A0),
        sky = Color(0xFF9FD4FF),
        horizon = Color(0xFF6E5BA8),
        sunTop = Color(0xFFFFB3D1),
        sunBottom = Color(0xFFFFD9A0),
        haze = Color(0xFF2A2152),
    ),
)

val AuroraDay = PaletteColors(
    background = Color(0xFFF6F2FF),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFEDE6FF),
    onBackground = Color(0xFF241D45),
    onSurfaceMuted = Color(0xFF6D648F),
    outline = Color(0xFFD8CEF6),
    primary = Color(0xFF7A5BD6),
    onPrimary = Color(0xFFFFFFFF),
    accents = Accents(
        mint = Color(0xFF4FC4AE),
        lilac = Color(0xFF9A7BE8),
        blossom = Color(0xFFEF86B2),
        amber = Color(0xFFE5A54A),
        sky = Color(0xFF5BA8E8),
        horizon = Color(0xFFBCA9EC),
        sunTop = Color(0xFFEF86B2),
        sunBottom = Color(0xFFE5A54A),
        haze = Color(0xFFE6DCFF),
    ),
)

// ---------------------------------------------------------------- SUNSET ----

val SunsetNight = PaletteColors(
    background = Color(0xFF190E26),
    surface = Color(0xFF241536),
    surfaceHigh = Color(0xFF2F1C45),
    onBackground = Color(0xFFFFEFF4),
    onSurfaceMuted = Color(0xFFC2A1B6),
    outline = Color(0xFF4A2C63),
    primary = Color(0xFFFFB199),
    onPrimary = Color(0xFF34182B),
    accents = Accents(
        mint = Color(0xFF8FE3D1),
        lilac = Color(0xFFC7A6FF),
        blossom = Color(0xFFFF9EC4),
        amber = Color(0xFFFFC2A1),
        sky = Color(0xFF8FE3F5),
        horizon = Color(0xFF8E4D8C),
        sunTop = Color(0xFFFFC2A1),
        sunBottom = Color(0xFFFF9EC4),
        haze = Color(0xFF3B1F52),
    ),
)

val SunsetDay = PaletteColors(
    background = Color(0xFFFFF5F2),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFFFE8E0),
    onBackground = Color(0xFF3A1F33),
    onSurfaceMuted = Color(0xFF8A6076),
    outline = Color(0xFFF3D2C8),
    primary = Color(0xFFD4674E),
    onPrimary = Color(0xFFFFFFFF),
    accents = Accents(
        mint = Color(0xFF3FB8A0),
        lilac = Color(0xFF9B6FE0),
        blossom = Color(0xFFE2638F),
        amber = Color(0xFFE08A5B),
        sky = Color(0xFF3FA8C4),
        horizon = Color(0xFFE2A9BE),
        sunTop = Color(0xFFE08A5B),
        sunBottom = Color(0xFFE2638F),
        haze = Color(0xFFFFE0D4),
    ),
)

// -------------------------------------------------------------- CASSETTE ----

val CassetteNight = PaletteColors(
    background = Color(0xFF0D171B),
    surface = Color(0xFF14232A),
    surfaceHigh = Color(0xFF1C2F38),
    onBackground = Color(0xFFEFFAF7),
    onSurfaceMuted = Color(0xFF93B0B4),
    outline = Color(0xFF2C4752),
    primary = Color(0xFFA8E6CF),
    onPrimary = Color(0xFF10262B),
    accents = Accents(
        mint = Color(0xFFA8E6CF),
        lilac = Color(0xFFB9B6FF),
        blossom = Color(0xFFFFAAA6),
        amber = Color(0xFFFFF3C4),
        sky = Color(0xFFB5D8FF),
        horizon = Color(0xFF3E7A76),
        sunTop = Color(0xFFFFF3C4),
        sunBottom = Color(0xFFFFAAA6),
        haze = Color(0xFF1B3740),
    ),
)

val CassetteDay = PaletteColors(
    background = Color(0xFFF2F8F5),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFE2F0EA),
    onBackground = Color(0xFF17302F),
    onSurfaceMuted = Color(0xFF5E7B79),
    outline = Color(0xFFCADED7),
    primary = Color(0xFF2E8B74),
    onPrimary = Color(0xFFFFFFFF),
    accents = Accents(
        mint = Color(0xFF2E8B74),
        lilac = Color(0xFF7E7AE0),
        blossom = Color(0xFFE0736C),
        amber = Color(0xFFD9A62B),
        sky = Color(0xFF4E92D4),
        horizon = Color(0xFF9CC9BF),
        sunTop = Color(0xFFD9A62B),
        sunBottom = Color(0xFFE0736C),
        haze = Color(0xFFD8ECE4),
    ),
)

// ---------------------------------------------------------------- KOSMOS ----
//
// Retrofuturist, post-soviet, pastel: the paper and inks of 1960s space-race
// posters, Plattenbau facades and tram tickets. No political symbols, just
// the graphic vocabulary. Accents double as text colours, so on paper they
// are the deep inks (all ≥ 4.5:1 on the background, WCAG AA) and the pastels
// live in [PaletteColors.fills]; at dusk the pastels are light enough to be
// both.

private val KosmosPastels = Accents(
    mint = Color(0xFFA8D5C2),
    lilac = Color(0xFFC9BEE6),
    blossom = Color(0xFFEBA3A0),
    amber = Color(0xFFF0D48A),
    sky = Color(0xFFA9C7DD),
    horizon = Color(0xFFA59F91),
    sunTop = Color(0xFFEBA3A0),
    sunBottom = Color(0xFFEBA3A0),
    haze = Color(0xFFECE6D6),
)

val KosmosDay = PaletteColors(
    background = Color(0xFFECE6D6),
    surface = Color(0xFFF7F2E4),
    surfaceHigh = Color(0xFFC9C3B3),
    onBackground = Color(0xFF20343F),
    onSurfaceMuted = Color(0xFF56666A),
    outline = Color(0xFFBDB8AB),
    primary = Color(0xFF2F5878),
    onPrimary = Color(0xFFF7F2E4),
    accents = Accents(
        mint = Color(0xFF2F6B5A),
        lilac = Color(0xFF5B4E8C),
        blossom = Color(0xFF8F3A36),
        amber = Color(0xFF7A560C),
        sky = Color(0xFF2F5878),
        horizon = Color(0xFFA59F91),
        sunTop = Color(0xFFEBA3A0),
        sunBottom = Color(0xFFEBA3A0),
        haze = Color(0xFFECE6D6),
    ),
    poster = true,
    ink = Color(0xFF20343F),
    shadow = Color(0xFFBDB8AB),
    onInk = Color(0xFFA8D5C2),
    fills = KosmosPastels,
    onFill = Color(0xFF20343F),
)

val KosmosNight = PaletteColors(
    background = Color(0xFF14262E),
    surface = Color(0xFF1D3540),
    surfaceHigh = Color(0xFF0F1D23),
    onBackground = Color(0xFFE8E2D0),
    onSurfaceMuted = Color(0xFF9DB0B0),
    outline = Color(0xFF2A4856),
    primary = Color(0xFFA8D5C2),
    onPrimary = Color(0xFF14262E),
    accents = KosmosPastels.copy(
        horizon = Color(0xFF2A4856),
        haze = Color(0xFF14262E),
    ),
    poster = true,
    ink = Color(0xFFE8E2D0),
    shadow = Color(0xFF0B171C),
    onInk = Color(0xFF14262E),
    fills = KosmosPastels,
    onFill = Color(0xFF14262E),
)
