package me.zkvl.beadsviewer.ui

import androidx.compose.runtime.Composable
import com.intellij.openapi.project.Project
import me.zkvl.beadsviewer.model.ViewMode
import me.zkvl.beadsviewer.ui.views.*

/**
 * Routes to the appropriate view component based on the selected view mode.
 * Acts as a central dispatcher for all view modes.
 */
@Composable
fun ViewRouter(
    project: Project,
    viewMode: ViewMode
) {
    when (viewMode) {
        ViewMode.LIST -> ListView(project)
        ViewMode.KANBAN -> KanbanView(project)
        ViewMode.GRAPH -> GraphView(project)
        ViewMode.INSIGHTS -> InsightsView(project)
        ViewMode.ACTIONABLE -> ActionableView(project)
        ViewMode.TREE -> TreeView(project)
        ViewMode.SPRINT -> SprintView(project)
        ViewMode.HISTORY -> HistoryView(project)
        ViewMode.ATTENTION -> AttentionView(project)
        ViewMode.FLOW -> FlowView(project)
    }
}
