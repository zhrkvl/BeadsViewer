package me.zkvl.beadsviewer.ui.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.service.IssueDetailTabService
import me.zkvl.beadsviewer.service.IssueService
import me.zkvl.beadsviewer.ui.components.IssueCard
import me.zkvl.beadsviewer.ui.theme.BeadsTheme
import org.jetbrains.jewel.ui.component.Text
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * History view that displays issues in timeline format.
 * Shows issues sorted by creation or update date.
 */
@Composable
fun HistoryView(project: Project) {
    val issueService = remember { IssueService.getInstance(project) }
    val tabService = remember { IssueDetailTabService.getInstance(project) }
    val issuesState by issueService.issuesState.collectAsState()

    when (val state = issuesState) {
        is IssueService.IssuesState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading history...")
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
            val colors = BeadsTheme.colors

            // Sort by created date (most recent first)
            val sortedIssues = issues.sortedByDescending { it.createdAt }

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "Issue History",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items = sortedIssues, key = { it.id }) { issue ->
                        Column {
                            val localDateTime = issue.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
                            val dateString = "${localDateTime.month.name.take(3)} ${localDateTime.dayOfMonth}, ${localDateTime.year}"

                            Text(
                                dateString,
                                fontSize = 11.sp,
                                color = colors.onSurfaceDisabled,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            IssueCard(
                                project = project,
                                issue = issue,
                                expandable = true,
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
