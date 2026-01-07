package me.zkvl.beadsviewer.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
 * Sprint view that displays sprint dashboard with burndown chart.
 * Shows current sprint progress and issue list.
 */
@Composable
fun SprintView(project: Project) {
    val issueService = remember { IssueService.getInstance(project) }
    val issuesState by issueService.issuesState.collectAsState()

    when (val state = issuesState) {
        is IssueService.IssuesState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading sprint view...")
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

            // Filter to active issues (not closed)
            val sprintIssues = issues.filter { it.status != Status.CLOSED }
            val totalIssues = sprintIssues.size
            val completedIssues = issues.count { it.status == Status.CLOSED }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Sprint Dashboard",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Sprint metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(
                        title = "Active Issues",
                        value = totalIssues.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Completed",
                        value = completedIssues.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Remaining",
                        value = (totalIssues).toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Sprint issues
                Text(
                    "Sprint Issues",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.height(600.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = sprintIssues, key = { it.id }) { issue ->
                        IssueCard(issue = issue, expandable = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = androidx.compose.ui.graphics.Color(0x08FFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            title,
            fontSize = 12.sp,
            color = androidx.compose.ui.graphics.Color(0xFF888888)
        )
    }
}
