package com.thoughtcapture.app.ui.capture

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.thoughtcapture.app.service.VoiceRecognitionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    viewModel: CaptureViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { bmp ->
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "capture_${System.currentTimeMillis()}.jpg")
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
            val imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            imageUri?.let { uri ->
                resolver.openOutputStream(uri)?.use { out ->
                    bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                viewModel.onMediaSelected(uri, "photo")
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onMediaSelected(it, "photo") }
    }

    // 保存成功动画
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            Toast.makeText(context, "✨ 已保存", Toast.LENGTH_SHORT).show()
            viewModel.clearSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 100.dp)
        ) {
            // 标题区 + 模式切换
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (uiState.isPlanMode) "📋" else "💡",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (uiState.isPlanMode) "今日规划" else "新想法",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (uiState.isPlanMode)
                        Color(0xFF10B981).copy(alpha = 0.12f)
                    else
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    onClick = { viewModel.togglePlanMode() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (uiState.isPlanMode) "做计划" else "记想法",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (uiState.isPlanMode) Color(0xFF10B981)
                                    else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 输入卡片
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                OutlinedTextField(
                    value = uiState.textInput,
                    onValueChange = viewModel::onTextChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp),
                    placeholder = { Text("此刻在想什么？写下来…") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surface,
                    ),
                    maxLines = 12
                )
            }

            // 图片预览
            AnimatedVisibility(
                visible = uiState.mediaUri != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (uiState.mediaUri != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                            AsyncImage(
                                model = uiState.mediaUri,
                                contentDescription = "预览",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            IconButton(
                                onClick = viewModel::onMediaRemoved,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    "移除",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .then(Modifier.size(28.dp))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 工具按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 语音按钮 — 点击弹出系统语音对话框
                val voiceHelper = remember { VoiceRecognitionHelper(context as androidx.activity.ComponentActivity) }
                var isRecording by remember { mutableStateOf(false) }
                var hasAudioPerm by remember {
                    mutableStateOf(
                        androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        == android.content.pm.PackageManager.PERMISSION_GRANTED
                    )
                }
                val audioPermLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted -> hasAudioPerm = granted }

                FilledTonalButton(
                    onClick = {
                        if (!hasAudioPerm) {
                            audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            isRecording = true
                            voiceHelper.startListening { result ->
                                isRecording = false
                                if (result != null) {
                                    val current = viewModel.uiState.value.textInput
                                    viewModel.onTextChanged(
                                        if (current.isBlank()) result else "$current\n$result"
                                    )
                                }
                            }
                        }
                    },
                    enabled = !uiState.isSaving,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    colors = if (isRecording)
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                            contentColor = Color(0xFFEF4444)
                        )
                    else ButtonDefaults.filledTonalButtonColors()
                ) {
                    Icon(Icons.Default.Mic, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (!hasAudioPerm) "点我授权麦克风"
                        else if (isRecording) "识别中…" else "语音",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // 拍照按钮
                FilledTonalButton(
                    onClick = { cameraLauncher.launch(null) },
                    enabled = !uiState.isSaving,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("拍照", style = MaterialTheme.typography.labelLarge)
                }

                // 相册按钮
                FilledTonalButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    enabled = !uiState.isSaving,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("图片", style = MaterialTheme.typography.labelLarge)
                }
            }

            // 错误提示
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                uiState.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 底部悬浮保存按钮
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
        ) {
            Button(
                onClick = { viewModel.onSave("app") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                enabled = !uiState.isSaving && (uiState.textInput.isNotBlank() || uiState.mediaUri != null),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedContent(targetState = uiState.isSaving, label = "save") { saving ->
                        if (saving) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("保存中…", style = MaterialTheme.typography.titleMedium)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("保存想法", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
