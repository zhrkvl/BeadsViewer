package me.zkvl.beadsviewer.ui.views.graph

import androidx.compose.ui.geometry.Offset
import me.zkvl.beadsviewer.model.Issue
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Force-directed graph layout using the Fruchterman-Reingold algorithm.
 *
 * Treats the graph as a physical system where:
 * - Edges act as springs (attractive force between connected nodes)
 * - Nodes repel each other (repulsive force between all nodes)
 * - System iteratively moves toward equilibrium
 *
 * Good for general graphs without clear hierarchy.
 */
object ForceDirectedLayout {
    private const val CANVAS_WIDTH = 800f
    private const val CANVAS_HEIGHT = 600f
    private const val ITERATIONS = 100
    private const val INITIAL_TEMPERATURE = 100f
    private const val COOLING_FACTOR = 0.95f

    /**
     * Compute force-directed layout positions for issues.
     */
    fun computeLayout(issues: List<Issue>): Map<String, Offset> {
        if (issues.isEmpty()) return emptyMap()

        // Initialize random positions
        val positions = initializePositions(issues)

        // Build adjacency list for edges
        val adjacencyList = LayoutAlgorithmUtils.buildAdjacencyList(issues)

        // Calculate optimal distance between nodes
        val area = CANVAS_WIDTH * CANVAS_HEIGHT
        val k = sqrt(area / issues.size) // Optimal distance

        // Simulate physics
        var temperature = INITIAL_TEMPERATURE

        repeat(ITERATIONS) {
            val forces = calculateForces(positions, adjacencyList, k)

            // Apply forces with temperature cooling
            positions.forEach { (nodeId, pos) ->
                val force = forces[nodeId] ?: Offset.Zero
                val displacement = limitMagnitude(force, temperature)
                positions[nodeId] = constrainToCanvas(pos + displacement)
            }

            // Cool down
            temperature *= COOLING_FACTOR
        }

        return positions
    }

    /**
     * Initialize nodes at random positions within canvas bounds.
     */
    private fun initializePositions(issues: List<Issue>): MutableMap<String, Offset> {
        val positions = mutableMapOf<String, Offset>()
        val random = Random(42) // Fixed seed for reproducibility

        issues.forEach { issue ->
            positions[issue.id] = Offset(
                x = random.nextFloat() * CANVAS_WIDTH,
                y = random.nextFloat() * CANVAS_HEIGHT
            )
        }

        return positions
    }

    /**
     * Calculate forces acting on each node.
     */
    private fun calculateForces(
        positions: Map<String, Offset>,
        adjacencyList: Map<String, List<String>>,
        k: Float
    ): Map<String, Offset> {
        val forces = positions.keys.associateWith { Offset.Zero }.toMutableMap()

        // Repulsive forces between all pairs
        positions.keys.forEach { nodeA ->
            positions.keys.forEach { nodeB ->
                if (nodeA != nodeB) {
                    val posA = positions[nodeA]!!
                    val posB = positions[nodeB]!!
                    val delta = posA - posB
                    val distance = delta.getDistance()

                    if (distance > 0.1f) { // Avoid division by zero
                        val repulsiveForce = repulsionForce(distance, k)
                        val direction = delta / distance
                        forces[nodeA] = forces[nodeA]!! + direction * repulsiveForce
                    }
                }
            }
        }

        // Attractive forces along edges
        adjacencyList.forEach { (nodeA, neighbors) ->
            neighbors.forEach { nodeB ->
                if (positions.containsKey(nodeB)) {
                    val posA = positions[nodeA]!!
                    val posB = positions[nodeB]!!
                    val delta = posB - posA
                    val distance = delta.getDistance()

                    if (distance > 0.1f) {
                        val attractiveForce = attractionForce(distance, k)
                        val direction = delta / distance
                        forces[nodeA] = forces[nodeA]!! + direction * attractiveForce
                        forces[nodeB] = forces[nodeB]!! - direction * attractiveForce
                    }
                }
            }
        }

        return forces
    }

    /**
     * Repulsive force between nodes (inversely proportional to distance).
     * Fruchterman-Reingold: fr(d) = k² / d
     */
    private fun repulsionForce(distance: Float, k: Float): Float {
        return (k * k) / distance
    }

    /**
     * Attractive force along edges (proportional to distance).
     * Fruchterman-Reingold: fa(d) = d² / k
     */
    private fun attractionForce(distance: Float, k: Float): Float {
        return (distance * distance) / k
    }

    /**
     * Limit force magnitude by temperature (prevents overshooting).
     */
    private fun limitMagnitude(force: Offset, maxMagnitude: Float): Offset {
        val magnitude = force.getDistance()
        return if (magnitude > maxMagnitude) {
            force * (maxMagnitude / magnitude)
        } else {
            force
        }
    }

    /**
     * Constrain position to canvas bounds with margin.
     */
    private fun constrainToCanvas(pos: Offset): Offset {
        val margin = 50f
        return Offset(
            x = pos.x.coerceIn(margin, CANVAS_WIDTH - margin),
            y = pos.y.coerceIn(margin, CANVAS_HEIGHT - margin)
        )
    }
}

/**
 * Extension function to calculate Euclidean distance of an Offset.
 */
private fun Offset.getDistance(): Float {
    return sqrt(x.pow(2) + y.pow(2))
}
