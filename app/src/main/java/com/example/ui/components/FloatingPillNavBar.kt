package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class BottomNavDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    POS("POS", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale, "nav_pos"),
    INVENTORY("Inventory", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "nav_inventory"),
    MORE("More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz, "nav_more")
}

@Composable
fun FloatingPillNavBar(
    currentDestination: BottomNavDestination,
    onNavigate: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    // Floating Pill navigation bar container adhering strictly to Section 7
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.6f),
                    spotColor = CyanPrimary.copy(alpha = 0.25f)
                ),
            shape = CircleShape,
            color = Slate900.copy(alpha = 0.96f),
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavDestination.values().forEach { destination ->
                    val isSelected = currentDestination == destination

                    val animatedBgColor by animateColorAsState(
                        targetValue = if (isSelected) CyanPrimary.copy(alpha = 0.18f) else Color.Transparent,
                        animationSpec = tween(durationMillis = 200),
                        label = "pillBgColor"
                    )
                    val animatedContentColor by animateColorAsState(
                        targetValue = if (isSelected) CyanPrimary else Slate400,
                        animationSpec = tween(durationMillis = 200),
                        label = "pillContentColor"
                    )

                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .testTag(destination.testTag)
                            .clip(CircleShape)
                            .background(animatedBgColor)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(bounded = true, color = CyanPrimary),
                                role = Role.Tab,
                                onClick = { onNavigate(destination) }
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.title,
                                tint = animatedContentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = destination.title,
                                    color = animatedContentColor,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
