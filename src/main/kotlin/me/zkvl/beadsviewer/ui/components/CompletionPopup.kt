package me.zkvl.beadsviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.jewel.ui.component.Text

/**
 * Completion popup for the query input field.
 * Displays a list of suggestions below the text field.
 *
 * Features:
 * - Shows up to 10 suggestions at a time
 * - Highlights selected suggestion
 * - Click to accept suggestion
 * - Dismiss on Escape or click away
 *
 * @param suggestions List of completion suggestions to display
 * @param selectedIndex Index of the currently selected suggestion
 * @param onSelect Callback when a suggestion is clicked
 * @param onDismiss Callback when the popup should be dismissed
 * @param modifier Modifier for the popup container
 */
@Composable
fun CompletionPopup(
    suggestions: List<CompletionSuggestion>,
    selectedIndex: Int = 0,
    onSelect: (CompletionSuggestion) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    // Colors matching ViewModeToolbar theme
    val bgColor = Color(0xF02B2B2B)  // Solid dark gray background for readability
    val borderColor = Color(0xFF3C3F41)  // Solid border for clear separation
    val selectedColor = Color(0xFF5C9FE5).copy(alpha = 0.3f)  // Blue selection (matches buttons)

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false)
    ) {
        Box(
            modifier = modifier
                .width(400.dp)
                .heightIn(max = 300.dp)
                .background(
                    color = bgColor,  // Matches ViewModeToolbar text field background
                    shape = RoundedCornerShape(4.dp)
                )
                .border(
                    width = 1.dp,
                    color = borderColor,  // Matches ViewModeToolbar text field border
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            LazyColumn {
                itemsIndexed(suggestions) { index, suggestion ->
                    CompletionItem(
                        suggestion = suggestion,
                        isSelected = index == selectedIndex,
                        onClick = { onSelect(suggestion) },
                        selectedColor = selectedColor
                    )
                }
            }
        }
    }
}

/**
 * Individual completion item in the popup list.
 *
 * Layout:
 * [displayText (bold/normal)] [padding] [typeText (gray)] [tailText (darker gray)]
 *
 * @param suggestion The completion suggestion to display
 * @param isSelected Whether this item is currently selected
 * @param onClick Callback when the item is clicked
 */
@Composable
private fun CompletionItem(
    suggestion: CompletionSuggestion,
    isSelected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color
) {
    // Text colors matching ViewModeToolbar
    val primaryTextColor = Color(0xFFCCCCCC)  // Standard text (matches ViewModeToolbar)
    val secondaryTextColor = Color(0xFFAAAAAA)  // Dimmer text
    val typeTextColor = Color(0xFF888888)  // Type info (medium gray)
    val tailTextColor = Color(0xFF666666)  // Tail text (darker gray)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) selectedColor  // Blue selection highlight
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Main text (left-aligned, takes available space)
        Text(
            text = suggestion.displayText,
            fontSize = 12.sp,
            color = if (suggestion.isBold) primaryTextColor else secondaryTextColor,
            modifier = Modifier.weight(1f)
        )

        // Type text (right-aligned, gray)
        suggestion.typeText?.let { typeText ->
            Text(
                text = typeText,
                fontSize = 11.sp,
                color = typeTextColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Tail text (additional info, darker gray)
        suggestion.tailText?.let { tailText ->
            Text(
                text = tailText,
                fontSize = 11.sp,
                color = tailTextColor,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
