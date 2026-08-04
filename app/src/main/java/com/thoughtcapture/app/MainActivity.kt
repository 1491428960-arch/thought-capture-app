package com.thoughtcapture.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.thoughtcapture.app.ui.navigation.AppNavigation
import com.thoughtcapture.app.ui.setup.SetupScreen
import com.thoughtcapture.app.ui.theme.ThoughtCaptureTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openCapture = intent?.getBooleanExtra("open_capture", false) ?: false
        val app = application as ThoughtCaptureApp

        if (!app.prefs.isConfigured) {
            setContent {
                ThoughtCaptureTheme {
                    SetupScreen(onComplete = { recreate() })
                }
            }
        } else {
            setContent {
                ThoughtCaptureTheme {
                    AppNavigation(startTab = if (openCapture) "capture" else null)
                }
            }
        }
    }
}
