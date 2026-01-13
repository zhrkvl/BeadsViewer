package me.zkvl.beadsviewer.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.service.BeadsCommandService
import me.zkvl.beadsviewer.service.IssueService
import me.zkvl.beadsviewer.ui.components.EditableTextField
import me.zkvl.beadsviewer.ui.components.PriorityBadge
import me.zkvl.beadsviewer.ui.components.StatusBadge
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text

/**
 * Issue detail view component.
 *
 * Displays full issue details in a dedicated tab with editable fields.
 *
 * @param project The current project
 * @param initialIssue The issue to display (initial state)
 */
@Composable
fun IssueDetailView(
    project: Project,
    initialIssue: Issue
) {
    val issueService = remember { IssueService.getInstance(project) }

    // Subscribe to issue updates from service
    val issuesState by issueService.issuesState.collectAsState()

    // Get current version of the issue
    val currentIssue = remember(issuesState) {
        when (val state = issuesState) {
            is IssueService.IssuesState.Loaded ->
                state.issues.find { it.id == initialIssue.id } ?: initialIssue
            else -> initialIssue
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section
        item {
            IssueHeaderSection(currentIssue)
        }

        // Title Section (Editable)
        item {
            EditableTextField(
                project = project,
                label = "Title",
                value = currentIssue.title,
                issueId = currentIssue.id,
                fieldName = "title",
                multiline = false
            )
        }

        // Metadata Section
        item {
            MetadataSection(project, currentIssue)
        }

        // Description Section (Editable)
        item {
            EditableTextField(
                project = project,
                label = "Description",
                value = currentIssue.description,
                issueId = currentIssue.id,
                fieldName = "description",
                multiline = true
            )
        }

        // Design Notes Section (Editable)
        item {
            EditableTextField(
                project = project,
                label = "Design Notes",
                value = currentIssue.design ?: "",
                issueId = currentIssue.id,
                fieldName = "design",
                multiline = true
            )
        }

        // Acceptance Criteria Section (Editable)
        item {
            EditableTextField(
                project = project,
                label = "Acceptance Criteria",
                value = currentIssue.acceptanceCriteria ?: "",
                issueId = currentIssue.id,
                fieldName = "acceptance",
                multiline = true
            )
        }

        // Notes Section (Editable)
        item {
            EditableTextField(
                project = project,
                label = "Notes",
                value = currentIssue.notes ?: "",
                issueId = currentIssue.id,
                fieldName = "notes",
                multiline = true
            )
        }

        // Dependencies Section
        if (currentIssue.dependencies.isNotEmpty()) {
            item {
                DependenciesSection(currentIssue)
            }
        }

        // Comments Section (with add comment)
        item {
            CommentsSection(project, currentIssue)
        }

        // Labels Section
        if (currentIssue.labels.isNotEmpty()) {
            item {
                LabelsSection(currentIssue)
            }
        }

        // Additional Metadata
        item {
            AdditionalMetadataSection(currentIssue)
        }
    }
}

@Composable
private fun IssueHeaderSection(issue: Issue) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PriorityBadge(issue.priority)

        Text(
            text = issue.id,
            fontSize = 16.sp,
            color = androidx.compose.ui.graphics.Color(0xFF888888)
        )

        Text(
            text = "•",
            fontSize = 16.sp,
            color = androidx.compose.ui.graphics.Color(0xFF888888)
        )

        Text(
            text = issue.issueType.value,
            fontSize = 16.sp,
            color = androidx.compose.ui.graphics.Color(0xFF888888)
        )

        Spacer(Modifier.weight(1f))

        StatusBadge(issue.status.name)
    }
}

@Composable
private fun TitleSection(issue: Issue) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = issue.title,
            fontSize = 20.sp,
            color = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
        )
    }
}

@Composable
private fun MetadataSection(project: Project, issue: Issue) {
    val commandService = remember { BeadsCommandService.getInstance(project) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = androidx.compose.ui.graphics.Color(0x08FFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MetadataRow("Created by", issue.createdBy)
        MetadataRow(
            "Created at",
            issue.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).toString()
        )
        MetadataRow(
            "Updated at",
            issue.updatedAt.toLocalDateTime(TimeZone.currentSystemDefault()).toString()
        )

        // Editable Priority
        EditableMetadataRow(
            project = project,
            label = "Priority",
            value = "P${issue.priority}",
            issueId = issue.id,
            onSave = { newValue ->
                scope.launch {
                    val priority = newValue.removePrefix("P").toIntOrNull() ?: issue.priority
                    commandService.updatePriority(issue.id, priority)
                }
            }
        )

        // Editable Status
        EditableMetadataRow(
            project = project,
            label = "Status",
            value = issue.status.name.lowercase(),
            issueId = issue.id,
            onSave = { newValue ->
                scope.launch {
                    commandService.updateStatus(issue.id, newValue.lowercase())
                }
            }
        )

        // Editable Assignee
        EditableMetadataRow(
            project = project,
            label = "Assignee",
            value = issue.assignee ?: "(none)",
            issueId = issue.id,
            onSave = { newValue ->
                scope.launch {
                    commandService.updateIssue(issue.id, "assignee", newValue)
                }
            }
        )

        // Editable Estimated Minutes
        EditableMetadataRow(
            project = project,
            label = "Estimated",
            value = issue.estimatedMinutes?.toString() ?: "0",
            issueId = issue.id,
            onSave = { newValue ->
                scope.launch {
                    commandService.updateIssue(issue.id, "estimate", newValue)
                }
            }
        )

        issue.closedAt?.let {
            MetadataRow(
                "Closed at",
                it.toLocalDateTime(TimeZone.currentSystemDefault()).toString()
            )
        }

        issue.closeReason?.let {
            MetadataRow("Close reason", it)
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$label:",
            fontSize = 12.sp,
            color = androidx.compose.ui.graphics.Color(0xFF888888),
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = androidx.compose.ui.graphics.Color(0xFFCCCCCC)
        )
    }
}

@Composable
private fun FieldSection(label: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = androidx.compose.ui.graphics.Color(0xFF888888)
        )
        Text(
            text = content,
            fontSize = 13.sp,
            color = androidx.compose.ui.graphics.Color(0xFFCCCCCC),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = androidx.compose.ui.graphics.Color(0x08FFFFFF),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        )
    }
}

@Composable
private fun DependenciesSection(issue: Issue) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Dependencies (${issue.dependencies.size})",
            fontSize = 14.sp,
            color = androidx.compose.ui.graphics.Color(0xFF888888)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = androidx.compose.ui.graphics.Color(0x08FFFFFF),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            issue.dependencies.forEach { dependency ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dependency.type.value,
                        fontSize = 11.sp,
                        color = androidx.compose.ui.graphics.Color(0xFF888888),
                        modifier = Modifier.width(80.dp)
                    )
                    Text(
                        text = dependency.dependsOnId,
                        fontSize = 12.sp,
                        color = androidx.compose.ui.graphics.Color(0xFFCCCCCC)
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentsSection(project: Project, issue: Issue) {
    val commandService = remember { BeadsCommandService.getInstance(project) }
    val scope = rememberCoroutineScope()
    var isAddingComment by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Comments (${issue.comments.size})",
            fontSize = 14.sp,
            color = androidx.compose.ui.graphics.Color(0xFF888888)
        )

        // Existing comments
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            issue.comments.forEach { comment ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = androidx.compose.ui.graphics.Color(0x08FFFFFF),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = comment.author,
                            fontSize = 12.sp,
                            color = androidx.compose.ui.graphics.Color(0xFF888888)
                        )
                        Text(
                            text = "•",
                            fontSize = 11.sp,
                            color = androidx.compose.ui.graphics.Color(0xFF888888)
                        )
                        Text(
                            text = comment.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).toString(),
                            fontSize = 11.sp,
                            color = androidx.compose.ui.graphics.Color(0xFF888888)
                        )
                    }
                    Text(
                        text = comment.text,
                        fontSize = 12.sp,
                        color = androidx.compose.ui.graphics.Color(0xFFCCCCCC)
                    )
                }
            }
        }

        // Add comment section
        if (isAddingComment) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp)
                        .background(
                            androidx.compose.ui.graphics.Color(0x10FFFFFF),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    textStyle = TextStyle(
                        color = androidx.compose.ui.graphics.Color(0xFFCCCCCC),
                        fontSize = 13.sp
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DefaultButton(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                commandService.addComment(issue.id, newCommentText)
                                    .onSuccess {
                                        newCommentText = ""
                                        isAddingComment = false
                                    }
                                isSaving = false
                            }
                        },
                        enabled = !isSaving && newCommentText.isNotBlank()
                    ) {
                        Text(if (isSaving) "Adding..." else "Add Comment")
                    }

                    DefaultButton(
                        onClick = {
                            newCommentText = ""
                            isAddingComment = false
                        },
                        enabled = !isSaving
                    ) {
                        Text("Cancel")
                    }
                }
            }
        } else {
            DefaultButton(
                onClick = { isAddingComment = true }
            ) {
                Text("Add Comment")
            }
        }
    }
}

@Composable
private fun LabelsSection(issue: Issue) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Labels",
            fontSize = 14.sp,
            color = androidx.compose.ui.graphics.Color(0xFF888888)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            issue.labels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = androidx.compose.ui.graphics.Color(0xFFCCCCCC),
                    modifier = Modifier
                        .background(
                            color = androidx.compose.ui.graphics.Color(0x18FFFFFF),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun EditableMetadataRow(
    project: Project,
    label: String,
    value: String,
    issueId: String,
    onSave: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editValue by remember(value) { mutableStateOf(value) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            fontSize = 12.sp,
            color = androidx.compose.ui.graphics.Color(0xFF888888),
            modifier = Modifier.width(120.dp)
        )

        if (isEditing) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                BasicTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            androidx.compose.ui.graphics.Color(0x10FFFFFF),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(4.dp),
                    textStyle = TextStyle(
                        color = androidx.compose.ui.graphics.Color(0xFFCCCCCC),
                        fontSize = 12.sp
                    ),
                    singleLine = true
                )
                DefaultButton(
                    onClick = {
                        onSave(editValue)
                        isEditing = false
                    }
                ) {
                    Text("Save", fontSize = 11.sp)
                }
                DefaultButton(
                    onClick = {
                        editValue = value
                        isEditing = false
                    }
                ) {
                    Text("Cancel", fontSize = 11.sp)
                }
            }
        } else {
            Text(
                text = value,
                fontSize = 12.sp,
                color = androidx.compose.ui.graphics.Color(0xFFCCCCCC),
                modifier = Modifier
                    .clickable { isEditing = true }
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun AdditionalMetadataSection(issue: Issue) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = androidx.compose.ui.graphics.Color(0x08FFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        issue.externalRef?.let {
            MetadataRow("External ref", it)
        }

        issue.dueDate?.let {
            MetadataRow(
                "Due date",
                it.toLocalDateTime(TimeZone.currentSystemDefault()).toString()
            )
        }

        issue.sourceRepo?.let {
            MetadataRow("Source repo", it)
        }
    }
}
