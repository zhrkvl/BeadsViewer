package me.zkvl.beadsviewer.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.model.Status
import me.zkvl.beadsviewer.service.IssueService
import me.zkvl.beadsviewer.ui.components.IssueCard
import org.jetbrains.jewel.ui.component.Text

/**
 * Kanban board view with columns for each status.
 * Issues are displayed as cards in vertical columns representing their status.
 */
@Composable
fun KanbanView(project: Project) {
    val issueService = remember { IssueService.getInstance(project) }
    val issuesState by issueService.issuesState.collectAsState()

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
                        status = status,
                        issues = issuesByStatus[status] ?: emptyList(),
                        modifier = Modifier.width(300.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KanbanColumn(
    status: Status,
    issues: List<Issue>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(
                color = androidx.compose.ui.graphics.Color(0x08FFFFFF),
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
                color = androidx.compose.ui.graphics.Color(0xFF888888)
            )
        }

        // Issues in column
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = issues, key = { it.id }) { issue ->
                IssueCard(
                    issue = issue,
                    expandable = true,
                    initiallyExpanded = false
                )
            }
        }
    }
}
