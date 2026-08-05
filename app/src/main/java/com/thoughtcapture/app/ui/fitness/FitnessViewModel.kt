package com.thoughtcapture.app.ui.fitness

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

class FitnessViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ThoughtCaptureApp
    private val _uiState = MutableStateFlow(FitnessUiState())
    val uiState: StateFlow<FitnessUiState> = _uiState

    init { loadToday() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            if (app.gitSync.canSync()) {
                app.gitSync.pull()
            }
            loadAvailableDates()
            loadPlan(_uiState.value.selectedDate.ifEmpty {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            })
        }
    }

    private fun loadToday() {
        viewModelScope.launch {
            if (app.gitSync.canSync()) app.gitSync.pull()
            loadAvailableDates()
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
            val planFile = File(app.gitSync.getRepoDir(), "fitness/plans/$date-plan.md")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                content = if (planFile.exists()) planFile.readText() else null,
                date = date
            )
        }
    }

    private fun loadAvailableDates() {
        val plansDir = File(app.gitSync.getRepoDir(), "fitness/plans")
        if (!plansDir.exists()) {
            _uiState.value = _uiState.value.copy(availableDates = emptyList())
            return
        }
        val files = plansDir.listFiles { f -> f.name.endsWith("-plan.md") } ?: emptyArray()
        val dates = files.map { it.name.removeSuffix("-plan.md") }
            .filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
            .sortedDescending()
        _uiState.value = _uiState.value.copy(availableDates = dates)
    }

    fun toggleCheckbox(lineIndex: Int) {
        viewModelScope.launch {
            val date = _uiState.value.date
            val planFile = File(app.gitSync.getRepoDir(), "fitness/plans/$date-plan.md")
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
            launch {
                if (app.gitSync.canSync()) app.gitSync.pushWithRetry("fitness: $date 训练打卡")
            }
        }
    }
}

data class FitnessUiState(
    val isLoading: Boolean = true,
    val content: String? = null,
    val date: String = "",
    val selectedDate: String = "",
    val availableDates: List<String> = emptyList()
)
