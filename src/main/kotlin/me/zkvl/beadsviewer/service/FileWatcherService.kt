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
 * Listens to VFS events for .beads/issues.jsonl and notifies IssueService.
 *
 * This class is registered as a project-level listener in plugin.xml.
 * It filters events to only process changes to the issues.jsonl file
 * for this specific project.
 *
 * When a relevant file event is detected (content change, creation, deletion),
 * it notifies the IssueService to reload the issues.
 */
@Service(Service.Level.PROJECT)
class FileWatcherService(private val project: Project) : BulkFileListener {
    private val logger = Logger.getInstance(FileWatcherService::class.java)

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
     * Check if this event is for our issues.jsonl file.
     *
     * @param event The VFS event to check
     * @return true if this event should be processed, false otherwise
     */
    private fun isRelevantEvent(event: VFileEvent): Boolean {
        val file = event.file ?: return false

        // Check if path ends with .beads/issues.jsonl
        val path = file.path
        val projectPath = project.basePath ?: return false
        val targetPath = "$projectPath/.beads/issues.jsonl"

        return path == targetPath
    }

    /**
     * Handle different types of file events.
     *
     * @param event The file event to handle
     */
    private fun handleFileEvent(event: VFileEvent) {
        val issueService = IssueService.getInstance(project)

        when (event) {
            is VFileContentChangeEvent -> {
                logger.info("issues.jsonl content changed in project: ${project.name}")
                issueService.onFileChanged()
            }
            is VFileCreateEvent -> {
                logger.info("issues.jsonl created in project: ${project.name}")
                issueService.onFileChanged()
            }
            is VFileDeleteEvent -> {
                logger.info("issues.jsonl deleted in project: ${project.name}")
                // Still notify service so it can show error state
                issueService.onFileChanged()
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
