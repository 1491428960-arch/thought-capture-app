package com.thoughtcapture.app.ui.review

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(viewModel: ReviewViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val rotationAngle by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = if (isRefreshing)
            infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart)
        else tween(300),
        label = "reviewRefresh"
    )

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch { isRefreshing = true; viewModel.refresh(); kotlinx.coroutines.delay(600); isRefreshing = false }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📖", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("今日复习", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                            Text(uiState.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = {
                        scope.launch { isRefreshing = true; viewModel.refresh(); kotlinx.coroutines.delay(600); isRefreshing = false }
                    }) {
                        Icon(Icons.Default.Refresh, "刷新",
                            tint = if (isRefreshing) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = if (isRefreshing) Modifier.rotate(rotationAngle) else Modifier)
                    }
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.content == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("📚", style = MaterialTheme.typography.headlineLarge)
                        Text("暂无复习内容", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Text("Agent 会每天自动推送\n错题、范文、常识、笔记", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    uiState.content!!.lines().filter { it.isNotBlank() }.forEach { line ->
                        when {
                            line.startsWith("## ") -> {
                                val section = line.removePrefix("## ").trim()
                                val (icon, sectionColor) = when {
                                    section.contains("错题") -> "📝" to Color(0xFFEF4444)
                                    section.contains("范文") -> "📄" to Color(0xFF8B5CF6)
                                    section.contains("常识") -> "⚡" to Color(0xFFF59E0B)
                                    section.contains("笔记") -> "📓" to Color(0xFF10B981)
                                    else -> "📌" to Color(0xFF2563EB)
                                }
                                Spacer(Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = sectionColor.copy(alpha = 0.08f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(icon, style = MaterialTheme.typography.titleMedium)
                                        Spacer(Modifier.width(8.dp))
                                        Text(section, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = sectionColor)
                                    }
                                }
                            }
                            line.startsWith("- ") -> Text(
                                line.removePrefix("- ").trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 16.dp, top = 3.dp, bottom = 3.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            else -> Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
