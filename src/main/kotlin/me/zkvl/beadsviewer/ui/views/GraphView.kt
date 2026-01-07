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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.parser.IssueRepository
import org.jetbrains.jewel.ui.component.Text
import java.nio.file.Paths
import kotlin.math.cos
import kotlin.math.sin

/**
 * Graph view that visualizes issue dependencies.
 * Displays issues as nodes with edges representing dependencies.
 */
@Composable
fun GraphView(project: Project) {
    var issues by remember { mutableStateOf<List<Issue>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(project) {
        val beadsFile = Paths.get(project.basePath ?: return@LaunchedEffect, ".beads", "issues.jsonl")
        IssueRepository().loadIssues(beadsFile)
            .onSuccess {
                // Filter to issues with dependencies for clearer graph
                issues = it.filter { issue -> issue.dependencies.isNotEmpty() }
                isLoading = false
            }
            .onFailure { isLoading = false }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading dependency graph...")
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
            "Dependency Graph (${issues.size} issues with dependencies)",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (issues.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No issues with dependencies found")
            }
        } else {
            // Simple circular graph layout
            DependencyGraphCanvas(issues)
        }
    }
}

@Composable
private fun DependencyGraphCanvas(issues: List<Issue>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(600.dp)
            .background(Color(0x08FFFFFF))
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = minOf(size.width, size.height) / 3

        // Position nodes in circle
        val positions = issues.mapIndexed { index, issue ->
            val angle = (2 * Math.PI * index) / issues.size
            val x = (centerX + radius * cos(angle)).toFloat()
            val y = (centerY + radius * sin(angle)).toFloat()
            issue.id to Offset(x, y)
        }.toMap()

        // Draw dependency edges first (behind nodes)
        issues.forEach { issue ->
            val fromPos = positions[issue.id] ?: return@forEach
            issue.dependencies.forEach { dep ->
                val toPos = positions[dep.dependsOnId]
                if (toPos != null) {
                    drawLine(
                        color = Color(0xFF5C9FE5),
                        start = fromPos,
                        end = toPos,
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
                    )
                }
            }
        }

        // Draw nodes
        positions.values.forEach { pos ->
            drawCircle(
                color = Color(0xFF5C9FE5),
                radius = 20f,
                center = pos
            )
        }
    }
}
