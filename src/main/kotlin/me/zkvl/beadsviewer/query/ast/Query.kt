package me.zkvl.beadsviewer.query.ast

/**
 * Complete query with filter and sort directives.
 *
 * Immutable representation of a parsed query, consisting of:
 * - Optional filter expression (QueryNode tree)
 * - Optional sort directives (list of field + direction pairs)
 *
 * Examples:
 *
 * 1. Filter only:
 *    Query: `status:open AND priority:0`
 *    AST: Query(
 *      filter = AndNode(
 *        EqualsNode(STATUS, StringValue("open")),
 *        EqualsNode(PRIORITY, IntValue(0))
 *      ),
 *      sort = emptyList()
 *    )
 *
 * 2. Filter + sort:
 *    Query: `status:open sort by: priority asc`
 *    AST: Query(
 *      filter = EqualsNode(STATUS, StringValue("open")),
 *      sort = listOf(SortDirective(PRIORITY, ASC))
 *    )
 *
 * 3. Sort only:
 *    Query: `sort by: updated desc`
 *    AST: Query(
 *      filter = null,
 *      sort = listOf(SortDirective(UPDATED_AT, DESC))
 *    )
 *
 * 4. Empty query (match all):
 *    Query: `` (empty string)
 *    AST: Query(filter = null, sort = emptyList())
 *
 * @param filter The filter expression (null = match all issues)
 * @param sort The sort directives (empty = no sorting, maintain original order)
 */
data class Query(
    val filter: QueryNode?,
    val sort: List<SortDirective> = emptyList()
) {
    /**
     * Returns true if this query matches all issues (no filter, no sort).
     */
    fun matchesAll(): Boolean = filter == null && sort.isEmpty()

    /**
     * Returns true if this query has a filter expression.
     */
    fun hasFilter(): Boolean = filter != null

    /**
     * Returns true if this query has sort directives.
     */
    fun hasSort(): Boolean = sort.isNotEmpty()

    companion object {
        /**
         * Empty query that matches all issues with no sorting.
         *
         * Equivalent to an empty query string.
         */
        val EMPTY = Query(filter = null, sort = emptyList())
    }
}
