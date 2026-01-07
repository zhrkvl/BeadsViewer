package me.zkvl.beadsviewer.ui.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.parser.IssueRepository
import me.zkvl.beadsviewer.ui.components.IssueCard
import org.jetbrains.jewel.ui.component.Text
import java.nio.file.Paths
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * History view that displays issues in timeline format.
 * Shows issues sorted by creation or update date.
 */
@Composable
fun HistoryView(project: Project) {
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
            Text("Loading history...")
        }
        return
    }

    // Sort by created date (most recent first)
    val sortedIssues = issues.sortedByDescending { it.createdAt }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Issue History",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items = sortedIssues, key = { it.id }) { issue ->
                Column {
                    val localDateTime = issue.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
                    val dateString = "${localDateTime.month.name.take(3)} ${localDateTime.dayOfMonth}, ${localDateTime.year}"

                    Text(
                        dateString,
                        fontSize = 11.sp,
                        color = androidx.compose.ui.graphics.Color(0xFF888888),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    IssueCard(issue = issue, expandable = true)
                }
            }
        }
    }
}
