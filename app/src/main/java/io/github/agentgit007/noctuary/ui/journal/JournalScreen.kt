/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.agentgit007.noctuary.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.agentgit007.noctuary.NoctuaryViewModel
import io.github.agentgit007.noctuary.data.Dream
import io.github.agentgit007.noctuary.data.date
import io.github.agentgit007.noctuary.data.displayTitle
import io.github.agentgit007.noctuary.ui.components.DashedRule
import io.github.agentgit007.noctuary.ui.components.EmptyState
import io.github.agentgit007.noctuary.ui.components.GlassCard
import io.github.agentgit007.noctuary.ui.components.Pill
import io.github.agentgit007.noctuary.ui.theme.LocalAccents
import io.github.agentgit007.noctuary.ui.theme.LocalPaletteColors
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MONTH_HEADER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val WEEKDAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE")

@Composable
fun JournalScreen(
    vm: NoctuaryViewModel,
    contentPadding: PaddingValues,
    onOpenDream: (Long) -> Unit,
) {
    val dreams by vm.visibleDreams.collectAsState()
    val filter by vm.filter.collectAsState()
    val tags by vm.allTags.collectAsState()
    val total by vm.totalCount.collectAsState()
    val accents = LocalAccents.current
    val palette = LocalPaletteColors.current

    val grouped = dreams.groupBy { YearMonth.from(it.date) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Box(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "NOCTUARY",
                        style = MaterialTheme.typography.displaySmall,
                        color = palette.onBackground,
                    )
                    Text(
                        text = if (total == 0) {
                            "the night archive"
                        } else {
                            "$total ${if (total == 1) "dream" else "dreams"} archived"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = accents.mint,
                    )
                }
                if (palette.poster && total > 0) {
                    // Rubber stamp: "СОН № n" — "dream no. n".
                    Text(
                        text = "СОН № $total",
                        style = MaterialTheme.typography.titleSmall,
                        color = accents.blossom,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 6.dp)
                            .rotate(-6f)
                            .border(2.dp, accents.blossom)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .clearAndSetSemantics { contentDescription = "Dream number $total" },
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = filter.query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Search dreams, tags, fragments…",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = if (palette.poster) palette.ink else accents.lilac,
                    )
                },
                singleLine = true,
                shape = if (palette.poster) RectangleShape else RoundedCornerShape(50),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = if (palette.poster) {
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.ink,
                        unfocusedBorderColor = palette.ink,
                        focusedContainerColor = palette.surface,
                        unfocusedContainerColor = palette.surface,
                        focusedLeadingIconColor = palette.ink,
                        unfocusedLeadingIconColor = palette.ink,
                    )
                } else {
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accents.lilac,
                        unfocusedBorderColor = palette.outline,
                        focusedContainerColor = palette.surface.copy(alpha = 0.7f),
                        unfocusedContainerColor = palette.surface.copy(alpha = 0.55f),
                    )
                },
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(
                    text = "FAVOURITES",
                    selected = filter.favoritesOnly,
                    onClick = vm::toggleFavorites,
                    accent = accents.amber,
                )
                Pill(
                    text = "LUCID",
                    selected = filter.lucidOnly,
                    onClick = vm::toggleLucid,
                    accent = accents.mint,
                )
                Pill(
                    text = "NIGHTMARES",
                    selected = filter.nightmaresOnly,
                    onClick = vm::toggleNightmares,
                    accent = accents.blossom,
                )
            }
        }

        if (tags.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tags, key = { it }) { tag ->
                        Pill(
                            text = "#$tag",
                            selected = filter.tag.equals(tag, ignoreCase = true),
                            onClick = { vm.toggleTag(tag) },
                            accent = accents.sky,
                        )
                    }
                }
            }
        }

        if (filter.isActive) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${dreams.size} of $total shown",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.onSurfaceMuted,
                    )
                    Pill(text = "CLEAR", selected = false, onClick = vm::clearFilters)
                }
            }
        }

        if (dreams.isEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                EmptyState(
                    title = if (total == 0) "The archive is empty" else "Nothing matches",
                    body = if (total == 0) {
                        "Tap the moon button and write down the first thing you remember — " +
                            "even a fragment counts. Recall gets better the moment you start recording it."
                    } else {
                        "Try a different search, or clear the filters."
                    },
                )
            }
        }

        grouped.forEach { (month, entries) ->
            item(key = "header-$month") {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = month.format(MONTH_HEADER).uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (palette.poster) palette.onBackground else accents.lilac,
                    )
                    if (palette.poster) {
                        DashedRule(palette.ink.copy(alpha = 0.4f), Modifier.weight(1f))
                    } else {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(palette.outline.copy(alpha = 0.6f))
                        )
                    }
                    Text(
                        text = entries.size.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.onSurfaceMuted,
                    )
                }
            }
            items(entries, key = { it.id }) { dream ->
                DreamCard(
                    dream = dream,
                    onClick = { onOpenDream(dream.id) },
                    onToggleFavorite = { vm.toggleFavorite(dream) },
                    onTagClick = { vm.toggleTag(it) },
                )
            }
        }
    }
}

@Composable
private fun DreamCard(
    dream: Dream,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTagClick: (String) -> Unit,
) {
    val accents = LocalAccents.current
    val palette = LocalPaletteColors.current
    val accent = when {
        dream.nightmare -> accents.blossom
        dream.lucid -> accents.mint
        dream.recurring -> accents.sky
        else -> accents.lilac
    }

    if (palette.poster) {
        TicketCard(dream, accent, onClick, onToggleFavorite, onTagClick)
        return
    }

    GlassCard(modifier = Modifier.fillMaxWidth(), accent = accent, onClick = onClick) {
        Row(Modifier.padding(14.dp)) {
            // date block
            Column(
                modifier = Modifier
                    .width(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f))
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = dream.date.dayOfMonth.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.headlineSmall,
                    color = accent,
                )
                Text(
                    text = dream.date.format(WEEKDAY).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.onSurfaceMuted,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = dream.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (dream.body.isNotBlank()) {
                    Text(
                        text = dream.body.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.onSurfaceMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (dream.lucid) Marker("LUCID", accents.mint)
                    if (dream.nightmare) Marker("NIGHTMARE", accents.blossom)
                    if (dream.recurring) Marker("RECURRING", accents.sky)
                    MoodDots(dream.mood, accent)
                }
                if (dream.tags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(dream.tags, key = { it }) { tag ->
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = accents.sky,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onTagClick(tag) }
                                    .border(
                                        1.dp,
                                        accents.sky.copy(alpha = 0.4f),
                                        RoundedCornerShape(50),
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (dream.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (dream.favorite) "Remove from favourites" else "Add to favourites",
                    tint = if (dream.favorite) accents.amber else palette.onSurfaceMuted,
                )
            }
        }
    }
}

@Composable
private fun Marker(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun MoodDots(mood: Int, accent: Color) {
    val palette = LocalPaletteColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(5) { i ->
            Box(
                Modifier
                    .size(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (i < mood) accent else palette.outline.copy(alpha = 0.7f)
                    )
            )
        }
    }
}

/**
 * Kosmos dream card: a tram-ticket stub. The coloured stub carries the date
 * and is torn off along a dashed perforation; the body sits on paper.
 * The stub is painted behind the row (rather than measured) so the card
 * keeps working with the lazy tag row inside it.
 */
@Composable
private fun TicketCard(
    dream: Dream,
    accent: Color,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTagClick: (String) -> Unit,
) {
    val accents = LocalAccents.current
    val palette = LocalPaletteColors.current
    val stubWidth = 60.dp
    val stubFill = palette.fillFor(accent)
    val perforation = palette.ink

    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            Modifier.drawBehind {
                val sw = stubWidth.toPx()
                drawRect(stubFill, size = Size(sw, size.height))
                val t = 2.dp.toPx()
                drawLine(
                    color = perforation,
                    start = Offset(sw - t / 2, 0f),
                    end = Offset(sw - t / 2, size.height),
                    strokeWidth = t,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(t * 2.5f, t * 2f)),
                )
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier
                    .width(stubWidth)
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = dream.date.dayOfMonth.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.onFill,
                )
                Text(
                    text = dream.date.format(WEEKDAY).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.onFill,
                )
            }

            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = dream.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (dream.body.isNotBlank()) {
                    Text(
                        text = dream.body.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.onSurfaceMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (dream.lucid) PosterMarker("LUCID", accents.mint)
                    if (dream.nightmare) PosterMarker("NIGHTMARE", accents.blossom)
                    if (dream.recurring) PosterMarker("RECURRING", accents.sky)
                    Spacer(Modifier.weight(1f))
                    MoodLamps(dream.mood, accent)
                }
                if (dream.tags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(dream.tags, key = { it }) { tag ->
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = accents.sky,
                                modifier = Modifier
                                    .clickable { onTagClick(tag) }
                                    .padding(vertical = 3.dp),
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (dream.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (dream.favorite) "Remove from favourites" else "Add to favourites",
                    tint = if (dream.favorite) accents.amber else palette.onSurfaceMuted,
                )
            }
        }
    }
}

@Composable
private fun PosterMarker(text: String, color: Color) {
    val palette = LocalPaletteColors.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = palette.onFill,
        modifier = Modifier
            .background(palette.fillFor(color))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Mood as five square indicator lamps, lit from the left. */
@Composable
private fun MoodLamps(mood: Int, accent: Color) {
    val palette = LocalPaletteColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "Mood $mood of 5"
        },
    ) {
        repeat(5) { i ->
            Box(
                Modifier
                    .size(7.dp)
                    .then(
                        if (i < mood) {
                            Modifier.background(accent)
                        } else {
                            Modifier.border(1.5.dp, palette.ink)
                        }
                    )
            )
        }
    }
}
