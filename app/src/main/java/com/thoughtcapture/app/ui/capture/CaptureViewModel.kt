package com.thoughtcapture.app.ui.capture

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thoughtcapture.app.ThoughtCaptureApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

data class CaptureUiState(
    val textInput: String = "",
    val mediaUri: Uri? = null,
    val mediaType: String? = null,
    val isRecording: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val isConfigured: Boolean = false,
    val isPlanMode: Boolean = false
)

class CaptureViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ThoughtCaptureApp
    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState

    init {
        _uiState.value = _uiState.value.copy(isConfigured = app.prefs.isConfigured)
    }

    fun onTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(textInput = text)
    }

    fun onMediaSelected(uri: Uri, type: String) {
        _uiState.value = _uiState.value.copy(mediaUri = uri, mediaType = type)
    }

    fun onMediaRemoved() {
        _uiState.value = _uiState.value.copy(mediaUri = null, mediaType = null)
    }

    fun onSave(source: String) {
        val state = _uiState.value
        if (state.textInput.isBlank() && state.mediaUri == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)

            try {
                var mediaPath: String? = null
                if (state.mediaUri != null) {
                    val mediaDir = app.gitSync.getMediaDir()
                    val entryId = app.repository.generateId()
                    val ext = if (state.mediaType == "photo") ".jpg" else ".tmp"
                    val destFile = File(mediaDir, "$entryId$ext")
                    getApplication<Application>().contentResolver.openInputStream(state.mediaUri!!)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    mediaPath = "media/${destFile.name}"
                }

                val type = when {
                    state.isPlanMode -> "brief"
                    state.mediaUri != null && state.mediaType == "photo" -> "photo"
                    state.textInput.isNotBlank() -> "text"
                    else -> "text"
                }

                val repoDir = app.gitSync.getRepoDir()
                val mediaDir = app.gitSync.getMediaDir()
                app.repository.saveEntry(
                    type = type,
                    source = source,
                    content = state.textInput,
                    mediaPath = mediaPath,
                    repoDir = repoDir,
                    mediaDir = mediaDir
                )

                launch {
                    if (app.gitSync.canSync()) {
                        app.gitSync.pushWithRetry("add: 新想法 ${app.repository.generateId()}")
                    }
                }

                _uiState.value = _uiState.value.copy(
                    textInput = "",
                    mediaUri = null,
                    mediaType = null,
                    isSaving = false,
                    saveSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun togglePlanMode() {
        _uiState.value = _uiState.value.copy(isPlanMode = !_uiState.value.isPlanMode)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
}
