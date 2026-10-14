/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package io.github.agentgit007.noctuary.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, NIGHT, DAY }

/** Named pastel palettes. Order is stable — it is persisted by name. */
enum class Palette { AURORA, SUNSET, CASSETTE }

data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val palette: Palette = Palette.AURORA,
    val scanlines: Boolean = true,
)

/**
 * Plain SharedPreferences — no DataStore dependency, nothing leaves the device.
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("noctuary_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private fun read() = Settings(
        themeMode = enumOrDefault(prefs.getString(KEY_THEME, null), ThemeMode.SYSTEM),
        palette = enumOrDefault(prefs.getString(KEY_PALETTE, null), Palette.AURORA),
        scanlines = prefs.getBoolean(KEY_SCANLINES, true),
    )

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setPalette(palette: Palette) {
        prefs.edit().putString(KEY_PALETTE, palette.name).apply()
        _settings.value = _settings.value.copy(palette = palette)
    }

    fun setScanlines(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCANLINES, enabled).apply()
        _settings.value = _settings.value.copy(scanlines = enabled)
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: fallback

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_PALETTE = "palette"
        const val KEY_SCANLINES = "scanlines"
    }
}
