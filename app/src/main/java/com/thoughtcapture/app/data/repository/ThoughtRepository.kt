package com.thoughtcapture.app.data.repository

import com.thoughtcapture.app.data.dao.ThoughtDao
import com.thoughtcapture.app.data.entity.ThoughtEntry
import com.thoughtcapture.app.util.MarkdownWriter
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ThoughtRepository(private val dao: ThoughtDao) {

    val allEntries: Flow<List<ThoughtEntry>> = dao.getAllEntries()

    fun generateId(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd-HHmmss-SSS", Locale.getDefault())
        return sdf.format(Date())
    }

    suspend fun saveEntry(
        type: String,
        source: String,
        content: String,
        mediaPath: String?,
        repoDir: File,
        mediaDir: File
    ): ThoughtEntry {
        val id = generateId()
        val entry = ThoughtEntry(
            id = id,
            type = type,
            status = "inbox",
            source = source,
            content = content,
            mediaPath = mediaPath,
            tags = "[]",
            createdAt = System.currentTimeMillis()
        )
        dao.insert(entry)
        MarkdownWriter.writeEntry(entry, repoDir, mediaDir)
        return entry
    }

    suspend fun getUnsyncedEntries(): List<ThoughtEntry> {
        return dao.getUnsyncedEntries()
    }

    suspend fun getById(id: String): ThoughtEntry? = dao.getById(id)

    suspend fun updateStatusAndTags(id: String, status: String, tags: String) {
        dao.updateStatusAndTags(id, status, tags)
    }
}
