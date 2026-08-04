package com.thoughtcapture.app.ui.plan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PlanScreen(
    viewModel: PlanViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("今日计划", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = viewModel::loadTodayPlan) {
                Icon(Icons.Default.Refresh, "刷新")
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.content == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "今日暂无计划",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "使用「今日规划」语音入口\n说出你今天的安排",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                uiState.content!!.lines().forEach { line ->
                    when {
                        line.startsWith("# ") -> Text(
                            line.removePrefix("# "),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        line.startsWith("## ") -> Text(
                            line.removePrefix("## "),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        line.startsWith("- [ ] ") -> Row(
                            modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp)
                        ) {
                            Checkbox(checked = false, onCheckedChange = {})
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(line.removePrefix("- [ ] "))
                        }
                        line.startsWith("- [x] ") -> Row(
                            modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp)
                        ) {
                            Checkbox(checked = true, onCheckedChange = {})
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(line.removePrefix("- [x] "))
                        }
                        line.isBlank() -> Spacer(modifier = Modifier.height(8.dp))
                        else -> Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
