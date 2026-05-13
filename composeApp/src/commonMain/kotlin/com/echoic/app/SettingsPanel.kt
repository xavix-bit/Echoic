package com.echoic.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echoic.shared.config.AppConfig
import com.echoic.shared.config.AppConfigData

@Composable
fun SettingsOverlay(
    visible: Boolean,
    config: AppConfig,
    configData: AppConfigData,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.46f))
                .clickable(onClick = onDismiss)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Escape) {
                        onDismiss()
                        true
                    } else false
                },
            contentAlignment = Alignment.Center,
        ) {
            // Card — stop click propagation
            Card(
                modifier = Modifier
                    .widthIn(min = 380.dp, max = 480.dp)
                    .heightIn(max = 600.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {},
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = strings.settingsTitle,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = strings.general,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Text("✕", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))

                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        // General
                        SettingsSection(title = strings.general) {
                            SettingRow(
                                title = strings.appearance,
                                subtitle = strings.appearanceDesc,
                            ) {
                                SegmentedPicker(
                                    options = listOf(strings.light, strings.dark, strings.system),
                                    selected = when (configData.appearance) {
                                        "light" -> strings.light
                                        "dark" -> strings.dark
                                        else -> strings.system
                                    },
                                    onSelect = {
                                        val value = when (it) {
                                            strings.light -> "light"
                                            strings.dark -> "dark"
                                            else -> "system"
                                        }
                                        config.updateAppearance(value)
                                        onUpdate()
                                    },
                                )
                            }

                            SettingRow(
                                title = strings.language,
                                subtitle = strings.languageDesc,
                            ) {
                                SegmentedPicker(
                                    options = listOf("English", "中文"),
                                    selected = if (configData.language == "zh") "中文" else "English",
                                    onSelect = {
                                        config.updateLanguage(if (it == "中文") "zh" else "en")
                                        onUpdate()
                                    },
                                )
                            }
                        }

                        // Data Management
                        SettingsSection(title = if (configData.language == "zh") "数据管理" else "Data") {
                            SettingRow(
                                title = if (configData.language == "zh") "存储位置" else "Storage",
                                subtitle = "~/.echoic/",
                            ) {
                                OutlinedButton(
                                    onClick = { openUrl(com.echoic.shared.platform.platformHomeDirectory() + "/.echoic") },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        if (configData.language == "zh") "打开" else "Open",
                                        fontSize = 11.sp,
                                    )
                                }
                            }

                            SettingRow(
                                title = if (configData.language == "zh") "重置配置" else "Reset Config",
                                subtitle = if (configData.language == "zh") "将所有设置恢复为默认值" else "Restore all settings to defaults",
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        config.reset()
                                        onUpdate()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                ) {
                                    Text(
                                        if (configData.language == "zh") "重置" else "Reset",
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }

                        // About
                        SettingsSection(title = if (configData.language == "zh") "关于" else "About") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        strings.version,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        "Kotlin Multiplatform • Compose Desktop",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { openUrl("https://github.com/ASleak/echoic-kmp") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text("GitHub ↗", fontSize = 12.sp)
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        control()
    }
}

@Composable
private fun SegmentedPicker(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            Text(
                text = option,
                fontSize = 12.sp,
                fontWeight = if (option == selected) FontWeight.Medium else FontWeight.Normal,
                color = if (option == selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (option == selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
