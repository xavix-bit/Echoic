package com.echoic.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echoic.shared.download.DownloadSource

/**
 * 下载源选择器对话框
 */
@Composable
fun DownloadSourceSelector(
    sources: List<DownloadSource>,
    selectedSource: DownloadSource?,
    isTestingSpeed: Boolean,
    sourceSpeeds: Map<String, Long>,
    onSelectSource: (DownloadSource) -> Unit,
    onAutoSelect: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    var currentSelection by remember { mutableStateOf(selectedSource) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.selectSource,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                sources.forEach { source ->
                    val isSelected = currentSelection == source
                    val speed = sourceSpeeds[source.url]

                    DownloadSourceItem(
                        source = source,
                        isSelected = isSelected,
                        speed = speed,
                        isTestingSpeed = isTestingSpeed,
                        onClick = { currentSelection = source },
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Auto select button
                OutlinedButton(
                    onClick = onAutoSelect,
                    enabled = !isTestingSpeed,
                ) {
                    if (isTestingSpeed) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(strings.autoSelect)
                }

                // Confirm button
                Button(
                    onClick = {
                        currentSelection?.let { onSelectSource(it) }
                        onConfirm()
                    },
                    enabled = currentSelection != null,
                ) {
                    Text(strings.save)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
    )
}

/**
 * 下载源列表项
 */
@Composable
private fun DownloadSourceItem(
    source: DownloadSource,
    isSelected: Boolean,
    speed: Long?,
    isTestingSpeed: Boolean,
    onClick: () -> Unit,
) {
    val strings = LocalStrings.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Radio button
            RadioButton(
                selected = isSelected,
                onClick = onClick,
            )

            // Source info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = source.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )

                    // Recommended badge
                    if (source.priority == 1) {
                        Text(
                            text = strings.recommended,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                // Description
                if (source.description.isNotEmpty()) {
                    Text(
                        text = source.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Speed and priority
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    // Speed
                    Text(
                        text = when {
                            isTestingSpeed -> "${strings.speed}: 测试中..."
                            speed != null && speed > 0 -> "${strings.speed}: ${formatSpeedLabel(speed)}"
                            speed != null && speed < 0 -> "${strings.speed}: 不可用"
                            else -> "${strings.speed}: 未测试"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Priority
                    Text(
                        text = "${strings.priority}: ${source.priority}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * 格式化速度
 */
private fun formatSpeedLabel(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond < 1024 -> "慢"
        bytesPerSecond < 100 * 1024 -> "中等"
        bytesPerSecond < 1024 * 1024 -> "快"
        else -> "很快"
    }
}
