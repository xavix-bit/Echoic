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
import com.echoic.shared.engine.CloudTTSEngine
import com.echoic.shared.engine.EngineCloudSynthesisGateway
import com.echoic.shared.engine.EngineLocalSynthesisGateway
import com.echoic.shared.engine.LocalTTSEngine
import com.echoic.shared.engine.SynthesisRequest
import com.echoic.shared.engine.SynthesisSelection
import com.echoic.shared.engine.SynthesisSession
import com.echoic.shared.engine.TTSError
import com.echoic.shared.model.LocalTTSVoice
import com.echoic.shared.model.LocalTTSProvider
import com.echoic.shared.model.TTSProvider
import com.echoic.shared.model.Voice
import com.echoic.shared.model.availableVoices
import kotlinx.coroutines.launch

enum class SynthesisMode { CLOUD, LOCAL }

@Composable
fun GenerateScreen(
    config: AppConfig,
    configData: AppConfigData,
    engine: CloudTTSEngine,
    localEngine: LocalTTSEngine?,
    audioPlayer: AudioPlayer,
    onOpenSettings: () -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current
    val synthesisSession = remember(engine, localEngine) {
        SynthesisSession(
            cloudGateway = EngineCloudSynthesisGateway(engine),
            localGateway = localEngine?.let { EngineLocalSynthesisGateway(it) },
        )
    }

    val defaultCloud = config.getDefaultCloudProvider()
    val defaultLocal = config.getDefaultLocalProvider()

    // Determine initial mode: prefer cloud if set, else local, else cloud
    val initialMode = when {
        defaultCloud != null -> SynthesisMode.CLOUD
        defaultLocal != null -> SynthesisMode.LOCAL
        else -> SynthesisMode.CLOUD
    }
    var mode by remember { mutableStateOf(initialMode) }
    var inputText by remember { mutableStateOf("") }
    var selectedVoice by remember { mutableStateOf<Voice?>(null) }
    var selectedLocalVoice by remember { mutableStateOf<LocalTTSVoice?>(null) }
    var isSynthesizing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var audioData by remember { mutableStateOf<ByteArray?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    // Auto-select default voice when cloud provider changes
    LaunchedEffect(defaultCloud) {
        selectedVoice = defaultCloud?.availableVoices?.firstOrNull()
    }

    val charCount = inputText.length

    // Determine cloud provider status
    val cloudProvider = defaultCloud
    val isCloudConfigured = cloudProvider != null && config.isProviderConfigured(cloudProvider)

    // Determine local provider status
    val localModelManager = remember { com.echoic.shared.model.LocalModelManager() }
    val installedSupportedLocal = remember(localEngine) {
        LocalTTSProvider.supportedEntries.firstOrNull { provider ->
            localEngine?.supports(provider) == true && localModelManager.isModelInstalled(provider)
        }
    }
    val localProvider = when {
        defaultLocal != null && localEngine?.supports(defaultLocal) == true -> defaultLocal
        installedSupportedLocal != null -> installedSupportedLocal
        else -> defaultLocal
    }
    val isLocalInstalled = localProvider != null && localModelManager.isModelInstalled(localProvider)
    val isLocalSupported = localProvider != null && localEngine != null && localEngine.supports(localProvider)
    val localVoices = localProvider?.availableVoices.orEmpty()

    LaunchedEffect(localProvider) {
        selectedLocalVoice = localVoices.firstOrNull()
    }

    val canGenerate = when (mode) {
        SynthesisMode.CLOUD -> cloudProvider != null && isCloudConfigured
        SynthesisMode.LOCAL -> isLocalInstalled && isLocalSupported
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── Header ──────────────────────────────────────────────
        Text(
            text = strings.generateSpeechTitle,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-0.3).sp,
        )
        Text(
            text = strings.generateSpeechDesc,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp,
        )

        // ── Mode Toggle ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.synthesisMode,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ModeToggleChip(
                label = strings.cloud,
                code = "CL",
                isSelected = mode == SynthesisMode.CLOUD,
                onClick = { mode = SynthesisMode.CLOUD },
            )
            ModeToggleChip(
                label = strings.local,
                code = "LC",
                isSelected = mode == SynthesisMode.LOCAL,
                onClick = { mode = SynthesisMode.LOCAL },
            )
        }

        // ── Default Model Status ───────────────────────────────
        when (mode) {
            SynthesisMode.CLOUD -> {
                if (cloudProvider != null) {
                    DefaultModelInfoCard(
                        providerName = cloudProvider.displayName,
                        isConfigured = isCloudConfigured,
                        subtitle = if (isCloudConfigured) strings.ready else strings.needsConfig,
                        subtitleColor = if (isCloudConfigured) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.error,
                    )
                } else {
                    NoDefaultPrompt(
                        message = strings.noDefaultCloud,
                        actionLabel = strings.goToProviders,
                        onAction = { onNavigate(Screen.PROVIDERS) },
                    )
                }
            }
            SynthesisMode.LOCAL -> {
                if (localProvider != null) {
                    DefaultModelInfoCard(
                        providerName = localProvider.displayName,
                        isConfigured = isLocalInstalled && isLocalSupported,
                        subtitle = when {
                            !isLocalSupported -> strings.notSupported
                            !isLocalInstalled -> strings.notInstalled
                            else -> strings.installed
                        },
                        subtitleColor = when {
                            !isLocalSupported -> MaterialTheme.colorScheme.error
                            !isLocalInstalled -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.tertiary
                        },
                    )
                } else {
                    NoDefaultPrompt(
                        message = strings.noDefaultLocal,
                        actionLabel = strings.goToLocalModels,
                        onAction = { onNavigate(Screen.PROVIDERS) },
                    )
                }
            }
        }

        // ── Voice selector (cloud only) ────────────────────────
        if (mode == SynthesisMode.CLOUD && cloudProvider != null && isCloudConfigured) {
            val voices = cloudProvider.availableVoices
            if (voices.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "${strings.voice}:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    voices.forEach { voice ->
                        VoiceChip(
                            name = voice.displayName,
                            isSelected = voice == selectedVoice,
                            onClick = { selectedVoice = voice },
                        )
                    }
                }
            }
        }
        if (mode == SynthesisMode.LOCAL && localProvider != null && isLocalInstalled && isLocalSupported) {
            LocalVoiceDropdown(
                voices = localVoices,
                selectedVoice = selectedLocalVoice,
                onVoiceSelected = { selectedLocalVoice = it },
            )
        }

        // ── API Key / Install warning ──────────────────────────
        if (mode == SynthesisMode.CLOUD && cloudProvider != null && !isCloudConfigured) {
            ApiKeyWarningCard(
                providerName = cloudProvider.displayName,
                onGoToProviders = { onNavigate(Screen.PROVIDERS) },
            )
        }
        if (mode == SynthesisMode.LOCAL && localProvider != null && (!isLocalInstalled || !isLocalSupported)) {
            LocalModelWarningCard(
                provider = localProvider,
                notSupportedReason = if (!isLocalSupported) strings.modelNotSupportedHint else null,
                onGoToLocalModels = { onNavigate(Screen.PROVIDERS) },
            )
        }

        // ── Text input (fills remaining space) ─────────────────
        OutlinedTextField(
            value = inputText,
            onValueChange = {
                if (it.length <= 5000) inputText = it
            },
            modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 120.dp),
            placeholder = { Text(strings.textPlaceholder) },
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, lineHeight = 22.sp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
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
                        synthesisSession.cancel()
                        isSynthesizing = false
                    }) { Text(strings.cancel) }
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            isSynthesizing = true
                            errorMessage = null
                            try {
                                val request = when (mode) {
                                    SynthesisMode.CLOUD -> {
                                        val provider = cloudProvider!!
                                        SynthesisRequest(
                                            text = inputText,
                                            selection = SynthesisSelection.Cloud(provider, selectedVoice),
                                        )
                                    }
                                    SynthesisMode.LOCAL -> {
                                        SynthesisRequest(
                                            text = inputText,
                                            selection = SynthesisSelection.Local(
                                                provider = localProvider!!,
                                                voiceId = selectedLocalVoice?.id ?: 0,
                                            ),
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
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                ) {
                    Text(strings.generateSpeech, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Error message ─────────────────────────────────────
        errorMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                    Text(msg, fontSize = 13.sp, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = { errorMessage = null }) { Text(strings.dismiss) }
                }
            }
        }

        // ── Audio player bar ──────────────────────────────────
        if (audioData != null) {
            AudioPlayerBar(audioPlayer = audioPlayer)
        }
    }
}

// ─── Sub-components ───────────────────────────────────────────────

@Composable
private fun ModeToggleChip(
    label: String,
    code: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = code,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        if (isSelected) contentColor.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun DefaultModelInfoCard(
    providerName: String,
    isConfigured: Boolean,
    subtitle: String,
    subtitleColor: Color,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = LocalStrings.current.defaultModel,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = providerName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = subtitleColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(subtitleColor.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun NoDefaultPrompt(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(actionLabel, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ApiKeyWarningCard(
    providerName: String,
    onGoToProviders: () -> Unit,
) {
    val strings = LocalStrings.current
    Card(
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
                    text = formatString(strings.apiKeyRequiredDesc, providerName),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onGoToProviders,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(strings.navigateToConfig, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LocalModelWarningCard(
    provider: LocalTTSProvider,
    notSupportedReason: String?,
    onGoToLocalModels: () -> Unit,
) {
    val strings = LocalStrings.current
    Card(
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
                    text = if (notSupportedReason != null) strings.modelNotSupported else strings.downloadFirst,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (notSupportedReason != null) {
                    Text(
                        text = notSupportedReason,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Button(
                onClick = onGoToLocalModels,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(strings.goToLocalModels, fontSize = 12.sp)
            }
        }
    }
}

// ─── Voice Chip ───────────────────────────────────────────────────

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

// ─── Audio Player Bar ─────────────────────────────────────────────

@Composable
private fun AudioPlayerBar(audioPlayer: AudioPlayer) {
    val strings = LocalStrings.current
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val currentTime by audioPlayer.currentTime.collectAsState()
    val duration by audioPlayer.duration.collectAsState()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Rounded play/pause button
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .width(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable { if (isPlaying) audioPlayer.pause() else audioPlayer.resume() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isPlaying) "Pause" else "Play",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Status text
            Text(
                text = if (isPlaying) strings.playing else strings.playbackStopped,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isPlaying) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Current time
            Text(
                text = formatTime(currentTime),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Progress slider
            Slider(
                value = if (duration > 0) (currentTime / duration).toFloat() else 0f,
                onValueChange = { fraction ->
                    audioPlayer.seek(fraction.toDouble() * duration)
                },
                modifier = Modifier.weight(1f).height(20.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                ),
            )

            // Duration
            Text(
                text = formatTime(duration),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
