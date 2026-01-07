package me.zkvl.beadsviewer.query.lexer

/**
 * Lexical analyzer for the query language.
 *
 * The lexer converts a query string into a stream of tokens, handling:
 * - Identifiers: status, priority, my_field
 * - Quoted strings: "value with spaces"
 * - Braced strings: {value with spaces}
 * - Numbers: 123, -45
 * - Operators: : , .. ( ) *
 * - Keywords: and, or, not, sort by, asc, desc
 * - Whitespace (skipped)
 *
 * Example:
 * ```kotlin
 * val lexer = Lexer("status:open AND priority:0")
 * val result = lexer.tokenize()
 * result.onSuccess { tokens ->
 *     tokens.forEach { println(it) }
 * }
 * ```
 *
 * @param source The query string to tokenize
 */
class Lexer(private val source: String) {
    private var current = 0
    private var start = 0
    private val tokens = mutableListOf<Token>()

    /**
     * Tokenize the entire input.
     *
     * @return Success with list of tokens, or Failure with LexerException
     */
    fun tokenize(): Result<List<Token>> {
        return try {
            while (!isAtEnd()) {
                start = current
                scanToken()
            }
            tokens.add(Token(TokenType.EOF, "", current))
            Result.success(tokens)
        } catch (e: LexerException) {
            Result.failure(e)
        }
    }

    private fun scanToken() {
        when (val c = advance()) {
            // Whitespace (skip)
            ' ', '\t', '\n', '\r' -> {}

            // Single-character operators
            ':' -> addToken(TokenType.COLON)
            ',' -> addToken(TokenType.COMMA)
            '(' -> addToken(TokenType.LEFT_PAREN)
            ')' -> addToken(TokenType.RIGHT_PAREN)
            '*' -> addToken(TokenType.STAR)

            // Braced string
            '{' -> bracedString()

            // Quoted string
            '"' -> quotedString()

            // Range operator or number
            '.' -> {
                if (match('.')) {
                    addToken(TokenType.DOT_DOT)
                } else {
                    throw LexerException("Unexpected character '.' at position $current", current - 1)
                }
            }

            // Minus: either start of negative number or part of identifier
            '-' -> {
                if (peek().isDigit()) {
                    number()
                } else if (peek().isLetter() || peek() == '_') {
                    // Part of hyphenated identifier like "merge-request"
                    identifier()
                } else {
                    throw LexerException("Unexpected character '-' at position ${current - 1}", current - 1)
                }
            }

            // Number or identifier
            else -> {
                when {
                    c.isDigit() -> number()
                    c.isLetter() || c == '_' -> identifier()
                    else -> throw LexerException(
                        "Unexpected character '$c' at position ${current - 1}",
                        current - 1
                    )
                }
            }
        }
    }

    private fun identifier() {
        while (peek().let { it.isLetterOrDigit() || it == '_' || it == '-' }) {
            advance()
        }

        val text = source.substring(start, current)
        val type = when (text.lowercase()) {
            "and" -> TokenType.AND
            "or" -> TokenType.OR
            "not" -> TokenType.NOT
            "sort" -> {
                // Check for "sort by"
                val savedPos = current
                skipWhitespace()
                if (matchWord("by")) {
                    TokenType.SORT_BY
                } else {
                    current = savedPos
                    TokenType.IDENTIFIER
                }
            }
            "asc" -> TokenType.ASC
            "desc" -> TokenType.DESC
            else -> TokenType.IDENTIFIER
        }

        addToken(type)
    }

    private fun number() {
        // Handle negative numbers (minus already consumed)
        val startChar = if (start > 0) source[start] else '0'

        while (peek().isDigit()) {
            advance()
        }

        val numberText = source.substring(start, current)
        val value = numberText.toIntOrNull()
            ?: throw LexerException("Invalid number '$numberText' at position $start", start)

        addToken(TokenType.NUMBER, value)
    }

    private fun quotedString() {
        // Opening quote already consumed
        val valueStart = current

        while (peek() != '"' && !isAtEnd()) {
            advance()
        }

        if (isAtEnd()) {
            throw LexerException("Unterminated string at position $start", start)
        }

        val value = source.substring(valueStart, current)
        advance() // Closing quote

        addToken(TokenType.STRING, value)
    }

    private fun bracedString() {
        // Opening brace already consumed
        val valueStart = current

        while (peek() != '}' && !isAtEnd()) {
            advance()
        }

        if (isAtEnd()) {
            throw LexerException("Unterminated braced string at position $start", start)
        }

        val value = source.substring(valueStart, current)
        advance() // Closing brace

        addToken(TokenType.STRING, value)
    }

    private fun matchWord(word: String): Boolean {
        val savedPos = current
        for (c in word) {
            if (isAtEnd() || advance().lowercaseChar() != c.lowercaseChar()) {
                current = savedPos
                return false
            }
        }
        // Check that word ends (not part of longer word)
        if (!isAtEnd() && (peek().isLetterOrDigit() || peek() == '_' || peek() == '-')) {
            current = savedPos
            return false
        }
        return true
    }

    private fun skipWhitespace() {
        while (!isAtEnd() && peek().isWhitespace()) {
            advance()
        }
    }

    private fun advance(): Char {
        return source[current++]
    }

    private fun peek(): Char {
        return if (isAtEnd()) '\u0000' else source[current]
    }

    private fun peekNext(): Char {
        return if (current + 1 >= source.length) '\u0000' else source[current + 1]
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (source[current] != expected) return false
        current++
        return true
    }

    private fun previous(): Char {
        return if (start > 0) source[start] else '\u0000'
    }

    private fun isAtEnd(): Boolean {
        return current >= source.length
    }

    private fun addToken(type: TokenType, literal: Any? = null) {
        val text = source.substring(start, current)
        tokens.add(Token(type, text, start, literal))
    }
}

/**
 * Exception thrown when lexical analysis fails.
 *
 * @param message The error message
 * @param position The character position where the error occurred
 */
class LexerException(
    message: String,
    val position: Int? = null
) : Exception(message)
