package me.zkvl.beadsviewer.query.ast

/**
 * Logical AND operation: both children must match.
 *
 * Example: `status:open AND priority:0`
 *
 * @param left The left operand
 * @param right The right operand
 */
data class AndNode(
    val left: QueryNode,
    val right: QueryNode
) : QueryNode {
    override fun <T> accept(visitor: QueryNodeVisitor<T>): T {
        return visitor.visitAndNode(this)
    }
}

/**
 * Logical OR operation: at least one child must match.
 *
 * Example: `status:blocked OR priority:0`
 *
 * @param left The left operand
 * @param right The right operand
 */
data class OrNode(
    val left: QueryNode,
    val right: QueryNode
) : QueryNode {
    override fun <T> accept(visitor: QueryNodeVisitor<T>): T {
        return visitor.visitOrNode(this)
    }
}

/**
 * Logical NOT operation: inverts the child result.
 *
 * Example: `NOT status:closed`
 *
 * @param child The child expression to negate
 */
data class NotNode(
    val child: QueryNode
) : QueryNode {
    override fun <T> accept(visitor: QueryNodeVisitor<T>): T {
        return visitor.visitNotNode(this)
    }
}
