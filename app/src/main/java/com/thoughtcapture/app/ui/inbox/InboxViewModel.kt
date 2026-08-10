package com.thoughtcapture.app.ui.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thoughtcapture.app.ThoughtCaptureApp
import com.thoughtcapture.app.data.entity.ThoughtEntry
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InboxViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ThoughtCaptureApp
    private val _entries = MutableStateFlow<List<ThoughtEntry>>(emptyList())
    val entries: StateFlow<List<ThoughtEntry>> = _entries

    private val deletedIdsFile = File(application.filesDir, "deleted_ids.txt")
    private val deletedIds = mutableSetOf<String>()

    init {
        // 加载持久化的删除记录
        if (deletedIdsFile.exists()) {
            deletedIds.addAll(deletedIdsFile.readText().split("\n").filter { it.isNotBlank() })
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            loadFromFiles()
        }
    }

    private fun loadFromFiles() {
        viewModelScope.launch {
            val repoDir = app.gitSync.getRepoDir()
            if (File(repoDir, ".git").exists()) {
                _entries.value = app.repository.loadAllFromFiles(repoDir)
                    .filter { it.id !in deletedIds }
            }
        }
    }

    fun deleteEntry(entry: ThoughtEntry) {
        deletedIds.add(entry.id)
        // 持久化到文件，重启也不丢
        deletedIdsFile.writeText(deletedIds.joinToString("\n"))
        // 立即从UI移除
        _entries.value = _entries.value.filter { it.id != entry.id }
        // 后台清理
        viewModelScope.launch {
            val repoDir = app.gitSync.getRepoDir()
            File(repoDir, "inbox/${entry.id}.md").delete()
            app.repository.deleteEntry(entry)
            if (app.gitSync.canSync()) app.gitSync.pushWithRetry("delete: ${entry.id}")
        }
    }

    fun refreshFromRemote() {
        viewModelScope.launch {
            val repoDir = app.gitSync.getRepoDir()
            if (!File(repoDir, ".git").exists()) app.gitSync.cloneIfNeeded()
            if (app.gitSync.canSync()) app.gitSync.pull()
            loadFromFiles()
        }
    }
}
