package com.echoic.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echoic.shared.audio.AudioPlayer
import com.echoic.shared.config.AppConfig
import com.echoic.shared.config.AppConfigData
import com.echoic.shared.download.DownloadManager
import com.echoic.shared.engine.CloudTTSEngine
import com.echoic.shared.engine.EngineCloudSynthesisGateway
import com.echoic.shared.engine.EngineLocalSynthesisGateway
import com.echoic.shared.engine.LocalTTSEngine
import com.echoic.shared.engine.SynthesisRequest
import com.echoic.shared.engine.SynthesisSelection
import com.echoic.shared.engine.SynthesisSession
import com.echoic.shared.engine.TTSError
import com.echoic.shared.installer.InstallState
import com.echoic.shared.installer.ModelInstaller
import com.echoic.shared.model.LocalModelManager
import com.echoic.shared.model.LocalTTSProvider
import com.echoic.shared.model.ProviderCatalog
import com.echoic.shared.model.ProviderOption
import com.echoic.shared.model.TTSProvider
import com.echoic.shared.model.TTSTag
import com.echoic.shared.model.Voice
import kotlinx.coroutines.launch

sealed class ModelSelection {
    data class Cloud(val provider: TTSProvider) : ModelSelection()
    data class Local(val provider: LocalTTSProvider) : ModelSelection()
}

@Composable
fun CloudTtsScreen(
    config: AppConfig,
    configData: AppConfigData,
    engine: CloudTTSEngine,
    localEngine: LocalTTSEngine?,
    audioPlayer: AudioPlayer,
    onOpenSettings: () -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val downloadManager = remember { DownloadManager() }
    val localModelManager = remember { LocalModelManager() }
    val installer = remember { ModelInstaller(downloadManager, localModelManager) }
    val providerCatalog = remember { ProviderCatalog() }
    val synthesisSession = remember(engine, localEngine) {
        SynthesisSession(
            cloudGateway = EngineCloudSynthesisGateway(engine),
            localGateway = localEngine?.let { EngineLocalSynthesisGateway(it) },
        )
    }
    var inputText by remember { mutableStateOf("") }
    var modelSelection by remember { mutableStateOf<ModelSelection>(ModelSelection.Cloud(TTSProvider.FISH_AUDIO)) }
    var selectedVoice by remember { mutableStateOf(Voice.FISH_DEFAULT) }
    var isSynthesizing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var audioData by remember { mutableStateOf<ByteArray?>(null) }
    var selectedTags by remember { mutableStateOf(setOf<TTSTag>()) }
    var showTagFilter by remember { mutableStateOf(false) }

    // Download state
    var downloadingProvider by remember { mutableStateOf<LocalTTSProvider?>(null) }
    var installState by remember { mutableStateOf<InstallState>(InstallState.Idle) }

    val strings = LocalStrings.current
    val charCount = inputText.length

    // Collect all available tags across provider catalog
    val allTags = remember(providerCatalog) { providerCatalog.options().flatMap { it.tags }.distinct() }

    // Build unified model items list
    val allItems = remember(providerCatalog, selectedTags) {
        providerCatalog.options(selectedTags).map { option ->
            when (option) {
                is ProviderOption.Cloud -> ModelSelection.Cloud(option.provider)
                is ProviderOption.Local -> ModelSelection.Local(option.provider)
            }
        }
    }

    // Current model state
    val currentCloudProvider = (modelSelection as? ModelSelection.Cloud)?.provider
    val isConfigured = remember(configData, modelSelection) {
        when (val sel = modelSelection) {
            is ModelSelection.Cloud -> config.isProviderConfigured(sel.provider)
            is ModelSelection.Local -> true
        }
    }
    val isLocalInstalled = remember(modelSelection) {
        (modelSelection as? ModelSelection.Local)?.let { localModelManager.isModelInstalled(it.provider) } ?: false
    }
    val canGenerate = remember(modelSelection, isConfigured, isLocalInstalled) {
        when (val sel = modelSelection) {
            is ModelSelection.Cloud -> isConfigured
            is ModelSelection.Local -> isLocalInstalled && localEngine != null && localEngine.supports(sel.provider)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Header ──────────────────────────────────────────────
        Text(
            text = strings.newGeneration,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = strings.newGenerationDesc,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ── Model selector ─────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.modelSelection,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box {
                Text(
                    text = "🏷 ${strings.filterTags} ▾",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (showTagFilter) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (showTagFilter) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else Color.Transparent
                        )
                        .clickable { showTagFilter = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                DropdownMenu(
                    expanded = showTagFilter,
                    onDismissRequest = { showTagFilter = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp).widthIn(min = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (allTags.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                allTags.forEach { tag ->
                                    TagChip(
                                        name = tag.displayName(configData.language),
                                        isSelected = tag in selectedTags,
                                        onClick = { selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag },
                                    )
                                }
                            }
                        }
                        if (selectedTags.isNotEmpty()) {
                            TextButton(onClick = { selectedTags = setOf() }) {
                                Text("✕ ${strings.clearAll}", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // ── Model cards grid ───────────────────────────────────
        val rows = allItems.chunked(4)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    rowItems.forEach { item ->
                        val isSelected = when {
                            item is ModelSelection.Cloud && modelSelection is ModelSelection.Cloud -> item.provider == modelSelection.let { (it as ModelSelection.Cloud).provider }
                            item is ModelSelection.Local && modelSelection is ModelSelection.Local -> item.provider == modelSelection.let { (it as ModelSelection.Local).provider }
                            else -> false
                        }
                        ModelCard(
                            item = item,
                            isSelected = isSelected,
                            config = config,
                            isInstalled = when (item) {
                                is ModelSelection.Local -> localModelManager.isModelInstalled(item.provider)
                                else -> false
                            },
                            isEngineSupported = when (item) {
                                is ModelSelection.Local -> localEngine?.supports(item.provider) ?: false
                                else -> true
                            },
                            modifier = Modifier.weight(1f),
                            onClick = {
                                modelSelection = item
                                if (item is ModelSelection.Cloud) {
                                    selectedVoice = item.provider.availableVoices.firstOrNull() ?: Voice.FISH_DEFAULT
                                }
                            },
                        )
                    }
                    // Fill remaining space if less than 4 items
                    repeat(4 - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // ── Voice selector (cloud only) ────────────────────────
        if (currentCloudProvider != null && isConfigured) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "${strings.voice}:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                currentCloudProvider.availableVoices.forEach { voice ->
                    VoiceChip(
                        name = voice.displayName,
                        isSelected = voice == selectedVoice,
                        onClick = { selectedVoice = voice },
                    )
                }
            }
        }

        // ── Config / download prompt ───────────────────────────
        when {
            currentCloudProvider != null && !isConfigured -> {
                CompactConfigPrompt(
                    provider = currentCloudProvider,
                    onNavigateToProviders = { onNavigate(Screen.PROVIDERS) },
                )
            }
            modelSelection is ModelSelection.Local && !isLocalInstalled -> {
                LocalInstallCard(
                    provider = (modelSelection as ModelSelection.Local).provider,
                    installState = if (downloadingProvider == (modelSelection as ModelSelection.Local).provider) installState else null,
                    onDownload = {
                        val provider = (modelSelection as ModelSelection.Local).provider
                        downloadingProvider = provider
                        installState = InstallState.Downloading(
                            progress = 0f, downloadedBytes = 0L, totalBytes = 0L, speed = 0L,
                        )
                        scope.launch {
                            installer.installModel(
                                provider = provider,
                                onStateChange = { state ->
                                    installState = state
                                    if (state is InstallState.Completed) {
                                        localModelManager.refreshAllStatuses()
                                    }
                                },
                            )
                        }
                    },
                    onCancelDownload = { downloadManager.cancelDownload() },
                )
            }
            modelSelection is ModelSelection.Local && !canGenerate -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(strings.modelNotSupported, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(strings.modelNotSupportedHint, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // ── Text input ─────────────────────────────────────────
        OutlinedTextField(
            value = inputText,
            onValueChange = {
                if (it.length <= 5000) inputText = it
            },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            placeholder = { Text(strings.textPlaceholder) },
            shape = RoundedCornerShape(10.dp),
        )

        // ── Bottom action bar ──────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatString(strings.charactersCount, charCount),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isSynthesizing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(strings.synthesizing, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = {
                        synthesisSession.cancel(); isSynthesizing = false
                    }) { Text(strings.cancel) }
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            isSynthesizing = true
                            errorMessage = null
                            try {
                                val request = when (val sel = modelSelection) {
                                    is ModelSelection.Cloud -> {
                                        SynthesisRequest(
                                            text = inputText,
                                            selection = SynthesisSelection.Cloud(sel.provider, selectedVoice),
                                        )
                                    }
                                    is ModelSelection.Local -> {
                                        SynthesisRequest(
                                            text = inputText,
                                            selection = SynthesisSelection.Local(sel.provider),
                                        )
                                    }
                                }
                                val result = synthesisSession.synthesize(request)
                                audioData = result.audioData
                                audioPlayer.play(result.audioData, result.format)
                            } catch (e: TTSError) {
                                errorMessage = e.message
                            } catch (e: Exception) {
                                errorMessage = e.message ?: strings.localSynthesisFailed
                            } finally {
                                isSynthesizing = false
                            }
                        }
                    },
                    enabled = inputText.isNotBlank() && canGenerate,
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Text(strings.generateSpeech, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Error message ─────────────────────────────────────
        errorMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("⚠", color = MaterialTheme.colorScheme.error)
                    Text(msg, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { errorMessage = null }) { Text(strings.dismiss) }
                }
            }
        }

        // ── Audio player ──────────────────────────────────────
        audioData?.let {
            AudioPlayerBar(audioPlayer = audioPlayer)
        }
    }
}

// ─── Model Card ─────────────────────────────────────────────────

@Composable
private fun ModelCard(
    item: ModelSelection,
    isSelected: Boolean,
    config: AppConfig,
    isInstalled: Boolean,
    isEngineSupported: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val strings = LocalStrings.current
    val providerName: String
    val isCloud: Boolean
    val statusText: String
    val statusColor: androidx.compose.ui.graphics.Color

    when (item) {
        is ModelSelection.Cloud -> {
            providerName = item.provider.displayName
            isCloud = true
            if (config.isProviderConfigured(item.provider)) {
                statusText = strings.ready
                statusColor = MaterialTheme.colorScheme.tertiary
            } else {
                statusText = strings.needsConfig
                statusColor = MaterialTheme.colorScheme.error
            }
        }
        is ModelSelection.Local -> {
            providerName = item.provider.displayName
            isCloud = false
            when {
                isInstalled -> {
                    statusText = strings.installed
                    statusColor = MaterialTheme.colorScheme.tertiary
                }
                !isEngineSupported -> {
                    statusText = strings.notSupported
                    statusColor = MaterialTheme.colorScheme.outline
                }
                else -> {
                    statusText = strings.notInstalled
                    statusColor = MaterialTheme.colorScheme.onSurfaceVariant
                }
            }
        }
    }

    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (isCloud) "☁️" else "💾",
                fontSize = 18.sp,
            )
            Text(
                text = providerName,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = statusText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = statusColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

// ─── Compact Config Prompt ──────────────────────────────────────

@Composable
private fun CompactConfigPrompt(
    provider: TTSProvider,
    onNavigateToProviders: () -> Unit,
) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = strings.apiKeyRequired,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = formatString(strings.apiKeyRequiredDesc, provider.displayName),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onNavigateToProviders,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(strings.navigateToConfig, fontSize = 12.sp)
            }
        }
    }
}

// ─── Local Install Card ─────────────────────────────────────────

@Composable
private fun LocalInstallCard(
    provider: LocalTTSProvider,
    installState: InstallState?,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
) {
    val strings = LocalStrings.current
    val isInstalling = installState != null &&
        installState !is InstallState.Idle &&
        installState !is InstallState.Completed &&
        installState !is InstallState.Failed &&
        installState !is InstallState.Cancelled

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Status display
            when (installState) {
                is InstallState.Downloading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "${strings.downloading}: ${(installState.progress * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (installState.currentFile > 0 && installState.totalFiles > 0) {
                                    Text(
                                        text = formatString(strings.downloadingMultiple, installState.currentFile, installState.totalFiles),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            TextButton(onClick = onCancelDownload) {
                                Text(strings.cancel, fontSize = 12.sp)
                            }
                        }
                        LinearProgressIndicator(
                            progress = { installState.progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Text(
                            text = if (installState.totalBytes > 0)
                                "${formatBytes(installState.downloadedBytes)} / ${formatBytes(installState.totalBytes)} · ${formatSpeed(installState.speed)}"
                            else strings.connectingDownload,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is InstallState.Extracting -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${strings.extracting}: ${(installState.progress * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        LinearProgressIndicator(progress = { installState.progress }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
                is InstallState.Verifying -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(strings.verifying, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
                is InstallState.Completed -> {
                    Text(strings.installComplete, fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
                }
                is InstallState.Cancelled -> {
                    Text(strings.downloadCancelled, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is InstallState.Failed -> {
                    Text("${strings.installFailed}: ${installState.error}", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
                else -> {}
            }

            // Download button (or retry for failed/cancelled)
            when (installState) {
                is InstallState.Failed, is InstallState.Cancelled -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onDownload, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                            Text(strings.retryInstall, fontSize = 12.sp)
                        }
                    }
                }
                is InstallState.Completed -> {
                    Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                        Text(strings.modelAlreadyInstalled, fontSize = 12.sp)
                    }
                }
                else -> {
                    Button(
                        onClick = onDownload,
                        enabled = !isInstalling,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (isInstalling) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.installing, fontSize = 12.sp)
                        } else {
                            Text(strings.downloadModel, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Reusable Components ────────────────────────────────────────

@Composable
private fun VoiceChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = name,
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(6.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun TagChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = name,
        fontSize = 11.sp,
        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun AudioPlayerBar(audioPlayer: AudioPlayer) {
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val currentTime by audioPlayer.currentTime.collectAsState()
    val duration by audioPlayer.duration.collectAsState()

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton(onClick = { if (isPlaying) audioPlayer.pause() else audioPlayer.resume() }) {
                Text(if (isPlaying) "⏸" else "▶", fontSize = 16.sp)
            }
            Text(text = formatTime(currentTime), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(
                progress = { if (duration > 0) (currentTime / duration).toFloat() else 0f },
                modifier = Modifier.weight(1f).height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(text = formatTime(duration), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
