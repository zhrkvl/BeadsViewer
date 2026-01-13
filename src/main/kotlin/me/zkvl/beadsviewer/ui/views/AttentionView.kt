package me.zkvl.beadsviewer.ui.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.model.Status
import me.zkvl.beadsviewer.service.IssueDetailTabService
import me.zkvl.beadsviewer.service.IssueService
import me.zkvl.beadsviewer.ui.components.IssueCard
import me.zkvl.beadsviewer.ui.theme.BeadsTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Attention view that shows issues needing immediate attention.
 * Filters for blocked issues, high priority items, and issues without assignees.
 */
@Composable
fun AttentionView(project: Project) {
    val colors = BeadsTheme.colors
    val issueService = remember { IssueService.getInstance(project) }
    val tabService = remember { IssueDetailTabService.getInstance(project) }
    val issuesState by issueService.issuesState.collectAsState()

    when (val state = issuesState) {
        is IssueService.IssuesState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading issues...", color = colors.onSurfaceVariant)
            }
            return
        }
        is IssueService.IssuesState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message, color = colors.error)
            }
            return
        }
        is IssueService.IssuesState.Loaded -> {
            val issues = state.issues
            val dirtyIssueIds = state.dirtyIssueIds

            // Filter issues needing attention
            val attentionIssues = issues.filter { issue ->
                issue.status == Status.BLOCKED ||
                (issue.priority <= 1 && issue.assignee == null) ||
                issue.status == Status.OPEN && issue.priority == 0
            }.sortedBy { it.priority }

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "Issues Needing Attention (${attentionIssues.size})",
                    fontSize = 18.sp,
                    color = colors.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (attentionIssues.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No issues need attention - great job!", color = colors.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = attentionIssues, key = { it.id }) { issue ->
                            IssueCard(
                                issue = issue,
                                expandable = true,
                                initiallyExpanded = true,
                                onOpenDetailTab = { selectedIssue ->
                                    tabService.openIssueDetailTab(selectedIssue)
                                },
                                isDirty = dirtyIssueIds.contains(issue.id)
                            )
                        }
                    }
                }
            }
        }
    }
}
