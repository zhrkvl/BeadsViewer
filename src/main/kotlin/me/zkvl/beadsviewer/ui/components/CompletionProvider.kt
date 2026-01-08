package me.zkvl.beadsviewer.ui.components

import me.zkvl.beadsviewer.model.IssueType
import me.zkvl.beadsviewer.model.Status
import me.zkvl.beadsviewer.query.ast.QueryField

/**
 * Represents a completion suggestion for the query input field.
 *
 * @property text The text to insert when selected
 * @property displayText The text to show in the popup
 * @property typeText Right-aligned type information (e.g., "status", "keyword", "STRING")
 * @property tailText Additional information shown after the type (e.g., aliases)
 * @property isBold Whether to display the text in bold (for primary suggestions)
 */
data class CompletionSuggestion(
    val text: String,
    val displayText: String,
    val typeText: String?,
    val tailText: String?,
    val isBold: Boolean = false
)

/**
 * Provides intelligent completion suggestions for the query language.
 *
 * Supports:
 * - Field name completion with aliases
 * - Keyword completion (and, or, not, sort)
 * - Context-aware value completion (status values, priorities, issue types, dates)
 *
 * Uses existing QueryField enum and model classes (Status, IssueType) for suggestions.
 */
object CompletionProvider {
    /**
     * Get completion suggestions for the given text and cursor position.
     *
     * @param text The current query text
     * @param cursorPos The cursor position in the text
     * @return List of completion suggestions (limited to 10)
     */
    fun getCompletions(text: String, cursorPos: Int): List<CompletionSuggestion> {
        val suggestions = mutableListOf<CompletionSuggestion>()

        // Extract current word being typed
        val beforeCursor = text.substring(0, minOf(cursorPos, text.length))
        val wordStart = beforeCursor.lastIndexOfAny(charArrayOf(' ', ':', ',', '(', ')')) + 1
        val currentWord = beforeCursor.substring(wordStart).lowercase()

        // Empty word = no completions
        if (currentWord.isEmpty()) return emptyList()

        // Detect context: after colon = value completion, otherwise = field/keyword
        // Fixed: Check for last colon position instead of trimEnd().endsWith(":")
        val lastColonIndex = beforeCursor.lastIndexOf(':')
        val lastSpaceIndex = beforeCursor.lastIndexOf(' ')

        // After colon if:
        // 1. Colon exists
        // 2. Colon is after the last space (or no space exists)
        // 3. Between colon and cursor is only whitespace/word characters (no operators)
        val isAfterColon = lastColonIndex != -1 &&
                          lastColonIndex > lastSpaceIndex &&
                          beforeCursor.substring(lastColonIndex + 1).all {
                              it.isWhitespace() || it.isLetterOrDigit() || it == '-' || it == '_'
                          }

        if (isAfterColon) {
            // VALUE COMPLETION: Get field name, suggest appropriate values
            val fieldName = extractFieldBeforeColon(beforeCursor)
            suggestions.addAll(getValueCompletions(fieldName, currentWord))
        } else {
            // FIELD/KEYWORD COMPLETION
            suggestions.addAll(getFieldCompletions(currentWord))
            suggestions.addAll(getKeywordCompletions(currentWord))
        }

        return suggestions.take(10)  // Limit to 10 suggestions
    }

    /**
     * Get field name and alias completions.
     */
    private fun getFieldCompletions(prefix: String): List<CompletionSuggestion> {
        return QueryField.entries.flatMap { field ->
            val suggestions = mutableListOf<CompletionSuggestion>()

            // Main field name
            if (field.fieldName.startsWith(prefix)) {
                suggestions.add(
                    CompletionSuggestion(
                        text = field.fieldName,
                        displayText = field.fieldName,
                        typeText = field.type.toString(),
                        tailText = if (field.aliases.isNotEmpty())
                            " (${field.aliases.joinToString(", ")})"
                        else null,
                        isBold = true
                    )
                )
            }

            // Aliases
            field.aliases.forEach { alias ->
                if (alias.startsWith(prefix)) {
                    suggestions.add(
                        CompletionSuggestion(
                            text = alias,
                            displayText = alias,
                            typeText = "→ ${field.fieldName}",
                            tailText = " (alias)",
                            isBold = false
                        )
                    )
                }
            }

            suggestions
        }
    }

    /**
     * Get keyword completions (and, or, not, sort).
     */
    private fun getKeywordCompletions(prefix: String): List<CompletionSuggestion> {
        return listOf("and", "or", "not", "sort").mapNotNull { keyword ->
            if (keyword.startsWith(prefix)) {
                CompletionSuggestion(
                    text = keyword,
                    displayText = keyword,
                    typeText = "keyword",
                    tailText = null,
                    isBold = true
                )
            } else null
        }
    }

    /**
     * Get value completions based on the field type.
     * Context-aware: suggests appropriate values for each field.
     */
    private fun getValueCompletions(fieldName: String?, prefix: String): List<CompletionSuggestion> {
        val field = QueryField.fromString(fieldName ?: "") ?: return emptyList()

        return when (field) {
            QueryField.STATUS -> {
                // Status enum values (using serialization names)
                listOf("open", "in_progress", "blocked", "closed", "tombstone", "hooked")
                    .mapNotNull { statusName ->
                        if (statusName.startsWith(prefix)) {
                            CompletionSuggestion(
                                text = statusName,
                                displayText = statusName,
                                typeText = "status",
                                tailText = null
                            )
                        } else null
                    }
            }

            QueryField.PRIORITY -> {
                // Priority values 0-4
                (0..4).mapNotNull { pri ->
                    if (pri.toString().startsWith(prefix)) {
                        CompletionSuggestion(
                            text = pri.toString(),
                            displayText = pri.toString(),
                            typeText = "priority",
                            tailText = null
                        )
                    } else null
                }
            }

            QueryField.ISSUE_TYPE -> {
                // Issue type values
                listOf("bug", "feature", "task", "epic", "chore", "merge-request")
                    .mapNotNull { typeName ->
                        if (typeName.startsWith(prefix)) {
                            CompletionSuggestion(
                                text = typeName,
                                displayText = typeName,
                                typeText = "type",
                                tailText = null
                            )
                        } else null
                    }
            }

            QueryField.CREATED_AT, QueryField.UPDATED_AT, QueryField.DUE_DATE, QueryField.CLOSED_AT -> {
                // Relative date values
                listOf("today", "yesterday", "tomorrow", "this-week", "last-week", "next-week", "this-month")
                    .mapNotNull { date ->
                        if (date.startsWith(prefix)) {
                            CompletionSuggestion(
                                text = date,
                                displayText = date,
                                typeText = "relative date",
                                tailText = null
                            )
                        } else null
                    }
            }

            QueryField.ASSIGNEE -> {
                // Special values for assignee
                listOf("null", "unassigned").mapNotNull { value ->
                    if (value.startsWith(prefix)) {
                        CompletionSuggestion(
                            text = value,
                            displayText = value,
                            typeText = "special",
                            tailText = null
                        )
                    } else null
                }
            }

            else -> emptyList()
        }
    }

    /**
     * Extract the field name before the colon in a query.
     * Example: "status:op" → "status"
     * Example: "status: " → "status"
     */
    private fun extractFieldBeforeColon(text: String): String? {
        val lastColonIndex = text.lastIndexOf(':')
        if (lastColonIndex == -1) return null

        // Find the word before the colon
        val beforeColon = text.substring(0, lastColonIndex).trimEnd()
        val wordStart = beforeColon.lastIndexOfAny(charArrayOf(' ', '(', ')')) + 1
        val fieldName = beforeColon.substring(wordStart).trim().lowercase()

        return if (fieldName.isNotEmpty()) fieldName else null
    }
}
