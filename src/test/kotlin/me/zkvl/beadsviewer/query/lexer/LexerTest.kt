package me.zkvl.beadsviewer.query.lexer

import kotlin.test.*

/**
 * Tests for the query lexer.
 *
 * These tests verify that the lexer correctly tokenizes query strings
 * and provides minimal working examples for debugging.
 */
class LexerTest {

    @Test
    fun `test empty input produces EOF token`() {
        val result = Lexer("").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        assertEquals(1, tokens.size)
        assertEquals(TokenType.EOF, tokens[0].type)
    }

    @Test
    fun `test simple identifier`() {
        val result = Lexer("status").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        assertEquals(2, tokens.size)
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("status", tokens[0].lexeme)
        assertEquals(TokenType.EOF, tokens[1].type)
    }

    @Test
    fun `test field comparison with colon`() {
        val result = Lexer("status:open").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        assertEquals(4, tokens.size)
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("status", tokens[0].lexeme)
        assertEquals(TokenType.COLON, tokens[1].type)
        assertEquals(TokenType.IDENTIFIER, tokens[2].type)
        assertEquals("open", tokens[2].lexeme)
        assertEquals(TokenType.EOF, tokens[3].type)
    }

    @Test
    fun `test number literal`() {
        val result = Lexer("priority:0").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        assertEquals(4, tokens.size)
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals(TokenType.COLON, tokens[1].type)
        assertEquals(TokenType.NUMBER, tokens[2].type)
        assertEquals(0, tokens[2].literal)
    }

    @Test
    fun `test negative number`() {
        val result = Lexer("-5").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        assertEquals(2, tokens.size)
        assertEquals(TokenType.NUMBER, tokens[0].type)
        assertEquals(-5, tokens[0].literal)
    }

    @Test
    fun `test quoted string`() {
        val result = Lexer("title:\"bug fix\"").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        assertEquals(4, tokens.size)
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals(TokenType.COLON, tokens[1].type)
        assertEquals(TokenType.STRING, tokens[2].type)
        assertEquals("bug fix", tokens[2].literal)
    }

    @Test
    fun `test braced string`() {
        val result = Lexer("label:{multi word label}").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        assertEquals(4, tokens.size)
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals(TokenType.COLON, tokens[1].type)
        assertEquals(TokenType.STRING, tokens[2].type)
        assertEquals("multi word label", tokens[2].literal)
    }

    @Test
    fun `test AND keyword`() {
        val result = Lexer("status:open AND priority:0").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        assertEquals(8, tokens.size)  // Including EOF token
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals(TokenType.COLON, tokens[1].type)
        assertEquals(TokenType.IDENTIFIER, tokens[2].type)
        assertEquals(TokenType.AND, tokens[3].type)
        assertEquals(TokenType.IDENTIFIER, tokens[4].type)
        assertEquals(TokenType.COLON, tokens[5].type)
        assertEquals(TokenType.NUMBER, tokens[6].type)
        assertEquals(TokenType.EOF, tokens[7].type)
    }

    @Test
    fun `test OR keyword`() {
        val result = Lexer("status:open OR status:blocked").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        val orToken = tokens.find { it.type == TokenType.OR }
        assertNotNull(orToken)
    }

    @Test
    fun `test NOT keyword`() {
        val result = Lexer("NOT status:closed").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        assertEquals(TokenType.NOT, tokens[0].type)
    }

    @Test
    fun `test range operator`() {
        val result = Lexer("priority:0..2").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        assertEquals(6, tokens.size)
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals(TokenType.COLON, tokens[1].type)
        assertEquals(TokenType.NUMBER, tokens[2].type)
        assertEquals(TokenType.DOT_DOT, tokens[3].type)
        assertEquals(TokenType.NUMBER, tokens[4].type)
    }

    @Test
    fun `test comma separator`() {
        val result = Lexer("priority:0,1,2").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        assertEquals(8, tokens.size)
        assertEquals(TokenType.COMMA, tokens[3].type)
        assertEquals(TokenType.COMMA, tokens[5].type)
    }

    @Test
    fun `test parentheses`() {
        val result = Lexer("(status:open)").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        assertEquals(6, tokens.size)  // (, status, :, open, ), EOF
        assertEquals(TokenType.LEFT_PAREN, tokens[0].type)
        assertEquals(TokenType.IDENTIFIER, tokens[1].type)
        assertEquals(TokenType.COLON, tokens[2].type)
        assertEquals(TokenType.IDENTIFIER, tokens[3].type)
        assertEquals(TokenType.RIGHT_PAREN, tokens[4].type)
        assertEquals(TokenType.EOF, tokens[5].type)
    }

    @Test
    fun `test sort by keyword`() {
        val result = Lexer("status:open sort by: priority asc").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        val sortByToken = tokens.find { it.type == TokenType.SORT_BY }
        assertNotNull(sortByToken)
    }

    @Test
    fun `test asc and desc keywords`() {
        val result = Lexer("asc desc").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        assertEquals(TokenType.ASC, tokens[0].type)
        assertEquals(TokenType.DESC, tokens[1].type)
    }

    @Test
    fun `test hyphenated identifier`() {
        val result = Lexer("type:merge-request").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        assertEquals(4, tokens.size)
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals(TokenType.COLON, tokens[1].type)
        assertEquals(TokenType.IDENTIFIER, tokens[2].type)
        assertEquals("merge-request", tokens[2].lexeme)
    }

    @Test
    fun `test whitespace is ignored`() {
        val result = Lexer("  status : open  ").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        assertEquals(4, tokens.size)
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals(TokenType.COLON, tokens[1].type)
        assertEquals(TokenType.IDENTIFIER, tokens[2].type)
    }

    @Test
    fun `test unterminated string error`() {
        val result = Lexer("title:\"unclosed").tokenize()
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertIs<LexerException>(error)
        assertContains(error.message ?: "", "Unterminated")
    }

    @Test
    fun `test unterminated braced string error`() {
        val result = Lexer("label:{unclosed").tokenize()
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertIs<LexerException>(error)
        assertContains(error.message ?: "", "Unterminated")
    }

    @Test
    fun `test unexpected character error`() {
        val result = Lexer("status:@invalid").tokenize()
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertIs<LexerException>(error)
        assertContains(error.message ?: "", "Unexpected")
    }

    @Test
    fun `test complex query with multiple operators`() {
        val result = Lexer("status:open AND priority:0..2 OR assignee:null").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        assertTrue(tokens.any { it.type == TokenType.AND })
        assertTrue(tokens.any { it.type == TokenType.OR })
        assertTrue(tokens.any { it.type == TokenType.DOT_DOT })
    }

    @Test
    fun `test query with sort clause`() {
        val result = Lexer("status:open sort by: priority desc, updated asc").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        assertTrue(tokens.any { it.type == TokenType.SORT_BY })
        assertTrue(tokens.any { it.type == TokenType.DESC })
        assertTrue(tokens.any { it.type == TokenType.ASC })
        assertTrue(tokens.any { it.type == TokenType.COMMA })
    }

    @Test
    fun `test position tracking`() {
        val result = Lexer("status:open").tokenize()
        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        assertEquals(0, tokens[0].position)  // "status" at position 0
        assertEquals(6, tokens[1].position)  // ":" at position 6
        assertEquals(7, tokens[2].position)  // "open" at position 7
    }
}
