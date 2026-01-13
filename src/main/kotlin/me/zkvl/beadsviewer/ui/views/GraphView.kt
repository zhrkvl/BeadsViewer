package me.zkvl.beadsviewer.ui.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.query.service.QueryFilterService
import me.zkvl.beadsviewer.service.IssueService
import org.jetbrains.jewel.ui.component.Text
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (issues.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No issues with dependencies found")
                    }
                } else {
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
    val showLabels: Boolean = false,
    val labelMode: LabelMode = LabelMode.ID_ONLY,
    val edgeStyle: EdgeStyle = EdgeStyle.UNIFORM,
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
 * Canvas component that renders the dependency graph.
 */
@OptIn(ExperimentalComposeUiApi::class)
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
        positions.forEach { (issueId, pos) ->
            // Highlight selected/hovered nodes
            val isSelected = graphState.selectedNodeId == issueId
            val isHovered = graphState.hoveredNodeId == issueId

            // Draw selection ring
            if (isSelected) {
                drawCircle(
                    color = Color.White,
                    radius = 24f,
                    center = pos,
                    alpha = 0.5f
                )
            }

            // Draw hover ring
            if (isHovered) {
                drawCircle(
                    color = Color.White,
                    radius = 22f,
                    center = pos,
                    alpha = 0.3f
                )
            }

            // Draw main node
            drawCircle(
                color = Color(0xFF5C9FE5),
                radius = 20f,
                center = pos
            )
        }
    }
}
