package com.thoughtcapture.app.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thoughtcapture.app.ThoughtCaptureApp
import com.thoughtcapture.app.data.entity.ThoughtEntry
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    entryId: String,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as ThoughtCaptureApp
    var entry by remember { mutableStateOf<ThoughtEntry?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(entryId) {
        // 优先从文件系统加载（反映Agent处理后的最新状态）
        val entries = app.repository.loadAllFromFiles(app.gitSync.getRepoDir())
        entry = entries.find { it.id == entryId }
        // 文件系统没找到再查Room DB
        if (entry == null) {
            entry = app.repository.getById(entryId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("想法详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        val entryData = entry
        if (entryData == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()) }
        val typeLabel = when (entryData.type) {
            "voice" -> "语音"
            "photo" -> "图片"
            "brief" -> "今日规划"
            else -> "文字"
        }
        val sourceLabel = when (entryData.source) {
            "tile" -> "通知栏快捷开关"
            "widget" -> "桌面小组件"
            else -> "App"
        }
        val typeIcon = when (entryData.type) {
            "voice" -> Icons.Default.Mic
            "photo" -> Icons.Default.Image
            "brief" -> Icons.Default.CalendarToday
            else -> Icons.Default.Edit
        }
        val typeColor = when (entryData.type) {
            "voice" -> Color(0xFF8B5CF6)
            "photo" -> Color(0xFFF59E0B)
            "brief" -> Color(0xFF10B981)
            else -> Color(0xFF2563EB)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // 类型标签
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = typeColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(typeIcon, null, modifier = Modifier.size(18.dp), tint = typeColor)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(typeLabel, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Text("$sourceLabel · ${dateFormat.format(Date(entryData.createdAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(10.dp))

            // 状态标签
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (entryData.status == "inbox")
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        if (entryData.status == "inbox") "待处理" else "已整理",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (entryData.status == "inbox")
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // 显示标签
                if (entryData.tags != "[]" && entryData.tags != "") {
                    val tagList = entryData.tags.removeSurrounding("[", "]")
                        .split(",").map { it.trim().removeSurrounding("\"") }
                    tagList.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                tag,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 内容卡片
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Text(
                    entryData.content.ifBlank { "[无文字内容]" },
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Agent 回复区域
            if (entryData.status == "processed") {
                // 从 Git 仓库查找回复文件
                val replyContent = remember(entryData.id) {
                    val repoDir = app.gitSync.getRepoDir()
                    val processedDir = java.io.File(repoDir, "processed")
                    var reply: String? = null
                    if (processedDir.exists()) {
                        processedDir.walkTopDown().forEach { file ->
                            if (file.name == "${entryData.id}.reply.md") {
                                reply = file.readText()
                            }
                        }
                    }
                    reply
                }

                Spacer(Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Agent 已处理", fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF8B5CF6).copy(alpha = 0.08f)
                    )
                ) {
                    if (replyContent != null) {
                        // 简单渲染回复的 Markdown 内容
                        Column(Modifier.padding(16.dp)) {
                            replyContent.lines().forEach { line ->
                                when {
                                    line.startsWith("# ") -> Text(
                                        line.removePrefix("# "),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                    line.startsWith("## ") -> Text(
                                        line.removePrefix("## "),
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                    line.startsWith("|") -> {} // 跳过表格行（简化）
                                    line.startsWith("- ") -> Text(
                                        line,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                    line.isBlank() -> Spacer(Modifier.height(4.dp))
                                    else -> Text(
                                        line,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        Text("正在等待 Agent 回复…", modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除这条想法？") },
            text = { Text("删除后不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    val entry = entry // capture current entry
                    if (entry != null) {
                        kotlinx.coroutines.MainScope().launch {
                            app.repository.deleteById(entry.id)
                            app.gitSync.pushWithRetry("delete: ${entry.id}")
                        }
                    }
                    onDelete()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}
