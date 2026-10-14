/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package io.github.agentgit007.noctuary.data

import kotlinx.coroutines.flow.Flow

class DreamRepository(private val dao: DreamDao) {

    fun observeAll(): Flow<List<Dream>> = dao.observeAll()

    suspend fun byId(id: Long): Dream? = dao.byId(id)

    suspend fun all(): List<Dream> = dao.all()

    suspend fun save(dream: Dream): Long {
        val stamped = dream.copy(updatedAt = System.currentTimeMillis())
        return if (stamped.id == 0L) dao.insert(stamped) else {
            dao.update(stamped); stamped.id
        }
    }

    suspend fun delete(dream: Dream) = dao.delete(dream)

    suspend fun toggleFavorite(dream: Dream) =
        dao.update(dream.copy(favorite = !dream.favorite, updatedAt = System.currentTimeMillis()))

    /** Replaces the whole journal — used by "replace" imports. */
    suspend fun replaceAll(dreams: List<Dream>) {
        dao.deleteAll()
        dao.insertAll(dreams.map { it.copy(id = 0L) })
    }

    /**
     * Adds [dreams] without touching existing entries. Entries that look like
     * duplicates (same dream date and same body) are skipped so importing the
     * same backup twice is harmless.
     */
    suspend fun merge(dreams: List<Dream>): Int {
        val existing = dao.all().map { it.dreamDate to it.body.trim() }.toHashSet()
        val fresh = dreams.filterNot { (it.dreamDate to it.body.trim()) in existing }
        dao.insertAll(fresh.map { it.copy(id = 0L) })
        return fresh.size
    }

    suspend fun deleteAll() = dao.deleteAll()
}
