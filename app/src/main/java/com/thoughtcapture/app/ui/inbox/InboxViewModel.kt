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

    init {
        // 延迟加载——等仓库 clone 完成
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            loadFromFiles()
        }
    }

    private fun loadFromFiles() {
        viewModelScope.launch {
            val repoDir = app.gitSync.getRepoDir()
            val gitDir = File(repoDir, ".git")
            if (gitDir.exists()) {
                _entries.value = app.repository.loadAllFromFiles(repoDir)
            }
        }
    }

    fun refreshFromRemote() {
        viewModelScope.launch {
            // 先确保仓库存在（clone兜底）
            val repoDir = app.gitSync.getRepoDir()
            if (!File(repoDir, ".git").exists()) {
                app.gitSync.cloneIfNeeded()
            }
            // 再拉取远程
            if (app.gitSync.canSync()) {
                app.gitSync.pull()
            }
            loadFromFiles()
        }
    }

    fun deleteEntry(entry: ThoughtEntry) {
        viewModelScope.launch {
            // 删本地文件
            val repoDir = app.gitSync.getRepoDir()
            val file = File(repoDir, "inbox/${entry.id}.md")
            if (file.exists()) file.delete()
            // 删 DB
            app.repository.deleteEntry(entry)
            // 推送到 GitHub
            if (app.gitSync.canSync()) {
                app.gitSync.pushWithRetry("delete: ${entry.id}")
            }
            loadFromFiles()
        }
    }
}
