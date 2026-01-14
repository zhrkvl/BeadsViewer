package me.zkvl.beadsviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import me.zkvl.beadsviewer.service.BeadsCommandService
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text

/**
 * Editable text field component with inline editing capability.
 *
 * Displays a value that can be clicked to edit. Shows Save/Cancel buttons when editing.
 *
 * @param project The current project
 * @param label The field label
 * @param value The current value
 * @param issueId The issue ID to update
 * @param fieldName The field name for the bd command
 * @param modifier Modifier for the component
 * @param multiline Whether this is a multi-line field
 */
@Composable
fun EditableTextField(
    project: Project,
    label: String,
    value: String,
    issueId: String,
    fieldName: String,
    modifier: Modifier = Modifier,
    multiline: Boolean = false
) {
    val commandService = remember { BeadsCommandService.getInstance(project) }
    var isEditing by remember { mutableStateOf(false) }
    var editValue by remember(value) { mutableStateOf(value) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF888888)
        )

        if (isEditing) {
            // Edit mode
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (multiline) {
                    BasicTextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 400.dp)
                            .background(
                                Color(0x10FFFFFF),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(8.dp),
                        textStyle = TextStyle(
                            color = Color(0xFFCCCCCC),
                            fontSize = 13.sp
                        )
                    )
                } else {
                    BasicTextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0x10FFFFFF),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(8.dp),
                        textStyle = TextStyle(
                            color = Color(0xFFCCCCCC),
                            fontSize = 13.sp
                        ),
                        singleLine = true
                    )
                }

                // Save/Cancel buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DefaultButton(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                commandService.updateIssue(issueId, fieldName, editValue)
                                    .onSuccess {
                                        isEditing = false
                                        Notifications.Bus.notify(
                                            Notification(
                                                "Beads",
                                                "Issue Updated",
                                                "Successfully updated $fieldName",
                                                NotificationType.INFORMATION
                                            ),
                                            project
                                        )
                                    }
                                    .onFailure { error ->
                                        Notifications.Bus.notify(
                                            Notification(
                                                "Beads",
                                                "Update Failed",
                                                error.message ?: "Unknown error",
                                                NotificationType.ERROR
                                            ),
                                            project
                                        )
                                    }
                                isSaving = false
                            }
                        },
                        enabled = !isSaving && editValue != value
                    ) {
                        Text(if (isSaving) "Saving..." else "Save")
                    }

                    DefaultButton(
                        onClick = {
                            editValue = value
                            isEditing = false
                        },
                        enabled = !isSaving
                    ) {
                        Text("Cancel")
                    }
                }
            }
        } else {
            // Display mode
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { isEditing = true })
                    }
                    .background(
                        Color(0x08FFFFFF),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = if (value.isBlank()) "(Click to edit)" else value,
                        fontSize = 13.sp,
                        color = if (value.isBlank()) Color(0x88CCCCCC) else Color(0xFFCCCCCC)
                    )
                }
            }
        }
    }
}
