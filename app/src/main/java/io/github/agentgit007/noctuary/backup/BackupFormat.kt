/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package io.github.agentgit007.noctuary.backup

import io.github.agentgit007.noctuary.data.Dream
import io.github.agentgit007.noctuary.data.date
import io.github.agentgit007.noctuary.data.displayTitle
import io.github.agentgit007.noctuary.data.wordCount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * The on-disk backup schema. Dates are ISO-8601 strings so a backup stays
 * readable (and hand-editable) years from now, and stays correct if the
 * phone's timezone changes between export and import.
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    @SerialName("exported_at") val exportedAt: String = "",
    val count: Int = 0,
    val dreams: List<BackupDream> = emptyList(),
) {
    companion object {
        const val FORMAT = "noctuary.journal"
        const val VERSION = 1
    }
}

@Serializable
data class BackupDream(
    val date: String,
    val title: String = "",
    val body: String = "",
    val mood: Int = 3,
    val clarity: Int = 3,
    val lucid: Boolean = false,
    val nightmare: Boolean = false,
    val recurring: Boolean = false,
    val favorite: Boolean = false,
    val tags: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

object BackupFormat {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // ------------------------------------------------------------- export --

    fun toJson(dreams: List<Dream>): String {
        val file = BackupFile(
            exportedAt = Instant.now().toString(),
            count = dreams.size,
            dreams = dreams.sortedBy { it.dreamDate }.map { it.toBackup() },
        )
        return json.encodeToString(BackupFile.serializer(), file)
    }

    fun toMarkdown(dreams: List<Dream>): String = buildString {
        appendLine("# Dream journal")
        appendLine()
        appendLine("_${dreams.size} entries · exported ${LocalDate.now()}_")
        appendLine()
        dreams.sortedByDescending { it.dreamDate }.forEach { d ->
            appendLine("## ${d.date} — ${d.displayTitle}")
            appendLine()
            val flags = buildList {
                if (d.lucid) add("lucid")
                if (d.nightmare) add("nightmare")
                if (d.recurring) add("recurring")
                if (d.favorite) add("favourite")
            }
            appendLine("mood ${d.mood}/5 · clarity ${d.clarity}/5" + if (flags.isEmpty()) "" else " · " + flags.joinToString(" · "))
            if (d.tags.isNotEmpty()) appendLine()
            if (d.tags.isNotEmpty()) appendLine(d.tags.joinToString(" ") { "#$it" })
            appendLine()
            appendLine(d.body.trim())
            appendLine()
            appendLine("---")
            appendLine()
        }
    }

    fun toCsv(dreams: List<Dream>): String = buildString {
        appendLine("date,title,mood,clarity,lucid,nightmare,recurring,favorite,tags,words,body")
        dreams.sortedBy { it.dreamDate }.forEach { d ->
            appendLine(
                listOf(
                    d.date.toString(),
                    d.title,
                    d.mood.toString(),
                    d.clarity.toString(),
                    d.lucid.toString(),
                    d.nightmare.toString(),
                    d.recurring.toString(),
                    d.favorite.toString(),
                    d.tags.joinToString(";"),
                    d.wordCount.toString(),
                    d.body,
                ).joinToString(",") { csv(it) }
            )
        }
    }

    private fun csv(value: String): String =
        "\"" + value.replace("\"", "\"\"").replace("\r\n", "\n") + "\""

    // ------------------------------------------------------------- import --

    /**
     * Parses a Noctuary backup. Falls back to a best-effort reading of any
     * JSON that looks like a list of journal entries, so exports from other
     * dream journals can usually be brought across without hand-editing.
     */
    fun parse(text: String): Result<List<Dream>> = runCatching {
        val root = json.parseToJsonElement(text.trim())
        val entries: List<JsonObject> = when {
            root is JsonArray -> root.mapNotNull { it as? JsonObject }
            root is JsonObject && root["dreams"] is JsonArray ->
                root["dreams"]!!.jsonArray.mapNotNull { it as? JsonObject }
            root is JsonObject && root["entries"] is JsonArray ->
                root["entries"]!!.jsonArray.mapNotNull { it as? JsonObject }
            root is JsonObject && root["items"] is JsonArray ->
                root["items"]!!.jsonArray.mapNotNull { it as? JsonObject }
            else -> throw IllegalArgumentException("No dream list found in this file.")
        }
        if (entries.isEmpty()) throw IllegalArgumentException("The file contains no entries.")
        entries.map { it.toDream() }
    }

    private fun Dream.toBackup() = BackupDream(
        date = date.format(ISO),
        title = title,
        body = body,
        mood = mood,
        clarity = clarity,
        lucid = lucid,
        nightmare = nightmare,
        recurring = recurring,
        favorite = favorite,
        tags = tags,
        createdAt = Instant.ofEpochMilli(createdAt).toString(),
        updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
    )

    private fun JsonObject.toDream(): Dream {
        val date = pickDate("date", "dream_date", "dreamDate", "dreamedOn", "day", "timestamp", "created_at", "createdAt")
            ?: LocalDate.now()
        return Dream(
            dreamDate = date.toEpochDay(),
            title = pickString("title", "name", "heading").orEmpty(),
            body = pickString("body", "content", "text", "description", "entry").orEmpty(),
            mood = pickInt("mood", "rating")?.coerceIn(1, 5) ?: 3,
            clarity = pickInt("clarity", "vividness", "lucidity_level")?.coerceIn(1, 5) ?: 3,
            lucid = pickBoolean("lucid", "isLucid", "is_lucid") ?: false,
            nightmare = pickBoolean("nightmare", "isNightmare", "is_nightmare") ?: false,
            recurring = pickBoolean("recurring", "isRecurring", "is_recurring") ?: false,
            favorite = pickBoolean("favorite", "favourite", "starred", "isFavorite", "is_favorite") ?: false,
            tags = pickTags(),
            createdAt = pickInstant("created_at", "createdAt") ?: System.currentTimeMillis(),
            updatedAt = pickInstant("updated_at", "updatedAt") ?: System.currentTimeMillis(),
        )
    }

    private fun JsonObject.value(vararg keys: String): JsonElement? =
        keys.firstNotNullOfOrNull { this[it]?.takeIf { v -> v !is JsonPrimitive || v.content != "null" } }

    private fun JsonObject.pickString(vararg keys: String): String? =
        (value(*keys) as? JsonPrimitive)?.content

    private fun JsonObject.pickInt(vararg keys: String): Int? {
        val p = value(*keys) as? JsonPrimitive ?: return null
        return p.intOrNull ?: p.content.toDoubleOrNull()?.toInt()
    }

    private fun JsonObject.pickBoolean(vararg keys: String): Boolean? {
        val p = value(*keys) as? JsonPrimitive ?: return null
        return p.booleanOrNull ?: when (p.content.lowercase()) {
            "1", "yes", "y" -> true
            "0", "no", "n" -> false
            else -> null
        }
    }

    private fun JsonObject.pickTags(): List<String> {
        val raw = value("tags", "labels", "categories") ?: return emptyList()
        return when (raw) {
            is JsonArray -> raw.mapNotNull { element ->
                when (element) {
                    is JsonPrimitive -> element.content
                    is JsonObject -> (element["name"] ?: element["label"] ?: element["tag"])
                        ?.jsonPrimitive?.content
                    else -> null
                }
            }
            is JsonPrimitive -> raw.content.split(',', ';', '#')
            else -> emptyList()
        }.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    private fun JsonObject.pickDate(vararg keys: String): LocalDate? {
        val p = value(*keys) as? JsonPrimitive ?: return null
        return parseDate(p.content)
    }

    private fun JsonObject.pickInstant(vararg keys: String): Long? {
        val p = value(*keys) as? JsonPrimitive ?: return null
        p.content.toLongOrNull()?.let { return normaliseMillis(it) }
        return runCatching { Instant.parse(p.content).toEpochMilli() }.getOrNull()
    }

    private fun parseDate(raw: String): LocalDate? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        value.toLongOrNull()?.let { number ->
            // Epoch days, seconds or milliseconds — tell them apart by magnitude.
            return when {
                number < 100_000L -> LocalDate.ofEpochDay(number)
                number < 100_000_000_000L ->
                    Instant.ofEpochSecond(number).atZone(ZoneId.systemDefault()).toLocalDate()
                else ->
                    Instant.ofEpochMilli(number).atZone(ZoneId.systemDefault()).toLocalDate()
            }
        }
        runCatching { return LocalDate.parse(value.take(10), ISO) }
        runCatching { return Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate() }
        return try {
            LocalDate.parse(value)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun normaliseMillis(number: Long): Long =
        if (number < 100_000_000_000L) number * 1000 else number
}
