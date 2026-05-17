package com.echoic.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.echoic.shared.engine.TTSEngineFactory
import com.echoic.shared.stats.StatsStorage
import com.echoic.shared.stats.UsageStatsManager

enum class Screen { HOME, GENERATE, PROVIDERS }

@Composable
fun App(config: AppConfig) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var configData by remember { mutableStateOf(config.data) }
    var showSettings by remember { mutableStateOf(false) }
    var sidebarExpanded by remember { mutableStateOf(false) }

    val engine = remember { TTSEngineFactory.createCloudEngine(config) }
    val localEngine = remember { TTSEngineFactory.createLocalEngine() }
    val audioPlayer = remember { AudioPlayer() }
    val statsManager = remember { UsageStatsManager(StatsStorage()) }

    LaunchedEffect(configData) {
        // Engine reads config dynamically, no need to recreate
    }

    EchoicTheme(darkTheme = configData.appearance == "dark") {
        CompositionLocalProvider(LocalStrings provides stringsFor(configData.language)) {
            Surface(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 2.dp,
                shadowElevation = 16.dp,
            ) {
                Box(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxSize()) {
                        Sidebar(
                            currentScreen = currentScreen,
                            expanded = sidebarExpanded,
                            onNavigate = { currentScreen = it },
                            onOpenSettings = { showSettings = true },
                        )

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            // Top bar with window controls + sidebar toggle
                            Column(Modifier.fillMaxSize()) {
                                val windowController = LocalWindow.current
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // Window control buttons (macOS traffic lights)
                                    WindowControlButton(
                                        color = Color(0xFFFF5F57),
                                        hoverColor = Color(0xFFFF3B30),
                                        onClick = { windowController.close() },
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    WindowControlButton(
                                        color = Color(0xFFFFBD2E),
                                        hoverColor = Color(0xFFFFAB00),
                                        onClick = { windowController.minimize() },
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    WindowControlButton(
                                        color = Color(0xFF28C840),
                                        hoverColor = Color(0xFF1DB954),
                                        onClick = { windowController.maximize() },
                                    )

                                    Spacer(Modifier.width(16.dp))

                                    TopBarToggle(
                                        expanded = sidebarExpanded,
                                        onClick = { sidebarExpanded = !sidebarExpanded },
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.26f))

                                // Screen content with crossfade transition
                                Box(modifier = Modifier.weight(1f)) {
                                    AnimatedContent(
                                        targetState = currentScreen,
                                        transitionSpec = {
                                            (fadeIn(tween(220, delayMillis = 60)) +
                                                scaleIn(tween(220, delayMillis = 60), initialScale = 0.97f))
                                                .togetherWith(fadeOut(tween(160)))
                                        },
                                        label = "screenTransition",
                                    ) { screen ->
                                        when (screen) {
                                            Screen.HOME -> HomeScreen(
                                                onNavigate = { currentScreen = it },
                                                statsManager = statsManager,
                                            )
                                            Screen.GENERATE -> GenerateScreen(
                                                config = config,
                                                configData = configData,
                                                engine = engine,
                                                localEngine = localEngine,
                                                audioPlayer = audioPlayer,
                                                onOpenSettings = { showSettings = true },
                                                onNavigate = { currentScreen = it },
                                            )
                                            Screen.PROVIDERS -> ProvidersScreen(
                                                config = config,
                                                configData = configData,
                                                onUpdate = { configData = config.data },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating settings overlay
                    SettingsOverlay(
                        visible = showSettings,
                        config = config,
                        configData = configData,
                        onUpdate = { configData = config.data },
                        onDismiss = { showSettings = false },
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.release()
        }
    }
}

@Composable
private fun WindowControlButton(
    color: Color,
    hoverColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(if (isHovered) hoverColor else color.copy(alpha = 0.8f))
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    )
}

@Composable
private fun TopBarToggle(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val strings = LocalStrings.current
    Text(
        text = if (expanded) strings.collapseSidebar else strings.expandSidebar,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = if (isHovered) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isHovered) MaterialTheme.colorScheme.surfaceVariant
                else Color.Transparent
            )
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}
