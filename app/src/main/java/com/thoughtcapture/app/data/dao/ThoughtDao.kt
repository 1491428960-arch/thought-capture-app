package com.thoughtcapture.app.data.dao

import androidx.room.*
import com.thoughtcapture.app.data.entity.ThoughtEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ThoughtDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ThoughtEntry)

    @Query("SELECT * FROM thought_entries ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<ThoughtEntry>>

    @Query("SELECT * FROM thought_entries WHERE id = :id")
    suspend fun getById(id: String): ThoughtEntry?

    @Query("SELECT * FROM thought_entries WHERE status = 'inbox' ORDER BY createdAt DESC")
    suspend fun getUnsyncedEntries(): List<ThoughtEntry>

    @Query("UPDATE thought_entries SET status = :status, tags = :tags WHERE id = :id")
    suspend fun updateStatusAndTags(id: String, status: String, tags: String)

    @Query("SELECT COUNT(*) FROM thought_entries WHERE status = 'inbox'")
    suspend fun getUnprocessedCount(): Int

    @Delete
    suspend fun delete(entry: ThoughtEntry)

    @Query("DELETE FROM thought_entries WHERE id = :id")
    suspend fun deleteById(id: String)
}
