/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package io.github.agentgit007.noctuary.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.agentgit007.noctuary.NoctuaryViewModel
import io.github.agentgit007.noctuary.data.displayTitle
import io.github.agentgit007.noctuary.data.wordCount
import io.github.agentgit007.noctuary.stats.StatsEngine
import io.github.agentgit007.noctuary.stats.StatsRange
import io.github.agentgit007.noctuary.ui.components.EmptyState
import io.github.agentgit007.noctuary.ui.components.GlassCard
import io.github.agentgit007.noctuary.ui.components.Pill
import io.github.agentgit007.noctuary.ui.components.SectionLabel
import io.github.agentgit007.noctuary.ui.components.StatTile
import io.github.agentgit007.noctuary.ui.theme.LocalAccents
import io.github.agentgit007.noctuary.ui.theme.LocalPaletteColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_FULL: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy")
private val MONTH_FULL: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

private val WEEKDAY_NAMES = listOf(
    "Mondays", "Tuesdays", "Wednesdays", "Thursdays", "Fridays", "Saturdays", "Sundays",
)

@Composable
fun StatsScreen(
    vm: NoctuaryViewModel,
    contentPadding: PaddingValues,
    onOpenDream: (Long) -> Unit,
) {
    val stats by vm.stats.collectAsState()
    val range by vm.statsRange.collectAsState()
    val allDreams by vm.dreams.collectAsState()
    val accents = LocalAccents.current
    val palette = LocalPaletteColors.current

    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var trendMetric by remember { mutableStateOf(TrendMetric.MOOD) }
    var weekdayIndex by remember { mutableStateOf<Int?>(null) }
    var monthIndex by remember { mutableStateOf<Int?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "PATTERNS",
                    style = MaterialTheme.typography.displaySmall,
                    color = palette.onBackground,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatsRange.entries.forEach { r ->
                        Pill(
                            text = r.label,
                            selected = range == r,
                            onClick = { vm.setStatsRange(r) },
                            accent = accents.lilac,
                        )
                    }
                }
            }
        }

        if (stats.isEmpty) {
            item {
                EmptyState(
                    title = "No data yet",
                    body = "Statistics appear once there are entries in this window. " +
                        "Record a few nights and the patterns start to show.",
                )
            }
            return@LazyColumn
        }

        // ------------------------------------------------------- summary --
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        value = stats.total.toString(),
                        label = "dreams",
                        accent = accents.lilac,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        value = "${StatsEngine.percent(stats.recallRate)}%",
                        label = "nights recalled",
                        accent = accents.mint,
                        modifier = Modifier.weight(1f),
                        footnote = "${stats.nights}/${stats.spanDays} nights",
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        value = stats.currentStreak.toString(),
                        label = "night streak",
                        accent = accents.amber,
                        modifier = Modifier.weight(1f),
                        footnote = "best ${stats.longestStreak}",
                    )
                    StatTile(
                        value = stats.meanWords.toString(),
                        label = "words / dream",
                        accent = accents.sky,
                        modifier = Modifier.weight(1f),
                        footnote = "${stats.words} in total",
                    )
                }
            }
        }

        // ------------------------------------------------------- heatmap --
        item {
            GlassCard(Modifier.fillMaxWidth(), accent = accents.mint) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionLabel("Recall calendar", accent = accents.mint)
                    RecallHeatmap(
                        heatmap = stats.heatmap,
                        start = stats.heatmapStart,
                        end = stats.heatmapEnd,
                        accent = accents.mint,
                        selectedDay = selectedDay,
                        onSelectDay = { selectedDay = it },
                    )
                    val day = selectedDay
                    if (day == null) {
                        Text(
                            text = if (palette.poster) {
                                "A year of nights as a housing block: a lit window is a night " +
                                    "you remembered. Tap one to see what you wrote."
                            } else {
                                "A year of nights. Tap any square to see what you wrote."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.onSurfaceMuted,
                        )
                    } else {
                        val entries = allDreams.filter { it.dreamDate == day.toEpochDay() }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = day.format(DAY_FULL),
                                style = MaterialTheme.typography.labelMedium,
                                color = accents.mint,
                            )
                            if (entries.isEmpty()) {
                                Text(
                                    text = "Nothing recorded for this night.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.onSurfaceMuted,
                                )
                            } else {
                                entries.forEach { d ->
                                    Text(
                                        text = "› ${d.displayTitle}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onOpenDream(d.id) }
                                            .padding(vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------- lucid dial --
        item {
            GlassCard(Modifier.fillMaxWidth(), accent = accents.lilac) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    RateDial(
                        rate = stats.lucidRate,
                        caption = "lucid",
                        accent = accents.mint,
                    )
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Readout("Lucid dreams", "${stats.lucid}", accents.mint)
                        Readout("Nightmares", "${stats.nightmares}", accents.blossom)
                        Readout("Recurring", "${stats.recurring}", accents.sky)
                        Readout("Favourites", "${stats.favorites}", accents.amber)
                        Readout(
                            "Average mood",
                            "${stats.meanMood.format1()} · ${describeMood(stats.meanMood)}",
                            accents.blossom,
                        )
                        Readout(
                            "Average clarity",
                            "${stats.meanClarity.format1()} · ${describeClarity(stats.meanClarity)}",
                            accents.sky,
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------- trends --
        item {
            GlassCard(Modifier.fillMaxWidth(), accent = accents.blossom) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionLabel("Night by night", accent = accents.blossom)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrendMetric.entries.forEach { metric ->
                            Pill(
                                text = metric.label,
                                selected = trendMetric == metric,
                                onClick = { trendMetric = metric },
                                accent = if (metric == TrendMetric.MOOD) accents.blossom else accents.sky,
                            )
                        }
                    }
                    TrendChart(
                        points = if (trendMetric == TrendMetric.MOOD) stats.moodTrend else stats.clarityTrend,
                        accent = if (trendMetric == TrendMetric.MOOD) accents.blossom else accents.sky,
                        minValue = 1f,
                        maxValue = 5f,
                        valueLabel = { v ->
                            if (trendMetric == TrendMetric.MOOD) {
                                "${v.format1()} · ${describeMood(v)}"
                            } else {
                                "${v.format1()} · ${describeClarity(v)}"
                            }
                        },
                    )
                }
            }
        }

        // ------------------------------------------------------ weekdays --
        item {
            GlassCard(Modifier.fillMaxWidth(), accent = accents.amber) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionLabel("Which nights you remember", accent = accents.amber)
                    BarChart(
                        values = stats.byWeekday,
                        labels = StatsEngine.WEEKDAY_LABELS,
                        accent = accents.amber,
                        overlay = stats.lucidByWeekday,
                        overlayAccent = accents.mint,
                        selectedIndex = weekdayIndex,
                        onSelect = { weekdayIndex = it },
                    )
                    val idx = weekdayIndex
                    val best = stats.byWeekday.indices.maxByOrNull { stats.byWeekday[it] }
                    Text(
                        text = when {
                            idx != null -> {
                                val lucid = stats.lucidByWeekday.getOrElse(idx) { 0 }
                                "${WEEKDAY_NAMES[idx]}: ${stats.byWeekday[idx]} dreams" +
                                    if (lucid > 0) ", $lucid lucid." else "."
                            }
                            best != null && stats.byWeekday[best] > 0 ->
                                "Your recall peaks on ${WEEKDAY_NAMES[best].lowercase(Locale.getDefault())}. " +
                                    "The mint overlay marks lucid nights — tap a bar for detail."
                            else -> "Tap a bar for detail."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.onSurfaceMuted,
                    )
                }
            }
        }

        // -------------------------------------------------------- months --
        if (stats.months.size > 1) {
            item {
                GlassCard(Modifier.fillMaxWidth(), accent = accents.sky) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SectionLabel("Month by month", accent = accents.sky)
                        MonthChart(
                            months = stats.months,
                            accent = accents.sky,
                            lucidAccent = accents.mint,
                            selectedIndex = monthIndex,
                            onSelect = { monthIndex = it },
                        )
                        val m = monthIndex?.let { stats.months.getOrNull(it) }
                        Text(
                            text = if (m == null) {
                                "Tap a column to see that month."
                            } else {
                                "${m.month.format(MONTH_FULL)}: ${m.count} dreams, ${m.lucid} lucid."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.onSurfaceMuted,
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------- tags --
        if (stats.tags.isNotEmpty()) {
            item {
                GlassCard(Modifier.fillMaxWidth(), accent = accents.lilac) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SectionLabel("Themes", accent = accents.lilac)
                        val max = stats.tags.firstOrNull()?.count ?: 1
                        stats.tags.take(8).forEach { tag ->
                            TagBar(
                                label = "#${tag.tag}",
                                count = tag.count,
                                max = max,
                                accent = accents.lilac,
                                selected = selectedTag == tag.tag,
                                onClick = {
                                    selectedTag = if (selectedTag == tag.tag) null else tag.tag
                                },
                            )
                        }
                        val t = stats.tags.firstOrNull { it.tag == selectedTag }
                        if (t != null) {
                            Text(
                                text = "#${t.tag}: ${t.count} dreams · mood ${t.meanMood.format1()} " +
                                    "(${describeMood(t.meanMood)}) · ${t.lucidCount} lucid",
                                style = MaterialTheme.typography.bodyMedium,
                                color = accents.lilac,
                            )
                        }
                    }
                }
            }
        }

        // --------------------------------------------------- dream signs --
        if (stats.signs.isNotEmpty()) {
            item {
                GlassCard(Modifier.fillMaxWidth(), accent = accents.mint) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SectionLabel("Dream signs", accent = accents.mint)
                        Text(
                            text = "Words that keep coming back across different nights. " +
                                "These are the things worth questioning while you are asleep.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.onSurfaceMuted,
                        )
                        stats.signs.take(12).chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { sign ->
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(accents.mint.copy(alpha = 0.14f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${sign.word} ·${sign.dreams}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = accents.mint,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ------------------------------------------------------ footnote --
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                stats.firstEntry?.let {
                    Text(
                        text = "First entry: ${it.format(DAY_FULL)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.onSurfaceMuted,
                    )
                }
                stats.longestEntry?.let {
                    Text(
                        text = "Longest entry: ${it.displayTitle} (${it.wordCount} words)",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.onSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onOpenDream(it.id) },
                    )
                }
            }
        }
    }
}

private enum class TrendMetric(val label: String) { MOOD("MOOD"), CLARITY("CLARITY") }

@Composable
private fun Readout(label: String, value: String, accent: androidx.compose.ui.graphics.Color) {
    val palette = LocalPaletteColors.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.onSurfaceMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
        )
    }
}
