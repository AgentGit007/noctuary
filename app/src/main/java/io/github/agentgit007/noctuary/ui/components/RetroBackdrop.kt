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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import io.github.agentgit007.noctuary.ui.theme.LocalAccents
import io.github.agentgit007.noctuary.ui.theme.LocalIsNight
import io.github.agentgit007.noctuary.ui.theme.LocalPaletteColors
import kotlin.math.pow
import kotlin.random.Random

/**
 * The thing people see before they read anything: a slow synthwave horizon
 * behind the whole app. It is drawn once per frame on a single canvas, uses
 * only the palette colours, and carries no content, so it never gets in the
 * way of the journal sitting on top of it.
 */
@Composable
fun RetroBackdrop(
    modifier: Modifier = Modifier,
    scanlines: Boolean = true,
    intensity: Float = 1f,
) {
    val accents = LocalAccents.current
    val palette = LocalPaletteColors.current
    val night = LocalIsNight.current

    val transition = rememberInfiniteTransition(label = "backdrop")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "grid-drift",
    )
    val shimmer by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5_200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sun-shimmer",
    )

    // Deterministic star field — same sky on every launch.
    val stars = remember(night) {
        val rnd = Random(0xBEEF)
        List(if (night) 70 else 24) {
            Triple(rnd.nextFloat(), rnd.nextFloat() * 0.56f, 0.4f + rnd.nextFloat() * 0.6f)
        }
    }

    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val horizon = h * 0.56f

            drawRect(
                brush = Brush.verticalGradient(
                    0f to palette.background,
                    0.45f to accents.haze,
                    0.56f to accents.haze,
                    1f to palette.background,
                )
            )

            // stars
            val starColor = if (night) accents.mint else accents.lilac
            stars.forEach { (fx, fy, a) ->
                drawCircle(
                    color = starColor.copy(alpha = a * 0.5f * intensity),
                    radius = (0.6f + a).dp.toPx() * 0.7f,
                    center = Offset(fx * w, fy * horizon),
                )
            }

            // sun
            val sunRadius = w * 0.21f
            val sunCenter = Offset(w * 0.5f, horizon - sunRadius * 0.42f)
            clipRect(top = 0f, bottom = horizon) {
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(accents.sunTop, accents.sunBottom),
                        startY = sunCenter.y - sunRadius,
                        endY = sunCenter.y + sunRadius,
                    ),
                    radius = sunRadius,
                    center = sunCenter,
                    alpha = (if (night) 0.55f else 0.4f) * shimmer * intensity,
                )
                // classic slit bands across the lower half of the disc
                var y = sunCenter.y + sunRadius * 0.08f
                var band = 0
                while (y < sunCenter.y + sunRadius) {
                    drawRect(
                        color = accents.haze.copy(alpha = 0.85f),
                        topLeft = Offset(sunCenter.x - sunRadius, y),
                        size = Size(sunRadius * 2f, (2 + band).dp.toPx() * 0.7f),
                    )
                    y += sunRadius * (0.11f + band * 0.015f)
                    band++
                }
            }

            drawHorizonGrid(
                horizon = horizon,
                color = accents.horizon.copy(alpha = (if (night) 0.55f else 0.4f) * intensity),
                drift = drift,
            )

            // horizon line glow
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.5f to accents.lilac.copy(alpha = 0.35f * intensity),
                    1f to Color.Transparent,
                ),
                topLeft = Offset(0f, horizon - 6.dp.toPx()),
                size = Size(w, 12.dp.toPx()),
            )

            if (scanlines) {
                val step = 3.dp.toPx()
                var y = 0f
                val lineColor = (if (night) Color.Black else palette.outline).copy(alpha = 0.06f)
                while (y < h) {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f,
                    )
                    y += step
                }
            }
        }
    }
}

private fun DrawScope.drawHorizonGrid(horizon: Float, color: Color, drift: Float) {
    val w = size.width
    val h = size.height
    val depth = h - horizon
    if (depth <= 0f) return
    val stroke = 1.2.dp.toPx()

    // Receding horizontals: squared spacing fakes perspective, and the drift
    // offset makes the grid flow toward the viewer.
    val rows = 14
    for (i in 0 until rows) {
        val t = ((i + drift) / rows).coerceIn(0f, 1f)
        val y = horizon + depth * t.pow(2.4f)
        drawLine(
            color = color.copy(alpha = color.alpha * (0.25f + t * 0.75f)),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = stroke,
        )
    }

    // Converging verticals.
    val columns = 11
    val spread = w * 1.9f
    for (i in -columns..columns) {
        val xBottom = w / 2f + (i.toFloat() / columns) * spread
        drawLine(
            color = color,
            start = Offset(w / 2f, horizon),
            end = Offset(xBottom, h),
            strokeWidth = stroke,
        )
    }
}
