package com.thoughtcapture.app.service

import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * 语音识别 — 使用系统弹窗（兼容所有安卓机型，包括小米）。
 */
class VoiceRecognitionHelper(private val activity: ComponentActivity) {

    private var onResult: ((String?) -> Unit)? = null

    private val launcher = activity.activityResultRegistry.register(
        "voice_input",
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            onResult?.invoke(matches?.firstOrNull())
        } else {
            onResult?.invoke(null)
        }
    }

    /**
     * 启动语音识别（系统弹窗）。完成后回调 result。
     */
    fun startListening(onResult: (String?) -> Unit) {
        this.onResult = onResult
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "说出你的想法…")
        }
        launcher.launch(intent)
    }

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(activity)
}
