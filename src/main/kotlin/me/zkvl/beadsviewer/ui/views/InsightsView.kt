package me.zkvl.beadsviewer.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import me.zkvl.beadsviewer.ui.theme.BeadsTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Insights dashboard with metrics and analytics.
 * Displays key statistics, trends, and health indicators.
 */
@Composable
fun InsightsView(project: Project) {
    val issueService = remember { IssueService.getInstance(project) }
    val issuesState by issueService.issuesState.collectAsState()

    when (val state = issuesState) {
        is IssueService.IssuesState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading insights...")
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Project Insights",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Calculate metrics
                val metrics = calculateMetrics(issues)

                // Display metric cards in grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(
                        title = "Total Issues",
                        value = metrics.totalIssues.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Open",
                        value = metrics.openIssues.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "In Progress",
                        value = metrics.inProgressIssues.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(
                        title = "Closed",
                        value = metrics.closedIssues.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Blocked",
                        value = metrics.blockedIssues.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Completion Rate",
                        value = "${metrics.completionRate}%",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Priority breakdown
                Text(
                    "Priority Breakdown",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                metrics.byPriority.toSortedMap().forEach { (priority, count) ->
                    PriorityRow(priority, count)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Type breakdown
                Text(
                    "Issue Type Breakdown",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                metrics.byType.forEach { (type, count) ->
                    TypeRow(type, count)
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
    val colors = BeadsTheme.colors
    Column(
        modifier = modifier
            .background(
                color = colors.surfaceHover,
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
            color = colors.onSurfaceDisabled
        )
    }
}

@Composable
private fun PriorityRow(priority: Int, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("P$priority", fontSize = 13.sp)
        Text("$count", fontSize = 13.sp)
    }
}

@Composable
private fun TypeRow(type: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(type, fontSize = 13.sp)
        Text("$count", fontSize = 13.sp)
    }
}

private data class Metrics(
    val totalIssues: Int,
    val openIssues: Int,
    val inProgressIssues: Int,
    val closedIssues: Int,
    val blockedIssues: Int,
    val completionRate: Int,
    val byPriority: Map<Int, Int>,
    val byType: Map<String, Int>
)

private fun calculateMetrics(issues: List<Issue>): Metrics {
    val total = issues.size
    val open = issues.count { it.status == Status.OPEN }
    val inProgress = issues.count { it.status == Status.IN_PROGRESS }
    val closed = issues.count { it.status == Status.CLOSED }
    val blocked = issues.count { it.status == Status.BLOCKED }
    val completionRate = if (total > 0) (closed * 100) / total else 0

    return Metrics(
        totalIssues = total,
        openIssues = open,
        inProgressIssues = inProgress,
        closedIssues = closed,
        blockedIssues = blocked,
        completionRate = completionRate,
        byPriority = issues.groupingBy { it.priority }.eachCount(),
        byType = issues.groupingBy { it.issueType.value }.eachCount()
    )
}
