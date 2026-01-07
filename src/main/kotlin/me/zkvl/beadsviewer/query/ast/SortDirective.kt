package me.zkvl.beadsviewer.query.ast

/**
 * Represents a sort directive: field + direction.
 *
 * Sort directives are used in the `sort by:` clause of queries
 * to specify how results should be ordered.
 *
 * Examples:
 * - `sort by: priority asc` → SortDirective(PRIORITY, ASC)
 * - `sort by: updated desc` → SortDirective(UPDATED_AT, DESC)
 * - `sort by: priority asc, updated desc` → [SortDirective(PRIORITY, ASC), SortDirective(UPDATED_AT, DESC)]
 *
 * @param field The field to sort by
 * @param direction The sort direction (ascending or descending)
 */
data class SortDirective(
    val field: QueryField,
    val direction: SortDirection
)

/**
 * Sort direction enumeration.
 *
 * - ASC: Ascending order (0→9, A→Z, oldest→newest)
 * - DESC: Descending order (9→0, Z→A, newest→oldest)
 */
enum class SortDirection {
    /** Ascending order */
    ASC,

    /** Descending order */
    DESC;

    companion object {
        /**
         * Parse sort direction from string.
         *
         * Supports common variations:
         * - "asc", "ascending" → ASC
         * - "desc", "descending" → DESC
         *
         * Case-insensitive.
         *
         * @param dir The direction string
         * @return The sort direction, or null if not recognized
         */
        fun fromString(dir: String): SortDirection? {
            return when (dir.lowercase()) {
                "asc", "ascending" -> ASC
                "desc", "descending" -> DESC
                else -> null
            }
        }
    }
}
