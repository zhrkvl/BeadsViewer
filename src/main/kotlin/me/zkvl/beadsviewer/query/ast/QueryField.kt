package me.zkvl.beadsviewer.query.ast

/**
 * Represents all queryable fields in the Issue model.
 *
 * Each field maps directly to an Issue property and includes:
 * - Field name (for parsing)
 * - Field type (for type checking and coercion)
 * - Aliases (alternative names for user convenience)
 *
 * Example queries:
 * - `status:open` → STATUS field
 * - `pri:0` → PRIORITY field (using alias)
 * - `type:bug` → ISSUE_TYPE field (using alias)
 */
enum class QueryField(
    val fieldName: String,
    val type: FieldType,
    val aliases: Set<String> = emptySet()
) {
    // ===== Core Fields =====

    /** Issue ID (e.g., "BeadsViewer-zw5") */
    ID("id", FieldType.STRING),

    /** Issue title/summary */
    TITLE("title", FieldType.STRING),

    /** Issue description (may contain markdown) */
    DESCRIPTION("description", FieldType.STRING, setOf("desc")),

    /** Issue status (OPEN, IN_PROGRESS, BLOCKED, CLOSED, TOMBSTONE, HOOKED) */
    STATUS("status", FieldType.ENUM_STATUS),

    /** Priority level (0-4, where 0 is highest) */
    PRIORITY("priority", FieldType.INTEGER, setOf("pri")),

    /** Issue type (Bug, Feature, Task, Epic, Chore, MergeRequest, or custom) */
    ISSUE_TYPE("type", FieldType.ENUM_ISSUE_TYPE, setOf("issue_type", "issuetype")),

    // ===== Optional Metadata =====

    /** Assigned user (nullable) */
    ASSIGNEE("assignee", FieldType.STRING_NULLABLE),

    /** Estimated time in minutes (nullable) */
    ESTIMATED_MINUTES("estimated", FieldType.INTEGER_NULLABLE, setOf("estimate", "estimated_minutes")),

    /** External reference (e.g., URL, ticket ID) (nullable) */
    EXTERNAL_REF("external", FieldType.STRING_NULLABLE, setOf("external_ref", "ref")),

    /** Source repository (nullable) */
    SOURCE_REPO("repo", FieldType.STRING_NULLABLE, setOf("source_repo")),

    // ===== Timestamps =====

    /** Creation timestamp */
    CREATED_AT("created", FieldType.TIMESTAMP, setOf("created_at")),

    /** Creator username */
    CREATED_BY("creator", FieldType.STRING, setOf("created_by")),

    /** Last update timestamp */
    UPDATED_AT("updated", FieldType.TIMESTAMP, setOf("updated_at")),

    /** Due date (nullable) */
    DUE_DATE("due", FieldType.TIMESTAMP_NULLABLE, setOf("due_date")),

    /** Close timestamp (nullable) */
    CLOSED_AT("closed", FieldType.TIMESTAMP_NULLABLE, setOf("closed_at")),

    // ===== Collections =====

    /** Labels/tags (multi-value field) */
    LABELS("label", FieldType.STRING_LIST, setOf("labels", "tag", "tags")),

    // ===== Optional Text Fields =====

    /** Design notes (nullable) */
    DESIGN("design", FieldType.STRING_NULLABLE),

    /** Acceptance criteria (nullable) */
    ACCEPTANCE_CRITERIA("acceptance", FieldType.STRING_NULLABLE, setOf("acceptance_criteria")),

    /** Additional notes (nullable) */
    NOTES("notes", FieldType.STRING_NULLABLE);

    companion object {
        /**
         * Parse field from string, supporting aliases.
         *
         * Performs case-insensitive matching against field names and aliases.
         *
         * @param name The field name to parse
         * @return The corresponding QueryField, or null if not found
         *
         * Examples:
         * - "status" → STATUS
         * - "pri" → PRIORITY
         * - "type" → ISSUE_TYPE
         * - "labels" → LABELS
         */
        fun fromString(name: String): QueryField? {
            val normalized = name.lowercase()
            return entries.find { field ->
                field.fieldName == normalized ||
                field.aliases.contains(normalized)
            }
        }

        /**
         * Fields that support text search (for ContainsNode).
         *
         * These fields are searched when no specific field is provided
         * in a text search query (e.g., just typing "authentication"
         * searches all these fields).
         */
        val TEXT_SEARCHABLE = setOf(
            TITLE,
            DESCRIPTION,
            DESIGN,
            ACCEPTANCE_CRITERIA,
            NOTES
        )

        /**
         * Get fuzzy match suggestions for a field name.
         *
         * Used for error messages like "Unknown field 'priorit'. Did you mean 'priority'?"
         *
         * @param name The field name to find suggestions for
         * @param maxSuggestions Maximum number of suggestions to return
         * @return List of suggested field names
         */
        fun getSuggestions(name: String, maxSuggestions: Int = 3): List<String> {
            val normalized = name.lowercase()

            // Exact prefix matches
            val prefixMatches = entries.filter { field ->
                field.fieldName.startsWith(normalized) ||
                field.aliases.any { it.startsWith(normalized) }
            }.map { it.fieldName }

            if (prefixMatches.size >= maxSuggestions) {
                return prefixMatches.take(maxSuggestions)
            }

            // Levenshtein distance matches (simple substring matching)
            val substringMatches = entries.filter { field ->
                field.fieldName.contains(normalized) ||
                field.aliases.any { it.contains(normalized) }
            }.map { it.fieldName }

            return (prefixMatches + substringMatches).distinct().take(maxSuggestions)
        }
    }
}
