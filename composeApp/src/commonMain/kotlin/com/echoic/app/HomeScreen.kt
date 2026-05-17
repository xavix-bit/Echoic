package com.echoic.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echoic.shared.stats.UsageStatsManager

@Composable
fun HomeScreen(
    onNavigate: (Screen) -> Unit,
    statsManager: UsageStatsManager,
) {
    val strings = LocalStrings.current
    val stats by statsManager.stats.collectAsState()
    val mostUsedCloud = stats.getMostUsedProvider()?.displayName
    val mostUsedLocal = stats.getMostUsedLocalProvider()?.displayName
    val lastUsedProvider = stats.lastUsedProvider
    val lastUsedTime = stats.lastUsedTime

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // ── Hero Card ────────────────────────────────────────
        HeroCard(
            title = strings.welcome,
            subtitle = "Echoic",
            totalUsage = stats.totalUsageCount,
            totalCharacters = stats.totalCharacters,
        )

        // ── Quick Actions ────────────────────────────────────
        SectionTitle(marker = "01", title = strings.quickActions)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = "G",
                label = strings.startGenerating,
                description = strings.generateSpeechDesc,
                onClick = { onNavigate(Screen.GENERATE) },
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = "P",
                label = strings.manageProviders,
                description = strings.cloudServicesDesc,
                onClick = { onNavigate(Screen.PROVIDERS) },
            )
        }

        // ── Stats Overview ───────────────────────────────────
        SectionTitle(marker = "02", title = strings.totalUsage)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = "R",
                label = strings.totalUsage,
                value = stats.totalUsageCount.toString(),
                accent = MaterialTheme.colorScheme.primary,
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = "C",
                label = strings.totalCharacters,
                value = formatNumber(stats.totalCharacters),
                accent = MaterialTheme.colorScheme.tertiary,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = "C",
                label = strings.mostUsedCloud,
                value = mostUsedCloud ?: "-",
                accent = MaterialTheme.colorScheme.primary,
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = "L",
                label = strings.mostUsedLocal,
                value = mostUsedLocal ?: "-",
                accent = MaterialTheme.colorScheme.tertiary,
            )
        }

        // ── Recent Usage ─────────────────────────────────────
        SectionTitle(marker = "03", title = strings.recentUsage)

        if (lastUsedProvider != null && lastUsedTime != null) {
            RecentUsageItem(
                providerName = lastUsedProvider,
                timestamp = lastUsedTime,
            )
        } else {
            EmptyRecentUsage()
        }
    }
}

// ─── Hero Card ────────────────────────────────────────────────────

@Composable
private fun HeroCard(
    title: String,
    subtitle: String,
    totalUsage: Int,
    totalCharacters: Int,
) {
    val strings = LocalStrings.current
    val infiniteTransition = rememberInfiniteTransition(label = "hero")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f + shimmerOffset * 0.04f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.03f + (1f - shimmerOffset) * 0.04f),
                        ),
                    )
                )
                .padding(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.3.sp,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroMetric(value = totalUsage.toString(), label = strings.runs)
                    HeroMetric(value = formatNumber(totalCharacters), label = strings.chars)
                }
            }
        }
    }
}

@Composable
private fun HeroMetric(value: String, label: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.3.sp,
        )
    }
}

// ─── Section Title ────────────────────────────────────────────────

@Composable
private fun SectionTitle(marker: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = marker,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.2.sp,
        )
    }
}

// ─── Quick Action Card ────────────────────────────────────────────

@Composable
private fun AccentBadge(
    label: String,
    color: Color,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    description: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(180),
        label = "scale",
    )

    Card(
        modifier = modifier
            .scale(scale)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHovered) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isHovered) 3.dp else 1.dp,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AccentBadge(label = icon, color = MaterialTheme.colorScheme.primary)
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                lineHeight = 16.sp,
            )
        }
    }
}

// ─── Stat Card ────────────────────────────────────────────────────

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    value: String,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Accent dot + icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AccentBadge(label = icon, color = accent)
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.2.sp,
            )
        }
    }
}

// ─── Recent Usage ─────────────────────────────────────────────────

@Composable
private fun RecentUsageItem(
    providerName: String,
    timestamp: Long,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        providerName.first().toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = providerName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = formatTimestamp(timestamp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val seconds = timestamp / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val totalDays = hours / 24

    var remainingDays = totalDays.toInt()
    var year = 1970
    while (true) {
        val daysInYear = if (isLeapYear(year)) 366 else 365
        if (remainingDays < daysInYear) break
        remainingDays -= daysInYear
        year++
    }

    val monthDays = if (isLeapYear(year))
        intArrayOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    else
        intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    var month = 0
    while (remainingDays >= monthDays[month]) {
        remainingDays -= monthDays[month]
        month++
    }
    val day = remainingDays + 1
    val hour = (hours % 24).toInt()
    val min = (minutes % 60).toInt()

    val m = (month + 1).toString().padStart(2, '0')
    val d = day.toString().padStart(2, '0')
    val h = hour.toString().padStart(2, '0')
    val mi = min.toString().padStart(2, '0')
    return "$year-$m-$d $h:$mi"
}

private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

@Composable
private fun EmptyRecentUsage() {
    val strings = LocalStrings.current

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = strings.noRecentUsage,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatNumber(number: Int): String {
    return when {
        number >= 1_000_000 -> "${number / 1_000_000}.${(number % 1_000_000) / 100_000}M"
        number >= 1_000 -> "${number / 1_000}.${(number % 1_000) / 100}K"
        else -> number.toString()
    }
}
