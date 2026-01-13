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
import me.zkvl.beadsviewer.ui.theme.BeadsColorScheme

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
 *
 * @param colorScheme The color scheme to use for syntax highlighting
 */
class QuerySyntaxHighlighter(private val colorScheme: BeadsColorScheme) : VisualTransformation {
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
                    SpanStyle(color = colorScheme.syntaxKeyword)

                // Field names: purple
                // Differentiate field names (before colon) from plain identifiers
                TokenType.IDENTIFIER -> when {
                    isFieldName(token, tokens) -> SpanStyle(color = colorScheme.syntaxField)
                    else -> SpanStyle(color = colorScheme.syntaxValue)
                }

                // Strings: green
                TokenType.STRING -> SpanStyle(color = colorScheme.syntaxString)

                // Numbers: blue
                TokenType.NUMBER -> SpanStyle(color = colorScheme.syntaxNumber)

                // Operators: gray
                TokenType.COLON, TokenType.COMMA, TokenType.DOT_DOT,
                TokenType.LEFT_PAREN, TokenType.RIGHT_PAREN,
                TokenType.STAR -> SpanStyle(color = colorScheme.syntaxOperator)

                // Braces: gray (for {braced strings})
                TokenType.LEFT_BRACE, TokenType.RIGHT_BRACE ->
                    SpanStyle(color = colorScheme.syntaxOperator)

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
