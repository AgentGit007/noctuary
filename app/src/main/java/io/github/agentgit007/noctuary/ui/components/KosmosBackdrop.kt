/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package io.github.agentgit007.noctuary.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.agentgit007.noctuary.ui.theme.LocalIsNight
import io.github.agentgit007.noctuary.ui.theme.LocalPaletteColors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The Kosmos backdrop: paper (or dusk) sky, a small planet with orbit rings
 * and a circling satellite in the top corner, and a row of prefab housing
 * blocks in front of a striped poster sun along the bottom. Windows light up
 * and go dark slowly, more of them at dusk. Everything is drawn on one
 * canvas from palette colours; no images.
 *
 * [skylineLift] raises the ground line above whatever bar sits at the
 * bottom of the screen; the system navigation-bar inset is added on top.
 */
@Composable
fun KosmosBackdrop(
    modifier: Modifier = Modifier,
    ruled: Boolean = true,
    intensity: Float = 1f,
    skylineLift: Dp = 0.dp,
) {
    val palette = LocalPaletteColors.current
    val night = LocalIsNight.current
    val fills = palette.fills ?: palette.accents
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val transition = rememberInfiniteTransition(label = "kosmos")
    val orbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "satellite",
    )
    // Integer "minute" that advances every few seconds; used to switch a few
    // windows on or off so the estate looks inhabited.
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 48_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "windows",
    )

    val stars = remember(night) {
        val rnd = Random(0x50B)
        if (!night) emptyList() else List(36) { Triple(rnd.nextFloat(), rnd.nextFloat() * 0.55f, rnd.nextFloat()) }
    }

    val ink = palette.ink
    val blockLight = if (night) Color(0xFF1F3A46) else Color(0xFFBDB8AB)
    val blockDark = if (night) Color(0xFF2A4856) else Color(0xFFA59F91)
    val windowOff = if (night) Color(0xFF0B171C).copy(alpha = 0.75f) else ink.copy(alpha = 0.30f)
    val windowOn = fills.amber
    val litShare = if (night) 0.34f else 0.12f

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(palette.background)

        if (ruled) {
            // Faint ruled lines, like the exercise books the journal lives in.
            val step = 28.dp.toPx()
            var y = step
            while (y < h) {
                drawLine(ink.copy(alpha = 0.045f * intensity), Offset(0f, y), Offset(w, y), 1f)
                y += step
            }
        }

        stars.forEach { (fx, fy, a) ->
            drawCircle(
                color = palette.onBackground.copy(alpha = (0.25f + a * 0.4f) * intensity),
                radius = (0.8f + a).dp.toPx(),
                center = Offset(fx * w, fy * h),
            )
        }

        drawOrbits(
            center = Offset(w - 70.dp.toPx(), 86.dp.toPx()),
            planet = fills.mint,
            moon = fills.blossom,
            ink = ink,
            orbit = orbit,
            intensity = intensity,
        )

        val ground = h - (skylineLift + navInset).toPx()
        drawSkyline(
            ground = ground,
            sun = fills.blossom,
            stripe = palette.background,
            ink = ink,
            blockLight = blockLight,
            blockDark = blockDark,
            windowOff = windowOff,
            windowOn = windowOn,
            litShare = litShare,
            tick = clock.toInt(),
            intensity = intensity,
        )
    }
}

private fun DrawScope.drawOrbits(
    center: Offset,
    planet: Color,
    moon: Color,
    ink: Color,
    orbit: Float,
    intensity: Float,
) {
    val stroke = 1.5.dp.toPx()
    drawCircle(planet.fade(intensity), radius = 30.dp.toPx(), center = center)
    drawCircle(
        ink.copy(alpha = 0.22f * intensity),
        radius = 52.dp.toPx(),
        center = center,
        style = Stroke(stroke),
    )
    drawCircle(
        ink.copy(alpha = 0.14f * intensity),
        radius = 76.dp.toPx(),
        center = center,
        style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 6.dp.toPx()))),
    )
    // Satellite on the inner ring.
    val a = orbit * 2.0 * Math.PI
    val r = 52.dp.toPx()
    val sat = Offset(center.x + (r * cos(a)).toFloat(), center.y + (r * sin(a)).toFloat())
    drawCircle(moon.fade(intensity), radius = 5.dp.toPx(), center = sat)
    drawCircle(ink.fade(intensity), radius = 5.dp.toPx(), center = sat, style = Stroke(stroke))
}

private fun Color.fade(f: Float): Color = copy(alpha = alpha * f)

/** Block outlines as fractions of the width: left, width, height (in dp). */
private val BLOCKS = listOf(
    Triple(0.025f, 0.18f, 90f),
    Triple(0.23f, 0.145f, 120f),
    Triple(0.40f, 0.215f, 75f),
    Triple(0.64f, 0.155f, 130f),
    Triple(0.82f, 0.155f, 95f),
)

private fun DrawScope.drawSkyline(
    ground: Float,
    sun: Color,
    stripe: Color,
    ink: Color,
    blockLight: Color,
    blockDark: Color,
    windowOff: Color,
    windowOn: Color,
    litShare: Float,
    tick: Int,
    intensity: Float,
) {
    val w = size.width
    val dp = 1.dp.toPx()

    // Poster sun with horizontal cut stripes across its lower half.
    val sunR = 56 * dp
    val sunC = Offset(w / 2f, ground - 78 * dp)
    clipRect(bottom = ground) {
        drawCircle(sun.fade(intensity), radius = sunR, center = sunC)
        listOf(12f, 20f, 28f).forEach { off ->
            drawRect(stripe, Offset(sunC.x - sunR, sunC.y + off * dp), Size(sunR * 2, 3 * dp))
        }
    }

    val winW = 7 * dp
    val winH = 9 * dp
    val stepX = 14 * dp
    val stepY = 16 * dp
    BLOCKS.forEachIndexed { b, (fx, fw, hDp) ->
        val left = fx * w
        val bw = fw * w
        val top = ground - hDp * dp
        drawRect(
            (if (b % 2 == 0) blockLight else blockDark).fade(intensity),
            Offset(left, top),
            Size(bw, ground - top),
        )
        val cols = ((bw - 3 * dp) / stepX).toInt()
        val rows = ((ground - top - 6 * dp) / stepY).toInt()
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                // Stable per-window seed; a few windows re-roll each tick.
                val id = b * 997 + c * 31 + r
                val flips = id % 7 == 0
                val seed = if (flips) id * 13 + tick else id
                val lit = Random(seed).nextFloat() < litShare
                drawRect(
                    (if (lit) windowOn else windowOff).fade(intensity),
                    Offset(left + 3 * dp + c * stepX, top + 4 * dp + r * stepY),
                    Size(winW, winH),
                )
            }
        }
    }
    drawRect(ink.fade(intensity), Offset(0f, ground - 3 * dp), Size(w, 3 * dp))
}
