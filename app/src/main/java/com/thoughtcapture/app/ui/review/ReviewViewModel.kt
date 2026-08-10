package com.thoughtcapture.app.ui.review

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

class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ThoughtCaptureApp
    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState

    init { loadTodayReview() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            if (app.gitSync.canSync()) {
                app.gitSync.pushWithRetry("sync: 刷新前同步")
                app.gitSync.pull()
            }
            loadAvailableReviews()
            loadReview(_uiState.value.selectedDate.ifEmpty {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            })
        }
    }

    private fun loadTodayReview() {
        viewModelScope.launch {
            if (app.gitSync.canSync()) {
                app.gitSync.pushWithRetry("sync: 刷新前同步")
                app.gitSync.pull()
            }
            loadAvailableReviews()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            _uiState.value = _uiState.value.copy(selectedDate = today)
            loadReview(today)
        }
    }

    fun selectDate(date: String) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadReview(date)
    }

    private fun loadReview(date: String) {
        viewModelScope.launch {
            val reviewFile = File(app.gitSync.getRepoDir(), "review/$date-review.md")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                content = if (reviewFile.exists()) reviewFile.readText() else null,
                date = date
            )
        }
    }

    private fun loadAvailableReviews() {
        val reviewDir = File(app.gitSync.getRepoDir(), "review")
        if (!reviewDir.exists()) {
            _uiState.value = _uiState.value.copy(availableReviews = emptyList())
            return
        }
        val files = reviewDir.listFiles { f -> f.name.endsWith("-review.md") } ?: emptyArray()
        val dates = files.map { it.name.removeSuffix("-review.md") }
            .filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
            .sortedDescending()
        _uiState.value = _uiState.value.copy(availableReviews = dates)
    }

    fun toggleCheckbox(lineIndex: Int) {
        viewModelScope.launch {
            val date = _uiState.value.date
            val reviewFile = File(app.gitSync.getRepoDir(), "review/$date-review.md")
            if (!reviewFile.exists()) return@launch
            val lines = reviewFile.readText().lines().toMutableList()
            if (lineIndex >= lines.size) return@launch
            val line = lines[lineIndex]
            lines[lineIndex] = when {
                line.trimStart().startsWith("- [ ] ") -> line.replace("- [ ] ", "- [x] ")
                line.trimStart().startsWith("- [x] ") -> line.replace("- [x] ", "- [ ] ")
                else -> return@launch
            }
            reviewFile.writeText(lines.joinToString("\n"))
            _uiState.value = _uiState.value.copy(content = reviewFile.readText())
            launch {
                if (app.gitSync.canSync()) app.gitSync.pushWithRetry("review: $date 打卡")
            }
        }
    }
}

data class ReviewUiState(
    val isLoading: Boolean = true,
    val content: String? = null,
    val date: String = "",
    val selectedDate: String = "",
    val availableReviews: List<String> = emptyList()
)
