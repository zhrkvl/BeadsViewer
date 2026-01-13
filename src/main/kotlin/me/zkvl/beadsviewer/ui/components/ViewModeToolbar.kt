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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import me.zkvl.beadsviewer.model.ViewMode
import me.zkvl.beadsviewer.query.service.QueryFilterService
import me.zkvl.beadsviewer.query.state.QueryStateService
import me.zkvl.beadsviewer.state.ThemeStateService
import me.zkvl.beadsviewer.state.ViewModeStateService
import me.zkvl.beadsviewer.ui.theme.BeadsTheme
import me.zkvl.beadsviewer.ui.theme.ThemeMode
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

    // Local state for text field (using TextFieldValue for proper cursor tracking)
    var textFieldValue by remember(currentMode) {
        val saved = queryStateService.getQueryForView(currentMode)
        mutableStateOf(TextFieldValue(text = saved ?: "", selection = TextRange.Zero))
    }

    // Auto-apply default query on first load for LIST view
    LaunchedEffect(currentMode) {
        val currentQuery = queryStateService.getQueryForView(currentMode)
        if (currentQuery != null && currentMode == ViewMode.LIST) {
            // Check if this is a default query that hasn't been explicitly saved
            val explicitlySaved = queryStateService.getState().viewQueries.containsKey(currentMode.name)
            if (!explicitlySaved) {
                // Apply the default query automatically
                queryFilterService.setQuery(currentQuery)
            }
        }
    }

    val colors = BeadsTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceHover)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // View mode buttons and theme toggle
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

            // Theme toggle button
            ThemeToggleButton(project = project)
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
    val colors = BeadsTheme.colors

    val backgroundColor = if (isSelected) {
        colors.primary.copy(alpha = 0.3f)
    } else {
        colors.surfaceHover
    }

    val textColor = if (isSelected) {
        colors.primary
    } else {
        colors.onSurface
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
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    queryFilterService: QueryFilterService,
    queryStateService: QueryStateService,
    filteredState: QueryFilterService.FilteredIssuesState
) {
    val colors = BeadsTheme.colors

    // Completion state
    var showCompletion by remember { mutableStateOf(false) }
    var completionSuggestions by remember { mutableStateOf<List<CompletionSuggestion>>(emptyList()) }
    var selectedCompletionIndex by remember { mutableStateOf(0) }

    // Trigger completion when text or cursor position changes
    LaunchedEffect(textFieldValue.text, textFieldValue.selection.start) {
        if (textFieldValue.text.isNotEmpty()) {
            completionSuggestions = CompletionProvider.getCompletions(
                textFieldValue.text,
                textFieldValue.selection.start
            )
            showCompletion = completionSuggestions.isNotEmpty()
            selectedCompletionIndex = 0
        } else {
            showCompletion = false
        }
    }

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
                        color = colors.surfaceSelected,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = colors.border,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue: TextFieldValue ->
                        onTextFieldValueChange(newValue)
                        // Cursor position is now tracked automatically in TextFieldValue.selection
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { keyEvent ->
                            // Handle keyboard events for completion popup
                            if (!showCompletion) return@onPreviewKeyEvent false

                            when (keyEvent.key) {
                                Key.DirectionDown -> {
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        selectedCompletionIndex =
                                            (selectedCompletionIndex + 1).coerceAtMost(completionSuggestions.size - 1)
                                    }
                                    true  // Consume event
                                }
                                Key.DirectionUp -> {
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        selectedCompletionIndex =
                                            (selectedCompletionIndex - 1).coerceAtLeast(0)
                                    }
                                    true
                                }
                                Key.Enter, Key.Tab -> {
                                    if (keyEvent.type == KeyEventType.KeyDown && completionSuggestions.isNotEmpty()) {
                                        // Accept selected suggestion
                                        val suggestion = completionSuggestions[selectedCompletionIndex]

                                        // Same logic as onSelect callback
                                        val cursorPos = textFieldValue.selection.start
                                        val beforeCursor = textFieldValue.text.substring(0, cursorPos)
                                        val afterCursor = textFieldValue.text.substring(cursorPos)

                                        val wordStart = beforeCursor.lastIndexOfAny(charArrayOf(' ', ':', ',', '(', ')')) + 1
                                        val wordEndOffset = afterCursor.indexOfFirst {
                                            it in charArrayOf(' ', ':', ',', '(', ')', '\n')
                                        }.let { if (it == -1) afterCursor.length else it }
                                        val wordEnd = cursorPos + wordEndOffset

                                        val newText = textFieldValue.text.substring(0, wordStart) +
                                                     suggestion.text +
                                                     textFieldValue.text.substring(wordEnd)

                                        val newCursorPos = wordStart + suggestion.text.length

                                        onTextFieldValueChange(
                                            TextFieldValue(
                                                text = newText,
                                                selection = TextRange(newCursorPos)
                                            )
                                        )
                                        showCompletion = false
                                    }
                                    true
                                }
                                Key.Escape -> {
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        showCompletion = false
                                    }
                                    true
                                }
                                else -> false  // Don't consume other keys
                            }
                        },
                    visualTransformation = QuerySyntaxHighlighter(colors),
                    textStyle = TextStyle(
                        color = colors.onSurface,
                        fontSize = 12.sp
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (textFieldValue.text.isNotBlank()) {
                                queryFilterService.setQuery(textFieldValue.text)
                                queryStateService.setQueryForView(currentMode, textFieldValue.text)
                                showCompletion = false
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                "Enter query (e.g., status:open AND priority:0-1)",
                                fontSize = 11.sp,
                                color = colors.onSurfaceDisabled
                            )
                        }
                        innerTextField()
                    }
                )

                // Show completion popup
                if (showCompletion) {
                    CompletionPopup(
                        suggestions = completionSuggestions,
                        selectedIndex = selectedCompletionIndex,
                        onSelect = { suggestion ->
                            // Insert completion at cursor with proper word boundary detection
                            val cursorPos = textFieldValue.selection.start
                            val beforeCursor = textFieldValue.text.substring(0, cursorPos)
                            val afterCursor = textFieldValue.text.substring(cursorPos)

                            // Find start of current word
                            val wordStart = beforeCursor.lastIndexOfAny(charArrayOf(' ', ':', ',', '(', ')')) + 1

                            // Find end of current word in afterCursor
                            val wordEndOffset = afterCursor.indexOfFirst {
                                it in charArrayOf(' ', ':', ',', '(', ')', '\n')
                            }.let { if (it == -1) afterCursor.length else it }
                            val wordEnd = cursorPos + wordEndOffset

                            // Build new text: before + suggestion + after (skipping current word)
                            val newText = textFieldValue.text.substring(0, wordStart) +
                                         suggestion.text +
                                         textFieldValue.text.substring(wordEnd)

                            val newCursorPos = wordStart + suggestion.text.length

                            onTextFieldValueChange(
                                TextFieldValue(
                                    text = newText,
                                    selection = TextRange(newCursorPos)
                                )
                            )
                            showCompletion = false
                        },
                        onDismiss = { showCompletion = false }
                    )
                }
            }

            // Apply button
            Box(
                modifier = Modifier
                    .background(
                        color = if (textFieldValue.text.isNotBlank()) {
                            colors.primary.copy(alpha = 0.3f)
                        } else {
                            colors.surfaceHover
                        },
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable(enabled = textFieldValue.text.isNotBlank()) {
                        queryFilterService.setQuery(textFieldValue.text)
                        queryStateService.setQueryForView(currentMode, textFieldValue.text)
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "Filter",
                    fontSize = 11.sp,
                    color = if (textFieldValue.text.isNotBlank()) {
                        colors.primary
                    } else {
                        colors.onSurfaceDisabled
                    }
                )
            }

            // Clear button
            if (textFieldValue.text.isNotBlank() ||
                filteredState !is QueryFilterService.FilteredIssuesState.NoFilter) {
                Box(
                    modifier = Modifier
                        .background(
                            color = colors.surfaceHover,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable {
                            onTextFieldValueChange(TextFieldValue(text = "", selection = TextRange.Zero))
                            queryFilterService.clearQuery()
                            queryStateService.clearQueryForView(currentMode)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "Clear",
                        fontSize = 11.sp,
                        color = colors.onSurface
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
                    color = colors.info,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            is QueryFilterService.FilteredIssuesState.Error -> {
                Text(
                    "Error: ${filteredState.message}",
                    fontSize = 10.sp,
                    color = colors.error,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            is QueryFilterService.FilteredIssuesState.Loading -> {
                Text(
                    "Filtering...",
                    fontSize = 10.sp,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            is QueryFilterService.FilteredIssuesState.NoFilter -> {
                // No message when no filter active
            }
        }
    }
}

/**
 * Theme toggle button that cycles through LIGHT -> DARK -> SYSTEM modes.
 */
@Composable
private fun ThemeToggleButton(project: Project) {
    val colors = BeadsTheme.colors
    val themeService = remember { ThemeStateService.getInstance(project) }
    val currentThemeMode by themeService.themeModeFlow.collectAsState()

    val (icon, label) = when (currentThemeMode) {
        ThemeMode.LIGHT -> "☀" to "Light"
        ThemeMode.DARK -> "●" to "Dark"
        ThemeMode.SYSTEM -> "◐" to "Auto"
    }

    Box(
        modifier = Modifier
            .widthIn(min = 60.dp)
            .background(
                color = colors.surfaceHover,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable {
                // Cycle through theme modes
                val nextMode = when (currentThemeMode) {
                    ThemeMode.LIGHT -> ThemeMode.DARK
                    ThemeMode.DARK -> ThemeMode.SYSTEM
                    ThemeMode.SYSTEM -> ThemeMode.LIGHT
                }
                themeService.setCurrentThemeMode(nextMode)
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                icon,
                fontSize = 11.sp,
                color = colors.onSurface
            )
            Text(
                label,
                fontSize = 11.sp,
                color = colors.onSurface
            )
        }
    }
}
