package com.thoughtcapture.app.ui.fitness

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
fun FitnessScreen(viewModel: FitnessViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val rotationAngle by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = if (isRefreshing)
            infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart)
        else tween(300),
        label = "fitRefresh"
    )

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 1.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💪", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("运动", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                            if (uiState.availableDates.size > 1) {
                                Spacer(Modifier.width(4.dp))
                                var showPicker by remember { mutableStateOf(false) }
                                Box {
                                    TextButton(onClick = { showPicker = true }) {
                                        Text(uiState.date, style = MaterialTheme.typography.labelSmall)
                                        Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp))
                                    }
                                    DropdownMenu(expanded = showPicker, onDismissRequest = { showPicker = false }) {
                                        uiState.availableDates.forEach { date ->
                                            DropdownMenuItem(
                                                text = { Text(date) },
                                                onClick = { showPicker = false; viewModel.selectDate(date) },
                                                leadingIcon = if (date == uiState.date) {
                                                    @Composable { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                IconButton(onClick = {
                    scope.launch { isRefreshing = true; viewModel.refresh(); delay(600); isRefreshing = false }
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
                    Text("🏋️", style = MaterialTheme.typography.headlineLarge)
                    Text("暂无运动计划", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Text("Agent 会每天推送训练安排\n做完来打卡", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                uiState.content!!.lines().forEachIndexed { index, rawLine ->
                    val line = rawLine.replace("**", "")
                    when {
                        line.startsWith("# ") -> Text(
                            line.removePrefix("# "), style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, color = Color(0xFFEF4444),
                            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                        )
                        line.startsWith("## ") -> {
                            val title = line.removePrefix("## ").trim()
                            val isNutrition = title.contains("热量") || title.contains("饮食")
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isNutrition) Color(0xFFF59E0B).copy(alpha = 0.15f)
                                        else Color(0xFFEF4444).copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text(title,
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                    color = if (isNutrition) Color(0xFFD97706) else Color(0xFFEF4444),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                            }
                        }
                        line.startsWith("- [ ] ") -> Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = false, onCheckedChange = { viewModel.toggleCheckbox(index) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)))
                                Spacer(Modifier.width(4.dp))
                                Text(line.removePrefix("- [ ] ").trim(), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        line.startsWith("- [x] ") -> Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = true, onCheckedChange = { viewModel.toggleCheckbox(index) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981)))
                                Spacer(Modifier.width(4.dp))
                                Text(line.removePrefix("- [x] ").trim(),
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        line.isBlank() -> Spacer(Modifier.height(4.dp))
                        else -> Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
        } // end Column

        // 底部训练笔记栏
        var noteInput by remember { mutableStateOf("") }
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
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("吃的/配重/问Agent…") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (noteInput.isNotBlank()) {
                            viewModel.askAgent(noteInput.trim())
                            noteInput = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("发")
                }
            }
        }
    } // end Surface & Box
}
