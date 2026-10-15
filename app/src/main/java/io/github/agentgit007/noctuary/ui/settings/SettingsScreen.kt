/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package io.github.agentgit007.noctuary.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.agentgit007.noctuary.BuildConfig
import io.github.agentgit007.noctuary.ExportFormat
import io.github.agentgit007.noctuary.ImportMode
import io.github.agentgit007.noctuary.NoctuaryViewModel
import io.github.agentgit007.noctuary.data.Palette
import io.github.agentgit007.noctuary.data.ThemeMode
import io.github.agentgit007.noctuary.ui.components.GlassCard
import io.github.agentgit007.noctuary.ui.components.Pill
import io.github.agentgit007.noctuary.ui.components.SectionLabel
import io.github.agentgit007.noctuary.ui.theme.LocalAccents
import io.github.agentgit007.noctuary.ui.theme.LocalPaletteColors

@Composable
fun SettingsScreen(
    vm: NoctuaryViewModel,
    contentPadding: PaddingValues,
) {
    val settings by vm.settings.collectAsState()
    val total by vm.totalCount.collectAsState()
    val busy by vm.busy.collectAsState()
    val accents = LocalAccents.current
    val palette = LocalPaletteColors.current
    val context = LocalContext.current

    var pendingImportMode by remember { mutableStateOf(ImportMode.MERGE) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showEraseDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.import(context.contentResolver, uri, pendingImportMode)
    }
    val exportJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportFormat.JSON.mime)
    ) { uri -> if (uri != null) vm.export(context.contentResolver, uri, ExportFormat.JSON) }
    val exportMarkdown = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportFormat.MARKDOWN.mime)
    ) { uri -> if (uri != null) vm.export(context.contentResolver, uri, ExportFormat.MARKDOWN) }
    val exportCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportFormat.CSV.mime)
    ) { uri -> if (uri != null) vm.export(context.contentResolver, uri, ExportFormat.CSV) }

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
            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.displaySmall,
                color = palette.onBackground,
            )
        }

        // ---------------------------------------------------- appearance --
        item {
            GlassCard(Modifier.fillMaxWidth(), accent = accents.lilac) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SectionLabel("Appearance", accent = accents.lilac)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.onSurfaceMuted,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.entries.forEach { mode ->
                                Pill(
                                    text = mode.name,
                                    selected = settings.themeMode == mode,
                                    onClick = { vm.setThemeMode(mode) },
                                    accent = accents.lilac,
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Palette",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.onSurfaceMuted,
                        )
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Palette.entries.forEach { p ->
                                Pill(
                                    text = p.name,
                                    selected = settings.palette == p,
                                    onClick = { vm.setPalette(p) },
                                    accent = accents.mint,
                                )
                            }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (palette.poster) "Ruled paper" else "Scanlines",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.onBackground,
                            )
                            Text(
                                if (palette.poster) {
                                    "Faint exercise-book lines behind the page."
                                } else {
                                    "CRT texture over the horizon."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.onSurfaceMuted,
                            )
                        }
                        Switch(
                            checked = settings.scanlines,
                            onCheckedChange = vm::setScanlines,
                            colors = if (palette.poster) {
                                SwitchDefaults.colors(
                                    checkedThumbColor = palette.surface,
                                    checkedTrackColor = palette.ink,
                                    checkedBorderColor = palette.ink,
                                    uncheckedThumbColor = palette.ink,
                                    uncheckedTrackColor = palette.surface,
                                    uncheckedBorderColor = palette.ink,
                                )
                            } else {
                                SwitchDefaults.colors(
                                    checkedThumbColor = palette.background,
                                    checkedTrackColor = accents.mint,
                                    uncheckedTrackColor = palette.surfaceHigh,
                                )
                            },
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------- data --
        item {
            GlassCard(Modifier.fillMaxWidth(), accent = accents.mint) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionLabel("Your data", accent = accents.mint)
                    Text(
                        text = "$total ${if (total == 1) "entry" else "entries"} stored on this " +
                            "device only. JSON is the full backup — it restores everything. " +
                            "Markdown and CSV are one-way exports for reading elsewhere.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.onSurfaceMuted,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(
                            text = "EXPORT JSON",
                            selected = false,
                            onClick = { exportJson.launch(vm.suggestedFileName(ExportFormat.JSON)) },
                            accent = accents.mint,
                        )
                        Pill(
                            text = "MARKDOWN",
                            selected = false,
                            onClick = {
                                exportMarkdown.launch(vm.suggestedFileName(ExportFormat.MARKDOWN))
                            },
                            accent = accents.sky,
                        )
                        Pill(
                            text = "CSV",
                            selected = false,
                            onClick = { exportCsv.launch(vm.suggestedFileName(ExportFormat.CSV)) },
                            accent = accents.amber,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(
                            text = "IMPORT",
                            selected = false,
                            onClick = { showImportDialog = true },
                            accent = accents.lilac,
                        )
                        Pill(
                            text = "ERASE ALL",
                            selected = false,
                            onClick = { showEraseDialog = true },
                            accent = accents.blossom,
                        )
                    }
                    if (busy) {
                        Text(
                            text = "Working…",
                            style = MaterialTheme.typography.labelMedium,
                            color = accents.amber,
                        )
                    }
                }
            }
        }

        // --------------------------------------------------------- about --
        item {
            GlassCard(Modifier.fillMaxWidth(), accent = accents.sky) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SectionLabel("About", accent = accents.sky)
                    Text(
                        text = "Noctuary ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.onBackground,
                    )
                    Text(
                        text = "Free software under the GNU GPL v3 or later. No accounts, no " +
                            "network permission, no analytics: the app cannot send your dreams " +
                            "anywhere because it cannot reach the network at all.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.onSurfaceMuted,
                    )
                    Text(
                        text = "Inspired by Midnight by misterwaztaken (GPL-3.0).",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.onSurfaceMuted,
                    )
                    Text(
                        text = "Kosmos headlines are set in Oswald © The Oswald Project Authors, " +
                            "SIL Open Font License 1.1.",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.onSurfaceMuted,
                    )
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import entries") },
            text = {
                Text(
                    "Merge keeps what is already here and skips entries that look identical. " +
                        "Replace erases the journal first. Replace cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportMode = ImportMode.MERGE
                    showImportDialog = false
                    importLauncher.launch(IMPORT_TYPES)
                }) { Text("MERGE") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingImportMode = ImportMode.REPLACE
                    showImportDialog = false
                    importLauncher.launch(IMPORT_TYPES)
                }) { Text("REPLACE", color = accents.blossom) }
            },
        )
    }

    if (showEraseDialog) {
        AlertDialog(
            onDismissRequest = { showEraseDialog = false },
            title = { Text("Erase the whole journal?") },
            text = { Text("Every entry on this device will be deleted. Export a backup first.") },
            confirmButton = {
                TextButton(onClick = {
                    showEraseDialog = false
                    vm.deleteEverything()
                }) { Text("ERASE", color = accents.blossom) }
            },
            dismissButton = {
                TextButton(onClick = { showEraseDialog = false }) { Text("CANCEL") }
            },
        )
    }
}

private val IMPORT_TYPES = arrayOf(
    "application/json",
    "text/plain",
    "text/*",
    "*/*",
)
