package me.zkvl.beadsviewer

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.jewel.bridge.addComposeTab
import org.jetbrains.jewel.ui.component.Text

class BeadsToolWindowFactory : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.addComposeTab("Beads Explorer", focusOnClickInside = true) {
            BeadsExplorerContent()
        }
    }
}

@Composable
private fun BeadsExplorerContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Main title
        Text("Beads Explorer")

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text("Git-backed issue tracker for AI workflows")

        Spacer(modifier = Modifier.height(24.dp))

        // Information section
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("• Open a project with .beads/ directory to get started")
            Text("• See references/ folder for example Beads projects")
        }
    }
}
