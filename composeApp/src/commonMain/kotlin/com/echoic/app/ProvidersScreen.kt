package com.echoic.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echoic.shared.config.AppConfig
import com.echoic.shared.config.AppConfigData
import com.echoic.shared.download.DownloadManager
import com.echoic.shared.download.DownloadSource
import com.echoic.shared.installer.InstallState
import com.echoic.shared.installer.ModelInstaller
import com.echoic.shared.model.LocalModelManager
import com.echoic.shared.model.LocalTTSProvider
import com.echoic.shared.model.ModelInstallationStatus
import com.echoic.shared.model.TTSProvider
import com.echoic.shared.model.TTSTag
import kotlinx.coroutines.launch

private enum class ProviderTab { CLOUD, LOCAL }

@Composable
fun ProvidersScreen(
    config: AppConfig,
    configData: AppConfigData,
    onUpdate: () -> Unit,
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    // ── Tab state ────────────────────────────────────────────────
    var selectedTab by remember { mutableStateOf(ProviderTab.CLOUD) }

    // ── Cloud tab state ──────────────────────────────────────────
    var selectedCloudTags by remember { mutableStateOf(setOf<TTSTag>()) }
    var expandedCloudProvider by remember { mutableStateOf<TTSProvider?>(null) }
    val allCloudTags = remember { TTSProvider.entries.flatMap { it.tags }.distinct() }
    val filteredProviders = remember(selectedCloudTags) {
        if (selectedCloudTags.isEmpty()) TTSProvider.entries
        else TTSProvider.entries.filter { p -> selectedCloudTags.all { it in p.tags } }
    }

    // ── Local tab state (merged from LocalModelsScreen) ──────────
    val localManager = remember { LocalModelManager() }
    val downloadManager = remember { DownloadManager() }
    val installer = remember { ModelInstaller(downloadManager, localManager) }
    val modelStatuses by localManager.modelStatuses.collectAsState()
    var expandedLocalProvider by remember { mutableStateOf<LocalTTSProvider?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showSourceSelector by remember { mutableStateOf(false) }
    var currentInstallProvider by remember { mutableStateOf<LocalTTSProvider?>(null) }
    var currentInstallState by remember { mutableStateOf<InstallState>(InstallState.Idle) }
    var availableSources by remember { mutableStateOf<List<DownloadSource>>(emptyList()) }
    var sourceSpeeds by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var isTestingSpeed by remember { mutableStateOf(false) }
    var showUninstallDialog by remember { mutableStateOf(false) }
    var uninstallTargetProvider by remember { mutableStateOf<LocalTTSProvider?>(null) }
    var uninstallTargetSize by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Page header ──────────────────────────────────────────
        Text(
            text = strings.providers,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = if (selectedTab == ProviderTab.CLOUD) strings.cloudServicesDesc
            else strings.localModelsDesc,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ── Tab row ──────────────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
        ) {
            Tab(
                selected = selectedTab == ProviderTab.CLOUD,
                onClick = { selectedTab = ProviderTab.CLOUD },
                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            ) {
                Text(
                    text = strings.cloudModels,
                    fontSize = 13.sp,
                    fontWeight = if (selectedTab == ProviderTab.CLOUD) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            Tab(
                selected = selectedTab == ProviderTab.LOCAL,
                onClick = { selectedTab = ProviderTab.LOCAL },
                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            ) {
                Text(
                    text = strings.localModels,
                    fontSize = 13.sp,
                    fontWeight = if (selectedTab == ProviderTab.LOCAL) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }

        // ── Tab content ──────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                ProviderTab.CLOUD -> CloudTabContent(
                allTags = allCloudTags,
                selectedTags = selectedCloudTags,
                onTagToggle = { tag ->
                    selectedCloudTags = if (tag in selectedCloudTags) selectedCloudTags - tag else selectedCloudTags + tag
                },
                onClearTags = { selectedCloudTags = setOf() },
                providers = filteredProviders,
                config = config,
                configData = configData,
                expandedProvider = expandedCloudProvider,
                onToggleExpand = { provider ->
                    expandedCloudProvider = if (expandedCloudProvider == provider) null else provider
                },
                onUpdate = onUpdate,
                language = configData.language,
            )
            ProviderTab.LOCAL -> LocalTabContent(
                manager = localManager,
                downloadManager = downloadManager,
                installer = installer,
                modelStatuses = modelStatuses,
                config = config,
                expandedProvider = expandedLocalProvider,
                onToggleExpand = { provider ->
                    expandedLocalProvider = if (expandedLocalProvider == provider) null else provider
                },
                currentInstallProvider = currentInstallProvider,
                currentInstallState = currentInstallState,
                onInstall = { provider ->
                    if (currentInstallState is InstallState.Downloading && currentInstallProvider != null && currentInstallProvider != provider) {
                        downloadManager.cancelDownload()
                    }
                    currentInstallProvider = provider
                    currentInstallState = InstallState.Idle
                    scope.launch {
                        installer.installModel(
                            provider = provider,
                            onStateChange = { state ->
                                currentInstallState = state
                                if (state is InstallState.Completed) localManager.refreshAllStatuses()
                            },
                        )
                    }
                },
                onRetryInstall = { provider ->
                    if (currentInstallState is InstallState.Downloading && currentInstallProvider != null && currentInstallProvider != provider) {
                        downloadManager.cancelDownload()
                    }
                    currentInstallProvider = provider
                    currentInstallState = InstallState.Idle
                    scope.launch {
                        installer.reinstallModel(
                            provider = provider,
                            onStateChange = { state ->
                                currentInstallState = state
                                if (state is InstallState.Completed) localManager.refreshAllStatuses()
                            },
                        )
                    }
                },
                onSelectOtherSource = { provider ->
                    currentInstallProvider = provider
                    availableSources = installer.getDownloadSources(provider)
                    showSourceSelector = true
                },
                onUninstall = { provider ->
                    val status = modelStatuses[provider] ?: ModelInstallationStatus(provider = provider)
                    val size = if (status.isInstalled) localManager.formatModelSize(status.sizeInBytes)
                    else "${provider.modelSizeMB ?: 0} MB"
                    uninstallTargetProvider = provider
                    uninstallTargetSize = size
                    showUninstallDialog = true
                },
                onCancelInstall = {
                    downloadManager.cancelDownload()
                },
                onClearCache = { provider ->
                    scope.launch { localManager.clearCache(provider) }
                },
                onCheckUpdates = {
                    scope.launch { localManager.checkForUpdates() }
                },
                onClearAll = { showClearAllDialog = true },
            )
        }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────
    if (showUninstallDialog && uninstallTargetProvider != null) {
        AlertDialog(
            onDismissRequest = { showUninstallDialog = false },
            title = { Text(strings.uninstall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("确定要卸载 ${uninstallTargetProvider!!.displayName} 吗？")
                    Text("模型大小: $uninstallTargetSize", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("此操作无法撤销。", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { localManager.uninstallModel(uninstallTargetProvider!!) }
                        showUninstallDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(strings.uninstall) }
            },
            dismissButton = {
                TextButton(onClick = { showUninstallDialog = false }) { Text(strings.cancel) }
            },
        )
    }

    if (showSourceSelector && currentInstallProvider != null) {
        DownloadSourceSelector(
            sources = availableSources,
            selectedSource = null,
            isTestingSpeed = isTestingSpeed,
            sourceSpeeds = sourceSpeeds,
            onSelectSource = { source ->
                showSourceSelector = false
                if (currentInstallState is InstallState.Downloading && currentInstallProvider != null) {
                    downloadManager.cancelDownload()
                }
                scope.launch {
                    installer.installWithSource(
                        provider = currentInstallProvider!!,
                        source = source,
                        onStateChange = { state ->
                            currentInstallState = state
                            if (state is InstallState.Completed) localManager.refreshAllStatuses()
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
            onConfirm = { showSourceSelector = false },
            onDismiss = { showSourceSelector = false },
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text(strings.clearAll) },
            text = { Text("确定要卸载所有本地模型吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { localManager.clearAll() }
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(strings.clearAll) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text(strings.cancel) }
            },
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Cloud Tab
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun CloudTabContent(
    allTags: List<TTSTag>,
    selectedTags: Set<TTSTag>,
    onTagToggle: (TTSTag) -> Unit,
    onClearTags: () -> Unit,
    providers: List<TTSProvider>,
    config: AppConfig,
    configData: AppConfigData,
    expandedProvider: TTSProvider?,
    onToggleExpand: (TTSProvider) -> Unit,
    onUpdate: () -> Unit,
    language: String,
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Tag filter
        if (allTags.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            ) {
                allTags.forEach { tag ->
                    TagChip(
                        name = tag.displayName(language),
                        isSelected = tag in selectedTags,
                        onClick = { onTagToggle(tag) },
                    )
                }
                if (selectedTags.isNotEmpty()) {
                    TextButton(onClick = onClearTags) {
                        Text(strings.clearAll, fontSize = 11.sp)
                    }
                }
            }
        }

        // Provider cards
        providers.forEach { provider ->
            CloudProviderCard(
                provider = provider,
                config = config,
                configData = configData,
                isExpanded = expandedProvider == provider,
                onToggleExpand = { onToggleExpand(provider) },
                onUpdate = onUpdate,
            )
        }
        if (providers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface).padding(18.dp),
            ) {
                Text(strings.noProvidersMatch, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CloudProviderCard(
    provider: TTSProvider,
    config: AppConfig,
    configData: AppConfigData,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onUpdate: () -> Unit,
) {
    val strings = LocalStrings.current
    val isConfigured = config.isProviderConfigured(provider)
    val isDefaultCloud = config.getDefaultCloudProvider() == provider

    val statusText = if (isConfigured) strings.ready else strings.needsConfig
    val statusColor = if (isConfigured) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val expandRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = tween(200),
        label = "cloudExpandArrow",
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().hoverable(interactionSource),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isHovered) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isHovered) 3.dp else 1.dp,
        ),
    ) {
        Column {
            // ── Header row (always visible) ─────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Icon
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(provider.displayName.first().toString(), fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                // Name + subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(provider.displayName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface)
                        if (isDefaultCloud) {
                            Text(strings.isDefault, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp))
                        }
                    }
                    Text(provider.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Status
                Text(statusText, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = statusColor,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(statusColor.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp))

                // Default toggle button (always visible)
                if (isDefaultCloud) {
                    OutlinedButton(
                        onClick = { config.setDefaultCloudProvider(null); onUpdate() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(strings.cancel, fontSize = 10.sp)
                    }
                } else {
                    Button(
                        onClick = { config.setDefaultCloudProvider(provider); onUpdate() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        enabled = isConfigured,
                    ) {
                        Text(strings.setAsDefault, fontSize = 11.sp)
                    }
                }

                // Expand toggle
                Text(
                    text = "▸",
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onToggleExpand)
                        .padding(4.dp)
                        .rotate(expandRotation),
                )
            }

            // ── Expanded details ─────────────────────────────────
            AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Models
                    provider.availableModels.takeIf { it.isNotEmpty() }?.let { models ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(strings.availableModels, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                models.forEach { model ->
                                    Text(model.displayName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                        }
                    }

                    // Voices
                    provider.availableVoices.takeIf { it.isNotEmpty() }?.let { voices ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(strings.availableVoices, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                voices.forEach { voice ->
                                    Text(voice.displayName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                        }
                    }

                    // API Key configuration
                    CloudConfigSection(provider = provider, config = config, onUpdate = onUpdate)

                    // Tags
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        provider.tags.forEach { tag ->
                            Text(tag.displayName(configData.language), fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    // Visit website
                    provider.websiteURL?.let { url ->
                        OutlinedButton(onClick = { openUrl(url) }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)) {
                            Text(strings.visitWebsite, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudConfigSection(provider: TTSProvider, config: AppConfig, onUpdate: () -> Unit) {
    val strings = LocalStrings.current
    var editKey by remember { mutableStateOf(config.getApiKey(provider) ?: "") }
    var editURL by remember { mutableStateOf(config.getBaseURL(provider)) }
    var showKey by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // API Key configuration
                    if (provider.requiresAPIKey) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(strings.apiKey, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(65.dp))
                            OutlinedTextField(
                                value = editKey, onValueChange = { editKey = it }, modifier = Modifier.weight(1f),
                                placeholder = { Text(provider.apiKeyPlaceholder, fontSize = 11.sp) },
                                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true, shape = RoundedCornerShape(8.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                            )
                            IconButton(onClick = { showKey = !showKey }, modifier = Modifier.size(24.dp)) {
                                Text(if (showKey) "Hide" else "Show", fontSize = 9.sp)
                            }
                        }
                    }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.baseUrl, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(65.dp))
            OutlinedTextField(
                value = editURL, onValueChange = { editURL = it }, modifier = Modifier.weight(1f),
                singleLine = true, shape = RoundedCornerShape(8.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
            )
            if (editURL != provider.defaultBaseURL) {
                IconButton(onClick = { editURL = provider.defaultBaseURL }, modifier = Modifier.size(24.dp)) {
                    Text("↺", fontSize = 10.sp)
                }
            }
        }
        provider.helpURL?.let { url ->
            Text(strings.getApiKey, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { openUrl(url) }.padding(vertical = 2.dp))
        }
        Row { Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { config.setApiKey(provider, editKey); config.setBaseURL(provider, editURL); onUpdate() },
                modifier = Modifier.height(30.dp)) {
                Text(strings.saveConfig, fontSize = 11.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Local Tab
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun LocalTabContent(
    manager: LocalModelManager,
    downloadManager: DownloadManager,
    installer: ModelInstaller,
    modelStatuses: Map<LocalTTSProvider, ModelInstallationStatus>,
    config: AppConfig,
    expandedProvider: LocalTTSProvider?,
    onToggleExpand: (LocalTTSProvider) -> Unit,
    currentInstallProvider: LocalTTSProvider?,
    currentInstallState: InstallState,
    onInstall: (LocalTTSProvider) -> Unit,
    onRetryInstall: (LocalTTSProvider) -> Unit,
    onSelectOtherSource: (LocalTTSProvider) -> Unit,
    onUninstall: (LocalTTSProvider) -> Unit,
    onCancelInstall: () -> Unit,
    onClearCache: (LocalTTSProvider) -> Unit,
    onCheckUpdates: () -> Unit,
    onClearAll: () -> Unit,
) {
    val strings = LocalStrings.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Batch operations
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCheckUpdates, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(strings.checkUpdates, fontSize = 12.sp)
                }
                Button(onClick = onClearAll,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(strings.clearAll, fontSize = 12.sp)
                }
            }
        }

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
                val effectiveExpanded = expandedProvider == provider || isCurrentInstalling

                LocalProviderCard(
                    provider = provider,
                    status = status,
                    manager = manager,
                    config = config,
                    installState = if (currentInstallProvider == provider) currentInstallState else null,
                    isExpanded = effectiveExpanded,
                    onToggleExpand = { onToggleExpand(provider) },
                    onInstall = { onInstall(provider) },
                    onRetryInstall = { onRetryInstall(provider) },
                    onSelectOtherSource = { onSelectOtherSource(provider) },
                    onUninstall = { onUninstall(provider) },
                    onCancelInstall = onCancelInstall,
                    onClearCache = { onClearCache(provider) },
                    isInstalling = isCurrentInstalling,
                )
            }
        }
    }
}

@Composable
private fun LocalProviderCard(
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
    onCancelInstall: () -> Unit,
    onClearCache: () -> Unit,
    isInstalling: Boolean,
) {
    val strings = LocalStrings.current
    val isDefaultLocal = config.getDefaultLocalProvider() == provider
    val isInstalled = status.isInstalled

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val expandRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = tween(200),
        label = "expandArrow",
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().hoverable(interactionSource),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isHovered) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isHovered) 3.dp else 1.dp,
        ),
    ) {
        Column {
            // ── Header row ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Icon
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (isInstalled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(provider.displayName.first().toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = if (isInstalled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Name + subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(provider.displayName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface)
                        if (isDefaultLocal) {
                            Text(strings.isDefault, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp))
                        }
                    }
                    Text(provider.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Status badge
                LocalStatusBadge(isInstalled = isInstalled, installState = installState)

                // Default toggle button (always visible)
                if (isDefaultLocal) {
                    OutlinedButton(
                        onClick = { config.setDefaultLocalProvider(null) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) { Text(strings.cancel, fontSize = 10.sp) }
                } else {
                    Button(
                        onClick = { config.setDefaultLocalProvider(provider) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        enabled = isInstalled,
                    ) { Text(strings.setAsDefault, fontSize = 11.sp) }
                }

                // Expand toggle
                Text(
                    text = "▸",
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onToggleExpand)
                        .padding(4.dp)
                        .rotate(expandRotation),
                )
            }

            // ── Inline progress (visible when installing, even collapsed) ──
            when (installState) {
                is InstallState.Downloading -> InlineDownloadProgress(strings, installState, onCancelInstall)
                is InstallState.Extracting -> InlineExtractProgress(strings, installState)
                is InstallState.Verifying -> InlineVerifyProgress(strings)
                is InstallState.Cancelled -> Text("下载已取消 — 可重试", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp))
                else -> {}
            }

            // ── Expanded details ─────────────────────────────────
            AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    Text(provider.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Platform support
                    if (provider.platformSupport.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            provider.platformSupport.forEach { platform ->
                                Text(platform.displayName, fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }

                    if (provider.requiresGPU) {
                        Text("需要 GPU (${provider.minVRAM}GB+ VRAM)", fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 6.dp, vertical = 2.dp))
                    }

                    // Size + last used
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text(strings.modelSize, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (isInstalled) manager.formatModelSize(status.sizeInBytes)
                            else "${provider.modelSizeMB ?: 0} MB", fontSize = 13.sp,
                                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface) }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(strings.lastUsed, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(manager.formatLastUsed(status.lastUsedTimestamp), fontSize = 13.sp,
                                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    // Error / success messages
                    if (installState is InstallState.Cancelled)
                        Text("下载已取消", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (installState is InstallState.Failed)
                        Text("${strings.installFailed}: ${installState.error}", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    if (installState is InstallState.Completed)
                        Text(strings.installComplete, fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)

                    // Action buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isInstalled) {
                            OutlinedButton(onClick = onUninstall, modifier = Modifier.weight(1f), enabled = !isInstalling,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(strings.uninstall, fontSize = 12.sp) }
                            OutlinedButton(onClick = onClearCache, modifier = Modifier.weight(1f), enabled = !isInstalling,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(strings.clearCache, fontSize = 12.sp) }
                            OutlinedButton(onClick = { openUrl(manager.getModelPath(provider)) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(strings.openDirectory, fontSize = 12.sp) }
                        } else {
                            when (installState) {
                                is InstallState.Failed, is InstallState.Cancelled -> {
                                    Button(onClick = onRetryInstall, modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(strings.retryInstall, fontSize = 12.sp) }
                                    OutlinedButton(onClick = onSelectOtherSource, modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(strings.selectOtherSource, fontSize = 12.sp) }
                                }
                                is InstallState.Completed -> {
                                    Button(onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(strings.modelAlreadyInstalled, fontSize = 12.sp) }
                                }
                                else -> {
                                    Button(onClick = onInstall, modifier = Modifier.fillMaxWidth(), enabled = !isInstalling,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(strings.install, fontSize = 12.sp) }
                                }
                            }
                        }
                    }

                    // GitHub link
                    provider.githubURL?.let { url ->
                        OutlinedButton(onClick = { openUrl(url) }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)) {
                            Text(strings.viewOnGitHub, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Inline progress composables ──────────────────────────────────

@Composable
private fun InlineDownloadProgress(strings: Strings, state: InstallState.Downloading, onCancel: () -> Unit) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress.coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "downloadProgress",
    )
    val percentText = "${(state.progress * 100).toInt()}%"
    val etaText = if (state.speed > 0 && state.totalBytes > 0) {
        val remainingBytes = (state.totalBytes - state.downloadedBytes).coerceAtLeast(0)
        val etaSeconds = remainingBytes / state.speed
        when {
            etaSeconds < 60 -> "${etaSeconds}s"
            etaSeconds < 3600 -> "${etaSeconds / 60}m ${etaSeconds % 60}s"
            else -> "${etaSeconds / 3600}h ${(etaSeconds % 3600) / 60}m"
        }
    } else null

    Column(
        modifier = Modifier
            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Top row: percent + speed + ETA
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    percentText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (etaText != null) {
                    Text(
                        "≈ $etaText",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                formatSpeed(state.speed),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Progress bar with rounded shape
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        )

        // Bottom row: downloaded/total + source + cancel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.totalBytes > 0) {
                Text(
                    "${formatBytes(state.downloadedBytes)} / ${formatBytes(state.totalBytes)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    formatBytes(state.downloadedBytes),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TextButton(
                onClick = onCancel,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(26.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(strings.cancel, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun InlineExtractProgress(strings: Strings, state: InstallState.Extracting) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress.coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "extractProgress",
    )

    Column(
        modifier = Modifier
            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                strings.extracting,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "${(state.progress * 100).toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        )
    }
}

@Composable
private fun InlineVerifyProgress(strings: Strings) {
    Column(
        modifier = Modifier
            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            strings.verifying,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        )
    }
}

// ─── Shared components ────────────────────────────────────────────

@Composable
private fun LocalStatusBadge(isInstalled: Boolean, installState: InstallState?) {
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
    Text(text, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = color,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp))
}

@Composable
private fun TagChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = name, fontSize = 11.sp,
        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
