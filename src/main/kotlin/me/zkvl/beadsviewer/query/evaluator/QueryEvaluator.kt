package me.zkvl.beadsviewer.query.evaluator

import kotlinx.datetime.*
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.model.IssueType
import me.zkvl.beadsviewer.model.Status
import me.zkvl.beadsviewer.query.ast.*

/**
 * Evaluates query AST against issues.
 *
 * This class is stateless and thread-safe.
 * Can be reused across multiple queries.
 *
 * Example:
 * ```kotlin
 * val evaluator = QueryEvaluator()
 * val query = Query(
 *     filter = AndNode(
 *         EqualsNode(QueryField.STATUS, QueryValue.StringValue("open")),
 *         EqualsNode(QueryField.PRIORITY, QueryValue.IntValue(0))
 *     )
 * )
 * val filtered = evaluator.filter(issues, query)
 * ```
 */
class QueryEvaluator {

    /**
     * Filter and sort a list of issues by query.
     *
     * @param issues The list of issues to filter
     * @param query The query to apply
     * @return The filtered and sorted list of issues
     */
    fun filter(issues: List<Issue>, query: Query): List<Issue> {
        // Apply filter
        val filtered = if (query.filter != null) {
            issues.filter { issue -> evaluate(query.filter, issue) }
        } else {
            issues
        }

        // Apply sort
        return if (query.sort.isNotEmpty()) {
            sort(filtered, query.sort)
        } else {
            filtered
        }
    }

    /**
     * Evaluate a query node against a single issue.
     *
     * @param node The query node to evaluate
     * @param issue The issue to evaluate against
     * @return True if the issue matches the query node
     */
    fun evaluate(node: QueryNode, issue: Issue): Boolean {
        return when (node) {
            is AndNode -> evaluate(node.left, issue) && evaluate(node.right, issue)
            is OrNode -> evaluate(node.left, issue) || evaluate(node.right, issue)
            is NotNode -> !evaluate(node.child, issue)
            is EqualsNode -> evaluateEquals(node, issue)
            is InNode -> evaluateIn(node, issue)
            is RangeNode -> evaluateRange(node, issue)
            is ContainsNode -> evaluateContains(node, issue)
            is HasNode -> evaluateHas(node, issue)
        }
    }

    private fun evaluateEquals(node: EqualsNode, issue: Issue): Boolean {
        val fieldValue = extractFieldValue(node.field, issue)

        // Handle null checks
        if (node.value is QueryValue.NullValue) {
            return fieldValue == null
        }
        if (fieldValue == null) {
            return false
        }

        return compareValues(fieldValue, node.value, ComparisonOp.EQUALS)
    }

    private fun evaluateIn(node: InNode, issue: Issue): Boolean {
        val fieldValue = extractFieldValue(node.field, issue) ?: return false

        return node.values.any { queryValue ->
            compareValues(fieldValue, queryValue, ComparisonOp.EQUALS)
        }
    }

    private fun evaluateRange(node: RangeNode, issue: Issue): Boolean {
        val fieldValue = extractFieldValue(node.field, issue) ?: return false

        val minMatch = compareValues(fieldValue, node.min, ComparisonOp.GREATER_EQUAL)
        val maxMatch = compareValues(fieldValue, node.max, ComparisonOp.LESS_EQUAL)

        return minMatch && maxMatch
    }

    private fun evaluateContains(node: ContainsNode, issue: Issue): Boolean {
        val searchFields = if (node.field != null) {
            listOf(node.field)
        } else {
            QueryField.TEXT_SEARCHABLE.toList()
        }

        return searchFields.any { field ->
            val value = extractFieldValue(field, issue) as? String ?: return@any false

            if (node.caseSensitive) {
                value.contains(node.value)
            } else {
                value.lowercase().contains(node.value.lowercase())
            }
        }
    }

    private fun evaluateHas(node: HasNode, issue: Issue): Boolean {
        val fieldValue = extractFieldValue(node.field, issue)

        // For list fields, check if list contains value
        if (fieldValue is List<*>) {
            return fieldValue.any { item ->
                compareValues(item, node.value, ComparisonOp.EQUALS)
            }
        }

        // For non-list fields, fall back to equality
        return compareValues(fieldValue, node.value, ComparisonOp.EQUALS)
    }

    /**
     * Extract field value from issue.
     */
    private fun extractFieldValue(field: QueryField, issue: Issue): Any? {
        return when (field) {
            QueryField.ID -> issue.id
            QueryField.TITLE -> issue.title
            QueryField.DESCRIPTION -> issue.description
            QueryField.STATUS -> issue.status
            QueryField.PRIORITY -> issue.priority
            QueryField.ISSUE_TYPE -> issue.issueType
            QueryField.ASSIGNEE -> issue.assignee
            QueryField.ESTIMATED_MINUTES -> issue.estimatedMinutes
            QueryField.EXTERNAL_REF -> issue.externalRef
            QueryField.SOURCE_REPO -> issue.sourceRepo
            QueryField.CREATED_AT -> issue.createdAt
            QueryField.CREATED_BY -> issue.createdBy
            QueryField.UPDATED_AT -> issue.updatedAt
            QueryField.DUE_DATE -> issue.dueDate
            QueryField.CLOSED_AT -> issue.closedAt
            QueryField.LABELS -> issue.labels
            QueryField.DESIGN -> issue.design
            QueryField.ACCEPTANCE_CRITERIA -> issue.acceptanceCriteria
            QueryField.NOTES -> issue.notes
        }
    }

    /**
     * Compare values with type coercion.
     */
    private fun compareValues(
        fieldValue: Any?,
        queryValue: QueryValue,
        op: ComparisonOp
    ): Boolean {
        if (fieldValue == null) return false

        return when (queryValue) {
            is QueryValue.StringValue -> {
                val fieldStr = when (fieldValue) {
                    is String -> fieldValue
                    is Status -> fieldValue.name.lowercase()
                    is IssueType -> fieldValue.value
                    else -> fieldValue.toString()
                }

                val queryStr = queryValue.value

                when (op) {
                    ComparisonOp.EQUALS -> fieldStr.equals(queryStr, ignoreCase = true)
                    else -> false  // String comparison only supports equality
                }
            }

            is QueryValue.IntValue -> {
                val fieldInt = when (fieldValue) {
                    is Int -> fieldValue
                    is String -> fieldValue.toIntOrNull()
                    else -> null
                } ?: return false

                when (op) {
                    ComparisonOp.EQUALS -> fieldInt == queryValue.value
                    ComparisonOp.GREATER_EQUAL -> fieldInt >= queryValue.value
                    ComparisonOp.LESS_EQUAL -> fieldInt <= queryValue.value
                }
            }

            is QueryValue.TimestampValue -> {
                val fieldInstant = fieldValue as? Instant ?: return false

                when (op) {
                    ComparisonOp.EQUALS -> fieldInstant == queryValue.value
                    ComparisonOp.GREATER_EQUAL -> fieldInstant >= queryValue.value
                    ComparisonOp.LESS_EQUAL -> fieldInstant <= queryValue.value
                }
            }

            is QueryValue.RelativeDate -> {
                val fieldInstant = fieldValue as? Instant ?: return false
                val queryInstant = resolveRelativeDate(queryValue.spec)

                when (op) {
                    ComparisonOp.EQUALS -> {
                        // For relative dates, "equals" means "on the same day"
                        val fieldDate = fieldInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date
                        val queryDate = queryInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date
                        fieldDate == queryDate
                    }
                    ComparisonOp.GREATER_EQUAL -> fieldInstant >= queryInstant
                    ComparisonOp.LESS_EQUAL -> fieldInstant <= queryInstant
                }
            }

            QueryValue.NullValue -> fieldValue == null
        }
    }

    /**
     * Resolve relative date specs to absolute timestamps.
     */
    private fun resolveRelativeDate(spec: RelativeDateSpec): Instant {
        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        val nowLocal = now.toLocalDateTime(tz)

        val targetDate = when (spec) {
            RelativeDateSpec.TODAY -> nowLocal.date
            RelativeDateSpec.YESTERDAY -> nowLocal.date.minus(1, DateTimeUnit.DAY)
            RelativeDateSpec.TOMORROW -> nowLocal.date.plus(1, DateTimeUnit.DAY)
            RelativeDateSpec.THIS_WEEK -> {
                // Start of current week (Monday)
                val dayOfWeek = nowLocal.dayOfWeek
                val daysToMonday = (dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
                nowLocal.date.minus(daysToMonday, DateTimeUnit.DAY)
            }
            RelativeDateSpec.LAST_WEEK -> {
                val thisWeekStart = resolveRelativeDate(RelativeDateSpec.THIS_WEEK)
                    .toLocalDateTime(tz).date
                thisWeekStart.minus(7, DateTimeUnit.DAY)
            }
            RelativeDateSpec.NEXT_WEEK -> {
                val thisWeekStart = resolveRelativeDate(RelativeDateSpec.THIS_WEEK)
                    .toLocalDateTime(tz).date
                thisWeekStart.plus(7, DateTimeUnit.DAY)
            }
            RelativeDateSpec.THIS_MONTH -> {
                LocalDate(nowLocal.year, nowLocal.month, 1)
            }
            RelativeDateSpec.LAST_MONTH -> {
                val thisMonth = LocalDate(nowLocal.year, nowLocal.month, 1)
                thisMonth.minus(1, DateTimeUnit.MONTH)
            }
            RelativeDateSpec.NEXT_MONTH -> {
                val thisMonth = LocalDate(nowLocal.year, nowLocal.month, 1)
                thisMonth.plus(1, DateTimeUnit.MONTH)
            }
        }

        // Convert to start of day
        val localDateTime = LocalDateTime(targetDate, LocalTime(0, 0))
        return localDateTime.toInstant(tz)
    }

    /**
     * Sort issues by directives.
     */
    private fun sort(issues: List<Issue>, directives: List<SortDirective>): List<Issue> {
        if (directives.isEmpty()) return issues

        // Create a comparator for multi-level sorting
        var comparator: Comparator<Issue>? = null

        for (directive in directives) {
            val fieldComparator = createFieldComparator(directive.field, directive.direction)
            comparator = if (comparator == null) {
                fieldComparator
            } else {
                comparator.thenComparing(fieldComparator)
            }
        }

        return issues.sortedWith(comparator!!)
    }

    /**
     * Create a comparator for a single field and direction.
     */
    private fun createFieldComparator(field: QueryField, direction: SortDirection): Comparator<Issue> {
        val baseComparator = Comparator<Issue> { a, b ->
            val valueA = extractFieldValue(field, a)
            val valueB = extractFieldValue(field, b)

            // Null handling: nulls last
            if (valueA == null && valueB == null) return@Comparator 0
            if (valueA == null) return@Comparator 1
            if (valueB == null) return@Comparator -1

            // Compare by type
            when {
                valueA is Comparable<*> && valueB is Comparable<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    (valueA as Comparable<Any>).compareTo(valueB as Any)
                }
                valueA is Status && valueB is Status -> {
                    valueA.ordinal.compareTo(valueB.ordinal)
                }
                valueA is IssueType && valueB is IssueType -> {
                    valueA.value.compareTo(valueB.value)
                }
                valueA is List<*> && valueB is List<*> -> {
                    valueA.size.compareTo(valueB.size)
                }
                else -> {
                    valueA.toString().compareTo(valueB.toString())
                }
            }
        }

        return if (direction == SortDirection.DESC) {
            baseComparator.reversed()
        } else {
            baseComparator
        }
    }
}

/**
 * Comparison operation types.
 */
enum class ComparisonOp {
    EQUALS,
    GREATER_EQUAL,
    LESS_EQUAL
}
