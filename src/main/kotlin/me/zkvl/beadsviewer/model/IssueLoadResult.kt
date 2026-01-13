package me.zkvl.beadsviewer.model

/**
 * Result of loading issues from a data source (SQLite or JSONL).
 *
 * This class encapsulates not just the list of issues, but also metadata about:
 * - Which issues have unsaved changes (dirty)
 * - Where the issues were loaded from (SQLite vs JSONL)
 * - When the issues were loaded (for cache invalidation)
 *
 * @property issues The list of loaded issues
 * @property dirtyIssueIds Set of issue IDs that have unsaved changes (not yet synced to git).
 *                         These issues should be displayed with a "Not Synced" badge in the UI.
 *                         Empty if loaded from JSONL (JSONL is always synced by definition).
 * @property source The data source from which issues were loaded (SQLITE or JSONL)
 * @property timestamp Unix timestamp (milliseconds) when the issues were loaded.
 *                     Used for cache invalidation and determining if a reload is needed.
 */
data class IssueLoadResult(
    val issues: List<Issue>,
    val dirtyIssueIds: Set<String> = emptySet(),
    val source: DataSource,
    val timestamp: Long = System.currentTimeMillis()
)
