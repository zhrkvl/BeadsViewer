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
import me.zkvl.beadsviewer.service.IssueService
import me.zkvl.beadsviewer.ui.components.IssueCard
import org.jetbrains.jewel.ui.component.Text

/**
 * Tree view that displays issues in hierarchical structure.
 * Shows parent-child relationships with indentation.
 */
@Composable
fun TreeView(project: Project) {
    val issueService = remember { IssueService.getInstance(project) }
    val issuesState by issueService.issuesState.collectAsState()

    when (val state = issuesState) {
        is IssueService.IssuesState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading tree view...")
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

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "Issue Tree",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Build tree structure (simplified: just show all issues for now)
                // TODO: Implement proper parent-child tree structure based on dependencies
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = issues, key = { it.id }) { issue ->
                        IssueCard(issue = issue, expandable = true)
                    }
                }
            }
        }
    }
}
