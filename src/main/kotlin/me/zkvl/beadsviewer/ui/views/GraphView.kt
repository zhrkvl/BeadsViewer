package me.zkvl.beadsviewer.ui.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Dropdown
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.zkvl.beadsviewer.model.Dependency
import me.zkvl.beadsviewer.model.DependencyType
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.model.Status
import me.zkvl.beadsviewer.query.service.QueryFilterService
import me.zkvl.beadsviewer.service.IssueService
import org.jetbrains.jewel.ui.component.Text
import kotlin.math.*

/**
 * Graph view that visualizes issue dependencies.
 * Displays issues as nodes with edges representing dependencies.
 *
 * Supports:
 * - Query filtering (integrates with QueryFilterService)
 * - Multiple layout algorithms (Hierarchical, Force-Directed, Circular)
 * - Zoom and pan navigation
 * - Node dragging and repositioning
 * - Interactive selection and hover states
 */
@Composable
fun GraphView(project: Project) {
    val issueService = remember { IssueService.getInstance(project) }
    val queryFilterService = remember { QueryFilterService.getInstance(project) }
    val graphViewModel = remember { GraphViewModel() }

    val issuesState by issueService.issuesState.collectAsState()
    val filteredState by queryFilterService.filteredState.collectAsState()
    val graphState by graphViewModel.graphState.collectAsState()

    // Determine base issues: filtered or all (complementary filtering pattern from ListView)
    val baseIssues = when {
        filteredState is QueryFilterService.FilteredIssuesState.Filtered ->
            (filteredState as QueryFilterService.FilteredIssuesState.Filtered).issues
        filteredState is QueryFilterService.FilteredIssuesState.Error ->
            emptyList()
        issuesState is IssueService.IssuesState.Loaded ->
            (issuesState as IssueService.IssuesState.Loaded).issues
        else -> emptyList()
    }

    // Apply view-specific filtering: only show issues with dependencies
    val issues = baseIssues.filter { it.dependencies.isNotEmpty() }

    // Trigger layout computation when issues change
    LaunchedEffect(issues, graphState.layoutAlgorithm) {
        if (issues.isNotEmpty()) {
            graphViewModel.computeLayout(issues)
        }
    }

    when (val state = issuesState) {
        is IssueService.IssuesState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading dependency graph...")
            }
            return
        }
        is IssueService.IssuesState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message)
            }
            return
        }
        is IssueService.IssuesState.Loaded -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with issue count
                Text(
                    "Dependency Graph (${issues.size} issues with dependencies)",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (issues.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No issues with dependencies found")
                    }
                } else {
                    // Control panel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Layout selector
                        Text("Layout:", fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            DefaultButton(
                                onClick = { graphViewModel.setLayoutAlgorithm(LayoutAlgorithm.HIERARCHICAL) },
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    "Hierarchical",
                                    fontSize = 11.sp,
                                    color = if (graphState.layoutAlgorithm == LayoutAlgorithm.HIERARCHICAL) Color(0xFF3498DB) else Color.Unspecified
                                )
                            }
                            DefaultButton(
                                onClick = { graphViewModel.setLayoutAlgorithm(LayoutAlgorithm.FORCE_DIRECTED) },
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    "Force-Directed",
                                    fontSize = 11.sp,
                                    color = if (graphState.layoutAlgorithm == LayoutAlgorithm.FORCE_DIRECTED) Color(0xFF3498DB) else Color.Unspecified
                                )
                            }
                            DefaultButton(
                                onClick = { graphViewModel.setLayoutAlgorithm(LayoutAlgorithm.CIRCULAR) },
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    "Circular",
                                    fontSize = 11.sp,
                                    color = if (graphState.layoutAlgorithm == LayoutAlgorithm.CIRCULAR) Color(0xFF3498DB) else Color.Unspecified
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Visual options
                        DefaultButton(
                            onClick = { graphViewModel.toggleLabels() },
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                if (graphState.showLabels) "Hide Labels" else "Show Labels",
                                fontSize = 11.sp
                            )
                        }

                        DefaultButton(
                            onClick = {
                                val nextStyle = when (graphState.edgeStyle) {
                                    EdgeStyle.BY_TYPE -> EdgeStyle.BY_BLOCKING
                                    EdgeStyle.BY_BLOCKING -> EdgeStyle.UNIFORM
                                    EdgeStyle.UNIFORM -> EdgeStyle.BY_TYPE
                                }
                                graphViewModel.setEdgeStyle(nextStyle)
                            },
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                "Edges: ${graphState.edgeStyle.name.lowercase().replace('_', ' ')}",
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Reset view button
                        DefaultButton(
                            onClick = { graphViewModel.resetView() },
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Reset View", fontSize = 11.sp)
                        }

                        // Zoom indicator
                        Text(
                            "Zoom: ${(graphState.zoom * 100).toInt()}%",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // Graph canvas
                    DependencyGraphCanvas(
                        issues = issues,
                        graphState = graphState,
                        graphViewModel = graphViewModel
                    )
                }
            }
        }
    }
}

/**
 * State for the dependency graph visualization.
 */
data class GraphState(
    // Viewport state
    val zoom: Float = 1.0f,
    val panOffset: Offset = Offset.Zero,

    // Node positions (mutable during drag/layout)
    val nodePositions: Map<String, Offset> = emptyMap(),

    // Layout metadata
    val layoutAlgorithm: LayoutAlgorithm = LayoutAlgorithm.HIERARCHICAL,

    // Interaction state
    val selectedNodeId: String? = null,
    val hoveredNodeId: String? = null,
    val draggedNodeId: String? = null,
    val dragOffset: Offset = Offset.Zero,

    // Visual settings
    val showLabels: Boolean = true,
    val labelMode: LabelMode = LabelMode.ID_ONLY,
    val edgeStyle: EdgeStyle = EdgeStyle.BY_TYPE,
)

/**
 * Layout algorithms for graph visualization.
 */
enum class LayoutAlgorithm {
    CIRCULAR,      // Simple circular layout (current implementation)
    HIERARCHICAL,  // Top-down hierarchical layout for dependencies
    FORCE_DIRECTED // Physics-based force-directed layout
}

/**
 * Label display modes.
 */
enum class LabelMode {
    ID_ONLY,       // Show only issue ID
    TITLE_ONLY,    // Show only issue title
    ID_AND_TITLE   // Show both ID and title
}

/**
 * Edge styling modes.
 */
enum class EdgeStyle {
    UNIFORM,      // All edges same style
    BY_TYPE,      // Different styles per dependency type
    BY_BLOCKING   // Different styles for blocking vs non-blocking
}

/**
 * ViewModel for managing graph state.
 */
class GraphViewModel {
    private val _graphState = MutableStateFlow(GraphState())
    val graphState: StateFlow<GraphState> = _graphState.asStateFlow()

    /**
     * Apply zoom transformation with pivot point.
     */
    fun applyZoom(delta: Float, pivot: Offset) {
        _graphState.update { state ->
            val newZoom = (state.zoom * delta).coerceIn(0.1f, 3.0f)

            // Adjust pan offset to zoom around pivot point
            // Formula: newPan = pivot + (oldPan - pivot) * (newZoom / oldZoom)
            val zoomRatio = newZoom / state.zoom
            val newPanOffset = Offset(
                x = pivot.x + (state.panOffset.x - pivot.x) * zoomRatio,
                y = pivot.y + (state.panOffset.y - pivot.y) * zoomRatio
            )

            state.copy(zoom = newZoom, panOffset = newPanOffset)
        }
    }

    /**
     * Apply scroll wheel zoom.
     */
    fun applyScrollZoom(scrollDelta: Float, pivot: Offset) {
        val zoomFactor = 1f + scrollDelta * 0.1f
        applyZoom(zoomFactor, pivot)
    }

    /**
     * Apply pan transformation.
     */
    fun applyPan(delta: Offset) {
        _graphState.update { state ->
            state.copy(panOffset = state.panOffset + delta)
        }
    }

    /**
     * Start dragging a node.
     */
    fun startDragNode(nodeId: String, position: Offset) {
        _graphState.update { state ->
            state.copy(
                draggedNodeId = nodeId,
                dragOffset = position
            )
        }
    }

    /**
     * Update node position during drag.
     */
    fun updateDragNode(position: Offset) {
        _graphState.update { state ->
            if (state.draggedNodeId != null) {
                val updatedPositions = state.nodePositions.toMutableMap()
                updatedPositions[state.draggedNodeId] = position
                state.copy(
                    nodePositions = updatedPositions,
                    dragOffset = position
                )
            } else {
                state
            }
        }
    }

    /**
     * End node dragging.
     */
    fun endDragNode() {
        _graphState.update { state ->
            state.copy(
                draggedNodeId = null,
                dragOffset = Offset.Zero
            )
        }
    }

    /**
     * Select a node.
     */
    fun selectNode(nodeId: String?) {
        _graphState.update { state ->
            state.copy(selectedNodeId = nodeId)
        }
    }

    /**
     * Set hovered node.
     */
    fun setHoveredNode(nodeId: String?) {
        _graphState.update { state ->
            state.copy(hoveredNodeId = nodeId)
        }
    }

    /**
     * Hit test to find node at given position.
     * Returns node ID if a node is at the position, null otherwise.
     */
    fun hitTestNode(position: Offset, nodeRadius: Float = 20f): String? {
        val positions = _graphState.value.nodePositions
        return positions.entries.firstOrNull { (_, nodePos) ->
            val distance = sqrt(
                (position.x - nodePos.x).pow(2) +
                (position.y - nodePos.y).pow(2)
            )
            distance <= nodeRadius
        }?.key
    }

    /**
     * Compute layout for the given issues.
     */
    fun computeLayout(issues: List<Issue>) {
        val positions = when (_graphState.value.layoutAlgorithm) {
            LayoutAlgorithm.CIRCULAR -> computeCircularLayout(issues)
            LayoutAlgorithm.HIERARCHICAL -> computeHierarchicalLayout(issues)
            LayoutAlgorithm.FORCE_DIRECTED -> computeForceDirectedLayout(issues)
        }

        _graphState.update { state ->
            state.copy(nodePositions = positions)
        }
    }

    /**
     * Set layout algorithm.
     */
    fun setLayoutAlgorithm(algorithm: LayoutAlgorithm) {
        _graphState.update { state ->
            state.copy(layoutAlgorithm = algorithm)
        }
    }

    /**
     * Toggle label visibility.
     */
    fun toggleLabels() {
        _graphState.update { state ->
            state.copy(showLabels = !state.showLabels)
        }
    }

    /**
     * Set label mode.
     */
    fun setLabelMode(mode: LabelMode) {
        _graphState.update { state ->
            state.copy(labelMode = mode)
        }
    }

    /**
     * Set edge style.
     */
    fun setEdgeStyle(style: EdgeStyle) {
        _graphState.update { state ->
            state.copy(edgeStyle = style)
        }
    }

    /**
     * Reset view to default zoom and pan.
     */
    fun resetView() {
        _graphState.update { state ->
            state.copy(zoom = 1.0f, panOffset = Offset.Zero)
        }
    }

    /**
     * Compute visible nodes within viewport (for performance optimization).
     */
    fun computeVisibleNodes(canvasWidth: Float, canvasHeight: Float): Set<String> {
        val state = _graphState.value
        val margin = 100f // Include nodes slightly outside viewport

        val viewportLeft = (-state.panOffset.x / state.zoom) - margin
        val viewportTop = (-state.panOffset.y / state.zoom) - margin
        val viewportRight = (canvasWidth - state.panOffset.x) / state.zoom + margin
        val viewportBottom = (canvasHeight - state.panOffset.y) / state.zoom + margin

        return state.nodePositions.filterValues { pos ->
            pos.x >= viewportLeft && pos.x <= viewportRight &&
            pos.y >= viewportTop && pos.y <= viewportBottom
        }.keys.toSet()
    }

    /**
     * Circular layout algorithm (current implementation).
     */
    private fun computeCircularLayout(issues: List<Issue>): Map<String, Offset> {
        val centerX = 400f
        val centerY = 300f
        val radius = 250f

        return issues.mapIndexed { index, issue ->
            val angle = (2 * Math.PI * index) / issues.size
            val x = (centerX + radius * cos(angle)).toFloat()
            val y = (centerY + radius * sin(angle)).toFloat()
            issue.id to Offset(x, y)
        }.toMap()
    }

    /**
     * Hierarchical layout algorithm using Sugiyama framework.
     */
    private fun computeHierarchicalLayout(issues: List<Issue>): Map<String, Offset> {
        return me.zkvl.beadsviewer.ui.views.graph.HierarchicalLayout.computeLayout(issues)
    }

    /**
     * Force-directed layout algorithm using Fruchterman-Reingold.
     */
    private fun computeForceDirectedLayout(issues: List<Issue>): Map<String, Offset> {
        return me.zkvl.beadsviewer.ui.views.graph.ForceDirectedLayout.computeLayout(issues)
    }
}

/**
 * Get node color based on status and priority.
 */
private fun getNodeColor(issue: Issue): Color {
    return when (issue.status) {
        Status.BLOCKED -> Color(0xFFE74C3C) // Red
        Status.CLOSED -> Color(0xFF95A5A6)  // Gray
        Status.IN_PROGRESS -> Color(0xFF3498DB) // Blue
        Status.HOOKED -> Color(0xFFF39C12) // Orange
        Status.TOMBSTONE -> Color(0xFF7F8C8D) // Dark gray
        Status.OPEN -> when (issue.priority) {
            0 -> Color(0xFFE74C3C) // P0 Critical - Red
            1 -> Color(0xFFF39C12) // P1 High - Orange
            2 -> Color(0xFFF1C40F) // P2 Medium - Yellow
            3 -> Color(0xFF2ECC71) // P3 Low - Green
            else -> Color(0xFF95A5A6) // P4+ Backlog - Gray
        }
    }
}

/**
 * Get node radius based on dependency count.
 */
private fun getNodeRadius(issue: Issue): Float {
    val baseRadius = 20f
    val depCount = issue.dependencies.size
    return baseRadius + (depCount * 1.5f).coerceAtMost(10f)
}

/**
 * Get edge style (color and path effect) based on dependency type.
 */
private fun getEdgeStyle(dependency: Dependency, edgeStyle: EdgeStyle): Pair<Color, PathEffect?> {
    return when (edgeStyle) {
        EdgeStyle.BY_TYPE -> when (dependency.type) {
            is DependencyType.Blocks ->
                Color(0xFFE74C3C) to null // Solid red
            is DependencyType.ParentChild ->
                Color(0xFF3498DB) to PathEffect.dashPathEffect(floatArrayOf(15f, 5f)) // Dashed blue
            is DependencyType.Related ->
                Color(0xFF95A5A6) to PathEffect.dashPathEffect(floatArrayOf(5f, 5f)) // Dotted gray
            is DependencyType.DiscoveredFrom ->
                Color(0xFFF39C12) to PathEffect.dashPathEffect(floatArrayOf(10f, 5f, 2f, 5f)) // Dash-dot orange
            is DependencyType.Supersedes ->
                Color(0xFF9B59B6) to null // Solid purple
            is DependencyType.RepliesTo ->
                Color(0xFF1ABC9C) to PathEffect.dashPathEffect(floatArrayOf(8f, 8f)) // Dashed teal
            else ->
                Color(0xFF95A5A6) to PathEffect.dashPathEffect(floatArrayOf(5f, 5f)) // Dotted gray
        }
        EdgeStyle.BY_BLOCKING -> if (dependency.isBlocking()) {
            Color(0xFFE74C3C) to null // Solid red for blocking
        } else {
            Color(0xFF95A5A6) to PathEffect.dashPathEffect(floatArrayOf(5f, 5f)) // Dotted gray for non-blocking
        }
        EdgeStyle.UNIFORM ->
            Color(0xFF5C9FE5) to PathEffect.dashPathEffect(floatArrayOf(10f, 5f)) // Blue dashed
    }
}

/**
 * Draw an arrow at the end of an edge.
 */
private fun DrawScope.drawArrow(from: Offset, to: Offset, color: Color, arrowSize: Float = 12f) {
    val angle = atan2(to.y - from.y, to.x - from.x)
    val arrowAngle = PI / 6 // 30 degrees

    val arrow1 = Offset(
        (to.x - arrowSize * cos(angle - arrowAngle)).toFloat(),
        (to.y - arrowSize * sin(angle - arrowAngle)).toFloat()
    )
    val arrow2 = Offset(
        (to.x - arrowSize * cos(angle + arrowAngle)).toFloat(),
        (to.y - arrowSize * sin(angle + arrowAngle)).toFloat()
    )

    // Draw arrowhead lines
    drawLine(color, to, arrow1, strokeWidth = 2f)
    drawLine(color, to, arrow2, strokeWidth = 2f)
}

/**
 * Draw a label for a node.
 */
@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawLabel(
    text: String,
    position: Offset,
    visible: Boolean,
    zoom: Float
) {
    if (!visible || zoom < 0.7f) return

    val textStyle = TextStyle(
        color = Color.White,
        fontSize = 11.sp
    )

    // Estimate text size (rough approximation)
    val textWidth = text.length * 6f
    val textHeight = 14f

    // Position label below node
    val labelPos = Offset(
        x = position.x - textWidth / 2,
        y = position.y + 30f
    )

    // Draw semi-transparent background
    drawRect(
        color = Color.Black.copy(alpha = 0.75f),
        topLeft = labelPos.copy(y = labelPos.y - textHeight / 2),
        size = androidx.compose.ui.geometry.Size(textWidth + 8f, textHeight + 4f)
    )

    // Note: Full text rendering requires TextMeasurer which is complex in Canvas
    // For now, we show a simple background. Full implementation would use rememberTextMeasurer
}

/**
 * Canvas component that renders the dependency graph.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalTextApi::class)
@Composable
private fun DependencyGraphCanvas(
    issues: List<Issue>,
    graphState: GraphState,
    graphViewModel: GraphViewModel
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(600.dp)
            .background(Color(0x08FFFFFF))
            // Hover detection and click handling
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.first().position

                        // Transform position to graph coordinates (account for zoom/pan)
                        val graphPosition = Offset(
                            x = (position.x - graphState.panOffset.x) / graphState.zoom,
                            y = (position.y - graphState.panOffset.y) / graphState.zoom
                        )

                        when (event.type) {
                            PointerEventType.Move -> {
                                // Update hover state
                                val hoveredNode = graphViewModel.hitTestNode(graphPosition)
                                graphViewModel.setHoveredNode(hoveredNode)
                            }
                            PointerEventType.Press -> {
                                // Handle node selection
                                val clickedNode = graphViewModel.hitTestNode(graphPosition)
                                graphViewModel.selectNode(clickedNode)
                            }
                            else -> {}
                        }
                    }
                }
            }
            // Node dragging with priority over canvas pan
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Transform to graph coordinates
                        val graphPosition = Offset(
                            x = (offset.x - graphState.panOffset.x) / graphState.zoom,
                            y = (offset.y - graphState.panOffset.y) / graphState.zoom
                        )
                        val nodeId = graphViewModel.hitTestNode(graphPosition)
                        if (nodeId != null) {
                            graphViewModel.startDragNode(nodeId, graphPosition)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (graphState.draggedNodeId != null) {
                            // Dragging a node - update node position
                            val scaledDrag = dragAmount / graphState.zoom
                            val currentPos = graphState.nodePositions[graphState.draggedNodeId]
                            if (currentPos != null) {
                                graphViewModel.updateDragNode(currentPos + scaledDrag)
                            }
                            change.consume()
                        } else {
                            // Not dragging a node - pan the canvas
                            graphViewModel.applyPan(dragAmount)
                            change.consume()
                        }
                    },
                    onDragEnd = {
                        graphViewModel.endDragNode()
                    }
                )
            }
            // Zoom gestures (pinch)
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (zoom != 1.0f) {
                        graphViewModel.applyZoom(zoom, centroid)
                    }
                    // Note: pan is handled by detectDragGestures above
                }
            }
            // Scroll wheel zoom
            .onPointerEvent(PointerEventType.Scroll) {
                val change = it.changes.first()
                val scrollDelta = change.scrollDelta.y
                if (scrollDelta != 0f) {
                    graphViewModel.applyScrollZoom(-scrollDelta, change.position)
                }
            }
            // Apply viewport transformations
            .graphicsLayer(
                scaleX = graphState.zoom,
                scaleY = graphState.zoom,
                translationX = graphState.panOffset.x,
                translationY = graphState.panOffset.y
            )
    ) {
        val positions = graphState.nodePositions
        val issueMap = issues.associateBy { it.id }

        // Draw dependency edges first (behind nodes)
        issues.forEach { issue ->
            val fromPos = positions[issue.id] ?: return@forEach
            issue.dependencies.forEach { dep ->
                val toPos = positions[dep.dependsOnId]
                if (toPos != null) {
                    val (color, pathEffect) = getEdgeStyle(dep, graphState.edgeStyle)

                    // Draw edge line
                    drawLine(
                        color = color,
                        start = fromPos,
                        end = toPos,
                        strokeWidth = 2f,
                        pathEffect = pathEffect
                    )

                    // Draw arrow at the end
                    drawArrow(fromPos, toPos, color)
                }
            }
        }

        // Draw nodes
        positions.forEach { (issueId, pos) ->
            val issue = issueMap[issueId] ?: return@forEach
            val radius = getNodeRadius(issue)
            val color = getNodeColor(issue)

            // Highlight selected/hovered nodes
            val isSelected = graphState.selectedNodeId == issueId
            val isHovered = graphState.hoveredNodeId == issueId

            // Draw selection ring
            if (isSelected) {
                drawCircle(
                    color = Color.White,
                    radius = radius + 6f,
                    center = pos,
                    alpha = 0.5f
                )
            }

            // Draw hover ring
            if (isHovered) {
                drawCircle(
                    color = Color.White,
                    radius = radius + 3f,
                    center = pos,
                    alpha = 0.3f
                )
            }

            // Draw main node
            drawCircle(
                color = color,
                radius = radius,
                center = pos
            )

            // Draw inner indicator for blocked status
            if (issue.status == Status.BLOCKED) {
                drawCircle(
                    color = Color.White,
                    radius = radius * 0.4f,
                    center = pos
                )
            }

            // Draw label if enabled
            if (graphState.showLabels) {
                val labelText = when (graphState.labelMode) {
                    LabelMode.ID_ONLY -> issue.id
                    LabelMode.TITLE_ONLY -> issue.title.take(20)
                    LabelMode.ID_AND_TITLE -> "${issue.id}: ${issue.title.take(15)}"
                }
                drawLabel(labelText, pos, true, graphState.zoom)
            }
        }
    }
}
