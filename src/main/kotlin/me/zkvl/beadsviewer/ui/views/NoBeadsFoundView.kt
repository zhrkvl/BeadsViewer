package me.zkvl.beadsviewer.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.zkvl.beadsviewer.Icons
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.painter.rememberResourcePainterProvider

/**
 * View displayed when no .beads directory is found in the project.
 * Shows a centered banner with the plugin icon and helpful instructions.
 */
@Composable
fun NoBeadsFoundView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Plugin logo banner
            val painterProvider = rememberResourcePainterProvider(
                "/META-INF/pluginIcon.svg",
                Icons::class.java
            )
            val painter = painterProvider.getPainter().value
            Image(
                painter = painter,
                contentDescription = "Beads Logo",
                modifier = Modifier.size(120.dp)
            )

            // Informative text
            Text(
                text = "I searched all over the project, but couldn't find any .beads directories.",
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 500.dp)
            )

            // Instructions with link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.widthIn(max = 600.dp)
            ) {
                Text(
                    text = "You can start from ",
                    textAlign = TextAlign.Center
                )
                Link(
                    text = "Beads GitHub package",
                    onClick = {
                        // Open browser to GitHub
                        java.awt.Desktop.getDesktop().browse(
                            java.net.URI("https://github.com/steveyegge/beads")
                        )
                    }
                )
                Text(
                    text = ", install it and then do ",
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "`bd init`",
                    textAlign = TextAlign.Center
                )
                Text(
                    text = " on your project.",
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
