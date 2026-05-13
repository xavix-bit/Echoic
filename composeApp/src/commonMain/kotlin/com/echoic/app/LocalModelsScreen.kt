package com.echoic.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echoic.shared.config.AppConfig
import com.echoic.shared.download.DownloadManager
import com.echoic.shared.download.DownloadSource
import com.echoic.shared.installer.InstallState
import com.echoic.shared.installer.ModelInstaller
import com.echoic.shared.model.LocalModelManager
import com.echoic.shared.model.LocalTTSProvider
import com.echoic.shared.model.ModelInstallationStatus
import kotlinx.coroutines.launch

@Composable
fun LocalModelsScreen(config: AppConfig) {
    val strings = LocalStrings.current
    val manager = remember { LocalModelManager() }
    val downloadManager = remember { DownloadManager() }
    val installer = remember { ModelInstaller(downloadManager, manager) }
    val modelStatuses by manager.modelStatuses.collectAsState()
    val scope = rememberCoroutineScope()

    var expandedProvider by remember { mutableStateOf<LocalTTSProvider?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showSourceSelector by remember { mutableStateOf(false) }
    var currentInstallProvider by remember { mutableStateOf<LocalTTSProvider?>(null) }
    var currentInstallState by remember { mutableStateOf<InstallState>(InstallState.Idle) }
    var availableSources by remember { mutableStateOf<List<DownloadSource>>(emptyList()) }
    var sourceSpeeds by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var isTestingSpeed by remember { mutableStateOf(false) }

    // Uninstall confirmation dialog state
    var showUninstallDialog by remember { mutableStateOf(false) }
    var uninstallTargetProvider by remember { mutableStateOf<LocalTTSProvider?>(null) }
    var uninstallTargetSize by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = strings.localModels,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${LocalTTSProvider.supportedEntries.size} providers available",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Batch operations
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            manager.checkForUpdates()
                        }
                    },
                ) {
                    Text(strings.checkUpdates, fontSize = 13.sp)
                }

                Button(
                    onClick = { showClearAllDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                ) {
                    Text(strings.clearAll, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Model list
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LocalTTSProvider.supportedEntries.forEach { provider ->
                val status = modelStatuses[provider] ?: ModelInstallationStatus(provider = provider)
                val isCurrentInstalling = currentInstallProvider == provider &&
                    currentInstallState !is InstallState.Idle &&
                    currentInstallState !is InstallState.Completed &&
                    currentInstallState !is InstallState.Failed

                // Auto-expand when installing
                val effectiveExpanded = expandedProvider == provider || isCurrentInstalling

                LocalModelCard(
                    provider = provider,
                    status = status,
                    manager = manager,
                    config = config,
                    installState = if (currentInstallProvider == provider) currentInstallState else null,
                    isExpanded = effectiveExpanded,
                    onToggleExpand = {
                        expandedProvider = if (expandedProvider == provider) null else provider
                    },
                    onInstall = {
                        currentInstallProvider = provider
                        currentInstallState = InstallState.Idle
                        scope.launch {
                            installer.installModel(
                                provider = provider,
                                onStateChange = { state ->
                                    currentInstallState = state
                                    if (state is InstallState.Completed) {
                                        manager.refreshAllStatuses()
                                    }
                                },
                            )
                        }
                    },
                    onRetryInstall = {
                        currentInstallProvider = provider
                        currentInstallState = InstallState.Idle
                        scope.launch {
                            installer.reinstallModel(
                                provider = provider,
                                onStateChange = { state ->
                                    currentInstallState = state
                                    if (state is InstallState.Completed) {
                                        manager.refreshAllStatuses()
                                    }
                                },
                            )
                        }
                    },
                    onSelectOtherSource = {
                        currentInstallProvider = provider
                        availableSources = installer.getDownloadSources(provider)
                        showSourceSelector = true
                    },
                    onUninstall = {
                        val size = if (status.isInstalled)
                            manager.formatModelSize(status.sizeInBytes)
                        else
                            "${provider.modelSizeMB ?: 0} MB"
                        uninstallTargetProvider = provider
                        uninstallTargetSize = size
                        showUninstallDialog = true
                    },
                    onClearCache = {
                        scope.launch {
                            manager.clearCache(provider)
                        }
                    },
                    isInstalling = isCurrentInstalling,
                )
            }
        }
    }

    // Uninstall confirmation dialog
    if (showUninstallDialog && uninstallTargetProvider != null) {
        AlertDialog(
            onDismissRequest = { showUninstallDialog = false },
            title = { Text(strings.uninstall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("确定要卸载 ${uninstallTargetProvider!!.displayName} 吗？")
                    Text(
                        text = "模型大小: $uninstallTargetSize",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "此操作无法撤销。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            manager.uninstallModel(uninstallTargetProvider!!)
                        }
                        showUninstallDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                ) {
                    Text(strings.uninstall)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUninstallDialog = false }) {
                    Text(strings.cancel)
                }
            },
        )
    }

    // Source selector dialog
    if (showSourceSelector && currentInstallProvider != null) {
        DownloadSourceSelector(
            sources = availableSources,
            selectedSource = null,
            isTestingSpeed = isTestingSpeed,
            sourceSpeeds = sourceSpeeds,
            onSelectSource = { source ->
                showSourceSelector = false
                scope.launch {
                    installer.installWithSource(
                        provider = currentInstallProvider!!,
                        source = source,
                        onStateChange = { state ->
                            currentInstallState = state
                            if (state is InstallState.Completed) {
                                manager.refreshAllStatuses()
                            }
                        },
                    )
                }
            },
            onAutoSelect = {
                scope.launch {
                    isTestingSpeed = true
                    val speeds = mutableMapOf<String, Long>()
                    availableSources.forEach { source ->
                        speeds[source.url] = downloadManager.testSourceSpeed(source.url)
                    }
                    sourceSpeeds = speeds
                    isTestingSpeed = false
                }
            },
            onConfirm = {
                showSourceSelector = false
            },
            onDismiss = {
                showSourceSelector = false
            },
        )
    }

    // Clear all confirmation dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text(strings.clearAll) },
            text = { Text("确定要卸载所有本地模型吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            manager.clearAll()
                        }
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                ) {
                    Text(strings.clearAll)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text(strings.cancel)
                }
            },
        )
    }
}

@Composable
private fun LocalModelCard(
    provider: LocalTTSProvider,
    status: ModelInstallationStatus,
    manager: LocalModelManager,
    config: AppConfig,
    installState: InstallState?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onInstall: () -> Unit,
    onRetryInstall: () -> Unit,
    onSelectOtherSource: () -> Unit,
    onUninstall: () -> Unit,
    onClearCache: () -> Unit,
    isInstalling: Boolean,
) {
    val strings = LocalStrings.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Provider icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (status.isInstalled)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = provider.displayName.first().toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (status.isInstalled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Provider info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = provider.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = provider.subtitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Status badge
                InstallStatusBadge(
                    isInstalled = status.isInstalled,
                    installState = installState,
                )

                // Expand indicator
                Text(
                    text = if (isExpanded) "\u25BC" else "\u25B6",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Inline progress bar (visible when installing, without expanding)
            if (installState is InstallState.Downloading) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "${(installState.progress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (installState.currentFile > 0 && installState.totalFiles > 0) {
                                Text(
                                    text = "文件 ${installState.currentFile}/${installState.totalFiles}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            text = formatSpeed(installState.speed),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { installState.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    if (installState.totalBytes > 0) {
                        Text(
                            text = "${formatBytes(installState.downloadedBytes)} / ${formatBytes(installState.totalBytes)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (installState is InstallState.Extracting) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "${strings.extracting}: ${(installState.progress * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    LinearProgressIndicator(
                        progress = { installState.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            } else if (installState is InstallState.Verifying) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = strings.verifying,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            } else if (installState is InstallState.Cancelled) {
                Text(
                    text = "下载已取消 — 可重试",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                )
            }

            // Expanded details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Description
                    Text(
                        text = provider.notes,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Platform support
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = strings.platformSupport,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            provider.platformSupport.take(5).forEach { platform ->
                                Text(
                                    text = platform.displayName,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                            if (provider.platformSupport.size > 5) {
                                Text(
                                    text = "+${provider.platformSupport.size - 5}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }

                    // GPU requirement
                    if (provider.requiresGPU) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "需要 GPU (${provider.minVRAM}GB+ VRAM)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    // Model info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = strings.modelSize,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = if (status.isInstalled)
                                    manager.formatModelSize(status.sizeInBytes)
                                else
                                    "${provider.modelSizeMB ?: 0} MB",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = strings.lastUsed,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = manager.formatLastUsed(status.lastUsedTimestamp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    // Cancelled message
                    if (installState is InstallState.Cancelled) {
                        Text(
                            text = "下载已取消",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Error message if failed
                    if (installState is InstallState.Failed) {
                        Text(
                            text = "${strings.installFailed}: ${installState.error}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    // Success message if completed
                    if (installState is InstallState.Completed) {
                        Text(
                            text = strings.installComplete,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (status.isInstalled) {
                            OutlinedButton(
                                onClick = onUninstall,
                                modifier = Modifier.weight(1f),
                                enabled = !isInstalling,
                            ) {
                                Text(strings.uninstall, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = onClearCache,
                                modifier = Modifier.weight(1f),
                                enabled = !isInstalling,
                            ) {
                                Text(strings.clearCache, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    openUrl(manager.getModelPath(provider))
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(strings.openDirectory, fontSize = 13.sp)
                            }
                        } else {
                            when (installState) {
                                is InstallState.Failed, is InstallState.Cancelled -> {
                                    Button(
                                        onClick = onRetryInstall,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(strings.retryInstall, fontSize = 13.sp)
                                    }

                                    OutlinedButton(
                                        onClick = onSelectOtherSource,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(strings.selectOtherSource, fontSize = 13.sp)
                                    }
                                }
                                is InstallState.Completed -> {
                                    Button(
                                        onClick = {},
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = false,
                                    ) {
                                        Text(strings.modelAlreadyInstalled, fontSize = 13.sp)
                                    }
                                }
                                else -> {
                                    Button(
                                        onClick = onInstall,
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !isInstalling,
                                    ) {
                                        Text(strings.install, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Set as default button
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(Modifier.height(8.dp))

                    val isDefaultLocal = config.getDefaultLocalProvider() == provider
                    if (isDefaultLocal) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(strings.isDefault, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.weight(1f))
                            OutlinedButton(
                                onClick = {
                                    config.setDefaultLocalProvider(null)
                                },
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(strings.clearAll, fontSize = 11.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                config.setDefaultLocalProvider(provider)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            enabled = status.isInstalled,
                        ) {
                            Text(strings.setAsDefault, fontSize = 12.sp)
                        }
                    }

                    // GitHub link
                    provider.githubURL?.let { url ->
                        OutlinedButton(
                            onClick = { openUrl(url) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "${strings.viewOnGitHub} \u2197",
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallStatusBadge(
    isInstalled: Boolean,
    installState: InstallState?,
) {
    val strings = LocalStrings.current

    val (text, color) = when {
        installState is InstallState.Downloading -> strings.installing to MaterialTheme.colorScheme.primary
        installState is InstallState.Extracting -> strings.extracting to MaterialTheme.colorScheme.primary
        installState is InstallState.Verifying -> strings.verifying to MaterialTheme.colorScheme.primary
        installState is InstallState.Cancelled -> "已取消" to MaterialTheme.colorScheme.outline
        installState is InstallState.Failed -> strings.installFailed to MaterialTheme.colorScheme.error
        installState is InstallState.Completed -> strings.installComplete to MaterialTheme.colorScheme.tertiary
        isInstalled -> strings.installed to MaterialTheme.colorScheme.tertiary
        else -> strings.notInstalled to MaterialTheme.colorScheme.outline
    }

    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

