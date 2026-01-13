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
import me.zkvl.beadsviewer.query.service.QueryFilterService
import me.zkvl.beadsviewer.service.IssueDetailTabService
import me.zkvl.beadsviewer.service.IssueService
import me.zkvl.beadsviewer.ui.components.IssueCard
import org.jetbrains.jewel.ui.component.Text

/**
 * Traditional list view that displays all issues in a scrollable list.
 * This is the default view, refactored from the original IssueListView.
 * Now supports query filtering with complementary filtering pattern.
 */
@Composable
fun ListView(project: Project) {
    val issueService = remember { IssueService.getInstance(project) }
    val queryFilterService = remember { QueryFilterService.getInstance(project) }
    val tabService = remember { IssueDetailTabService.getInstance(project) }

    val issuesState by issueService.issuesState.collectAsState()
    val filteredState by queryFilterService.filteredState.collectAsState()

    // Determine base issues: filtered or all
    val baseIssues = when {
        filteredState is QueryFilterService.FilteredIssuesState.Filtered ->
            (filteredState as QueryFilterService.FilteredIssuesState.Filtered).issues
        filteredState is QueryFilterService.FilteredIssuesState.Error ->
            emptyList()
        issuesState is IssueService.IssuesState.Loaded ->
            (issuesState as IssueService.IssuesState.Loaded).issues
        else -> emptyList()
    }

    // Apply view-specific sorting (ListView sorts by priority)
    val issues = baseIssues.sortedBy { it.priority }

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
                            IssueCard(
                                issue = issue,
                                onOpenDetailTab = { selectedIssue ->
                                    tabService.openIssueDetailTab(selectedIssue)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
