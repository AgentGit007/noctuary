/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package io.github.agentgit007.noctuary

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.agentgit007.noctuary.backup.BackupFormat
import io.github.agentgit007.noctuary.data.Dream
import io.github.agentgit007.noctuary.data.DreamRepository
import io.github.agentgit007.noctuary.data.Palette
import io.github.agentgit007.noctuary.data.Settings
import io.github.agentgit007.noctuary.data.SettingsStore
import io.github.agentgit007.noctuary.data.ThemeMode
import io.github.agentgit007.noctuary.stats.DreamStats
import io.github.agentgit007.noctuary.stats.StatsEngine
import io.github.agentgit007.noctuary.stats.StatsRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

enum class ExportFormat(val label: String, val mime: String, val extension: String) {
    JSON("JSON backup", "application/json", "json"),
    MARKDOWN("Markdown", "text/markdown", "md"),
    CSV("CSV", "text/csv", "csv"),
}

enum class ImportMode { MERGE, REPLACE }

data class JournalFilter(
    val query: String = "",
    val tag: String? = null,
    val favoritesOnly: Boolean = false,
    val lucidOnly: Boolean = false,
    val nightmaresOnly: Boolean = false,
) {
    val isActive: Boolean
        get() = query.isNotBlank() || tag != null || favoritesOnly || lucidOnly || nightmaresOnly
}

class NoctuaryViewModel(
    private val repo: DreamRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    val settings: StateFlow<Settings> = settingsStore.settings

    /** The whole journal, newest first. Screens that need raw entries read this. */
    val dreams: StateFlow<List<Dream>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _filter = MutableStateFlow(JournalFilter())
    val filter: StateFlow<JournalFilter> = _filter.asStateFlow()

    private val _statsRange = MutableStateFlow(StatsRange.QUARTER)
    val statsRange: StateFlow<StatsRange> = _statsRange.asStateFlow()

    private val _draft = MutableStateFlow<Dream?>(null)
    val draft: StateFlow<Dream?> = _draft.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    val visibleDreams: StateFlow<List<Dream>> =
        combine(dreams, _filter) { list, f -> apply(list, f) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allTags: StateFlow<List<String>> = dreams
        .map { list ->
            list.flatMap { it.tags }
                .groupingBy { it.lowercase() }
                .eachCount()
                .entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .map { it.key }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalCount: StateFlow<Int> = dreams
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val stats: StateFlow<DreamStats> =
        combine(dreams, _statsRange) { list, range -> list to range }
            .map { (list, range) -> StatsEngine.compute(list, range) }
            .flowOn(Dispatchers.Default)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                StatsEngine.compute(emptyList(), StatsRange.QUARTER),
            )

    // ------------------------------------------------------------ filters --

    fun setQuery(value: String) {
        _filter.value = _filter.value.copy(query = value)
    }

    fun toggleTag(tag: String?) {
        val current = _filter.value
        _filter.value = current.copy(tag = if (current.tag == tag) null else tag)
    }

    fun toggleFavorites() {
        _filter.value = _filter.value.copy(favoritesOnly = !_filter.value.favoritesOnly)
    }

    fun toggleLucid() {
        _filter.value = _filter.value.copy(lucidOnly = !_filter.value.lucidOnly)
    }

    fun toggleNightmares() {
        _filter.value = _filter.value.copy(nightmaresOnly = !_filter.value.nightmaresOnly)
    }

    fun clearFilters() {
        _filter.value = JournalFilter()
    }

    private fun apply(list: List<Dream>, f: JournalFilter): List<Dream> {
        val q = f.query.trim().lowercase()
        return list.filter { d ->
            (!f.favoritesOnly || d.favorite) &&
                (!f.lucidOnly || d.lucid) &&
                (!f.nightmaresOnly || d.nightmare) &&
                (f.tag == null || d.tags.any { it.equals(f.tag, ignoreCase = true) }) &&
                (q.isEmpty() ||
                    d.title.lowercase().contains(q) ||
                    d.body.lowercase().contains(q) ||
                    d.tags.any { it.lowercase().contains(q) })
        }
    }

    fun setStatsRange(range: StatsRange) {
        _statsRange.value = range
    }

    // ------------------------------------------------------------- editor --

    fun openEditor(id: Long) {
        viewModelScope.launch {
            _draft.value = if (id <= 0L) Dream.blank() else repo.byId(id) ?: Dream.blank()
        }
    }

    fun openEditorFor(date: LocalDate) {
        _draft.value = Dream.blank(date)
    }

    fun editDraft(transform: (Dream) -> Dream) {
        _draft.value = _draft.value?.let(transform)
    }

    fun addTagToDraft(raw: String) {
        val tag = raw.trim().removePrefix("#").lowercase()
        if (tag.isEmpty()) return
        editDraft { d -> if (d.tags.any { it.equals(tag, true) }) d else d.copy(tags = d.tags + tag) }
    }

    fun removeTagFromDraft(tag: String) {
        editDraft { d -> d.copy(tags = d.tags.filterNot { it.equals(tag, true) }) }
    }

    /** @return true when something was stored (an entirely empty entry is discarded). */
    fun saveDraft(): Boolean {
        val d = _draft.value ?: return false
        if (d.title.isBlank() && d.body.isBlank() && d.tags.isEmpty()) {
            _message.value = "Nothing to save yet."
            return false
        }
        viewModelScope.launch {
            repo.save(d)
            _message.value = if (d.id == 0L) "Dream recorded." else "Dream updated."
        }
        return true
    }

    fun deleteDraft() {
        val d = _draft.value ?: return
        if (d.id == 0L) return
        viewModelScope.launch {
            repo.delete(d)
            _message.value = "Dream deleted."
        }
    }

    fun toggleFavorite(dream: Dream) {
        viewModelScope.launch { repo.toggleFavorite(dream) }
    }

    fun delete(dream: Dream) {
        viewModelScope.launch {
            repo.delete(dream)
            _message.value = "Dream deleted."
        }
    }

    // ------------------------------------------------------------ backups --

    fun export(resolver: ContentResolver, uri: Uri, format: ExportFormat) {
        viewModelScope.launch {
            _busy.value = true
            val result = runCatching {
                val all = repo.all()
                val text = when (format) {
                    ExportFormat.JSON -> BackupFormat.toJson(all)
                    ExportFormat.MARKDOWN -> BackupFormat.toMarkdown(all)
                    ExportFormat.CSV -> BackupFormat.toCsv(all)
                }
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri, "wt")?.use { out ->
                        out.write(text.toByteArray(Charsets.UTF_8))
                    } ?: error("Could not open that file for writing.")
                }
                all.size
            }
            _busy.value = false
            _message.value = result.fold(
                onSuccess = { "Exported $it ${if (it == 1) "dream" else "dreams"} as ${format.label}." },
                onFailure = { "Export failed: ${it.message ?: "unknown error"}" },
            )
        }
    }

    fun import(resolver: ContentResolver, uri: Uri, mode: ImportMode) {
        viewModelScope.launch {
            _busy.value = true
            val result = runCatching {
                val text = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().use { reader -> reader.readText() }
                    } ?: error("Could not read that file.")
                }
                val parsed = BackupFormat.parse(text).getOrThrow()
                when (mode) {
                    ImportMode.REPLACE -> {
                        repo.replaceAll(parsed); parsed.size
                    }
                    ImportMode.MERGE -> repo.merge(parsed)
                }
            }
            _busy.value = false
            _message.value = result.fold(
                onSuccess = {
                    when (mode) {
                        ImportMode.REPLACE -> "Journal replaced with $it entries."
                        ImportMode.MERGE -> if (it == 0) "Nothing new — those entries were already here." else "Added $it new entries."
                    }
                },
                onFailure = { "Import failed: ${it.message ?: "unreadable file"}" },
            )
        }
    }

    fun deleteEverything() {
        viewModelScope.launch {
            repo.deleteAll()
            _message.value = "Journal erased."
        }
    }

    fun suggestedFileName(format: ExportFormat): String =
        "noctuary-${LocalDate.now()}.${format.extension}"

    // ----------------------------------------------------------- settings --

    fun setThemeMode(mode: ThemeMode) = settingsStore.setThemeMode(mode)

    fun setPalette(palette: Palette) = settingsStore.setPalette(palette)

    fun setScanlines(enabled: Boolean) = settingsStore.setScanlines(enabled)

    fun consumeMessage() {
        _message.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as NoctuaryApplication
                return NoctuaryViewModel(app.container.repository, app.container.settings) as T
            }
        }
    }
}
