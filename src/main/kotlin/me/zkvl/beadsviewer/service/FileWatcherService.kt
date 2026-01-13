package me.zkvl.beadsviewer.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

/**
 * Listens to VFS events for Beads files and notifies IssueService.
 *
 * Watches:
 * - .beads/beads.db: SQLite database (primary source)
 * - .beads/beads.db-wal: Write-Ahead Log (indicates DB writes)
 * - .beads/issues.jsonl: JSONL file (fallback source)
 *
 * This class is registered as a project-level listener in plugin.xml.
 * When a relevant file event is detected, it notifies the IssueService
 * to reload issues with appropriate debounce delays:
 * - SQLite changes: 100ms debounce (rapid writes from bd daemon)
 * - JSONL changes: 300ms debounce (manual edits)
 */
@Service(Service.Level.PROJECT)
class FileWatcherService(private val project: Project) : BulkFileListener {
    private val logger = Logger.getInstance(FileWatcherService::class.java)

    // Debounce delays
    private val SQLITE_DEBOUNCE_MS = 100L  // Fast for real-time updates
    private val JSONL_DEBOUNCE_MS = 300L   // Slower for manual edits

    /**
     * Called after a batch of VFS events have been processed.
     * We filter to only handle events for our issues.jsonl file.
     *
     * @param events List of file system events
     */
    override fun after(events: List<VFileEvent>) {
        events.forEach { event ->
            if (isRelevantEvent(event)) {
                handleFileEvent(event)
            }
        }
    }

    /**
     * Check if this event is for a Beads file we care about.
     *
     * @param event The VFS event to check
     * @return true if this event should be processed, false otherwise
     */
    private fun isRelevantEvent(event: VFileEvent): Boolean {
        val file = event.file ?: return false

        val path = file.path
        val projectPath = project.basePath ?: return false
        val beadsDir = "$projectPath/.beads"

        // Check for any of our watched files
        return path == "$beadsDir/beads.db" ||
               path == "$beadsDir/beads.db-wal" ||
               path == "$beadsDir/issues.jsonl"
    }

    /**
     * Determine which file type triggered the event.
     *
     * @param event The VFS event
     * @return The file type (sqlite, jsonl, or unknown)
     */
    private fun getFileType(event: VFileEvent): String {
        val file = event.file ?: return "unknown"
        val path = file.path

        return when {
            path.endsWith("/beads.db") -> "sqlite"
            path.endsWith("/beads.db-wal") -> "sqlite-wal"
            path.endsWith("/issues.jsonl") -> "jsonl"
            else -> "unknown"
        }
    }

    /**
     * Handle different types of file events with appropriate debounce.
     *
     * @param event The file event to handle
     */
    private fun handleFileEvent(event: VFileEvent) {
        val issueService = IssueService.getInstance(project)
        val fileType = getFileType(event)

        // Determine debounce delay based on file type
        // SQLite changes use short debounce for real-time updates
        // JSONL changes use longer debounce for manual edits
        val debounceMs = when (fileType) {
            "sqlite", "sqlite-wal" -> SQLITE_DEBOUNCE_MS
            "jsonl" -> JSONL_DEBOUNCE_MS
            else -> 300L
        }

        when (event) {
            is VFileContentChangeEvent -> {
                logger.info("Beads $fileType file content changed in project: ${project.name}")
                issueService.onFileChanged(debounceMs)
            }
            is VFileCreateEvent -> {
                logger.info("Beads $fileType file created in project: ${project.name}")
                issueService.onFileChanged(debounceMs)
            }
            is VFileDeleteEvent -> {
                logger.info("Beads $fileType file deleted in project: ${project.name}")
                // Still notify service so it can show error state or fallback
                issueService.onFileChanged(debounceMs)
            }
        }
    }

    companion object {
        /**
         * Get the FileWatcherService instance for a project.
         *
         * @param project The project to get the service for
         * @return The project's FileWatcherService instance
         */
        fun getInstance(project: Project): FileWatcherService = project.service()
    }
}
