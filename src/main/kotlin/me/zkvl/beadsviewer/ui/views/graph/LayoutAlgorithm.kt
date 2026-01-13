package me.zkvl.beadsviewer.ui.views.graph

import androidx.compose.ui.geometry.Offset
import me.zkvl.beadsviewer.model.Issue

/**
 * Common utilities and interface for graph layout algorithms.
 */
object LayoutAlgorithmUtils {
    /**
     * Build adjacency list from issues for graph traversal.
     * Returns map of issue ID to list of dependent issue IDs (edges point from dependent to dependency).
     */
    fun buildAdjacencyList(issues: List<Issue>): Map<String, List<String>> {
        val adjacencyList = mutableMapOf<String, MutableList<String>>()

        // Initialize all nodes
        issues.forEach { issue ->
            adjacencyList.putIfAbsent(issue.id, mutableListOf())
        }

        // Add edges
        issues.forEach { issue ->
            issue.dependencies.forEach { dep ->
                // Edge from issue to what it depends on
                adjacencyList[issue.id]?.add(dep.dependsOnId)
                // Ensure target exists
                adjacencyList.putIfAbsent(dep.dependsOnId, mutableListOf())
            }
        }

        return adjacencyList
    }

    /**
     * Build reverse adjacency list (dependents -> blockers).
     * For each node, lists which nodes depend on it.
     */
    fun buildReverseAdjacencyList(issues: List<Issue>): Map<String, List<String>> {
        val reverseList = mutableMapOf<String, MutableList<String>>()

        // Initialize all nodes
        issues.forEach { issue ->
            reverseList.putIfAbsent(issue.id, mutableListOf())
        }

        // Add reverse edges
        issues.forEach { issue ->
            issue.dependencies.forEach { dep ->
                // dep.dependsOnId is blocked by issue
                reverseList.getOrPut(dep.dependsOnId) { mutableListOf() }.add(issue.id)
            }
        }

        return reverseList
    }

    /**
     * Detect cycles in the dependency graph using DFS.
     * Returns list of nodes involved in cycles.
     */
    fun detectCycles(adjacencyList: Map<String, List<String>>): Set<String> {
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()
        val cycleNodes = mutableSetOf<String>()

        fun dfs(node: String): Boolean {
            visited.add(node)
            recursionStack.add(node)

            adjacencyList[node]?.forEach { neighbor ->
                if (!visited.contains(neighbor)) {
                    if (dfs(neighbor)) {
                        cycleNodes.add(node)
                        return true
                    }
                } else if (recursionStack.contains(neighbor)) {
                    // Found cycle
                    cycleNodes.add(node)
                    cycleNodes.add(neighbor)
                    return true
                }
            }

            recursionStack.remove(node)
            return false
        }

        adjacencyList.keys.forEach { node ->
            if (!visited.contains(node)) {
                dfs(node)
            }
        }

        return cycleNodes
    }

    /**
     * Topological sort using Kahn's algorithm.
     * Returns null if graph has cycles, otherwise returns sorted list.
     */
    fun topologicalSort(adjacencyList: Map<String, List<String>>): List<String>? {
        val inDegree = mutableMapOf<String, Int>()
        val queue = ArrayDeque<String>()
        val result = mutableListOf<String>()

        // Calculate in-degrees
        adjacencyList.keys.forEach { node ->
            inDegree[node] = 0
        }
        adjacencyList.values.forEach { neighbors ->
            neighbors.forEach { neighbor ->
                inDegree[neighbor] = inDegree.getOrDefault(neighbor, 0) + 1
            }
        }

        // Add nodes with in-degree 0 to queue
        inDegree.forEach { (node, degree) ->
            if (degree == 0) {
                queue.add(node)
            }
        }

        // Process queue
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            result.add(node)

            adjacencyList[node]?.forEach { neighbor ->
                inDegree[neighbor] = inDegree.getOrDefault(neighbor, 1) - 1
                if (inDegree[neighbor] == 0) {
                    queue.add(neighbor)
                }
            }
        }

        // If result doesn't contain all nodes, there's a cycle
        return if (result.size == adjacencyList.size) result else null
    }

    /**
     * Find all nodes with no incoming edges (root nodes).
     */
    fun findRootNodes(adjacencyList: Map<String, List<String>>): List<String> {
        val hasIncomingEdge = mutableSetOf<String>()

        adjacencyList.values.forEach { neighbors ->
            hasIncomingEdge.addAll(neighbors)
        }

        return adjacencyList.keys.filter { it !in hasIncomingEdge }
    }
}
