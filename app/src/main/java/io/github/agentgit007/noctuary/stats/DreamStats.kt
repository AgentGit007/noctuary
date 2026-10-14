/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package io.github.agentgit007.noctuary.stats

import io.github.agentgit007.noctuary.data.Dream
import io.github.agentgit007.noctuary.data.date
import io.github.agentgit007.noctuary.data.wordCount
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

enum class StatsRange(val label: String, val days: Int?) {
    MONTH("30D", 30),
    QUARTER("90D", 90),
    YEAR("1Y", 365),
    ALL("ALL", null),
}

data class TagStat(val tag: String, val count: Int, val lucidCount: Int, val meanMood: Float)

data class SignWord(val word: String, val count: Int, val dreams: Int)

data class MonthBucket(val month: YearMonth, val count: Int, val lucid: Int)

data class TrendPoint(val day: Long, val value: Float, val count: Int)

/**
 * Everything the statistics screen draws. Computed in one pass off the
 * journal so the UI never touches raw entries.
 */
data class DreamStats(
    val range: StatsRange,
    val total: Int,
    val nights: Int,
    val spanDays: Int,
    val recallRate: Float,
    val lucid: Int,
    val nightmares: Int,
    val recurring: Int,
    val favorites: Int,
    val meanMood: Float,
    val meanClarity: Float,
    val words: Int,
    val meanWords: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val byWeekday: List<Int>,
    val lucidByWeekday: List<Int>,
    val months: List<MonthBucket>,
    val moodTrend: List<TrendPoint>,
    val clarityTrend: List<TrendPoint>,
    val heatmap: Map<Long, Int>,
    val heatmapStart: LocalDate,
    val heatmapEnd: LocalDate,
    val tags: List<TagStat>,
    val signs: List<SignWord>,
    val longestEntry: Dream?,
    val firstEntry: LocalDate?,
) {
    val lucidRate: Float get() = if (total == 0) 0f else lucid.toFloat() / total
    val isEmpty: Boolean get() = total == 0
}

object StatsEngine {

    fun compute(
        all: List<Dream>,
        range: StatsRange,
        today: LocalDate = LocalDate.now(),
    ): DreamStats {
        val from = range.days?.let { today.minusDays((it - 1).toLong()) }
        val dreams = if (from == null) all else all.filter { it.dreamDate >= from.toEpochDay() }

        val sorted = dreams.sortedBy { it.dreamDate }
        val days = sorted.map { it.dreamDate }.toSortedSet()

        val firstDay = sorted.firstOrNull()?.dreamDate
        val spanDays = when {
            range.days != null -> range.days
            firstDay == null -> 0
            else -> (today.toEpochDay() - firstDay + 1).toInt().coerceAtLeast(1)
        }

        val byWeekday = MutableList(7) { 0 }
        val lucidByWeekday = MutableList(7) { 0 }
        sorted.forEach {
            val idx = it.date.dayOfWeek.value - 1 // Monday = 0
            byWeekday[idx] = byWeekday[idx] + 1
            if (it.lucid) lucidByWeekday[idx] = lucidByWeekday[idx] + 1
        }

        val heatmapEnd = today
        val heatmapStart = heatmapEnd.minusDays(364)
        val heatmap = sorted
            .filter { it.dreamDate >= heatmapStart.toEpochDay() }
            .groupingBy { it.dreamDate }
            .eachCount()

        return DreamStats(
            range = range,
            total = sorted.size,
            nights = days.size,
            spanDays = spanDays,
            recallRate = if (spanDays == 0) 0f else days.size.toFloat() / spanDays,
            lucid = sorted.count { it.lucid },
            nightmares = sorted.count { it.nightmare },
            recurring = sorted.count { it.recurring },
            favorites = sorted.count { it.favorite },
            meanMood = sorted.meanOf { it.mood.toFloat() },
            meanClarity = sorted.meanOf { it.clarity.toFloat() },
            words = sorted.sumOf { it.wordCount },
            meanWords = if (sorted.isEmpty()) 0 else sorted.sumOf { it.wordCount } / sorted.size,
            currentStreak = currentStreak(days, today),
            longestStreak = longestStreak(days),
            byWeekday = byWeekday,
            lucidByWeekday = lucidByWeekday,
            months = monthBuckets(sorted, range, today),
            moodTrend = trend(sorted) { it.mood.toFloat() },
            clarityTrend = trend(sorted) { it.clarity.toFloat() },
            heatmap = heatmap,
            heatmapStart = heatmapStart,
            heatmapEnd = heatmapEnd,
            tags = tagStats(sorted),
            signs = signWords(sorted),
            longestEntry = sorted.maxByOrNull { it.wordCount },
            firstEntry = all.minByOrNull { it.dreamDate }?.date,
        )
    }

    private inline fun List<Dream>.meanOf(selector: (Dream) -> Float): Float =
        if (isEmpty()) 0f else sumOf { selector(it).toDouble() }.toFloat() / size

    /** Consecutive recall days ending today or yesterday (so a morning that has not been written up yet does not break it). */
    private fun currentStreak(days: Set<Long>, today: LocalDate): Int {
        if (days.isEmpty()) return 0
        var cursor = today.toEpochDay()
        if (cursor !in days) {
            cursor -= 1
            if (cursor !in days) return 0
        }
        var streak = 0
        while (cursor in days) {
            streak++
            cursor--
        }
        return streak
    }

    private fun longestStreak(days: Set<Long>): Int {
        if (days.isEmpty()) return 0
        var best = 1
        var run = 1
        val ordered = days.sorted()
        for (i in 1 until ordered.size) {
            run = if (ordered[i] == ordered[i - 1] + 1) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    private fun monthBuckets(
        dreams: List<Dream>,
        range: StatsRange,
        today: LocalDate,
    ): List<MonthBucket> {
        if (dreams.isEmpty()) return emptyList()
        val first = YearMonth.from(dreams.first().date)
        val last = YearMonth.from(today)
        val months = when (range) {
            StatsRange.MONTH -> 2L
            StatsRange.QUARTER -> 4L
            StatsRange.YEAR -> 12L
            StatsRange.ALL -> (java.time.temporal.ChronoUnit.MONTHS.between(first, last) + 1)
                .coerceIn(1L, 36L)
        }
        val start = last.minusMonths(months - 1)
        val grouped = dreams.groupBy { YearMonth.from(it.date) }
        return (0 until months).map { offset ->
            val m = start.plusMonths(offset)
            val bucket = grouped[m].orEmpty()
            MonthBucket(m, bucket.size, bucket.count { it.lucid })
        }
    }

    /** One point per recorded night, averaged when several dreams share a date. */
    private fun trend(dreams: List<Dream>, selector: (Dream) -> Float): List<TrendPoint> =
        dreams.groupBy { it.dreamDate }
            .toSortedMap()
            .map { (day, group) ->
                TrendPoint(
                    day = day,
                    value = group.map(selector).average().toFloat(),
                    count = group.size,
                )
            }

    private fun tagStats(dreams: List<Dream>): List<TagStat> =
        dreams.flatMap { d -> d.tags.map { it to d } }
            .groupBy({ it.first.lowercase() }, { it.second })
            .map { (tag, group) ->
                TagStat(
                    tag = tag,
                    count = group.size,
                    lucidCount = group.count { it.lucid },
                    meanMood = group.map { it.mood.toFloat() }.average().toFloat(),
                )
            }
            .sortedWith(compareByDescending<TagStat> { it.count }.thenBy { it.tag })

    /**
     * "Dream signs": the nouns that keep coming back across nights. Counting
     * the number of *distinct* dreams a word shows up in (not raw frequency)
     * keeps one long entry from dominating the list.
     */
    private fun signWords(dreams: List<Dream>): List<SignWord> {
        if (dreams.isEmpty()) return emptyList()
        val total = HashMap<String, Int>()
        val perDream = HashMap<String, Int>()
        dreams.forEach { dream ->
            val seen = HashSet<String>()
            WORD.findAll(dream.title + " " + dream.body).forEach { m ->
                val w = m.value.lowercase()
                if (w.length >= 4 && w !in STOPWORDS) {
                    total[w] = (total[w] ?: 0) + 1
                    seen += w
                }
            }
            seen.forEach { perDream[it] = (perDream[it] ?: 0) + 1 }
        }
        return perDream.entries
            .filter { it.value >= 2 }
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }
                .thenByDescending { total[it.key] ?: 0 }
                .thenBy { it.key })
            .take(24)
            .map { SignWord(it.key, total[it.key] ?: 0, it.value) }
    }

    fun percent(value: Float): Int = (value * 100).roundToInt()

    /** Monday-first, matching java.time.DayOfWeek#getValue. */
    val WEEKDAY_LABELS: List<String> = listOf("M", "T", "W", "T", "F", "S", "S")

    private val WORD = Regex("[\\p{L}']{3,}")

    private val STOPWORDS: Set<String> = setOf(
        "about", "after", "again", "against", "almost", "alone", "along", "already", "also",
        "although", "always", "another", "anyone", "anything", "around", "back", "because",
        "been", "before", "behind", "being", "below", "between", "both", "came", "come",
        "coming", "could", "didn", "does", "doing", "done", "down", "dream", "dreamed",
        "dreaming", "dreams", "each", "even", "ever", "every", "everyone", "everything",
        "felt", "from", "gets", "getting", "give", "going", "gone", "good", "have",
        "having", "here", "hers", "herself", "himself", "into", "just", "keep", "kind",
        "knew", "know", "last", "later", "like", "little", "long", "look", "looked",
        "looking", "made", "make", "many", "maybe", "mine", "more", "most", "much", "must",
        "myself", "near", "need", "never", "next", "nothing", "once", "only", "onto",
        "other", "others", "ours", "over", "own", "part", "people", "really", "right",
        "said", "same", "saw", "say", "says", "seem", "seemed", "seen", "several", "she",
        "should", "side", "since", "some", "somehow", "someone", "something", "sometimes",
        "somewhere", "soon", "start", "started", "still", "stuff", "such", "sure", "take",
        "taken", "than", "that", "their", "them", "themselves", "then", "there", "these",
        "they", "thing", "things", "think", "this", "those", "though", "thought",
        "through", "time", "told", "took", "toward", "tried", "try", "turn", "turned",
        "until", "upon", "used", "very", "wanted", "was", "wasn", "went", "were", "what",
        "when", "where", "which", "while", "who", "whole", "will", "with", "within",
        "without", "would", "yeah", "your", "yours", "yourself",
    )
}
