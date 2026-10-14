/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package io.github.agentgit007.noctuary.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * One recorded dream.
 *
 * [dreamDate] is the *morning* the dream was remembered on, stored as an epoch
 * day so it is timezone-stable: a journal entry must not shift because the
 * phone crossed a timezone. It is set by the user and is independent of
 * [createdAt], which is when the entry was typed.
 *
 * Everything derived from these columns lives in the extension properties
 * below rather than in the class, so Room only ever sees real columns.
 */
@Entity(tableName = "dreams")
data class Dream(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "dream_date") val dreamDate: Long,
    val title: String = "",
    val body: String = "",
    /** 1 (bleak) .. 5 (blissful). */
    val mood: Int = 3,
    /** 1 (fragments) .. 5 (vivid). */
    val clarity: Int = 3,
    val lucid: Boolean = false,
    val nightmare: Boolean = false,
    val recurring: Boolean = false,
    val favorite: Boolean = false,
    val tags: List<String> = emptyList(),
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        fun blank(date: LocalDate = LocalDate.now()) = Dream(dreamDate = date.toEpochDay())
    }
}

/** The night this entry belongs to. */
val Dream.date: LocalDate get() = LocalDate.ofEpochDay(dreamDate)

/** Title if there is one, otherwise the opening line of the entry. */
val Dream.displayTitle: String
    get() = title.ifBlank {
        body.trim().lineSequence().firstOrNull()?.take(60)?.ifBlank { null } ?: "Untitled dream"
    }

val Dream.wordCount: Int
    get() = body.split(WHITESPACE).count { it.isNotBlank() }

private val WHITESPACE = Regex("\\s+")

/** Mood and clarity are stored as plain ints; tags need a converter. */
class Converters {
    @TypeConverter
    fun fromTags(tags: List<String>): String = tags.joinToString(SEPARATOR)

    @TypeConverter
    fun toTags(raw: String): List<String> =
        if (raw.isEmpty()) emptyList() else raw.split(SEPARATOR).filter { it.isNotBlank() }

    private companion object {
        /** ASCII unit separator — cannot occur in user input from a soft keyboard. */
        const val SEPARATOR = "\u001F"
    }
}
