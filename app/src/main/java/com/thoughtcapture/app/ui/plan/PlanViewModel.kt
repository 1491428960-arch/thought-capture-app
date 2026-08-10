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
                // 先推送本地改动（打勾、小结），再拉取远程
                app.gitSync.pushWithRetry("sync: 刷新前同步")
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
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                content = if (planFile.exists()) planFile.readText() else null,
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

    fun addSummary(text: String) {
        val content = _uiState.value.content ?: ""
        val newContent = content + "\n\n## 小结\n\n- " + text
        _uiState.value = _uiState.value.copy(content = newContent)
        viewModelScope.launch {
            val date = _uiState.value.date
            val planFile = File(app.gitSync.getRepoDir(), "plans/$date-plan.md")
            planFile.writeText(newContent)
            if (app.gitSync.canSync()) app.gitSync.pushWithRetry("summary: $date 小结")
        }
    }

    fun deletePlan(date: String) {
        viewModelScope.launch {
            val planFile = File(app.gitSync.getRepoDir(), "plans/$date-plan.md")
            if (planFile.exists()) planFile.delete()
            // 推到 GitHub，防止下次 pull 复活
            if (app.gitSync.canSync()) {
                app.gitSync.pushWithRetry("delete plan: $date")
            }
            loadAvailablePlans()
            // 如果删的是当前显示的日期，自动切到最近一个
            if (date == _uiState.value.selectedDate) {
                val next = _uiState.value.availablePlans.firstOrNull()
                if (next != null) {
                    _uiState.value = _uiState.value.copy(selectedDate = next)
                    loadPlan(next)
                } else {
                    _uiState.value = _uiState.value.copy(content = null, date = "")
                }
            }
        }
    }

    fun toggleCheckbox(lineIndex: Int) {
        val content = _uiState.value.content ?: return
        val lines = content.lines().toMutableList()
        if (lineIndex >= lines.size) return

        val line = lines[lineIndex]
        lines[lineIndex] = when {
            line.trimStart().startsWith("- [ ] ") -> line.replace("- [ ] ", "- [x] ")
            line.trimStart().startsWith("- [x] ") -> line.replace("- [x] ", "- [ ] ")
            else -> return
        }

        // 先更新 UI（即时响应）
        _uiState.value = _uiState.value.copy(content = lines.joinToString("\n"))

        // 后台保存+推送
        viewModelScope.launch {
            val date = _uiState.value.date
            val planFile = File(app.gitSync.getRepoDir(), "plans/$date-plan.md")
            planFile.writeText(lines.joinToString("\n"))
            if (app.gitSync.canSync()) app.gitSync.pushWithRetry("update: 计划 $date 勾选")
        }
    }
}

data class PlanUiState(
    val isLoading: Boolean = true,
    val content: String? = null,
    val date: String = "",
    val selectedDate: String = "",
    val availablePlans: List<String> = emptyList()
)
