package com.thoughtcapture.app.ui.plan

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
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
fun PlanScreen(
    viewModel: PlanViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 旋转动画
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = if (isRefreshing) {
            infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart)
        } else { tween(300) },
        label = "refreshRotation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
            // 顶栏
            Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📋", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "计划",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                // 日期选择器
                                if (uiState.availablePlans.size > 1) {
                                    Spacer(Modifier.width(4.dp))
                                    Box {
                                        TextButton(onClick = { showDatePicker = true }) {
                                            Text(uiState.date, style = MaterialTheme.typography.labelSmall)
                                            Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp))
                                        }
                                        DropdownMenu(
                                            expanded = showDatePicker,
                                            onDismissRequest = { showDatePicker = false }
                                        ) {
                                            uiState.availablePlans.forEach { date ->
                                                DropdownMenuItem(
                                                    text = { Text(date) },
                                                    onClick = {
                                                        showDatePicker = false
                                                        viewModel.selectDate(date)
                                                    },
                                                    leadingIcon = if (date == uiState.date) {
                                                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                                    } else { null }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Row {
                        // 删除按钮
                        if (uiState.content != null) {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(Icons.Default.Delete, "删除计划",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp))
                            }
                        }
                        // 刷新按钮
                        IconButton(onClick = {
                            scope.launch {
                                isRefreshing = true
                                viewModel.refresh()
                                delay(600)
                                isRefreshing = false
                            }
                        }) {
                            Icon(Icons.Default.Refresh, "刷新",
                                tint = if (isRefreshing) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = if (isRefreshing) Modifier.rotate(rotationAngle) else Modifier)
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.content == null) {
                EmptyPlan()
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 72.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    uiState.content!!.lines().forEachIndexed { index, rawLine ->
                        val line = rawLine.replace("**", "")
                        when {
                            line.startsWith("# ") -> Text(
                                line.removePrefix("# "),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                            )
                            line.startsWith("## ") -> {
                                val sectionTitle = line.removePrefix("## ").trim()
                                val isLifeSection = sectionTitle.contains("生活") || sectionTitle.contains("健身") || sectionTitle.contains("饮食")
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isLifeSection) Color(0xFF10B981).copy(alpha = 0.1f)
                                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                Text(
                                    sectionTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                                }
                            }
                            line.startsWith("- [ ] ") -> Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = false, onCheckedChange = { viewModel.toggleCheckbox(index) },
                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary))
                                    Spacer(Modifier.width(4.dp))
                                    Text(line.removePrefix("- [ ] ").trim(), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            line.startsWith("- [x] ") -> Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = true, onCheckedChange = { viewModel.toggleCheckbox(index) },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981)))
                                    Spacer(Modifier.width(4.dp))
                                    Text(line.removePrefix("- [x] ").trim(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            line.trim() == "---" -> Spacer(Modifier.height(8.dp))
                            line.startsWith("### ") -> {
                                val subTitle = line.removePrefix("### ").trim()
                                val isLifeSub = subTitle.contains("健身") || subTitle.contains("饮食")
                                Text(
                                    subTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isLifeSub) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            line.startsWith("**") && line.endsWith("**") -> Text(
                                line.removeSurrounding("**"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            line.isBlank() -> Spacer(Modifier.height(4.dp))
                            else -> Text(line, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

    // 底部小结输入栏
    var summaryInput by remember { mutableStateOf("") }
    Surface(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = summaryInput,
                onValueChange = { summaryInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("今日小结…") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (summaryInput.isNotBlank()) {
                        scope.launch {
                            viewModel.addSummary(summaryInput.trim())
                            summaryInput = ""
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("记")
            }
        }
    }

    // 删除确认
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除这条计划？") },
            text = { Text("删除 ${uiState.date} 的计划，不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deletePlan(uiState.date)
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
    } // end Box
}

@Composable
fun EmptyPlan() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("☀️", style = MaterialTheme.typography.headlineLarge)
            Text("暂无计划", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("切换「做计划」模式说出安排\n我会帮你整理成结构化计划",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
