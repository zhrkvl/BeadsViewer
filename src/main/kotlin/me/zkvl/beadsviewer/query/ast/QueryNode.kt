package me.zkvl.beadsviewer.query.ast

/**
 * Base interface for all query AST nodes.
 *
 * A sealed interface ensures exhaustive when() expressions and type safety
 * throughout the query processing pipeline.
 *
 * Example AST for query "status:open AND priority:0":
 * ```
 * AndNode(
 *   left = EqualsNode(field = STATUS, value = StringValue("open")),
 *   right = EqualsNode(field = PRIORITY, value = IntValue(0))
 * )
 * ```
 */
sealed interface QueryNode {
    /**
     * Accept method for visitor pattern.
     * Enables AST traversal, transformation, and pretty-printing.
     *
     * @param visitor The visitor to accept
     * @return The result of the visitor operation
     */
    fun <T> accept(visitor: QueryNodeVisitor<T>): T
}

/**
 * Visitor interface for traversing and processing query AST nodes.
 *
 * Implement this interface to create operations on the AST such as:
 * - Pretty printing
 * - Optimization
 * - Validation
 * - Code generation
 */
interface QueryNodeVisitor<T> {
    fun visitAndNode(node: AndNode): T
    fun visitOrNode(node: OrNode): T
    fun visitNotNode(node: NotNode): T
    fun visitEqualsNode(node: EqualsNode): T
    fun visitInNode(node: InNode): T
    fun visitRangeNode(node: RangeNode): T
    fun visitContainsNode(node: ContainsNode): T
    fun visitHasNode(node: HasNode): T
}
