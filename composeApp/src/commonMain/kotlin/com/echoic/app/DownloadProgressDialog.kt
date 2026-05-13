package com.echoic.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echoic.shared.download.DownloadState

/**
 * 下载进度对话框
 */
@Composable
fun DownloadProgressDialog(
    downloadState: DownloadState,
    providerName: String,
    onSwitchSource: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current

    AlertDialog(
        onDismissRequest = {
            if (downloadState !is DownloadState.Downloading && downloadState !is DownloadState.Idle) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = strings.downloading,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Provider name
                Text(
                    text = providerName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )

                // Download state content
                when (downloadState) {
                    is DownloadState.Idle -> {
                        Text(
                            text = "准备下载...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    is DownloadState.Downloading -> {
                        // Source info
                        Text(
                            text = "下载源: ${downloadState.sourceName}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        // Progress bar
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(
                                progress = { downloadState.progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )

                            // Progress text and speed
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${(downloadState.progress * 100).toInt()}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = formatSpeed(downloadState.speed),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            // Downloaded / Total bytes
                            if (downloadState.totalBytes > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "${formatBytes(downloadState.downloadedBytes)} / ${formatBytes(downloadState.totalBytes)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    // Estimated remaining time
                                    if (downloadState.speed > 0) {
                                        val remainingBytes = downloadState.totalBytes - downloadState.downloadedBytes
                                        val remainingSeconds = remainingBytes / downloadState.speed
                                        Text(
                                            text = "剩余: ${formatTime(remainingSeconds)}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            if (downloadState.currentFile > 0 && downloadState.totalFiles > 0) {
                                Text(
                                    text = "文件 ${downloadState.currentFile}/${downloadState.totalFiles}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    is DownloadState.Completed -> {
                        Text(
                            text = strings.downloadComplete,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "已保存到: ${downloadState.path}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    is DownloadState.Failed -> {
                        Text(
                            text = strings.downloadFailed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = downloadState.error,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    is DownloadState.Cancelled -> {
                        Text(
                            text = "下载已取消",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Switch source button (only during download)
                if (downloadState is DownloadState.Downloading) {
                    OutlinedButton(onClick = onSwitchSource) {
                        Text(strings.switchSource)
                    }
                }

                // Cancel / Close button
                when (downloadState) {
                    is DownloadState.Downloading -> {
                        TextButton(
                            onClick = onCancel,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text(strings.cancel)
                        }
                    }

                    is DownloadState.Completed -> {
                        Button(onClick = onDismiss) {
                            Text(strings.save)
                        }
                    }

                    is DownloadState.Cancelled, is DownloadState.Failed -> {
                        Button(onClick = onDismiss) {
                            Text(strings.save)
                        }
                    }

                    else -> {
                        TextButton(onClick = onCancel) {
                            Text(strings.cancel)
                        }
                    }
                }
            }
        },
    )
}


/**
 * 格式化时间（秒）为人类可读字符串
 */
private fun formatTime(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}秒"
        seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
        else -> "${seconds / 3600}时${(seconds % 3600) / 60}分"
    }
}
