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
import me.zkvl.beadsviewer.parser.IssueRepository
import me.zkvl.beadsviewer.ui.components.IssueCard
import org.jetbrains.jewel.ui.component.Text
import java.nio.file.Paths

/**
 * Attention view that shows issues needing immediate attention.
 * Filters for blocked issues, high priority items, and issues without assignees.
 */
@Composable
fun AttentionView(project: Project) {
    var issues by remember { mutableStateOf<List<Issue>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(project) {
        val beadsFile = Paths.get(project.basePath ?: return@LaunchedEffect, ".beads", "issues.jsonl")
        IssueRepository().loadIssues(beadsFile)
            .onSuccess { issues = it; isLoading = false }
            .onFailure { isLoading = false }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading issues...")
        }
        return
    }

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
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (attentionIssues.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No issues need attention - great job!")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = attentionIssues, key = { it.id }) { issue ->
                    IssueCard(issue = issue, expandable = true, initiallyExpanded = true)
                }
            }
        }
    }
}
