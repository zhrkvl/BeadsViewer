package me.zkvl.beadsviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import me.zkvl.beadsviewer.model.ViewMode
import me.zkvl.beadsviewer.query.service.QueryFilterService
import me.zkvl.beadsviewer.query.state.QueryStateService
import me.zkvl.beadsviewer.state.ViewModeStateService
import org.jetbrains.jewel.ui.component.Text

/**
 * Toolbar for switching between view modes with query input.
 * Displays current view mode, query input, and buttons to switch between all modes.
 */
@Composable
fun ViewModeToolbar(
    project: Project,
    currentMode: ViewMode,
    onModeChange: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val queryFilterService = remember { QueryFilterService.getInstance(project) }
    val queryStateService = remember { QueryStateService.getInstance(project) }

    // Subscribe to query state
    val filteredState by queryFilterService.filteredState.collectAsState()

    // Local state for text field
    var textFieldValue by remember(currentMode) {
        val saved = queryStateService.getQueryForView(currentMode)
        mutableStateOf(saved ?: "")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color(0x08FFFFFF))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // View mode buttons
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

        // Query input section
        QueryInputSection(
            project = project,
            currentMode = currentMode,
            textFieldValue = textFieldValue,
            onTextFieldValueChange = { textFieldValue = it },
            queryFilterService = queryFilterService,
            queryStateService = queryStateService,
            filteredState = filteredState
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

@Composable
private fun QueryInputSection(
    project: Project,
    currentMode: ViewMode,
    textFieldValue: String,
    onTextFieldValueChange: (String) -> Unit,
    queryFilterService: QueryFilterService,
    queryStateService: QueryStateService,
    filteredState: QueryFilterService.FilteredIssuesState
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Query text field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = androidx.compose.ui.graphics.Color(0x10FFFFFF),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = androidx.compose.ui.graphics.Color(0x20FFFFFF),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = onTextFieldValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = androidx.compose.ui.graphics.Color(0xFFCCCCCC),
                        fontSize = 12.sp
                    ),
                    cursorBrush = SolidColor(androidx.compose.ui.graphics.Color(0xFF5C9FE5)),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (textFieldValue.isNotBlank()) {
                                queryFilterService.setQuery(textFieldValue)
                                queryStateService.setQueryForView(currentMode, textFieldValue)
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        if (textFieldValue.isEmpty()) {
                            Text(
                                "Enter query (e.g., status:open AND priority:0-1)",
                                fontSize = 11.sp,
                                color = androidx.compose.ui.graphics.Color(0x88CCCCCC)
                            )
                        }
                        innerTextField()
                    }
                )
            }

            // Apply button
            Box(
                modifier = Modifier
                    .background(
                        color = if (textFieldValue.isNotBlank()) {
                            androidx.compose.ui.graphics.Color(0xFF5C9FE5).copy(alpha = 0.3f)
                        } else {
                            androidx.compose.ui.graphics.Color(0x08FFFFFF)
                        },
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable(enabled = textFieldValue.isNotBlank()) {
                        queryFilterService.setQuery(textFieldValue)
                        queryStateService.setQueryForView(currentMode, textFieldValue)
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "Filter",
                    fontSize = 11.sp,
                    color = if (textFieldValue.isNotBlank()) {
                        androidx.compose.ui.graphics.Color(0xFF5C9FE5)
                    } else {
                        androidx.compose.ui.graphics.Color(0x88CCCCCC)
                    }
                )
            }

            // Clear button
            if (textFieldValue.isNotBlank() ||
                filteredState !is QueryFilterService.FilteredIssuesState.NoFilter) {
                Box(
                    modifier = Modifier
                        .background(
                            color = androidx.compose.ui.graphics.Color(0x08FFFFFF),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable {
                            onTextFieldValueChange("")
                            queryFilterService.clearQuery()
                            queryStateService.clearQueryForView(currentMode)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "Clear",
                        fontSize = 11.sp,
                        color = androidx.compose.ui.graphics.Color(0xFFCCCCCC)
                    )
                }
            }
        }

        // Status/error message row
        when (filteredState) {
            is QueryFilterService.FilteredIssuesState.Filtered -> {
                Text(
                    "Showing ${filteredState.issues.size} of ${filteredState.totalCount} issues",
                    fontSize = 10.sp,
                    color = androidx.compose.ui.graphics.Color(0xFF5C9FE5),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            is QueryFilterService.FilteredIssuesState.Error -> {
                Text(
                    "Error: ${filteredState.message}",
                    fontSize = 10.sp,
                    color = androidx.compose.ui.graphics.Color(0xFFFF5555),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            is QueryFilterService.FilteredIssuesState.Loading -> {
                Text(
                    "Filtering...",
                    fontSize = 10.sp,
                    color = androidx.compose.ui.graphics.Color(0xFFCCCCCC),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            is QueryFilterService.FilteredIssuesState.NoFilter -> {
                // No message when no filter active
            }
        }
    }
}
