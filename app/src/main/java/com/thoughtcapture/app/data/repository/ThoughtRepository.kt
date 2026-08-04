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

    /**
     * 从 Git 仓库的 processed/ 目录同步处理状态回本地数据库。
     * Agent 在 PC 端处理后会移动文件到 processed/<分类>/并更新 frontmatter。
     */
    suspend fun syncProcessedStatus(repoDir: File) {
        val processedDir = File(repoDir, "processed")
        if (!processedDir.exists()) return

        processedDir.walkTopDown().filter { it.isFile && it.extension == "md" }.forEach { file ->
            val id = file.nameWithoutExtension
            val entry = dao.getById(id)
            if (entry != null && entry.status == "inbox") {
                // 从 frontmatter 中提取 tags
                val content = file.readText()
                val tags = extractTagsFromFrontmatter(content)
                dao.updateStatusAndTags(id, "processed", tags)
            }
        }
    }

    private fun extractTagsFromFrontmatter(text: String): String {
        if (!text.startsWith("---")) return "[]"
        val end = text.indexOf("---", 3)
        if (end == -1) return "[]"
        val fm = text.substring(3, end)
        for (line in fm.split("\n")) {
            val trimmed = line.trim()
            if (trimmed.startsWith("tags:")) {
                return trimmed.removePrefix("tags:").trim()
            }
        }
        return "[]"
    }

    suspend fun deleteEntry(entry: ThoughtEntry) {
        dao.delete(entry)
    }

    suspend fun deleteById(id: String) {
        dao.deleteById(id)
    }
}
