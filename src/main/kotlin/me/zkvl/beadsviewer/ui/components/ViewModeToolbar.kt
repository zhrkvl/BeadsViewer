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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color(0x08FFFFFF))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // View mode selector row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "View:",
                fontSize = 12.sp,
                color = androidx.compose.ui.graphics.Color(0xFF888888)
            )

            // Display view mode buttons
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

        // Current view description
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            currentMode.description,
            fontSize = 11.sp,
            color = androidx.compose.ui.graphics.Color(0xFF666666)
        )
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
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            mode.displayName,
            fontSize = 11.sp,
            color = textColor
        )
    }
}
