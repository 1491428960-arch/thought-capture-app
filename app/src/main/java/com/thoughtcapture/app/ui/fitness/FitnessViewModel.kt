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
            if (app.gitSync.canSync()) { app.gitSync.pushWithRetry("sync: 刷新前同步"); app.gitSync.pull() }
            loadAvailableDates()
            loadPlan(_uiState.value.selectedDate.ifEmpty {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            })
        }
    }

    private fun loadToday() {
        viewModelScope.launch {
            if (app.gitSync.canSync()) { app.gitSync.pushWithRetry("sync: 刷新前同步"); app.gitSync.pull() }
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
        val content = _uiState.value.content ?: return
        val lines = content.lines().toMutableList()
        if (lineIndex >= lines.size) return
        val line = lines[lineIndex]
        lines[lineIndex] = when {
            line.trimStart().startsWith("- [ ] ") -> line.replace("- [ ] ", "- [x] ")
            line.trimStart().startsWith("- [x] ") -> line.replace("- [x] ", "- [ ] ")
            else -> return
        }
        _uiState.value = _uiState.value.copy(content = lines.joinToString("\n"))
        viewModelScope.launch {
            val date = _uiState.value.date
            val planFile = File(app.gitSync.getRepoDir(), "fitness/plans/$date-plan.md")
            planFile.writeText(lines.joinToString("\n"))
            if (app.gitSync.canSync()) app.gitSync.pushWithRetry("fitness: $date 打卡")
        }
    }

    fun askAgent(question: String) {
        viewModelScope.launch {
            val repoDir = app.gitSync.getRepoDir()
            val inboxDir = File(repoDir, "inbox")
            inboxDir.mkdirs()
            val id = SimpleDateFormat("yyyy-MM-dd-HHmmss-SSS", Locale.getDefault()).format(Date())
            val mdFile = File(inboxDir, "$id.md")
            mdFile.writeText("---\nid: $id\ntype: text\nstatus: inbox\ntags: []\nsource: app\n---\n\n[健身] $question")
            launch {
                if (app.gitSync.canSync()) app.gitSync.pushWithRetry("ask: $question")
            }
        }
    }

    fun addFoodItem(food: String) {
        val date = _uiState.value.date
        val planFile = File(app.gitSync.getRepoDir(), "fitness/plans/$date-plan.md")
        // 文件不存在则先创建基础模板（同步写，即时响应）
        if (!planFile.exists()) {
            planFile.parentFile.mkdirs()
            planFile.writeText(
                "# $date 运动计划\n\n" +
                "> 训练日 | 固定器械+哑铃\n\n" +
                "## 热量日志\n\n" +
                "基础代谢 2200kcal | 总消耗 ~2500kcal\n" +
                "目标摄入 1800-2000kcal | 缺口 400-600kcal\n\n" +
                "- 早餐：待记录\n- 午餐：待记录\n- 晚餐：待记录\n- 加餐：待记录\n" +
                "- 今日合计：0 kcal | 总消耗：2500 kcal | 缺口：- kcal\n\n" +
                "> 告诉Agent吃了什么，帮你算热量\n"
            )
        }
        // 即时写入 + 更新 UI
        val lines = planFile.readText().lines().toMutableList()
        val totalIdx = lines.indexOfFirst { it.startsWith("- 今日合计") }
        val insertAt = if (totalIdx > 0) totalIdx else lines.size
        lines.add(insertAt, "- ${food.trim()}")
        planFile.writeText(lines.joinToString("\n"))
        _uiState.value = _uiState.value.copy(content = planFile.readText())
        // 后台推送
        viewModelScope.launch {
            if (app.gitSync.canSync()) app.gitSync.pushWithRetry("fitness: $date 饮食")
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
