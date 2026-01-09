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

        /** Successfully loaded issues. */
        data class Loaded(val issues: List<Issue>, val timestamp: Long) : IssuesState()

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
     * Get the path to issues.jsonl for this project.
     *
     * @return Path to issues.jsonl or null if project path not available
     */
    fun getIssuesFilePath(): Path? {
        val basePath = project.basePath ?: return null
        return Paths.get(basePath, ".beads", "issues.jsonl")
    }

    /**
     * Load/reload issues from disk.
     * This method is debounced to handle rapid file changes.
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

            val file = getIssuesFilePath()
            if (file == null) {
                _issuesState.value = IssuesState.Error("Project path not found")
                logger.warn("Cannot load issues: project basePath is null")
                return@launch
            }

            _issuesState.value = IssuesState.Loading

            repository.loadIssues(file)
                .onSuccess { issues ->
                    _issuesState.value = IssuesState.Loaded(issues, System.currentTimeMillis())
                    logger.info("Loaded ${issues.size} issues for project: ${project.name}")
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
     * Called when file watcher detects changes to issues.jsonl.
     * Uses 300ms debounce to handle rapid edits (e.g., save + auto-format).
     */
    fun onFileChanged() {
        logger.info("File change detected for project: ${project.name}, scheduling reload")
        loadIssues(debounceMs = 300)
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
