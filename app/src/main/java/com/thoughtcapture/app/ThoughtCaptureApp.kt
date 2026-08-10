package com.thoughtcapture.app

import android.app.Application
import com.thoughtcapture.app.data.database.AppDatabase
import com.thoughtcapture.app.data.repository.ThoughtRepository
import com.thoughtcapture.app.sync.GitSyncManager
import com.thoughtcapture.app.util.PreferencesManager
import kotlinx.coroutines.*

class ThoughtCaptureApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var repository: ThoughtRepository
        private set
    lateinit var prefs: PreferencesManager
        private set
    lateinit var gitSync: GitSyncManager
        private set

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = ThoughtRepository(database.thoughtDao())
        prefs = PreferencesManager(this)
        gitSync = GitSyncManager(this, prefs)

        // 启动后台定时同步，每 3 分钟拉取一次处理结果
        startPeriodicSync()
    }

    private fun startPeriodicSync() {
        syncScope.launch {
            while (isActive) {
                delay(3 * 60 * 1000L) // 3 分钟
                try {
                    if (prefs.isConfigured && gitSync.canSync()) {
                        // 先推送断网期间积压的本地改动
                        gitSync.pushWithRetry("sync: 断网恢复补推")
                        // 再拉取远程最新状态
                        gitSync.pull()
                        repository.syncProcessedStatus(gitSync.getRepoDir())
                    }
                } catch (_: Exception) { }
            }
        }
    }
}
