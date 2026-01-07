package me.zkvl.beadsviewer.ui.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.model.Status
import me.zkvl.beadsviewer.parser.IssueRepository
import me.zkvl.beadsviewer.ui.theme.BeadsColors
import org.jetbrains.jewel.ui.component.Text
import java.nio.file.Paths

/**
 * Flow view that displays a cumulative flow diagram.
 * Shows issue status distribution over time.
 */
@Composable
fun FlowView(project: Project) {
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
            Text("Loading flow diagram...")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Cumulative Flow Diagram",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Calculate status distribution
        val statusCounts = issues.groupingBy { it.status }.eachCount()
        val totalIssues = issues.size

        // Display as simple bar chart
        FlowChart(statusCounts, totalIssues)
    }
}

@Composable
private fun FlowChart(statusCounts: Map<Status, Int>, totalIssues: Int) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(Color(0x08FFFFFF))
    ) {
        val width = size.width
        val height = size.height
        val barHeight = 60f
        var currentY = 50f

        // Draw bars for each status
        Status.entries.filter { it != Status.TOMBSTONE }.forEach { status ->
            val count = statusCounts[status] ?: 0
            val barWidth = if (totalIssues > 0) (count.toFloat() / totalIssues) * (width - 200) else 0f
            val color = BeadsColors.statusColor(status.name)

            // Draw bar
            drawRect(
                color = color,
                topLeft = Offset(100f, currentY),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )

            currentY += barHeight + 20f
        }
    }

    // Legend
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Status.entries.filter { it != Status.TOMBSTONE }.forEach { status ->
            val count = statusCounts[status] ?: 0
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    status.name.replace("_", " "),
                    fontSize = 13.sp
                )
                Text(
                    "$count issues",
                    fontSize = 13.sp,
                    color = androidx.compose.ui.graphics.Color(0xFF888888)
                )
            }
        }
    }
}
