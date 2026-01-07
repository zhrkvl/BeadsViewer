package me.zkvl.beadsviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import me.zkvl.beadsviewer.model.ViewMode
import me.zkvl.beadsviewer.state.ViewModeStateService
import org.jetbrains.jewel.ui.component.Text

/**
 * Toolbar for switching between view modes.
 * Displays current view mode and provides buttons to switch between all modes.
 */
@Composable
fun ViewModeToolbar(
    project: Project,
    currentMode: ViewMode,
    onModeChange: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    // Use FlowRow for responsive wrapping when toolbar is narrow
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color(0x08FFFFFF))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Display view mode buttons (removed "View:" label and description)
        ViewMode.entries.forEach { mode ->
            ViewModeButton(
                mode = mode,
                isSelected = mode == currentMode,
                onClick = {
                    onModeChange(mode)
                    ViewModeStateService.getInstance(project).setCurrentViewMode(mode)
                }
            )
        }
    }
}

@Composable
private fun ViewModeButton(
    mode: ViewMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        androidx.compose.ui.graphics.Color(0xFF5C9FE5).copy(alpha = 0.3f)
    } else {
        androidx.compose.ui.graphics.Color(0x08FFFFFF)
    }

    val textColor = if (isSelected) {
        androidx.compose.ui.graphics.Color(0xFF5C9FE5)
    } else {
        androidx.compose.ui.graphics.Color(0xFFCCCCCC)
    }

    Box(
        modifier = Modifier
            .widthIn(min = 60.dp) // Fixed minimum width for consistent button size
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            mode.displayName,
            fontSize = 11.sp,
            color = textColor
        )
    }
}
