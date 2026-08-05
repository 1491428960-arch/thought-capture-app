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
                app.gitSync.pull()
            }
            loadTodayReview()
        }
    }

    private fun loadTodayReview() {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val reviewFile = File(app.gitSync.getRepoDir(), "review/$today-review.md")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                content = if (reviewFile.exists()) reviewFile.readText() else null,
                date = today
            )
        }
    }
}

data class ReviewUiState(
    val isLoading: Boolean = true,
    val content: String? = null,
    val date: String = ""
)
