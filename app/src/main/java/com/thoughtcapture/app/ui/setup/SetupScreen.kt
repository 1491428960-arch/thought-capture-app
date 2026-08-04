package com.thoughtcapture.app.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as com.thoughtcapture.app.ThoughtCaptureApp
    var pat by remember { mutableStateOf("") }
    var repoUrl by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("main") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // 如果已配置，预填现有值
    LaunchedEffect(Unit) {
        if (app.prefs.isConfigured) {
            repoUrl = app.prefs.repoUrl ?: ""
            pat = app.prefs.githubPat ?: ""
            branch = app.prefs.repoBranch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("欢迎使用想法捕捉", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "配置 GitHub 仓库以开始同步",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = repoUrl,
            onValueChange = { repoUrl = it },
            label = { Text("仓库地址") },
            placeholder = { Text("https://github.com/xxx/ideas.git") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = pat,
            onValueChange = { pat = it },
            label = { Text("Personal Access Token") },
            placeholder = { Text("ghp_xxxxxxxxxxxx") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = branch,
            onValueChange = { branch = it },
            label = { Text("分支") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        var syncOnMobileData by remember { mutableStateOf(app.prefs.syncOnMobileData) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("使用流量同步", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = syncOnMobileData,
                onCheckedChange = {
                    syncOnMobileData = it
                    app.prefs.syncOnMobileData = it
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                isLoading = true
                error = null

                scope.launch {
                    app.prefs.githubPat = pat
                    app.prefs.repoUrl = repoUrl
                    app.prefs.repoBranch = branch

                    val result = app.gitSync.cloneIfNeeded()
                    if (result.isSuccess) {
                        onComplete()
                    } else {
                        app.prefs.githubPat = null
                        app.prefs.repoUrl = null
                        error = "克隆失败：${result.exceptionOrNull()?.message}"
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading && repoUrl.isNotBlank() && pat.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("克隆中…")
            } else {
                Text("完成配置")
            }
        }

        error?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
