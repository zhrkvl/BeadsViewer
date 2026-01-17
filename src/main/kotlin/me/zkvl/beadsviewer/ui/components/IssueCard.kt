@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package me.zkvl.beadsviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.model.Status
import me.zkvl.beadsviewer.service.BeadsCommandService
import me.zkvl.beadsviewer.service.IssueService
import me.zkvl.beadsviewer.ui.theme.BeadsTheme
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.Text

/**
 * Reusable issue card component.
 * Displays an issue with its ID, title, status, priority, and expandable details.
 *
 * @param issue The issue to display
 * @param project The current project (for services)
 * @param modifier Modifier for the card container
 * @param expandable Whether the card can be clicked to expand/collapse
 * @param initiallyExpanded Initial expansion state
 * @param isDirty Whether this issue has unsaved changes (not yet synced to git)
 */
@Composable
fun IssueCard(
    issue: Issue,
    project: Project? = null,
    modifier: Modifier = Modifier,
    expandable: Boolean = true,
    initiallyExpanded: Boolean = false,
    onOpenDetailTab: ((Issue) -> Unit)? = null,
    isDirty: Boolean = false,
    enableGestures: Boolean = true
) {
    val colors = BeadsTheme.colors
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    // Context menu state
    var showContextMenu by remember { mutableStateOf(false) }
    var showStatusSubmenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (enableGestures && expandable) {
                        Modifier.pointerInput(Unit) {
                            // Handle left-click for expand/collapse
                            detectTapGestures(onTap = { expanded = !expanded })
                        }
                    } else Modifier
                )
                .then(
                    if (enableGestures && project != null) {
                        Modifier.pointerInput(project) {
                            // Handle right-click for context menu
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.button == PointerButton.Secondary) {
                                        showContextMenu = true
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                    } else Modifier
                )
                .background(
                    color = colors.surfaceHover,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
        SelectionContainer {
            Column {
                // Title row with ID, priority, and status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority badge
            PriorityBadge(issue.priority)

            // Issue ID (clickable if callback provided)
            if (onOpenDetailTab != null) {
                Link(
                    text = issue.id,
                    onClick = { onOpenDetailTab(issue) }
                )
            } else {
                Text(
                    issue.id,
                    fontSize = 12.sp,
                    color = colors.onSurfaceDisabled
                )
            }

            // Title
            Text(
                issue.title,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                color = colors.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis
            )

            // Status indicator
            StatusBadge(issue.status.name)

            // Dirty sync indicator (if issue not synced to git)
            if (isDirty) {
                DirtySyncBadge()
            }
        }

        // Expanded details
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))

            // Type and creator
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Type: ${issue.issueType.value}",
                    fontSize = 11.sp,
                    color = colors.onSurfaceDisabled
                )
                Text(
                    "By: ${issue.createdBy}",
                    fontSize = 11.sp,
                    color = colors.onSurfaceDisabled
                )
            }

            // Description
            if (issue.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    issue.description,
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Dependencies
            if (issue.dependencies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Dependencies: ${issue.dependencies.size}",
                    fontSize = 11.sp,
                    color = colors.onSurfaceDisabled
                )
            }
        }
            }
        }
        }

        // Context menu
        if (project != null && showContextMenu) {
            Popup(
                onDismissRequest = {
                    showContextMenu = false
                    showStatusSubmenu = false
                },
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    modifier = Modifier
                        .width(180.dp)
                        .background(
                            color = colors.surface,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = colors.border,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(vertical = 4.dp)
                ) {
                    // Open action
                    if (onOpenDetailTab != null) {
                        ContextMenuItem(
                            text = "Open",
                            onClick = {
                                onOpenDetailTab(issue)
                                showContextMenu = false
                            }
                        )
                    }

                    // Status submenu trigger
                    ContextMenuItem(
                        text = "Status",
                        trailingIcon = if (showStatusSubmenu) "▼" else "▶",
                        onClick = { showStatusSubmenu = !showStatusSubmenu }
                    )

                    // Status submenu items
                    if (showStatusSubmenu) {
                        val statusOptions = listOf(
                            Status.OPEN,
                            Status.IN_PROGRESS,
                            Status.BLOCKED,
                            Status.HOOKED,
                            Status.CLOSED
                        )

                        statusOptions.forEach { status ->
                            ContextMenuItem(
                                text = "  ${status.name.replace("_", " ")}",
                                trailingIcon = if (status == issue.status) "✓" else null,
                                onClick = {
                                    scope.launch {
                                        val commandService = BeadsCommandService.getInstance(project)
                                        val issueService = IssueService.getInstance(project)

                                        commandService.updateIssueStatus(issue.id, status)
                                            .onSuccess {
                                                // Reload issues
                                                issueService.loadIssues(debounceMs = 100)

                                                // Show success notification
                                                NotificationGroupManager.getInstance()
                                                    .getNotificationGroup("Beads Notifications")
                                                    .createNotification(
                                                        "Status updated to ${status.name}",
                                                        NotificationType.INFORMATION
                                                    )
                                                    .notify(project)
                                            }
                                            .onFailure { error ->
                                                // Show error notification
                                                NotificationGroupManager.getInstance()
                                                    .getNotificationGroup("Beads Notifications")
                                                    .createNotification(
                                                        "Failed to update status: ${error.message}",
                                                        NotificationType.ERROR
                                                    )
                                                    .notify(project)
                                            }
                                    }
                                    showContextMenu = false
                                    showStatusSubmenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Context menu item component.
 */
@Composable
private fun ContextMenuItem(
    text: String,
    trailingIcon: String? = null,
    onClick: () -> Unit
) {
    val colors = BeadsTheme.colors
    var isHovered by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isHovered) colors.surfaceHover else colors.surface
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        isHovered = event.type.toString().contains("Enter")
                    }
                }
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = colors.onSurface
        )
        if (trailingIcon != null) {
            Text(
                text = trailingIcon,
                fontSize = 10.sp,
                color = colors.onSurfaceDisabled
            )
        }
    }
}
