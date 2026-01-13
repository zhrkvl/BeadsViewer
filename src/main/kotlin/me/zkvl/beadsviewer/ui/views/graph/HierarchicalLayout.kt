package me.zkvl.beadsviewer.ui.views.graph

import androidx.compose.ui.geometry.Offset
import me.zkvl.beadsviewer.model.Issue
import kotlin.math.max

/**
 * Hierarchical graph layout implementation using the Sugiyama framework.
 *
 * Produces a layered, top-to-bottom layout ideal for dependency graphs.
 * Phases:
 * 1. Cycle removal (break cycles to create DAG)
 * 2. Layer assignment (assign nodes to horizontal layers)
 * 3. Crossing reduction (minimize edge crossings)
 * 4. Coordinate assignment (position nodes within layers)
 */
object HierarchicalLayout {
    private const val LAYER_SPACING = 150f
    private const val NODE_SPACING = 100f
    private const val MARGIN = 50f

    /**
     * Compute hierarchical layout positions for issues.
     */
    fun computeLayout(issues: List<Issue>): Map<String, Offset> {
        if (issues.isEmpty()) return emptyMap()

        // Phase 1: Build graph structure
        val adjacencyList = LayoutAlgorithmUtils.buildAdjacencyList(issues)
        val reverseAdjacencyList = LayoutAlgorithmUtils.buildReverseAdjacencyList(issues)

        // Phase 2: Detect and handle cycles
        val cycleNodes = LayoutAlgorithmUtils.detectCycles(adjacencyList)
        val acyclicAdjacencyList = if (cycleNodes.isNotEmpty()) {
            // For now, simply exclude cycle edges (could be improved)
            removeCycleEdges(adjacencyList, cycleNodes)
        } else {
            adjacencyList
        }

        // Phase 3: Layer assignment (longest path method)
        val layers = assignLayers(acyclicAdjacencyList)

        // Phase 4: Crossing reduction (barycenter heuristic)
        val orderedLayers = reduceCrossings(layers, acyclicAdjacencyList)

        // Phase 5: Coordinate assignment
        return assignCoordinates(orderedLayers)
    }

    /**
     * Remove edges that create cycles (simple approach: break edges involving cycle nodes).
     */
    private fun removeCycleEdges(
        adjacencyList: Map<String, List<String>>,
        cycleNodes: Set<String>
    ): Map<String, List<String>> {
        return adjacencyList.mapValues { (node, neighbors) ->
            if (node in cycleNodes) {
                // Remove edges from cycle nodes to other cycle nodes
                neighbors.filter { it !in cycleNodes }
            } else {
                neighbors
            }
        }
    }

    /**
     * Assign nodes to layers using longest path method.
     * Layer 0 = nodes with no dependencies (roots)
     * Layer N = nodes depending on layer N-1
     */
    private fun assignLayers(adjacencyList: Map<String, List<String>>): Map<Int, List<String>> {
        val layers = mutableMapOf<Int, MutableList<String>>()
        val nodeLayer = mutableMapOf<String, Int>()

        // Find root nodes (no dependencies)
        val rootNodes = LayoutAlgorithmUtils.findRootNodes(adjacencyList)

        // BFS to assign layers
        val queue = ArrayDeque<Pair<String, Int>>()
        rootNodes.forEach { node ->
            queue.add(node to 0)
            nodeLayer[node] = 0
        }

        while (queue.isNotEmpty()) {
            val (node, layer) = queue.removeFirst()

            // Update layer for this node (take maximum if already assigned)
            val currentLayer = max(nodeLayer.getOrDefault(node, 0), layer)
            nodeLayer[node] = currentLayer

            layers.getOrPut(currentLayer) { mutableListOf() }.add(node)

            // Process neighbors (nodes this node depends on should be at higher layers)
            adjacencyList[node]?.forEach { neighbor ->
                val neighborLayer = currentLayer + 1
                val existingLayer = nodeLayer[neighbor]

                if (existingLayer == null || neighborLayer > existingLayer) {
                    // Remove from old layer if exists
                    if (existingLayer != null) {
                        layers[existingLayer]?.remove(neighbor)
                    }
                    queue.add(neighbor to neighborLayer)
                }
            }
        }

        // Handle disconnected nodes (nodes not visited)
        adjacencyList.keys.forEach { node ->
            if (!nodeLayer.containsKey(node)) {
                val layer = 0
                nodeLayer[node] = layer
                layers.getOrPut(layer) { mutableListOf() }.add(node)
            }
        }

        // Convert to sorted map for consistent ordering
        return layers.toSortedMap()
    }

    /**
     * Reduce edge crossings using barycenter heuristic.
     * Iteratively reorder nodes within layers to minimize crossings.
     */
    private fun reduceCrossings(
        layers: Map<Int, List<String>>,
        adjacencyList: Map<String, List<String>>
    ): Map<Int, List<String>> {
        val orderedLayers = layers.toMutableMap()
        val maxIterations = 10
        val layerKeys = orderedLayers.keys.sorted()

        repeat(maxIterations) { iteration ->
            // Forward pass (top to bottom)
            for (i in 1 until layerKeys.size) {
                val currentLayer = layerKeys[i]
                val previousLayer = layerKeys[i - 1]
                orderedLayers[currentLayer] = reorderLayerByBarycenter(
                    orderedLayers[currentLayer]!!,
                    orderedLayers[previousLayer]!!,
                    adjacencyList,
                    forward = true
                )
            }

            // Backward pass (bottom to top)
            if (iteration % 2 == 1) {
                for (i in layerKeys.size - 2 downTo 0) {
                    val currentLayer = layerKeys[i]
                    val nextLayer = layerKeys[i + 1]
                    orderedLayers[currentLayer] = reorderLayerByBarycenter(
                        orderedLayers[currentLayer]!!,
                        orderedLayers[nextLayer]!!,
                        adjacencyList,
                        forward = false
                    )
                }
            }
        }

        return orderedLayers
    }

    /**
     * Reorder nodes in a layer based on barycenter of connected nodes in adjacent layer.
     */
    private fun reorderLayerByBarycenter(
        currentLayer: List<String>,
        adjacentLayer: List<String>,
        adjacencyList: Map<String, List<String>>,
        forward: Boolean
    ): List<String> {
        // Calculate barycenter for each node
        val barycenters = currentLayer.map { node ->
            val positions = mutableListOf<Int>()

            if (forward) {
                // Find connections to previous layer
                adjacentLayer.forEachIndexed { index, prevNode ->
                    if (adjacencyList[node]?.contains(prevNode) == true) {
                        positions.add(index)
                    }
                }
            } else {
                // Find connections to next layer
                adjacentLayer.forEachIndexed { index, nextNode ->
                    if (adjacencyList[nextNode]?.contains(node) == true) {
                        positions.add(index)
                    }
                }
            }

            val barycenter = if (positions.isNotEmpty()) {
                positions.average()
            } else {
                Double.MAX_VALUE // Nodes with no connections go to end
            }

            node to barycenter
        }

        // Sort by barycenter
        return barycenters.sortedBy { it.second }.map { it.first }
    }

    /**
     * Assign final coordinates to nodes based on layer assignment and ordering.
     */
    private fun assignCoordinates(layers: Map<Int, List<String>>): Map<String, Offset> {
        val positions = mutableMapOf<String, Offset>()

        layers.forEach { (layerIndex, nodes) ->
            val y = MARGIN + layerIndex * LAYER_SPACING

            // Calculate total width needed for this layer
            val totalWidth = (nodes.size - 1) * NODE_SPACING

            // Center the layer horizontally
            val startX = MARGIN + (800f - totalWidth) / 2f // Assuming 800px canvas width

            nodes.forEachIndexed { nodeIndex, nodeId ->
                val x = if (nodes.size == 1) {
                    MARGIN + 400f // Center single node
                } else {
                    startX + nodeIndex * NODE_SPACING
                }
                positions[nodeId] = Offset(x, y)
            }
        }

        return positions
    }
}
