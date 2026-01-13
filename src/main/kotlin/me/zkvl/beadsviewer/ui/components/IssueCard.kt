package me.zkvl.beadsviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.ui.theme.BeadsTheme
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.Text

/**
 * Reusable issue card component.
 * Displays an issue with its ID, title, status, priority, and expandable details.
 */
@Composable
fun IssueCard(
    issue: Issue,
    modifier: Modifier = Modifier,
    expandable: Boolean = true,
    initiallyExpanded: Boolean = false,
    onOpenDetailTab: ((Issue) -> Unit)? = null
) {
    val colors = BeadsTheme.colors
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (expandable) Modifier.clickable { expanded = !expanded }
                else Modifier
            )
            .background(
                color = colors.surfaceHover,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        // Title row with ID, priority, and status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority badge
            PriorityBadge(issue.priority)

            // Issue ID
            Text(
                issue.id,
                fontSize = 12.sp,
                color = colors.onSurfaceDisabled
            )

            // Title
            Text(
                issue.title,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                color = colors.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis
            )

            // Status indicator
            StatusBadge(issue.status.name)
        }

        // Expanded details
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))

            // Type and creator
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Type: ${issue.issueType.value}",
                    fontSize = 11.sp,
                    color = colors.onSurfaceDisabled
                )
                Text(
                    "By: ${issue.createdBy}",
                    fontSize = 11.sp,
                    color = colors.onSurfaceDisabled
                )
            }

            // Description
            if (issue.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    issue.description,
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Dependencies with Open link
            if (issue.dependencies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Dependencies: ${issue.dependencies.size}",
                        fontSize = 11.sp,
                        color = colors.onSurfaceDisabled
                    )
                    if (onOpenDetailTab != null) {
                        Text("•", fontSize = 11.sp, color = colors.onSurfaceDisabled)
                        Link(
                            text = "Open",
                            onClick = { onOpenDetailTab(issue) }
                        )
                    }
                }
            }
        }
    }
}
