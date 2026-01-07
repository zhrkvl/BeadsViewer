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
 * Traditional list view that displays all issues in a scrollable list.
 * This is the default view, refactored from the original IssueListView.
 */
@Composable
fun ListView(project: Project) {
    val issueService = remember { IssueService.getInstance(project) }
    val issuesState by issueService.issuesState.collectAsState()

    val issues = when (val state = issuesState) {
        is IssueService.IssuesState.Loaded -> state.issues.sortedBy { it.priority }
        else -> emptyList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when (val state = issuesState) {
            is IssueService.IssuesState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading issues...")
                }
            }
            is IssueService.IssuesState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message)
                }
            }
            is IssueService.IssuesState.Loaded -> {
                if (issues.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No issues found in .beads/issues.jsonl")
                    }
                } else {
                    // Header
                    Text(
                        "Beads Issues (${issues.size})",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 16.sp
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = issues, key = { it.id }) { issue ->
                            IssueCard(issue = issue)
                        }
                    }
                }
            }
        }
    }
}
