package com.thoughtcapture.app.ui.inbox

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thoughtcapture.app.data.entity.ThoughtEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: InboxViewModel = viewModel(),
    onNavigateToCapture: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {}
) {
    val entries by viewModel.entries.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 刷新旋转动画
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = if (isRefreshing)
            infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart)
        else tween(300),
        label = "syncRotation"
    )

    // 首次打开自动刷新
    LaunchedEffect(Unit) {
        delay(1500)
        isRefreshing = true
        viewModel.refreshFromRemote()
        isRefreshing = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶栏
            Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📥", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(8.dp))
                        Text("收件箱", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    Row {
                        IconButton(onClick = {
                            scope.launch {
                                isRefreshing = true
                                viewModel.refreshFromRemote()
                                delay(800) // 最小展示时间让动画可见
                                isRefreshing = false
                            }
                        }) {
                            Icon(
                                Icons.Default.Sync, "刷新",
                                tint = if (isRefreshing) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = if (isRefreshing) Modifier.rotate(rotationAngle) else Modifier
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, "设置", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 内容
            if (entries.isEmpty()) {
                EmptyInbox()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        ThoughtEntryCard(
                            entry = entry,
                            onClick = { onNavigateToDetail(entry.id) },
                            onDelete = { viewModel.deleteEntry(entry) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = onNavigateToCapture,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "快速记录")
        }
    }
}

@Composable
fun EmptyInbox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("📝", style = MaterialTheme.typography.headlineLarge)
            Text("还没有记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text("点击右下角按钮开始记录\n或在下拉通知栏中使用快捷开关", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ThoughtEntryCard(
    entry: ThoughtEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    val dateLabel = remember(entry.createdAt) {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val entryDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(entry.createdAt))
        if (today == entryDay) "今天 " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.createdAt))
        else dateFormat.format(Date(entry.createdAt))
    }
    val (typeIcon, typeLabel, iconColor) = when {
        entry.type == "voice" -> Triple(Icons.Default.Mic, "语音", Color(0xFF8B5CF6))
        entry.type == "photo" -> Triple(Icons.Default.Image, "图片", Color(0xFFF59E0B))
        entry.type == "brief" -> Triple(Icons.Default.CalendarToday, "规划", Color(0xFF10B981))
        entry.type == "practice" -> Triple(Icons.Default.School, "练习", Color(0xFFF59E0B))
        entry.content.contains("[健身]") -> Triple(Icons.Default.FitnessCenter, "健身", Color(0xFFEF4444))
        entry.tags.contains("考公") -> Triple(Icons.Default.MenuBook, "考公", Color(0xFF2563EB))
        entry.tags.contains("健身") -> Triple(Icons.Default.FitnessCenter, "健身", Color(0xFFEF4444))
        entry.tags.contains("技术") -> Triple(Icons.Default.Code, "技术", Color(0xFF6366F1))
        entry.tags.contains("计划") -> Triple(Icons.Default.CalendarMonth, "计划", Color(0xFF10B981))
        entry.tags.contains("批改") -> Triple(Icons.Default.RateReview, "批改", Color(0xFFF59E0B))
        entry.status == "processed" -> Triple(Icons.Default.CheckCircle, "已整理", Color(0xFF10B981))
        else -> Triple(Icons.Default.ChatBubbleOutline, "想法", Color(0xFF64748B))
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 类型图标
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(typeIcon, typeLabel, modifier = Modifier.size(20.dp), tint = iconColor)
                }
            }
            Spacer(Modifier.width(12.dp))
            // 文字内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.content.ifBlank { "[${typeLabel}条目]" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (entry.status == "inbox")
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            if (entry.status == "inbox") "待处理" else "已整理",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (entry.status == "inbox")
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // 删除按钮
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    "删除",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除") },
            text = { Text("确定删除这条想法？") },
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
