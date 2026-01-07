package me.zkvl.beadsviewer.query.parser

import me.zkvl.beadsviewer.query.ast.*
import me.zkvl.beadsviewer.query.lexer.Token
import me.zkvl.beadsviewer.query.lexer.TokenType

/**
 * Recursive descent parser for the query language.
 *
 * Grammar (simplified):
 * ```
 * query          → filter? sort_clause?
 * filter         → or_expr
 * or_expr        → and_expr ( "or" and_expr )*
 * and_expr       → not_expr ( "and"? not_expr )*      // Implicit AND
 * not_expr       → "not" not_expr | primary
 * primary        → "(" or_expr ")" | comparison | text_search
 * comparison     → field ":" value_expr
 * value_expr     → range | value_list | value
 * range          → value ".." value
 * value_list     → value ( "," value )+
 * text_search    → STRING | IDENTIFIER
 * sort_clause    → "sort" "by" ":" sort_directive ( "," sort_directive )*
 * sort_directive → field direction?
 * direction      → "asc" | "desc"
 * ```
 *
 * Operator precedence (high to low):
 * 1. NOT (unary)
 * 2. AND (binary)
 * 3. OR (binary)
 *
 * Example:
 * ```kotlin
 * val tokens = Lexer("status:open AND priority:0").tokenize().getOrThrow()
 * val query = Parser(tokens).parse().getOrThrow()
 * ```
 *
 * @param tokens The list of tokens from the lexer
 */
class Parser(private val tokens: List<Token>) {
    private var current = 0

    /**
     * Parse the complete query.
     *
     * @return Success with Query, or Failure with ParserException
     */
    fun parse(): Result<Query> {
        return try {
            // Empty query
            if (isAtEnd()) {
                return Result.success(Query.EMPTY)
            }

            // Parse filter expression
            val filter = if (!check(TokenType.SORT_BY) && !isAtEnd()) {
                parseOrExpression()
            } else {
                null
            }

            // Parse sort clause
            val sort = if (match(TokenType.SORT_BY)) {
                parseSortClause()
            } else {
                emptyList()
            }

            // Ensure we consumed all tokens
            if (!isAtEnd()) {
                throw ParserException(
                    "Unexpected token '${peek().lexeme}' at position ${peek().position}",
                    peek().position
                )
            }

            Result.success(Query(filter, sort))
        } catch (e: ParserException) {
            Result.failure(e)
        }
    }

    /**
     * or_expr → and_expr ( "or" and_expr )*
     */
    private fun parseOrExpression(): QueryNode {
        var expr = parseAndExpression()

        while (match(TokenType.OR)) {
            val right = parseAndExpression()
            expr = OrNode(expr, right)
        }

        return expr
    }

    /**
     * and_expr → not_expr ( "and"? not_expr )*
     *
     * Note: AND is implicit - consecutive expressions without OR are AND'd
     */
    private fun parseAndExpression(): QueryNode {
        var expr = parseNotExpression()

        while (match(TokenType.AND) || shouldImplicitAnd()) {
            val right = parseNotExpression()
            expr = AndNode(expr, right)
        }

        return expr
    }

    /**
     * Check if we should insert implicit AND.
     * True if next token starts a new expression without OR.
     */
    private fun shouldImplicitAnd(): Boolean {
        if (isAtEnd() || check(TokenType.SORT_BY) || check(TokenType.RIGHT_PAREN)) {
            return false
        }

        val next = peek().type
        return when (next) {
            TokenType.NOT,
            TokenType.LEFT_PAREN,
            TokenType.IDENTIFIER,
            TokenType.STRING,
            TokenType.NUMBER -> true
            else -> false
        }
    }

    /**
     * not_expr → "not" not_expr | primary
     */
    private fun parseNotExpression(): QueryNode {
        if (match(TokenType.NOT)) {
            val expr = parseNotExpression()  // Right-associative
            return NotNode(expr)
        }

        return parsePrimary()
    }

    /**
     * primary → "(" or_expr ")" | comparison | text_search
     */
    private fun parsePrimary(): QueryNode {
        // Grouped expression
        if (match(TokenType.LEFT_PAREN)) {
            val expr = parseOrExpression()
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression")
            return expr
        }

        // Field comparison or text search
        if (check(TokenType.IDENTIFIER) || check(TokenType.STRING)) {
            return parseComparisonOrTextSearch()
        }

        throw ParserException(
            "Expected expression, got '${peek().lexeme}'",
            peek().position
        )
    }

    /**
     * comparison → field ":" value_expr
     * text_search → STRING | IDENTIFIER
     */
    private fun parseComparisonOrTextSearch(): QueryNode {
        val token = advance()

        // Check if this is a field comparison (field:value)
        if (match(TokenType.COLON)) {
            // Parse field
            val field = QueryField.fromString(token.lexeme)
            if (field == null) {
                val suggestions = QueryField.getSuggestions(token.lexeme, 3)
                val suggestionText = if (suggestions.isNotEmpty()) {
                    " Did you mean: ${suggestions.joinToString(", ")}?"
                } else {
                    ""
                }
                throw ParserException(
                    "Unknown field '${token.lexeme}'.${suggestionText}",
                    token.position
                )
            }

            // Parse value expression
            return parseValueExpression(field)
        } else {
            // Text search on all searchable fields
            val searchText = when (token.type) {
                TokenType.STRING -> token.literal as String
                TokenType.IDENTIFIER -> token.lexeme
                else -> throw ParserException(
                    "Expected text search term",
                    token.position
                )
            }

            return ContainsNode(
                field = null,  // Search all text fields
                value = searchText,
                caseSensitive = false
            )
        }
    }

    /**
     * value_expr → range | value_list | value
     */
    private fun parseValueExpression(field: QueryField): QueryNode {
        val firstValue = parseValue(field)

        // Check for range (value..value)
        if (match(TokenType.DOT_DOT)) {
            val secondValue = parseValue(field)
            return RangeNode(field, firstValue, secondValue)
        }

        // Check for value list (value,value,value)
        if (match(TokenType.COMMA)) {
            val values = mutableListOf(firstValue)
            values.add(parseValue(field))

            while (match(TokenType.COMMA)) {
                values.add(parseValue(field))
            }

            return InNode(field, values)
        }

        // Check if field is labels (multi-value field) - use HasNode
        if (field == QueryField.LABELS) {
            return HasNode(field, firstValue)
        }

        // Single value
        return EqualsNode(field, firstValue)
    }

    /**
     * Parse a single value (string, number, date, null)
     */
    private fun parseValue(field: QueryField): QueryValue {
        if (isAtEnd()) {
            throw ParserException("Expected value after ':'", previous().position + previous().lexeme.length)
        }

        val token = advance()

        val rawValue = when (token.type) {
            TokenType.STRING -> QueryValue.StringValue(token.literal as String)
            TokenType.NUMBER -> QueryValue.IntValue(token.literal as Int)
            TokenType.IDENTIFIER -> {
                val text = token.lexeme.lowercase()

                // Check for null
                if (text == "null" || text == "unassigned" || text == "none") {
                    return QueryValue.NullValue
                }

                // Check for relative dates
                RelativeDateSpec.fromString(text)?.let {
                    return QueryValue.RelativeDate(it)
                }

                // Otherwise treat as string
                QueryValue.StringValue(token.lexeme)
            }
            else -> throw ParserException(
                "Expected value, got '${token.lexeme}'",
                token.position
            )
        }

        // Coerce to field type
        return rawValue.coerceToFieldType(field).getOrElse { error ->
            throw ParserException(
                "Cannot use value '${token.lexeme}' for field '${field.fieldName}': ${error.message}",
                token.position
            )
        }
    }

    /**
     * sort_clause → sort_directive ( "," sort_directive )*
     * (SORT_BY token already consumed)
     */
    private fun parseSortClause(): List<SortDirective> {
        consume(TokenType.COLON, "Expected ':' after 'sort by'")

        val directives = mutableListOf<SortDirective>()

        do {
            directives.add(parseSortDirective())
        } while (match(TokenType.COMMA))

        return directives
    }

    /**
     * sort_directive → field direction?
     */
    private fun parseSortDirective(): SortDirective {
        val fieldToken = consume(TokenType.IDENTIFIER, "Expected field name")
        val field = QueryField.fromString(fieldToken.lexeme)
        if (field == null) {
            val suggestions = QueryField.getSuggestions(fieldToken.lexeme, 3)
            val suggestionText = if (suggestions.isNotEmpty()) {
                " Did you mean: ${suggestions.joinToString(", ")}?"
            } else {
                ""
            }
            throw ParserException(
                "Unknown field '${fieldToken.lexeme}'.${suggestionText}",
                fieldToken.position
            )
        }

        val direction = if (match(TokenType.ASC)) {
            SortDirection.ASC
        } else if (match(TokenType.DESC)) {
            SortDirection.DESC
        } else {
            SortDirection.ASC  // Default
        }

        return SortDirective(field, direction)
    }

    // ===== Helper Methods =====

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun peek(): Token {
        return tokens[current]
    }

    private fun previous(): Token {
        return tokens[current - 1]
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()

        throw ParserException(
            "$message, got '${peek().lexeme}'",
            peek().position
        )
    }

    private fun isAtEnd(): Boolean {
        return peek().type == TokenType.EOF
    }
}

/**
 * Exception thrown when parsing fails.
 *
 * @param message The error message
 * @param position The character position where the error occurred
 */
class ParserException(
    message: String,
    val position: Int? = null
) : Exception(message)
