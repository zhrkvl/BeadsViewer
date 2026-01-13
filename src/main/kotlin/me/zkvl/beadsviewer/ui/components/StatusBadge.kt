package me.zkvl.beadsviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.zkvl.beadsviewer.ui.theme.BeadsTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Status indicator badge with colored dot and text.
 * Displays issue status with semantic coloring from BeadsTheme.
 */
@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val colors = BeadsTheme.colors
    val statusColor = when (status.lowercase()) {
        "open" -> colors.statusOpen
        "in_progress" -> colors.statusInProgress
        "blocked" -> colors.statusBlocked
        "closed" -> colors.statusClosed
        "deferred" -> colors.statusDeferred
        "hooked" -> colors.statusHooked
        else -> colors.statusOpen
    }
    val displayName = status.replace("_", " ").replaceFirstChar { it.uppercase() }

    Row(
        modifier = modifier
            .background(
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(statusColor, CircleShape)
        )

        Text(
            displayName,
            fontSize = 11.sp,
            color = statusColor
        )
    }
}
