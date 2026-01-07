package me.zkvl.beadsviewer.query.state

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import me.zkvl.beadsviewer.model.ViewMode

/**
 * Persistent state component for query state.
 *
 * Stores:
 * - Current query per view mode (so each view can have its own query)
 * - Query history (last 50 queries for autocomplete/reuse)
 *
 * State is persisted in .idea/workspace.xml (project-level), so queries
 * are preserved across IDE restarts but not committed to version control.
 *
 * Example usage:
 * ```kotlin
 * val service = QueryStateService.getInstance(project)
 *
 * // Save query for current view
 * service.setQueryForView(ViewMode.LIST, "status:open AND priority:0")
 *
 * // Load query for current view
 * val query = service.getQueryForView(ViewMode.LIST)
 *
 * // Get query history for autocomplete
 * val history = service.getQueryHistory()
 * ```
 */
@Service(Service.Level.PROJECT)
@State(
    name = "BeadsViewerQueryState",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class QueryStateService : PersistentStateComponent<QueryStateService.State> {

    /**
     * Data class holding persisted state.
     * Must be mutable for IntelliJ Platform's state management.
     */
    data class State(
        /**
         * Current query for each view mode.
         * Map: ViewMode.name -> query string
         *
         * Example: {"LIST": "status:open", "KANBAN": "priority:0"}
         */
        var viewQueries: MutableMap<String, String> = mutableMapOf(),

        /**
         * Query history (most recent first).
         * Limited to 50 entries with LRU eviction.
         *
         * Example: ["status:open AND priority:0", "label:frontend", ...]
         */
        var queryHistory: MutableList<String> = mutableListOf()
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    /**
     * Get the current query for a view mode.
     *
     * @param viewMode The view mode to get the query for
     * @return The query string, or null if no query is set for this view
     */
    fun getQueryForView(viewMode: ViewMode): String? {
        return myState.viewQueries[viewMode.name]
    }

    /**
     * Set the current query for a view mode.
     *
     * If query is null or blank, removes the query for this view.
     * Otherwise, saves the query and adds it to history.
     *
     * @param viewMode The view mode to set the query for
     * @param query The query string (null or blank to clear)
     */
    fun setQueryForView(viewMode: ViewMode, query: String?) {
        if (query.isNullOrBlank()) {
            myState.viewQueries.remove(viewMode.name)
        } else {
            myState.viewQueries[viewMode.name] = query
            addToHistory(query)
        }
    }

    /**
     * Clear query for a view mode.
     *
     * @param viewMode The view mode to clear the query for
     */
    fun clearQueryForView(viewMode: ViewMode) {
        myState.viewQueries.remove(viewMode.name)
    }

    /**
     * Clear all queries for all view modes.
     *
     * Does not affect query history.
     */
    fun clearAllQueries() {
        myState.viewQueries.clear()
    }

    /**
     * Get query history (most recent first).
     *
     * Returns up to 50 most recent queries, ordered with newest first.
     *
     * @return List of query strings from history
     */
    fun getQueryHistory(): List<String> {
        return myState.queryHistory.toList()
    }

    /**
     * Add query to history.
     *
     * - Deduplicates: If query already exists, moves it to front
     * - Maintains max 50 entries with LRU eviction
     * - Trims whitespace before storing
     *
     * @param query The query string to add
     */
    private fun addToHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        // Remove if already exists (move to front)
        myState.queryHistory.remove(trimmed)

        // Add to front
        myState.queryHistory.add(0, trimmed)

        // Trim to 50 entries
        if (myState.queryHistory.size > MAX_HISTORY_SIZE) {
            myState.queryHistory = myState.queryHistory.take(MAX_HISTORY_SIZE).toMutableList()
        }
    }

    /**
     * Clear query history.
     *
     * Does not affect saved queries per view.
     */
    fun clearHistory() {
        myState.queryHistory.clear()
    }

    companion object {
        /**
         * Maximum number of queries to keep in history.
         */
        private const val MAX_HISTORY_SIZE = 50

        /**
         * Gets the QueryStateService instance for a project.
         *
         * @param project The project to get the service for
         * @return The QueryStateService instance
         */
        fun getInstance(project: Project): QueryStateService {
            return project.service<QueryStateService>()
        }
    }
}
