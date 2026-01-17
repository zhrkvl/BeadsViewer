package me.zkvl.beadsviewer.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.zkvl.beadsviewer.model.DataSource
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.parser.IssueRepository
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Project-level service managing Beads issues with reactive state.
 *
 * This service:
 * - Maintains a single IssueRepository per project
 * - Exposes reactive state via StateFlow for UI consumption
 * - Handles file watching notifications from FileWatcherService
 * - Provides cache statistics and manual refresh capabilities
 *
 * The service is automatically created when the project opens and disposed when it closes.
 */
@Service(Service.Level.PROJECT)
class IssueService(private val project: Project) : Disposable {
    private val logger = Logger.getInstance(IssueService::class.java)
    private val repository = IssueRepository()

    // Coroutine scope tied to service lifecycle
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Reactive state for UI consumption
    private val _issuesState = MutableStateFlow<IssuesState>(IssuesState.Loading)
    val issuesState: StateFlow<IssuesState> = _issuesState.asStateFlow()

    // Debounce mechanism for rapid file changes
    private var reloadJob: Job? = null

    /**
     * Sealed class representing the state of issues loading.
     */
    sealed class IssuesState {
        /** Initial loading state or reload in progress. */
        object Loading : IssuesState()

        /** Successfully loaded issues with metadata. */
        data class Loaded(
            val issues: List<Issue>,
            val dirtyIssueIds: Set<String>,
            val source: DataSource,
            val timestamp: Long
        ) : IssuesState()

        /** Error occurred during loading. */
        data class Error(val message: String) : IssuesState()
    }

    /**
     * Cache statistics for monitoring and UI display.
     */
    data class CacheStatistics(
        val isCached: Boolean,
        val issueCount: Int?,
        val cacheAgeMs: Long?,
        val lastRefreshTimestamp: Long?
    )

    init {
        logger.info("IssueService initialized for project: ${project.name}")
        // Initial load
        loadIssues()
    }

    /**
     * Get the path to .beads directory for this project.
     *
     * @return Path to .beads directory or null if project path not available
     */
    fun getBeadsDir(): Path? {
        val basePath = project.basePath ?: return null
        return Paths.get(basePath, ".beads")
    }

    /**
     * Load/reload issues from disk.
     * This method is debounced to handle rapid file changes.
     *
     * Uses SQLite-first strategy: tries database, falls back to JSONL.
     *
     * @param debounceMs Milliseconds to wait before loading (0 for immediate)
     */
    fun loadIssues(debounceMs: Long = 0) {
        // Cancel any pending reload
        reloadJob?.cancel()
        reloadJob = scope.launch {
            if (debounceMs > 0) {
                delay(debounceMs)
            }

            val beadsDir = getBeadsDir()
            if (beadsDir == null) {
                _issuesState.value = IssuesState.Error("Project path not found")
                logger.warn("Cannot load issues: project basePath is null")
                return@launch
            }

            _issuesState.value = IssuesState.Loading

            repository.loadIssues(beadsDir)
                .onSuccess { result ->
                    _issuesState.value = IssuesState.Loaded(
                        issues = result.issues,
                        dirtyIssueIds = result.dirtyIssueIds,
                        source = result.source,
                        timestamp = result.timestamp
                    )
                    logger.info("Loaded ${result.issues.size} issues from ${result.source} (${result.dirtyIssueIds.size} dirty) for project: ${project.name}")
                }
                .onFailure { error ->
                    _issuesState.value = IssuesState.Error(error.message ?: "Unknown error")
                    // Only log as info if file not found (normal for projects without beads)
                    if (error is me.zkvl.beadsviewer.parser.ParseException.FileNotFound) {
                        logger.info("No beads directory found for project: ${project.name}")
                    } else {
                        logger.error("Failed to load issues for project: ${project.name}", error)
                    }
                }
        }
    }

    /**
     * Force cache invalidation and reload.
     * Use this when you need to bypass the cache and reload from disk.
     */
    fun refresh() {
        logger.info("Manual refresh requested for project: ${project.name}")
        repository.invalidateCache()
        loadIssues()
    }

    /**
     * Get cache statistics for monitoring and UI display.
     *
     * @return Cache statistics including age, issue count, etc.
     */
    fun getCacheStats(): CacheStatistics {
        val stats = repository.getCacheStats()
        return CacheStatistics(
            isCached = stats["cached"] as? Boolean ?: false,
            issueCount = stats["issueCount"] as? Int,
            cacheAgeMs = stats["ageMs"] as? Long,
            lastRefreshTimestamp = stats["timestamp"] as? Long
        )
    }

    /**
     * Check if any issues in the repository have labels.
     * Used to conditionally show/hide label-based views like Actionable.
     *
     * @return true if at least one issue has labels, false otherwise
     */
    fun hasLabels(): Boolean {
        return when (val state = issuesState.value) {
            is IssuesState.Loaded -> state.issues.any { it.labels.isNotEmpty() }
            else -> false
        }
    }

    /**
     * Called when file watcher detects changes to Beads files.
     * Uses configurable debounce to handle rapid changes.
     *
     * @param debounceMs Milliseconds to debounce (default 300ms for JSONL, 100ms for SQLite)
     */
    fun onFileChanged(debounceMs: Long = 300) {
        logger.info("File change detected for project: ${project.name}, scheduling reload with ${debounceMs}ms debounce")
        loadIssues(debounceMs = debounceMs)
    }

    override fun dispose() {
        logger.info("IssueService disposing for project: ${project.name}")
        scope.cancel()
        repository.invalidateCache()
    }

    companion object {
        /**
         * Get the IssueService instance for a project.
         *
         * @param project The project to get the service for
         * @return The project's IssueService instance
         */
        fun getInstance(project: Project): IssueService = project.service()
    }
}
