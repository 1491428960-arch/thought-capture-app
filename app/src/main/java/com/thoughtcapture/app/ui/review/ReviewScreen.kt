package com.thoughtcapture.app.ui.review

import androidx.compose.animation.core.*
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

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶栏
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("复习", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                            if (uiState.availableReviews.size > 1) {
                                Spacer(Modifier.width(4.dp))
                                var showPicker by remember { mutableStateOf(false) }
                                Box {
                                    TextButton(onClick = { showPicker = true }) {
                                        Text(uiState.date, style = MaterialTheme.typography.labelSmall)
                                        Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp))
                                    }
                                    DropdownMenu(expanded = showPicker, onDismissRequest = { showPicker = false }) {
                                        uiState.availableReviews.forEach { date ->
                                            DropdownMenuItem(
                                                text = { Text(date) },
                                                onClick = { showPicker = false; viewModel.selectDate(date) },
                                                leadingIcon = if (date == uiState.date) ({
                                                    Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                                                }) else null
                                            )
                                        }
                                    }
                                }
                            }
                        }
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
                    Text("Agent 会每天自动推送\n错题、范文、常识、笔记", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var currentSection = ""
                var currentSectionColor = Color(0xFF2563EB)

                uiState.content!!.lines().filter { it.isNotBlank() }.forEachIndexed { index, rawLine ->
                    val line = rawLine.trim().replace("**", "")

                    when {
                        line.startsWith("## ") -> {
                            currentSection = line.removePrefix("## ").trim()
                            currentSectionColor = when {
                                currentSection.contains("错题") -> Color(0xFFEF4444)
                                currentSection.contains("范文") -> Color(0xFF8B5CF6)
                                currentSection.contains("常识") -> Color(0xFFF59E0B)
                                currentSection.contains("笔记") -> Color(0xFF10B981)
                                currentSection.contains("生活") -> Color(0xFFEC4899)
                                else -> Color(0xFF2563EB)
                            }
                            Spacer(Modifier.height(20.dp))
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = currentSectionColor.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val emoji = when {
                                        currentSection.contains("错题") -> "📝"
                                        currentSection.contains("范文") -> "📄"
                                        currentSection.contains("常识") -> "⚡"
                                        currentSection.contains("笔记") -> "📓"
                                        else -> "📌"
                                    }
                                    Text(emoji, style = MaterialTheme.typography.titleLarge)
                                    Spacer(Modifier.width(10.dp))
                                    Text(currentSection, fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium, color = currentSectionColor)
                                }
                            }
                        }
                        line.startsWith("- [ ] ") -> Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                        ) {
                            Row(modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = false, onCheckedChange = { _ -> viewModel.toggleCheckbox(index) },
                                    colors = CheckboxDefaults.colors(checkedColor = currentSectionColor))
                                Spacer(Modifier.width(6.dp))
                                Text(line.removePrefix("- [ ] ").trim(), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        line.startsWith("- [x] ") -> Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                        ) {
                            Row(modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = true, onCheckedChange = { _ -> viewModel.toggleCheckbox(index) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981)))
                                Spacer(Modifier.width(6.dp))
                                Text(line.removePrefix("- [x] ").trim(), style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        line.startsWith("### ") -> Text(
                            line.removePrefix("### ").trim(),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                        )
                        line.startsWith("> ") -> {
                            val quoteText = line.removePrefix("> ").trim()
                            if (quoteText.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF6B7280).copy(alpha = 0.06f),
                                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                                ) {
                                    Text(quoteText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF6B7280),
                                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp))
                                }
                            }
                        }
                        line.startsWith("- ") -> Text(
                            line.removePrefix("- ").trim(),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        else -> Text(line, style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
