package me.zkvl.beadsviewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import me.zkvl.beadsviewer.service.IssueService
import me.zkvl.beadsviewer.state.ThemeStateService
import me.zkvl.beadsviewer.state.ViewModeStateService
import me.zkvl.beadsviewer.ui.ViewRouter
import me.zkvl.beadsviewer.ui.components.ViewModeToolbar
import me.zkvl.beadsviewer.ui.theme.BeadsTheme
import me.zkvl.beadsviewer.ui.views.NoBeadsFoundView
import org.jetbrains.jewel.bridge.addComposeTab

class BeadsToolWindowFactory : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.addComposeTab("Issues", focusOnClickInside = true) {
            BeadsViewerContent(project)
        }
    }
}

@Composable
private fun BeadsViewerContent(project: Project) {
    // Get theme service and observe theme mode
    val themeService = remember { ThemeStateService.getInstance(project) }
    val themeMode by themeService.themeModeFlow.collectAsState()

    // Wrap entire content with BeadsTheme
    BeadsTheme(themeMode = themeMode) {
        // Get issue service and observe its state
        val issueService = remember { IssueService.getInstance(project) }
        val issuesState by issueService.issuesState.collectAsState()

        // Show no beads found view if there's an error
        if (issuesState is IssueService.IssuesState.Error) {
            NoBeadsFoundView()
            return@BeadsTheme
        }

        // Load persisted view mode
        val stateService = remember { ViewModeStateService.getInstance(project) }
        var currentViewMode by remember { mutableStateOf(stateService.getCurrentViewMode()) }

        Column(modifier = Modifier.fillMaxSize()) {
            // Toolbar at the top
            ViewModeToolbar(
                project = project,
                currentMode = currentViewMode,
                onModeChange = { mode ->
                    currentViewMode = mode
                    stateService.setCurrentViewMode(mode)
                }
            )

            // View content based on selected mode
            ViewRouter(
                project = project,
                viewMode = currentViewMode
            )
        }
    }
}
