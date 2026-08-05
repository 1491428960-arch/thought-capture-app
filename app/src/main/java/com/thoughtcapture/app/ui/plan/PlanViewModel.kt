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

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            if (app.gitSync.canSync()) {
                app.gitSync.pull()
                app.repository.syncProcessedStatus(app.gitSync.getRepoDir())
            }
            loadAvailablePlans()
            val date = _uiState.value.selectedDate
            loadPlan(date)
        }
    }

    fun loadTodayPlan() {
        viewModelScope.launch {
            if (app.gitSync.canSync()) {
                app.gitSync.pull()
                app.repository.syncProcessedStatus(app.gitSync.getRepoDir())
            }
            loadAvailablePlans()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            _uiState.value = _uiState.value.copy(selectedDate = today)
            loadPlan(today)
        }
    }

    fun selectDate(date: String) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadPlan(date)
    }

    private fun loadPlan(date: String) {
        viewModelScope.launch {
            val planFile = File(app.gitSync.getRepoDir(), "plans/$date-plan.md")
            val reviewFile = File(app.gitSync.getRepoDir(), "review/$date-review.md")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                content = if (planFile.exists()) planFile.readText() else null,
                reviewContent = if (reviewFile.exists()) reviewFile.readText() else null,
                date = date
            )
        }
    }

    private fun loadAvailablePlans() {
        val plansDir = File(app.gitSync.getRepoDir(), "plans")
        if (!plansDir.exists()) {
            _uiState.value = _uiState.value.copy(availablePlans = emptyList())
            return
        }
        val files = plansDir.listFiles { f -> f.name.endsWith("-plan.md") } ?: emptyArray()
        val dates = files.map { it.name.removeSuffix("-plan.md") }
            .filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
            .sortedDescending()
        _uiState.value = _uiState.value.copy(availablePlans = dates)
    }

    fun deletePlan(date: String) {
        val planFile = File(app.gitSync.getRepoDir(), "plans/$date-plan.md")
        if (planFile.exists()) planFile.delete()
        loadAvailablePlans()
        if (date == _uiState.value.selectedDate) {
            loadPlan(date)
        }
    }

    fun toggleCheckbox(lineIndex: Int) {
        viewModelScope.launch {
            val date = _uiState.value.date
            val planFile = File(app.gitSync.getRepoDir(), "plans/$date-plan.md")
            if (!planFile.exists()) return@launch

            val lines = planFile.readText().lines().toMutableList()
            if (lineIndex >= lines.size) return@launch

            val line = lines[lineIndex]
            lines[lineIndex] = when {
                line.trimStart().startsWith("- [ ] ") -> line.replace("- [ ] ", "- [x] ")
                line.trimStart().startsWith("- [x] ") -> line.replace("- [x] ", "- [ ] ")
                else -> return@launch
            }

            planFile.writeText(lines.joinToString("\n"))
            _uiState.value = _uiState.value.copy(content = planFile.readText())

            // 异步推送到 GitHub
            launch {
                if (app.gitSync.canSync()) {
                    app.gitSync.pushWithRetry("update: 计划 $date 勾选更新")
                }
            }
        }
    }
}

data class PlanUiState(
    val isLoading: Boolean = true,
    val content: String? = null,
    val reviewContent: String? = null,
    val date: String = "",
    val selectedDate: String = "",
    val availablePlans: List<String> = emptyList()
)
