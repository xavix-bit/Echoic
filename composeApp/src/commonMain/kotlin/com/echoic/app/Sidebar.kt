package com.echoic.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import echoic.composeapp.generated.resources.Res
import echoic.composeapp.generated.resources.icon
import org.jetbrains.compose.resources.painterResource

data class SidebarItem(
    val id: Screen,
    val icon: String,
    val label: String,
)

@Composable
fun Sidebar(
    currentScreen: Screen,
    expanded: Boolean,
    onNavigate: (Screen) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val strings = LocalStrings.current

    val items = listOf(
        SidebarItem(Screen.HOME, "H", strings.home),
        SidebarItem(Screen.GENERATE, "G", strings.generateSpeechTitle),
        SidebarItem(Screen.PROVIDERS, "P", strings.providers),
    )

    // Smooth spring-animated width transition
    val animatedWidth by animateDpAsState(
        targetValue = if (expanded) 180.dp else 64.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "sidebarWidth",
    )

    Column(
        modifier = Modifier
            .width(animatedWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))

        // Brand logo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.spacedBy(8.dp) else Arrangement.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.icon),
                contentDescription = "Echoic Logo",
                modifier = Modifier.size(32.dp),
                contentScale = ContentScale.Fit,
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandHorizontally(expandFrom = Alignment.Start, animationSpec = tween(180)),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start, animationSpec = tween(120)),
            ) {
                Text(
                    text = "Echoic",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // Navigation items
        items.forEach { item ->
            SidebarNavItem(
                item = item,
                isActive = currentScreen == item.id,
                expanded = expanded,
                onClick = { onNavigate(item.id) },
            )
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.weight(1f))

        // Divider before settings
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 14.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
        )

        Spacer(Modifier.height(8.dp))

        // Settings — bottom
        SidebarNavItem(
            item = SidebarItem(Screen.HOME, "S", strings.settings),
            isActive = false,
            expanded = expanded,
            onClick = onOpenSettings,
        )

        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun SidebarNavItem(
    item: SidebarItem,
    isActive: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val itemShape = RoundedCornerShape(12.dp)
    val containerColor = when {
        isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        else -> Color.Transparent
    }
    val contentColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        isHovered -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .fillMaxWidth()
            .clip(itemShape)
            .background(containerColor)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 3.dp)
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.spacedBy(12.dp) else Arrangement.Center,
        ) {
            Text(
                text = item.icon,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) MaterialTheme.colorScheme.onPrimary else contentColor,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                    )
                    .wrapContentSize(Alignment.Center),
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandHorizontally(expandFrom = Alignment.Start, animationSpec = tween(180)),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start, animationSpec = tween(120)),
            ) {
                Text(
                    text = item.label,
                    fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                )
            }
        }
    }
}
