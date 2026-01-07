package me.zkvl.beadsviewer.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Holds computed metrics for an issue.
 *
 * These metrics are calculated by graph analysis algorithms and provide
 * insights into the importance and impact of issues within a project.
 * Used for prioritization, bottleneck detection, and workflow optimization.
 */
@Serializable
data class IssueMetrics(
    /**
     * PageRank score indicating the issue's importance in the dependency graph.
     * Higher values suggest the issue is more central/influential.
     */
    val pagerank: Double = 0.0,

    /**
     * Betweenness centrality measuring how often this issue lies on paths between other issues.
     * High betweenness indicates a bottleneck that many other issues depend on.
     */
    val betweenness: Double = 0.0,

    /**
     * Maximum depth of this issue in the critical path.
     * Indicates how many levels of dependencies must be resolved before this issue.
     */
    @SerialName("critical_path_depth")
    val criticalPathDepth: Int = 0,

    /**
     * Combined triage score used for prioritization.
     * Takes into account priority, dependencies, and graph metrics.
     */
    @SerialName("triage_score")
    val triageScore: Double = 0.0,

    /**
     * Number of issues that this issue blocks (outgoing edges).
     */
    @SerialName("blocks_count")
    val blocksCount: Int = 0,

    /**
     * Number of issues that block this issue (incoming edges).
     */
    @SerialName("blocked_by_count")
    val blockedByCount: Int = 0
)
