package me.zkvl.beadsviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.model.IssueType
import me.zkvl.beadsviewer.parser.IssueRepository
import me.zkvl.beadsviewer.ui.theme.BeadsColors
import org.jetbrains.jewel.ui.component.Text
import java.nio.file.Paths

/**
 * Main issue list view that displays all issues from .beads/issues.jsonl.
 */
@Composable
fun IssueListView(project: Project) {
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
                        IssueCard(issue)
                    }
                }
            }
        }
    }
}

/**
 * Individual issue card component.
 */
@Composable
private fun IssueCard(issue: Issue) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .background(
                color = androidx.compose.ui.graphics.Color(0x08FFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        // Title row with ID, priority, and status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority badge
            PriorityBadge(issue.priority)

            // Issue ID
            Text(
                issue.id,
                fontSize = 12.sp,
                color = androidx.compose.ui.graphics.Color(0xFF888888)
            )

            // Title
            Text(
                issue.title,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis
            )

            // Status indicator
            StatusBadge(issue.status.name)
        }

        // Expanded details
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))

            // Type and creator
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Type: ${issue.issueType.value}",
                    fontSize = 11.sp,
                    color = androidx.compose.ui.graphics.Color(0xFF888888)
                )
                Text(
                    "By: ${issue.createdBy}",
                    fontSize = 11.sp,
                    color = androidx.compose.ui.graphics.Color(0xFF888888)
                )
            }

            // Description
            if (issue.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    issue.description,
                    fontSize = 12.sp,
                    color = androidx.compose.ui.graphics.Color(0xFFCCCCCC),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Dependencies
            if (issue.dependencies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Dependencies: ${issue.dependencies.size}",
                    fontSize = 11.sp,
                    color = androidx.compose.ui.graphics.Color(0xFF888888)
                )
            }
        }
    }
}

/**
 * Status indicator badge with color dot.
 */
@Composable
private fun StatusBadge(status: String) {
    val statusColor = BeadsColors.statusColor(status)
    val displayName = status.replace("_", " ").replaceFirstChar { it.uppercase() }

    Row(
        modifier = Modifier
            .background(
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(statusColor, CircleShape)
        )

        Text(
            displayName,
            fontSize = 11.sp,
            color = statusColor
        )
    }
}

/**
 * Priority badge (P0-P4).
 */
@Composable
private fun PriorityBadge(priority: Int) {
    val priorityColor = BeadsColors.priorityColor(priority)

    Box(
        modifier = Modifier
            .background(
                color = priorityColor.copy(alpha = 0.15f),
                shape = CircleShape
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            "P$priority",
            fontSize = 11.sp,
            color = priorityColor
        )
    }
}
