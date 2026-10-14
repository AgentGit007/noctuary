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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.agentgit007.noctuary.stats.MonthBucket
import io.github.agentgit007.noctuary.stats.TrendPoint
import io.github.agentgit007.noctuary.ui.theme.LocalPaletteColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

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
    val cell = 13.dp
    val gap = 3.dp
    val step = cell + gap

    // Align the first column to a Monday so rows mean weekdays.
    val firstMonday = start.minusDays((start.dayOfWeek.value - 1).toLong())
    val totalDays = (end.toEpochDay() - firstMonday.toEpochDay() + 1).toInt().coerceAtLeast(7)
    val weeks = (totalDays + 6) / 7
    val max = (heatmap.values.maxOrNull() ?: 1).coerceAtLeast(1)

    val scroll = rememberScrollState()
    LaunchedEffect(weeks) { scroll.scrollTo(scroll.maxValue) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(scroll)) {
            val stepPx = with(density) { step.toPx() }
            val cellPx = with(density) { cell.toPx() }
            Canvas(
                Modifier
                    .width(step * weeks)
                    .height(step * 7)
                    .pointerInput(weeks, heatmap) {
                        detectTapGestures { offset ->
                            val col = (offset.x / stepPx).toInt()
                            val row = (offset.y / stepPx).toInt()
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
                        val topLeft = Offset(col * stepPx, row * stepPx)
                        drawRoundRect(
                            color = if (count == 0) {
                                palette.outline.copy(alpha = 0.35f)
                            } else {
                                accent.copy(alpha = strength)
                            },
                            topLeft = topLeft,
                            size = Size(cellPx, cellPx),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                        )
                        if (day == selectedDay) {
                            drawRoundRect(
                                color = palette.onBackground,
                                topLeft = topLeft,
                                size = Size(cellPx, cellPx),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
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
                            .clip(RoundedCornerShape(2.dp))
                            .background(accent.copy(alpha = a))
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
