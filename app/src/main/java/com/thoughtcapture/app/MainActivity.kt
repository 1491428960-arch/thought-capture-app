package com.thoughtcapture.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.thoughtcapture.app.ui.navigation.AppNavigation
import com.thoughtcapture.app.ui.setup.SetupScreen
import com.thoughtcapture.app.ui.theme.ThoughtCaptureTheme
import com.thoughtcapture.app.util.VersionManager

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
                    val updateInfo = remember { mutableStateOf<VersionManager.VersionInfo?>(null) }
                    val vm = remember {
                        VersionManager(this@MainActivity, app.prefs, app.gitSync.getRepoDir())
                    }

                    LaunchedEffect(Unit) {
                        val info = vm.checkForUpdate()
                        if (info != null) updateInfo.value = info
                    }

                    AppNavigation(startTab = if (openCapture) "capture" else null)

                    updateInfo.value?.let { info ->
                        val isForced = info.minVersionCode > app.prefs.remoteVersionCode
                        AlertDialog(
                            onDismissRequest = { if (!isForced) updateInfo.value = null },
                            title = { Text("发现新版本 ${info.version}") },
                            text = {
                                Text(
                                    if (info.changelog.isNotBlank()) info.changelog
                                    else "有新版本可用，建议更新以获得最佳体验。"
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    vm.startDownload(info)
                                    updateInfo.value = null
                                }) {
                                    Text("立即更新")
                                }
                            },
                            dismissButton = if (!isForced) {
                                {
                                    TextButton(onClick = { updateInfo.value = null }) {
                                        Text("稍后")
                                    }
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
}
