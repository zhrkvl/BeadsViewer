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
import me.zkvl.beadsviewer.parser.IssueRepository
import me.zkvl.beadsviewer.ui.components.IssueCard
import org.jetbrains.jewel.ui.component.Text
import java.nio.file.Paths

/**
 * Tree view that displays issues in hierarchical structure.
 * Shows parent-child relationships with indentation.
 */
@Composable
fun TreeView(project: Project) {
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
            Text("Loading tree view...")
        }
        return
    }

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
