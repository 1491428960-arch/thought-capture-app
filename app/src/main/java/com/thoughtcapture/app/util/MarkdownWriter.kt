package com.thoughtcapture.app.util

import com.thoughtcapture.app.data.entity.ThoughtEntry
import java.io.File

object MarkdownWriter {

    fun writeEntry(entry: ThoughtEntry, repoDir: File, mediaDir: File): File {
        val inboxDir = File(repoDir, "inbox")
        if (!inboxDir.exists()) inboxDir.mkdirs()
        if (!mediaDir.exists()) mediaDir.mkdirs()

        val mdFile = File(inboxDir, "${entry.id}.md")

        val frontMatter = buildString {
            appendLine("---")
            appendLine("id: ${entry.id}")
            appendLine("type: ${entry.type}")
            appendLine("status: ${entry.status}")
            appendLine("tags: ${entry.tags}")
            appendLine("source: ${entry.source}")
            appendLine("---")
            appendLine()
        }

        mdFile.writeText(frontMatter + entry.content)

        return mdFile
    }
}
