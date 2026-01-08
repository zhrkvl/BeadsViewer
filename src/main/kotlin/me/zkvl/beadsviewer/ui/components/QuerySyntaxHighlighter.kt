package me.zkvl.beadsviewer.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import me.zkvl.beadsviewer.query.lexer.Lexer
import me.zkvl.beadsviewer.query.lexer.Token
import me.zkvl.beadsviewer.query.lexer.TokenType

/**
 * Visual transformation that applies syntax highlighting to query text.
 * Uses the existing Lexer to tokenize and colorize text based on token types.
 *
 * Color scheme follows IntelliJ default theme:
 * - Keywords (and, or, not): Orange
 * - Fields (status, priority): Purple
 * - Strings ("value"): Green
 * - Numbers (0, 1, 2): Blue
 * - Operators (:, ,, ..): Gray
 */
class QuerySyntaxHighlighter : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Tokenize input using existing Lexer
        val lexer = Lexer(text.text)
        val tokensResult = lexer.tokenize()

        // If lexing fails, return original text without highlighting
        if (tokensResult.isFailure) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val tokens = tokensResult.getOrNull() ?: return TransformedText(text, OffsetMapping.Identity)
        val builder = AnnotatedString.Builder(text.text)

        // Apply colors based on token types
        tokens.forEach { token ->
            if (token.type == TokenType.EOF) return@forEach

            val style = when (token.type) {
                // Keywords: orange/red
                TokenType.AND, TokenType.OR, TokenType.NOT,
                TokenType.SORT_BY, TokenType.ASC, TokenType.DESC ->
                    SpanStyle(color = QueryColors.Keyword)

                // Field names: purple
                // Differentiate field names (before colon) from plain identifiers
                TokenType.IDENTIFIER -> when {
                    isFieldName(token, tokens) -> SpanStyle(color = QueryColors.Field)
                    else -> SpanStyle(color = QueryColors.Value)
                }

                // Strings: green
                TokenType.STRING -> SpanStyle(color = QueryColors.String)

                // Numbers: blue
                TokenType.NUMBER -> SpanStyle(color = QueryColors.Number)

                // Operators: gray
                TokenType.COLON, TokenType.COMMA, TokenType.DOT_DOT,
                TokenType.LEFT_PAREN, TokenType.RIGHT_PAREN,
                TokenType.STAR -> SpanStyle(color = QueryColors.Operator)

                // Braces: gray (for {braced strings})
                TokenType.LEFT_BRACE, TokenType.RIGHT_BRACE ->
                    SpanStyle(color = QueryColors.Operator)

                else -> null
            }

            style?.let {
                val endPos = token.position + token.lexeme.length
                builder.addStyle(it, token.position, endPos)
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    /**
     * Check if this identifier token is a field name (followed by colon).
     */
    private fun isFieldName(token: Token, allTokens: List<Token>): Boolean {
        val currentIndex = allTokens.indexOf(token)
        if (currentIndex == -1) return false

        val nextToken = allTokens.getOrNull(currentIndex + 1)
        return nextToken?.type == TokenType.COLON
    }
}

/**
 * Color scheme for query syntax highlighting.
 * Colors chosen to match IntelliJ IDEA default (Darcula) theme.
 */
object QueryColors {
    val Keyword = Color(0xFFCC7832)      // Orange - keywords (and, or, not)
    val Field = Color(0xFF9876AA)        // Purple - field names (status, priority)
    val String = Color(0xFF6A8759)       // Green - quoted strings
    val Number = Color(0xFF6897BB)       // Blue - numbers
    val Value = Color(0xFFA9B7C6)        // Light gray - unquoted values
    val Operator = Color(0xFFCCCCCC)     // Gray - operators (:, ,, ..)
    val Error = Color(0xFFBC3F3C)        // Red - errors (for future use)
}
