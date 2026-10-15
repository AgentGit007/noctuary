/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package io.github.agentgit007.noctuary.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.agentgit007.noctuary.stats.MonthBucket
import io.github.agentgit007.noctuary.stats.TrendPoint
import io.github.agentgit007.noctuary.ui.theme.LocalPaletteColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val DAY_LABEL: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

// ------------------------------------------------------------- heatmap -----

/**
 * A year of nights. Each column is a week, each row a weekday; tapping a cell
 * reports the date so the screen above can say what was dreamt that night.
 */
@Composable
fun RecallHeatmap(
    heatmap: Map<Long, Int>,
    start: LocalDate,
    end: LocalDate,
    accent: Color,
    selectedDay: LocalDate?,
    onSelectDay: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPaletteColors.current
    val density = LocalDensity.current
    // Poster palettes draw the calendar as a prefab facade: taller-than-wide
    // windows on a concrete slab, lit when you remembered that night.
    val poster = palette.poster
    val cellW = if (poster) 11.dp else 13.dp
    val cellH = 13.dp
    val gap = if (poster) 4.dp else 3.dp
    val stepX = cellW + gap
    val stepY = cellH + gap
    val unlit = if (poster) palette.ink.copy(alpha = 0.16f) else palette.outline.copy(alpha = 0.35f)
    val litLow = if (poster) palette.fillFor(palette.accents.amber) else accent
    val litHigh = if (poster) palette.fillFor(palette.accents.blossom) else accent

    // Align the first column to a Monday so rows mean weekdays.
    val firstMonday = start.minusDays((start.dayOfWeek.value - 1).toLong())
    val totalDays = (end.toEpochDay() - firstMonday.toEpochDay() + 1).toInt().coerceAtLeast(7)
    val weeks = (totalDays + 6) / 7
    val max = (heatmap.values.maxOrNull() ?: 1).coerceAtLeast(1)

    val scroll = rememberScrollState()
    LaunchedEffect(weeks) { scroll.scrollTo(scroll.maxValue) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(scroll)) {
            val stepXPx = with(density) { stepX.toPx() }
            val stepYPx = with(density) { stepY.toPx() }
            val cellWPx = with(density) { cellW.toPx() }
            val cellHPx = with(density) { cellH.toPx() }
            Canvas(
                Modifier
                    .then(
                        if (poster) {
                            Modifier
                                .background(palette.surfaceHigh)
                                .border(2.dp, palette.ink)
                                .padding(start = 6.dp, top = 6.dp, end = 2.dp, bottom = 2.dp)
                        } else {
                            Modifier
                        }
                    )
                    .width(stepX * weeks)
                    .height(stepY * 7)
                    .pointerInput(weeks, heatmap) {
                        detectTapGestures { offset ->
                            val col = (offset.x / stepXPx).toInt()
                            val row = (offset.y / stepYPx).toInt()
                            if (col in 0 until weeks && row in 0..6) {
                                val day = firstMonday.plusDays((col * 7 + row).toLong())
                                if (!day.isAfter(end)) {
                                    onSelectDay(if (day == selectedDay) null else day)
                                }
                            }
                        }
                    }
            ) {
                for (col in 0 until weeks) {
                    for (row in 0..6) {
                        val day = firstMonday.plusDays((col * 7 + row).toLong())
                        if (day.isAfter(end)) continue
                        val count = heatmap[day.toEpochDay()] ?: 0
                        val strength = if (count == 0) 0f else 0.25f + 0.75f * (count.toFloat() / max)
                        val topLeft = Offset(col * stepXPx, row * stepYPx)
                        val corner = androidx.compose.ui.geometry.CornerRadius(
                            if (poster) 0f else 3.dp.toPx()
                        )
                        val fill = when {
                            count == 0 -> unlit
                            poster -> lerp(litLow, litHigh, if (max <= 1) 0f else (count - 1f) / (max - 1f))
                            else -> accent.copy(alpha = strength)
                        }
                        drawRoundRect(
                            color = fill,
                            topLeft = topLeft,
                            size = Size(cellWPx, cellHPx),
                            cornerRadius = corner,
                        )
                        if (day == selectedDay) {
                            drawRoundRect(
                                color = if (poster) palette.ink else palette.onBackground,
                                topLeft = topLeft,
                                size = Size(cellWPx, cellHPx),
                                cornerRadius = corner,
                                style = Stroke(width = 2.dp.toPx()),
                            )
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = start.format(DAY_LABEL),
                style = MaterialTheme.typography.labelSmall,
                color = palette.onSurfaceMuted,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "LESS",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.onSurfaceMuted,
                )
                listOf(0.25f, 0.5f, 0.75f, 1f).forEach { a ->
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(RoundedCornerShape(if (poster) 0.dp else 2.dp))
                            .background(
                                if (poster) lerp(litLow, litHigh, (a - 0.25f) / 0.75f) else accent.copy(alpha = a)
                            )
                    )
                }
                Text(
                    text = "MORE",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.onSurfaceMuted,
                )
            }
        }
    }
}

// --------------------------------------------------------------- trend -----

/**
 * Drag across the chart to scrub: the readout above follows your finger.
 */
@Composable
fun TrendChart(
    points: List<TrendPoint>,
    accent: Color,
    minValue: Float,
    maxValue: Float,
    valueLabel: (Float) -> String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPaletteColors.current
    var selected by remember(points) { mutableStateOf<Int?>(null) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val focus = selected?.let { points.getOrNull(it) }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = focus?.let { LocalDate.ofEpochDay(it.day).format(DAY_LABEL) }
                    ?: "Drag to scrub",
                style = MaterialTheme.typography.labelMedium,
                color = palette.onSurfaceMuted,
            )
            Text(
                text = focus?.let { valueLabel(it.value) } ?: "",
                style = MaterialTheme.typography.labelMedium,
                color = accent,
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            if (points.size < 2) {
                Text(
                    text = "Two nights of entries draw the first line.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.onSurfaceMuted,
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center,
                )
            } else {
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .pointerInput(points) {
                            detectTapGestures { o ->
                                selected = indexAt(o.x, size.width.toFloat(), points.size)
                            }
                        }
                        .pointerInput(points) {
                            detectHorizontalDragGestures(
                                onDragStart = { o ->
                                    selected = indexAt(o.x, size.width.toFloat(), points.size)
                                },
                            ) { change, _ ->
                                selected = indexAt(
                                    change.position.x,
                                    size.width.toFloat(),
                                    points.size,
                                )
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val span = (maxValue - minValue).takeIf { it > 0f } ?: 1f
                    val xs = { i: Int -> if (points.size == 1) w / 2f else w * i / (points.size - 1f) }
                    val ys = { v: Float -> h - ((v - minValue) / span).coerceIn(0f, 1f) * (h - 8.dp.toPx()) - 4.dp.toPx() }

                    // baseline guides
                    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 6.dp.toPx()))
                    listOf(0f, 0.5f, 1f).forEach { t ->
                        val y = ys(minValue + span * t)
                        drawLine(
                            color = palette.outline.copy(alpha = 0.6f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f,
                            pathEffect = dash,
                        )
                    }

                    val line = Path()
                    val area = Path()
                    points.forEachIndexed { i, p ->
                        val x = xs(i)
                        val y = ys(p.value)
                        if (i == 0) {
                            line.moveTo(x, y)
                            area.moveTo(x, h)
                            area.lineTo(x, y)
                        } else {
                            line.lineTo(x, y)
                            area.lineTo(x, y)
                        }
                    }
                    area.lineTo(xs(points.lastIndex), h)
                    area.close()

                    drawPath(
                        path = area,
                        brush = Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.32f), Color.Transparent)
                        ),
                    )
                    drawPath(
                        path = line,
                        color = accent,
                        style = Stroke(width = 2.5.dp.toPx()),
                    )

                    selected?.let { idx ->
                        val p = points.getOrNull(idx) ?: return@let
                        val x = xs(idx)
                        val y = ys(p.value)
                        drawLine(
                            color = accent.copy(alpha = 0.5f),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1.dp.toPx(),
                        )
                        drawCircle(color = palette.surface, radius = 6.dp.toPx(), center = Offset(x, y))
                        drawCircle(
                            color = accent,
                            radius = 6.dp.toPx(),
                            center = Offset(x, y),
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }
            }
        }
    }
}

private fun indexAt(x: Float, width: Float, count: Int): Int {
    if (count <= 1 || width <= 0f) return 0
    return ((x / width) * (count - 1)).roundToInt().coerceIn(0, count - 1)
}

// ---------------------------------------------------------------- bars -----

/** Tappable bars; the caller decides what the selection means. */
@Composable
fun BarChart(
    values: List<Int>,
    labels: List<String>,
    accent: Color,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    overlay: List<Int>? = null,
    overlayAccent: Color = accent,
    height: androidx.compose.ui.unit.Dp = 120.dp,
) {
    val palette = LocalPaletteColors.current
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth().height(height),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            values.forEachIndexed { i, v ->
                val isSelected = selectedIndex == i
                val fraction by animateFloatAsState(
                    targetValue = v.toFloat() / max,
                    animationSpec = tween(520),
                    label = "bar$i",
                )
                val overlayFraction = overlay?.getOrNull(i)?.let { it.toFloat() / max } ?: 0f
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .height(height)
                        .clip(RoundedCornerShape(6.dp))
                        .pointerInput(i) {
                            detectTapGestures { onSelect(if (isSelected) null else i) }
                        },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(height * fraction.coerceAtLeast(0.012f))
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        accent.copy(alpha = if (isSelected) 1f else 0.75f),
                                        accent.copy(alpha = if (isSelected) 0.5f else 0.3f),
                                    )
                                )
                            )
                    )
                    if (overlayFraction > 0f) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.45f)
                                .height(height * overlayFraction)
                                .clip(RoundedCornerShape(6.dp))
                                .background(overlayAccent.copy(alpha = 0.95f))
                        )
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            labels.forEachIndexed { i, label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selectedIndex == i) accent else palette.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun MonthChart(
    months: List<MonthBucket>,
    accent: Color,
    lucidAccent: Color,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    BarChart(
        values = months.map { it.count },
        labels = months.map { it.month.month.name.take(1) },
        accent = accent,
        overlay = months.map { it.lucid },
        overlayAccent = lucidAccent,
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        modifier = modifier,
    )
}

// ---------------------------------------------------------------- dial -----

/** Radial gauge for a 0..1 rate, with the figure in the middle. */
@Composable
fun RateDial(
    rate: Float,
    caption: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPaletteColors.current
    val sweep by animateFloatAsState(
        targetValue = rate.coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "dial",
    )
    if (palette.poster) {
        CockpitGauge(sweep, caption, accent, modifier)
        return
    }
    Box(modifier.size(124.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(124.dp)) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            drawArc(
                color = palette.outline.copy(alpha = 0.6f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(accent.copy(alpha = 0.6f), accent)),
                startAngle = 135f,
                sweepAngle = 270f * sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(sweep * 100).roundToInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                color = accent,
            )
            Text(
                text = caption.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = palette.onSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Kosmos variant of [RateDial]: a half-round instrument gauge with five
 * ticks and a needle, the figure printed underneath.
 */
@Composable
private fun CockpitGauge(
    value: Float,
    caption: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPaletteColors.current
    val needle = palette.accents.blossom
    val fill = palette.fillFor(accent)
    Column(modifier.width(124.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            Modifier
                .width(124.dp)
                .height(70.dp)
        ) {
            val band = 12.dp.toPx()
            val r = size.width / 2f - band / 2f
            val c = Offset(size.width / 2f, size.height - 6.dp.toPx())
            val box = Size(r * 2, r * 2)
            val tl = Offset(c.x - r, c.y - r)
            drawArc(
                color = palette.ink.copy(alpha = 0.16f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = tl,
                size = box,
                style = Stroke(width = band, cap = StrokeCap.Butt),
            )
            drawArc(
                color = fill,
                startAngle = 180f,
                sweepAngle = 180f * value,
                useCenter = false,
                topLeft = tl,
                size = box,
                style = Stroke(width = band, cap = StrokeCap.Butt),
            )
            // ticks at 0, 25, 50, 75, 100 %
            for (i in 0..4) {
                val a = Math.toRadians(180.0 + i * 45.0)
                val outer = r + band / 2f
                val inner = r - band / 2f - 3.dp.toPx()
                drawLine(
                    color = palette.ink,
                    start = Offset(c.x + (outer * cos(a)).toFloat(), c.y + (outer * sin(a)).toFloat()),
                    end = Offset(c.x + (inner * cos(a)).toFloat(), c.y + (inner * sin(a)).toFloat()),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            val na = Math.toRadians(180.0 + 180.0 * value)
            val len = r - band / 2f - 6.dp.toPx()
            drawLine(
                color = needle,
                start = c,
                end = Offset(c.x + (len * cos(na)).toFloat(), c.y + (len * sin(na)).toFloat()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(palette.fillFor(needle), radius = 6.dp.toPx(), center = c)
            drawCircle(palette.ink, radius = 6.dp.toPx(), center = c, style = Stroke(2.dp.toPx()))
        }
        Text(
            text = "${(value * 100).roundToInt()}%",
            style = MaterialTheme.typography.headlineMedium,
            color = palette.onBackground,
        )
        Text(
            text = caption.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = palette.onSurfaceMuted,
            textAlign = TextAlign.Center,
        )
    }
}

// ------------------------------------------------------------ tag bars -----

@Composable
fun TagBar(
    label: String,
    count: Int,
    max: Int,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    val palette = LocalPaletteColors.current
    val fraction by animateFloatAsState(
        targetValue = if (max == 0) 0f else count.toFloat() / max,
        animationSpec = tween(520),
        label = "tagbar",
    )
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(palette.surfaceHigh.copy(alpha = 0.5f))
            .pointerInput(label) { detectTapGestures { onClick() } }
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0.04f, 1f))
                .height(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = if (selected) 0.55f else 0.3f))
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(34.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.onBackground,
            )
            Text(
                text = trailing ?: count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = palette.onSurfaceMuted,
            )
        }
    }
}

/** Used by the mood/clarity readout to describe a 1..5 average in words. */
fun describeMood(value: Float): String = when {
    value <= 0f -> "—"
    value < 1.8f -> "bleak"
    value < 2.6f -> "uneasy"
    value < 3.4f -> "even"
    value < 4.2f -> "warm"
    else -> "blissful"
}

fun describeClarity(value: Float): String = when {
    value <= 0f -> "—"
    value < 1.8f -> "fragments"
    value < 2.6f -> "hazy"
    value < 3.4f -> "clear"
    value < 4.2f -> "sharp"
    else -> "vivid"
}

/** One decimal place, in the device's locale. */
internal fun Float.format1(): String = String.format("%.1f", this)
