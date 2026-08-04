package com.thoughtcapture.app.ui.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thoughtcapture.app.ThoughtCaptureApp
import com.thoughtcapture.app.data.entity.ThoughtEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InboxViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ThoughtCaptureApp

    val entries: StateFlow<List<ThoughtEntry>> = app.repository.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshFromRemote() {
        viewModelScope.launch {
            if (app.gitSync.canSync()) {
                app.gitSync.pull()
                app.repository.syncProcessedStatus(app.gitSync.getRepoDir())
            }
        }
    }

    fun deleteEntry(entry: ThoughtEntry) {
        viewModelScope.launch {
            app.repository.deleteEntry(entry)
        }
    }
}
