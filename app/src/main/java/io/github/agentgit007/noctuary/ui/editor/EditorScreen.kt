/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.agentgit007.noctuary.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.agentgit007.noctuary.NoctuaryViewModel
import io.github.agentgit007.noctuary.data.date
import io.github.agentgit007.noctuary.ui.components.GlassCard
import io.github.agentgit007.noctuary.ui.components.Pill
import io.github.agentgit007.noctuary.ui.components.SectionLabel
import io.github.agentgit007.noctuary.ui.theme.LocalAccents
import io.github.agentgit007.noctuary.ui.theme.LocalPaletteColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FULL_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy")

private val MOOD_LABELS = listOf("bleak", "uneasy", "even", "warm", "blissful")
private val CLARITY_LABELS = listOf("fragments", "hazy", "clear", "sharp", "vivid")

@Composable
fun EditorScreen(
    vm: NoctuaryViewModel,
    contentPadding: PaddingValues,
    onDone: () -> Unit,
) {
    val draft by vm.draft.collectAsState()
    val accents = LocalAccents.current
    val palette = LocalPaletteColors.current

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }

    val dream = draft ?: return

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = contentPadding.calculateTopPadding() + 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 120.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onDone) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.onBackground,
                    )
                }
                Text(
                    text = if (dream.id == 0L) "NEW ENTRY" else "EDIT ENTRY",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.onSurfaceMuted,
                )
                if (dream.id == 0L) {
                    Spacer(Modifier.size(48.dp))
                } else {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete dream",
                            tint = accents.blossom,
                        )
                    }
                }
            }

            // ------------------------------------------------------ date --
            SectionLabel("Night of", accent = accents.lilac)
            GlassCard(Modifier.fillMaxWidth(), accent = accents.lilac) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(accents.lilac.copy(alpha = 0.14f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = accents.lilac,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = dream.date.format(FULL_DATE),
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.onBackground,
                            )
                            Text(
                                text = relativeLabel(dream.date),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.onSurfaceMuted,
                            )
                        }
                        Pill(
                            text = "CHANGE",
                            selected = false,
                            onClick = { showDatePicker = true },
                            accent = accents.lilac,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val today = LocalDate.now()
                        listOf(
                            "LAST NIGHT" to today,
                            "NIGHT BEFORE" to today.minusDays(1),
                            "2 NIGHTS AGO" to today.minusDays(2),
                        ).forEach { (label, date) ->
                            Pill(
                                text = label,
                                selected = dream.date == date,
                                onClick = { vm.editDraft { it.copy(dreamDate = date.toEpochDay()) } },
                                accent = accents.mint,
                            )
                        }
                    }
                }
            }

            // ------------------------------------------------- the dream --
            SectionLabel("The dream", accent = accents.mint)
            OutlinedTextField(
                value = dream.title,
                onValueChange = { value -> vm.editDraft { it.copy(title = value) } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Title (optional)") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = fieldColors(accents.mint, palette.outline, palette.surface),
            )
            OutlinedTextField(
                value = dream.body,
                onValueChange = { value -> vm.editDraft { it.copy(body = value) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp),
                placeholder = {
                    Text("What happened? Write it before it fades — fragments are fine.")
                },
                shape = MaterialTheme.shapes.small,
                colors = fieldColors(accents.mint, palette.outline, palette.surface),
            )

            // ------------------------------------------------ the rating --
            SectionLabel("How it felt", accent = accents.blossom)
            ScaleSelector(
                value = dream.mood,
                labels = MOOD_LABELS,
                accent = accents.blossom,
                caption = "Mood",
                onSelect = { v -> vm.editDraft { it.copy(mood = v) } },
            )
            ScaleSelector(
                value = dream.clarity,
                labels = CLARITY_LABELS,
                accent = accents.sky,
                caption = "Clarity",
                onSelect = { v -> vm.editDraft { it.copy(clarity = v) } },
            )

            SectionLabel("Markers", accent = accents.amber)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(
                    text = "LUCID",
                    selected = dream.lucid,
                    onClick = { vm.editDraft { it.copy(lucid = !it.lucid) } },
                    accent = accents.mint,
                )
                Pill(
                    text = "NIGHTMARE",
                    selected = dream.nightmare,
                    onClick = { vm.editDraft { it.copy(nightmare = !it.nightmare) } },
                    accent = accents.blossom,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(
                    text = "RECURRING",
                    selected = dream.recurring,
                    onClick = { vm.editDraft { it.copy(recurring = !it.recurring) } },
                    accent = accents.sky,
                )
                Pill(
                    text = "FAVOURITE",
                    selected = dream.favorite,
                    onClick = { vm.editDraft { it.copy(favorite = !it.favorite) } },
                    accent = accents.amber,
                )
            }

            // ------------------------------------------------------ tags --
            SectionLabel("Tags", accent = accents.sky)
            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("flying, water, childhood home…") },
                singleLine = true,
                shape = RoundedCornerShape(50),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    tagInput.split(',').forEach(vm::addTagToDraft)
                    tagInput = ""
                }),
                trailingIcon = {
                    TextButton(onClick = {
                        tagInput.split(',').forEach(vm::addTagToDraft)
                        tagInput = ""
                    }) { Text("ADD", color = accents.sky) }
                },
                colors = fieldColors(accents.sky, palette.outline, palette.surface),
            )
            if (dream.tags.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dream.tags.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { tag ->
                                    Pill(
                                        text = "#$tag  ×",
                                        selected = true,
                                        onClick = { vm.removeTagFromDraft(tag) },
                                        accent = accents.sky,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { if (vm.saveDraft()) onDone() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 20.dp,
                    bottom = contentPadding.calculateBottomPadding() + 24.dp,
                ),
            containerColor = accents.mint,
            contentColor = palette.background,
            icon = { Icon(Icons.Filled.Check, contentDescription = null) },
            text = { Text("SAVE", style = MaterialTheme.typography.labelLarge) },
        )
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dream.dreamDate * MILLIS_PER_DAY,
            selectableDates = PastOnly,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        vm.editDraft { it.copy(dreamDate = picked.toEpochDay()) }
                    }
                    showDatePicker = false
                }) { Text("SET DATE") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("CANCEL") }
            },
        ) {
            DatePicker(state = state, showModeToggle = true)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this dream?") },
            text = { Text("This cannot be undone. Export a backup first if you want to keep it.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    vm.deleteDraft()
                    onDone()
                }) { Text("DELETE", color = accents.blossom) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("KEEP") }
            },
        )
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

/** A dream cannot be remembered from a morning that has not happened yet. */
private object PastOnly : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis <= (LocalDate.now().toEpochDay() + 1) * MILLIS_PER_DAY

    override fun isSelectableYear(year: Int): Boolean = year <= LocalDate.now().year
}

@Composable
private fun ScaleSelector(
    value: Int,
    labels: List<String>,
    accent: Color,
    caption: String,
    onSelect: (Int) -> Unit,
) {
    val palette = LocalPaletteColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = caption.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = palette.onSurfaceMuted,
            )
            Text(
                text = labels.getOrElse(value - 1) { "" },
                style = MaterialTheme.typography.labelMedium,
                color = accent,
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (1..5).forEach { step ->
                val selected = step <= value
                Box(
                    Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) accent.copy(alpha = 0.18f + 0.1f * step)
                            else palette.surface.copy(alpha = 0.5f)
                        )
                        .clickable { onSelect(step) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = step.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (step == value) accent else palette.onSurfaceMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun fieldColors(
    focused: Color,
    unfocused: Color,
    container: Color,
) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = focused,
    unfocusedBorderColor = unfocused,
    focusedContainerColor = container.copy(alpha = 0.72f),
    unfocusedContainerColor = container.copy(alpha = 0.55f),
    cursorColor = focused,
)

private fun relativeLabel(date: LocalDate): String {
    val days = LocalDate.now().toEpochDay() - date.toEpochDay()
    return when {
        days == 0L -> "last night"
        days == 1L -> "the night before last"
        days < 0L -> "in the future"
        days < 7L -> "$days nights ago"
        days < 30L -> "${days / 7} ${if (days / 7 == 1L) "week" else "weeks"} ago"
        days < 365L -> "${days / 30} ${if (days / 30 == 1L) "month" else "months"} ago"
        else -> "${days / 365} ${if (days / 365 == 1L) "year" else "years"} ago"
    }
}
