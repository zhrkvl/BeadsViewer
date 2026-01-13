package me.zkvl.beadsviewer.model

/**
 * Represents the available view modes for displaying issues.
 * Each mode provides a different perspective on the issue data.
 */
enum class ViewMode(
    val displayName: String,
    val description: String,
) {
    LIST(
        displayName = "List",
        description = "Traditional scrollable list of all issues"
    ),

    KANBAN(
        displayName = "Kanban Board",
        description = "Board view with columns by status"
    ),

    GRAPH(
        displayName = "Dependency Graph",
        description = "Visual graph of issue dependencies"
    ),

    INSIGHTS(
        displayName = "Insights",
        description = "Metrics, analytics, and statistics dashboard"
    ),

    ACTIONABLE(
        displayName = "Actionable",
        description = "Issues grouped by tracks and labels"
    ),

    SPRINT(
        displayName = "Sprint",
        description = "Sprint dashboard with burndown chart"
    ),

    HISTORY(
        displayName = "History",
        description = "Timeline view of issue lifecycle"
    ),

    ATTENTION(
        displayName = "Needs Attention",
        description = "Issues requiring immediate attention"
    );

    companion object {
        /**
         * Default view mode shown on first launch
         */
        val DEFAULT = LIST

        /**
         * Parses a view mode from string, returns DEFAULT if invalid
         */
        fun fromString(value: String?): ViewMode {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: DEFAULT
        }
    }
}
