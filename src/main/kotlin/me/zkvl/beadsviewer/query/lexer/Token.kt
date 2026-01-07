package me.zkvl.beadsviewer.query.lexer

/**
 * Represents a lexical token in the query language.
 *
 * A token is a single unit of the query language, such as:
 * - A keyword ("and", "or", "not")
 * - An operator (":", ",", "..")
 * - A literal value ("status", "0", "frontend")
 *
 * Tokens include position information for error reporting.
 *
 * Example tokenization:
 * ```
 * Input: "status:open AND priority:0"
 * Tokens:
 *   Token(IDENTIFIER, "status", 0)
 *   Token(COLON, ":", 6)
 *   Token(IDENTIFIER, "open", 7)
 *   Token(AND, "and", 12)
 *   Token(IDENTIFIER, "priority", 16)
 *   Token(COLON, ":", 24)
 *   Token(NUMBER, "0", 25, literal=0)
 *   Token(EOF, "", 26)
 * ```
 *
 * @param type The type of token
 * @param lexeme The raw text of the token
 * @param position The character position in the source string (0-indexed)
 * @param literal The parsed literal value (for STRING and NUMBER tokens)
 */
data class Token(
    val type: TokenType,
    val lexeme: String,
    val position: Int,
    val literal: Any? = null
)

/**
 * Enumeration of all token types in the query language.
 */
enum class TokenType {
    // ===== Literals =====

    /** Identifier (field name or unquoted value): status, priority, frontend */
    IDENTIFIER,

    /** String literal (quoted or braced): "value", {multi word value} */
    STRING,

    /** Number literal: 0, 42, -5 */
    NUMBER,

    // ===== Operators =====

    /** Colon operator: : */
    COLON,

    /** Comma separator: , */
    COMMA,

    /** Range operator: .. */
    DOT_DOT,

    /** Wildcard (future use): * */
    STAR,

    // ===== Grouping =====

    /** Left parenthesis: ( */
    LEFT_PAREN,

    /** Right parenthesis: ) */
    RIGHT_PAREN,

    /** Left brace: { (for braced strings) */
    LEFT_BRACE,

    /** Right brace: } (for braced strings) */
    RIGHT_BRACE,

    // ===== Keywords =====

    /** Logical AND: and */
    AND,

    /** Logical OR: or */
    OR,

    /** Logical NOT: not */
    NOT,

    /** Sort clause introducer: sort by */
    SORT_BY,

    /** Ascending sort direction: asc */
    ASC,

    /** Descending sort direction: desc */
    DESC,

    // ===== Special =====

    /** End of input */
    EOF,

    /** Lexing error */
    ERROR;

    override fun toString(): String {
        return when (this) {
            IDENTIFIER -> "identifier"
            STRING -> "string"
            NUMBER -> "number"
            COLON -> "':'"
            COMMA -> "','"
            DOT_DOT -> "'..'"
            STAR -> "'*'"
            LEFT_PAREN -> "'('"
            RIGHT_PAREN -> "')'"
            LEFT_BRACE -> "'{'"
            RIGHT_BRACE -> "'}'"
            AND -> "'and'"
            OR -> "'or'"
            NOT -> "'not'"
            SORT_BY -> "'sort by'"
            ASC -> "'asc'"
            DESC -> "'desc'"
            EOF -> "end of input"
            ERROR -> "error"
        }
    }
}
