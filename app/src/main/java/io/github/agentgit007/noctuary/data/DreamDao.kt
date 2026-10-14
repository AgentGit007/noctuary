/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package io.github.agentgit007.noctuary.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DreamDao {

    @Query("SELECT * FROM dreams ORDER BY dream_date DESC, created_at DESC")
    fun observeAll(): Flow<List<Dream>>

    @Query("SELECT * FROM dreams WHERE id = :id")
    suspend fun byId(id: Long): Dream?

    @Query("SELECT * FROM dreams ORDER BY dream_date DESC, created_at DESC")
    suspend fun all(): List<Dream>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dream: Dream): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dreams: List<Dream>)

    @Update
    suspend fun update(dream: Dream)

    @Delete
    suspend fun delete(dream: Dream)

    @Query("DELETE FROM dreams")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM dreams")
    suspend fun count(): Int
}
