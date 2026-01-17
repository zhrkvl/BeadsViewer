package me.zkvl.beadsviewer.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.model.Status
import me.zkvl.beadsviewer.service.BeadsCommandService
import me.zkvl.beadsviewer.service.IssueDetailTabService
import me.zkvl.beadsviewer.service.IssueService
import me.zkvl.beadsviewer.ui.components.IssueCard
import me.zkvl.beadsviewer.ui.theme.BeadsTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Kanban board view with columns for each status.
 * Issues are displayed as cards in vertical columns representing their status.
 */
@Composable
fun KanbanView(project: Project) {
    val issueService = remember { IssueService.getInstance(project) }
    val commandService = remember { BeadsCommandService.getInstance(project) }
    val tabService = remember { IssueDetailTabService.getInstance(project) }
    val issuesState by issueService.issuesState.collectAsState()
    val scope = rememberCoroutineScope()

    // Drag-and-drop state
    var draggedIssue by remember { mutableStateOf<Issue?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var hoveredColumn by remember { mutableStateOf<Status?>(null) }

    when (val state = issuesState) {
        is IssueService.IssuesState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading kanban board...")
            }
            return
        }
        is IssueService.IssuesState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message)
            }
            return
        }
        is IssueService.IssuesState.Loaded -> {
            val issues = state.issues
            val dirtyIssueIds = state.dirtyIssueIds

            // Group issues by status
            val issuesByStatus = issues
                .filter { it.status != Status.TOMBSTONE } // Exclude tombstones
                .groupBy { it.status }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Display columns for relevant statuses
                listOf(
                    Status.OPEN,
                    Status.IN_PROGRESS,
                    Status.BLOCKED,
                    Status.HOOKED,
                    Status.CLOSED
                ).forEach { status ->
                    KanbanColumn(
                        project = project,
                        status = status,
                        issues = issuesByStatus[status] ?: emptyList(),
                        dirtyIssueIds = dirtyIssueIds,
                        draggedIssue = draggedIssue,
                        hoveredColumn = hoveredColumn,
                        onDragStart = { issue ->
                            draggedIssue = issue
                        },
                        onDragEnd = { },
                        onHoverChange = { isHovering ->
                            hoveredColumn = if (isHovering) status else null
                        },
                        onDrop = { issue, targetStatus ->
                            draggedIssue = null
                            hoveredColumn = null

                            // Only update if status changed
                            if (issue.status != targetStatus) {
                                scope.launch {
                                    commandService.updateIssueStatus(issue.id, targetStatus)
                                        .onSuccess {
                                            // Reload issues to reflect the change
                                            issueService.loadIssues(debounceMs = 100)

                                            // Show success notification
                                            NotificationGroupManager.getInstance()
                                                .getNotificationGroup("Beads Notifications")
                                                .createNotification(
                                                    "Issue ${issue.id} moved to ${targetStatus.name}",
                                                    NotificationType.INFORMATION
                                                )
                                                .notify(project)
                                        }
                                        .onFailure { error ->
                                            // Show error notification
                                            NotificationGroupManager.getInstance()
                                                .getNotificationGroup("Beads Notifications")
                                                .createNotification(
                                                    "Failed to update issue: ${error.message}",
                                                    NotificationType.ERROR
                                                )
                                                .notify(project)
                                        }
                                }
                            }
                        },
                        modifier = Modifier.width(300.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KanbanColumn(
    project: Project,
    status: Status,
    issues: List<Issue>,
    dirtyIssueIds: Set<String>,
    draggedIssue: Issue?,
    hoveredColumn: Status?,
    onDragStart: (Issue) -> Unit,
    onDragEnd: () -> Unit,
    onHoverChange: (Boolean) -> Unit,
    onDrop: (Issue, Status) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabService = remember { IssueDetailTabService.getInstance(project) }
    val colors = BeadsTheme.colors

    // Track column bounds for drop detection
    var columnBounds by remember { mutableStateOf<Pair<Offset, IntSize>?>(null) }
    var isDragOver by remember { mutableStateOf(false) }

    val isHighlighted = hoveredColumn == status && draggedIssue != null && draggedIssue.status != status

    Column(
        modifier = modifier
            .fillMaxHeight()
            .onGloballyPositioned { coordinates ->
                columnBounds = Pair(coordinates.positionInWindow(), coordinates.size)
            }
            .background(
                color = if (isHighlighted) colors.primary.copy(alpha = 0.1f) else colors.surfaceHover,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isHighlighted) 2.dp else 0.dp,
                color = if (isHighlighted) colors.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        // Column header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                status.name.replace("_", " "),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${issues.size}",
                fontSize = 12.sp,
                color = colors.onSurfaceDisabled
            )
        }

        // Issues in column
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = issues, key = { it.id }) { issue ->
                var dragOffset by remember { mutableStateOf(Offset.Zero) }

                // Wrap IssueCard in Box to apply drag gesture without conflicts
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(issue.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    onDragStart(issue)
                                },
                                onDrag = { change, offset ->
                                    change.consume()
                                    dragOffset += offset

                                    // Check if dragging over this column
                                    columnBounds?.let { (position, size) ->
                                        val dragX = change.position.x + dragOffset.x
                                        val dragY = change.position.y + dragOffset.y

                                        val isOver = dragX >= position.x &&
                                                dragX <= position.x + size.width &&
                                                dragY >= position.y &&
                                                dragY <= position.y + size.height

                                        onHoverChange(isOver)
                                    }
                                },
                                onDragEnd = {
                                    dragOffset = Offset.Zero
                                    onDragEnd()

                                    // Drop on hovered column
                                    if (hoveredColumn != null && hoveredColumn != issue.status) {
                                        onDrop(issue, hoveredColumn)
                                    }
                                },
                                onDragCancel = {
                                    dragOffset = Offset.Zero
                                    onDragEnd()
                                }
                            )
                        }
                ) {
                    IssueCard(
                        project = project,
                        issue = issue,
                        expandable = false,
                        initiallyExpanded = false,
                        onOpenDetailTab = { selectedIssue ->
                            tabService.openIssueDetailTab(selectedIssue)
                        },
                        isDirty = dirtyIssueIds.contains(issue.id),
                        enableGestures = false // Disable gestures to allow outer drag detection
                    )
                }
            }
        }
    }
}
