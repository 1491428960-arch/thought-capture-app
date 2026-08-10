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

        processedDir.walkTopDown().filter { it.isFile && it.extension == "md" && !it.name.endsWith(".reply.md") }.forEach { file ->
            val id = file.nameWithoutExtension
            val entry = dao.getById(id)
            if (entry != null) {
                if (entry.status == "inbox") {
                    val content = file.readText()
                    val tags = extractTagsFromFrontmatter(content)
                    dao.updateStatusAndTags(id, "processed", tags)
                }
            } else {
                // 新安装/清数据后，从processed重建条目
                val content = file.readText()
                val fm = parseFrontmatter(content)
                val newEntry = ThoughtEntry(
                    id = id,
                    type = fm.getOrDefault("type", "text"),
                    status = "processed",
                    source = fm.getOrDefault("source", "app"),
                    content = content.split("---\n").lastOrNull()?.trim() ?: "",
                    mediaPath = null,
                    tags = extractTagsFromFrontmatter(content),
                    createdAt = extractTimestamp(id)
                )
                dao.insert(newEntry)
            }
        }
    }

    private fun parseFrontmatter(text: String): Map<String, String> {
        if (!text.startsWith("---")) return emptyMap()
        val end = text.indexOf("---", 3)
        if (end == -1) return emptyMap()
        return text.substring(3, end).split("\n")
            .mapNotNull { line ->
                val colonIdx = line.indexOf(":")
                if (colonIdx > 0) {
                    line.substring(0, colonIdx).trim() to line.substring(colonIdx + 1).trim()
                } else null
            }.toMap()
    }

    private fun extractTimestamp(id: String): Long {
        // id 格式: 2026-08-05-123322-716
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd-HHmmss-SSS", Locale.getDefault())
            sdf.parse(id)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
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

    /**
     * 直接从文件系统加载所有条目（绕过数据库同步问题）。
     * 扫描 inbox/ 和 processed/ 目录。
     */
    fun loadAllFromFiles(repoDir: File): List<ThoughtEntry> {
        val entries = mutableListOf<ThoughtEntry>()

        // 先扫描 processed/（已处理条目优先）
        val processedDir = File(repoDir, "processed")
        if (processedDir.exists()) {
            processedDir.walkTopDown().filter { it.isFile && it.extension == "md" && !it.name.endsWith(".reply.md") }.forEach { file ->
                entries.add(parseEntryFromFile(file, "processed"))
            }
        }

        // 再扫描 inbox/（只保留不在 processed 中的）
        val inboxDir = File(repoDir, "inbox")
        if (inboxDir.exists()) {
            inboxDir.listFiles { f -> f.extension == "md" }?.forEach { file ->
                entries.add(parseEntryFromFile(file, "inbox"))
            }
        }

        return entries.distinctBy { it.id }.sortedByDescending { it.createdAt }
    }

    private fun parseEntryFromFile(file: File, defaultStatus: String): ThoughtEntry {
        val id = file.nameWithoutExtension
        val content = file.readText()
        val fm = parseFrontmatter(content)
        return ThoughtEntry(
            id = id,
            type = fm.getOrDefault("type", "text"),
            status = fm.getOrDefault("status", defaultStatus),
            source = fm.getOrDefault("source", "app"),
            content = content.split("---\n").lastOrNull()?.trim() ?: "",
            mediaPath = null,
            tags = fm.getOrDefault("tags", "[]"),
            createdAt = extractTimestamp(id)
        )
    }
}
