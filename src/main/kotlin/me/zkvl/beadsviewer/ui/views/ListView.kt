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
 * Traditional list view that displays all issues in a scrollable list.
 * This is the default view, refactored from the original IssueListView.
 */
@Composable
fun ListView(project: Project) {
    var issues by remember { mutableStateOf<List<Issue>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load issues on composition
    LaunchedEffect(project) {
        val beadsFile = Paths.get(project.basePath ?: return@LaunchedEffect, ".beads", "issues.jsonl")
        val repository = IssueRepository()

        repository.loadIssues(beadsFile)
            .onSuccess { loadedIssues ->
                issues = loadedIssues.sortedBy { it.priority }
                isLoading = false
            }
            .onFailure { error ->
                errorMessage = "Failed to load issues: ${error.message}"
                isLoading = false
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Text(
            "Beads Issues (${issues.size})",
            modifier = Modifier.padding(16.dp),
            fontSize = 16.sp
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading issues...")
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(errorMessage ?: "Unknown error")
                }
            }
            issues.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No issues found in .beads/issues.jsonl")
                }
            }
            else -> {
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
