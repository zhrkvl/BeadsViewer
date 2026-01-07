package me.zkvl.beadsviewer.query.ast

/**
 * Exact equality comparison: field:value
 *
 * Matches issues where the field exactly equals the given value.
 *
 * Examples:
 * - `status:open` → status equals OPEN
 * - `priority:0` → priority equals 0
 * - `assignee:john` → assignee equals "john"
 * - `assignee:null` → assignee is null
 *
 * @param field The field to compare
 * @param value The value to compare against
 */
data class EqualsNode(
    val field: QueryField,
    val value: QueryValue
) : QueryNode {
    override fun <T> accept(visitor: QueryNodeVisitor<T>): T {
        return visitor.visitEqualsNode(this)
    }
}

/**
 * Set membership comparison: field:value1,value2,value3
 *
 * Matches issues where the field equals any of the given values.
 *
 * Examples:
 * - `status:open,in_progress` → status is OPEN or IN_PROGRESS
 * - `priority:0,1,2` → priority is 0, 1, or 2
 *
 * @param field The field to compare
 * @param values The list of values to match against
 */
data class InNode(
    val field: QueryField,
    val values: List<QueryValue>
) : QueryNode {
    override fun <T> accept(visitor: QueryNodeVisitor<T>): T {
        return visitor.visitInNode(this)
    }
}

/**
 * Range comparison: field:min..max (inclusive)
 *
 * Matches issues where the field value is between min and max (inclusive).
 *
 * Examples:
 * - `priority:0..2` → priority is 0, 1, or 2
 * - `created:2024-01-01..2024-12-31` → created in 2024
 *
 * @param field The field to compare
 * @param min The minimum value (inclusive)
 * @param max The maximum value (inclusive)
 */
data class RangeNode(
    val field: QueryField,
    val min: QueryValue,
    val max: QueryValue
) : QueryNode {
    override fun <T> accept(visitor: QueryNodeVisitor<T>): T {
        return visitor.visitRangeNode(this)
    }
}

/**
 * Text search: searchable fields contain value.
 *
 * Performs case-insensitive substring search in text fields.
 *
 * If field is null, searches all text-searchable fields
 * (title, description, design, acceptance_criteria, notes).
 *
 * If field is specified, searches only that field.
 *
 * Examples:
 * - `authentication` → search all text fields for "authentication"
 * - `title:login` → search title field for "login"
 * - `description:bug` → search description field for "bug"
 *
 * @param field The specific field to search (null = all text fields)
 * @param value The text to search for
 * @param caseSensitive Whether search is case-sensitive (default: false)
 */
data class ContainsNode(
    val field: QueryField?,
    val value: String,
    val caseSensitive: Boolean = false
) : QueryNode {
    override fun <T> accept(visitor: QueryNodeVisitor<T>): T {
        return visitor.visitContainsNode(this)
    }
}

/**
 * List contains: for multi-value fields like labels.
 *
 * Matches issues where the list field contains the given value.
 *
 * Examples:
 * - `label:frontend` → labels list contains "frontend"
 * - `label:{multi word}` → labels list contains "multi word"
 *
 * Note: For single-value fields, this behaves the same as EqualsNode.
 *
 * @param field The field to check (should be a list field)
 * @param value The value to search for in the list
 */
data class HasNode(
    val field: QueryField,
    val value: QueryValue
) : QueryNode {
    override fun <T> accept(visitor: QueryNodeVisitor<T>): T {
        return visitor.visitHasNode(this)
    }
}
