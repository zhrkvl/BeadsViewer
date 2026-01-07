package me.zkvl.beadsviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import me.zkvl.beadsviewer.ui.IssueListView
import org.jetbrains.jewel.bridge.addComposeTab

class BeadsToolWindowFactory : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.addComposeTab("Issues", focusOnClickInside = true) {
            IssueListView(project)
        }
    }
}
