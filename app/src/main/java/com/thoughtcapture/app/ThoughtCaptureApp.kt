package com.thoughtcapture.app

import android.app.Application
import com.thoughtcapture.app.data.database.AppDatabase
import com.thoughtcapture.app.data.repository.ThoughtRepository
import com.thoughtcapture.app.sync.GitSyncManager
import com.thoughtcapture.app.util.PreferencesManager

class ThoughtCaptureApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var repository: ThoughtRepository
        private set
    lateinit var prefs: PreferencesManager
        private set
    lateinit var gitSync: GitSyncManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = ThoughtRepository(database.thoughtDao())
        prefs = PreferencesManager(this)
        gitSync = GitSyncManager(this, prefs)
    }
}
