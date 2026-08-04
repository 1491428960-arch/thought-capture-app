package com.thoughtcapture.app.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
        entry = app.repository.getById(entryId)
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

            // 已处理时的 Agent 回复区域
            if (entryData.status == "processed") {
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
                    Column(Modifier.padding(16.dp)) {
                        Text("此条目已由 Agent 自动分类整理。",
                            style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("💬 如需查看完整回复，请在 Claude Code 对话中查看处理结果。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
