package me.zkvl.beadsviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.zkvl.beadsviewer.ui.theme.BeadsColors
import org.jetbrains.jewel.ui.component.Text

/**
 * Priority badge (P0-P4) with semantic coloring.
 * Displays issue priority with colors from BeadsColors.
 */
@Composable
fun PriorityBadge(priority: Int, modifier: Modifier = Modifier) {
    val priorityColor = BeadsColors.priorityColor(priority)

    Box(
        modifier = modifier
            .background(
                color = priorityColor.copy(alpha = 0.15f),
                shape = CircleShape
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            "P$priority",
            fontSize = 11.sp,
            color = priorityColor
        )
    }
}
