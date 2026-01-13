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
import me.zkvl.beadsviewer.service.IssueService
import me.zkvl.beadsviewer.ui.components.IssueCard
import me.zkvl.beadsviewer.ui.theme.BeadsTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Actionable view that groups issues by tracks and labels.
 * Helps identify action items within specific categories.
 */
@Composable
fun ActionableView(project: Project) {
    val issueService = remember { IssueService.getInstance(project) }
    val issuesState by issueService.issuesState.collectAsState()

    when (val state = issuesState) {
        is IssueService.IssuesState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading actionable issues...")
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

            // Group by labels (tracks)
            val issuesByLabel = issues
                .filter { it.labels.isNotEmpty() }
                .flatMap { issue -> issue.labels.map { label -> label to issue } }
                .groupBy({ it.first }, { it.second })

            val unlabeled = issues.filter { it.labels.isEmpty() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Actionable Issues by Track",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Display each track/label group
                issuesByLabel.forEach { (label, issuesInTrack) ->
                    TrackSection(label, issuesInTrack)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Unlabeled issues
                if (unlabeled.isNotEmpty()) {
                    TrackSection("No Label", unlabeled)
                }
            }
        }
    }
}

@Composable
private fun TrackSection(label: String, issues: List<Issue>) {
    val colors = BeadsTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colors.surfaceHover,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${issues.size} issues",
                fontSize = 12.sp,
                color = colors.onSurfaceDisabled
            )
        }

        // List issues in track
        issues.forEach { issue ->
            IssueCard(
                issue = issue,
                expandable = true,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}
