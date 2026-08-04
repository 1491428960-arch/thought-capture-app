package com.thoughtcapture.app.ui.plan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thoughtcapture.app.ThoughtCaptureApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PlanViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ThoughtCaptureApp
    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState: StateFlow<PlanUiState> = _uiState

    init {
        loadTodayPlan()
    }

    fun loadTodayPlan() {
        viewModelScope.launch {
            if (app.gitSync.canSync()) {
                app.gitSync.pull()
            }

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val planFile = File(app.gitSync.getRepoDir(), "plans/$today-plan.md")

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                content = if (planFile.exists()) planFile.readText() else null,
                date = today
            )
        }
    }
}

data class PlanUiState(
    val isLoading: Boolean = true,
    val content: String? = null,
    val date: String = ""
)
