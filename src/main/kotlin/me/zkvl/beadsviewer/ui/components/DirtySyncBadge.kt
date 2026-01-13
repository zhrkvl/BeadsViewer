package me.zkvl.beadsviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.ui.component.Text

/**
 * Badge indicating an issue has unsaved changes (not yet synced to git).
 *
 * This badge appears when:
 * - Issue was modified via bd CLI
 * - Changes are in SQLite database but not yet exported to issues.jsonl
 * - User hasn't run `bd sync` yet
 *
 * Visual style: Amber/yellow color to indicate "warning" state
 */
@Composable
fun DirtySyncBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(
                color = Color(0xFFFF9800).copy(alpha = 0.15f),  // Amber background
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Warning indicator (small filled circle)
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color(0xFFFF9800), shape = androidx.compose.foundation.shape.CircleShape)
        )

        Text(
            "Local",
            fontSize = 11.sp,
            color = Color(0xFFFF9800)  // Amber text
        )
    }
}
