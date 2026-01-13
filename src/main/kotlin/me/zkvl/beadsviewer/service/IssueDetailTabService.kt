package me.zkvl.beadsviewer.service

import androidx.compose.ui.awt.ComposePanel
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import me.zkvl.beadsviewer.Icons
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.state.ThemeStateService
import me.zkvl.beadsviewer.ui.theme.BeadsTheme
import me.zkvl.beadsviewer.ui.views.IssueDetailView
import org.jetbrains.jewel.bridge.JewelComposePanel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

/**
 * Project-level service managing issue detail tabs.
 *
 * This service:
 * - Creates dynamic tabs for individual issues
 * - Tracks open tabs to prevent duplicates
 * - Focuses existing tab if issue already open
 * - Handles tab closing and cleanup
 */
@Service(Service.Level.PROJECT)
class IssueDetailTabService(private val project: Project) : Disposable {
    private val logger = Logger.getInstance(IssueDetailTabService::class.java)

    // Track open tabs: issueId -> Content reference
    private val openTabs = mutableMapOf<String, Content>()

    /**
     * Opens or focuses a detail tab for the given issue.
     *
     * If a tab for this issue already exists, focuses it.
     * Otherwise, creates a new tab.
     *
     * @param issue The issue to display in the tab
     */
    fun openIssueDetailTab(issue: Issue) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("BeadsExplorer")
        if (toolWindow == null) {
            logger.error("BeadsExplorer tool window not found")
            return
        }

        val contentManager = toolWindow.contentManager

        // Check if tab already exists
        val existingContent = openTabs[issue.id]
        if (existingContent != null && contentManager.getIndexOfContent(existingContent) >= 0) {
            // Tab exists, just focus it
            contentManager.setSelectedContent(existingContent)
            logger.info("Focused existing tab for ${issue.id}")
            return
        }

        // Create new tab with Compose content using JewelComposePanel for proper theming
        val composePanel = JewelComposePanel {
            // Get theme service and observe theme mode (same pattern as BeadsToolWindow)
            val themeService = remember { ThemeStateService.getInstance(project) }
            val themeMode by themeService.themeModeFlow.collectAsState()

            // Wrap with BeadsTheme to provide theme context
            BeadsTheme(themeMode = themeMode) {
                IssueDetailView(project = project, initialIssue = issue)
            }
        }

        val content = contentManager.factory.createContent(
            composePanel,
            "Issue: ${issue.id}",
            true  // closeable
        )

        // Set icon (same as main tool window)
        content.icon = Icons.PluginIcon

        // Add close listener
        content.setDisposer {
            openTabs.remove(issue.id)
            logger.info("Closed tab for ${issue.id}")
        }

        contentManager.addContent(content)
        contentManager.setSelectedContent(content)

        openTabs[issue.id] = content
        logger.info("Opened new tab for ${issue.id}")
    }

    /**
     * Closes the tab for a specific issue (if open).
     *
     * @param issueId The issue ID whose tab to close
     */
    fun closeIssueDetailTab(issueId: String) {
        val content = openTabs[issueId] ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("BeadsExplorer") ?: return
        toolWindow.contentManager.removeContent(content, true)
    }

    /**
     * Closes all issue detail tabs.
     */
    fun closeAllDetailTabs() {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("BeadsExplorer") ?: return
        val contentManager = toolWindow.contentManager
        openTabs.values.forEach { content ->
            contentManager.removeContent(content, true)
        }
    }

    override fun dispose() {
        openTabs.clear()
    }

    companion object {
        /**
         * Get the IssueDetailTabService instance for a project.
         *
         * @param project The project to get the service for
         * @return The project's IssueDetailTabService instance
         */
        fun getInstance(project: Project): IssueDetailTabService = project.service()
    }
}
