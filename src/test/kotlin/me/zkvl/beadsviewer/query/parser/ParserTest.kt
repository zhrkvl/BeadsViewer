package me.zkvl.beadsviewer.query.parser

import me.zkvl.beadsviewer.query.ast.*
import me.zkvl.beadsviewer.query.lexer.Lexer
import kotlin.test.*

/**
 * Tests for the query parser.
 *
 * These tests verify that the parser correctly builds AST from tokens
 * and provides minimal working examples for debugging.
 */
class ParserTest {

    private fun parseQuery(queryString: String): Query {
        val tokens = Lexer(queryString).tokenize().getOrThrow()
        return Parser(tokens).parse().getOrThrow()
    }

    @Test
    fun `test empty query`() {
        val query = parseQuery("")
        assertEquals(Query.EMPTY, query)
        assertNull(query.filter)
        assertTrue(query.sort.isEmpty())
    }

    @Test
    fun `test simple equals comparison`() {
        val query = parseQuery("status:open")
        assertNotNull(query.filter)
        assertIs<EqualsNode>(query.filter)
        val equals = query.filter as EqualsNode
        assertEquals(QueryField.STATUS, equals.field)
        assertIs<QueryValue.StringValue>(equals.value)
        assertEquals("open", (equals.value as QueryValue.StringValue).value)
    }

    @Test
    fun `test number comparison`() {
        val query = parseQuery("priority:0")
        assertNotNull(query.filter)
        assertIs<EqualsNode>(query.filter)
        val equals = query.filter as EqualsNode
        assertEquals(QueryField.PRIORITY, equals.field)
        assertIs<QueryValue.IntValue>(equals.value)
        assertEquals(0, (equals.value as QueryValue.IntValue).value)
    }

    @Test
    fun `test null comparison`() {
        val query = parseQuery("assignee:null")
        assertNotNull(query.filter)
        assertIs<EqualsNode>(query.filter)
        val equals = query.filter as EqualsNode
        assertEquals(QueryField.ASSIGNEE, equals.field)
        assertIs<QueryValue.NullValue>(equals.value)
    }

    @Test
    fun `test range comparison`() {
        val query = parseQuery("priority:0..2")
        assertNotNull(query.filter)
        assertIs<RangeNode>(query.filter)
        val range = query.filter as RangeNode
        assertEquals(QueryField.PRIORITY, range.field)
        assertIs<QueryValue.IntValue>(range.min)
        assertIs<QueryValue.IntValue>(range.max)
        assertEquals(0, (range.min as QueryValue.IntValue).value)
        assertEquals(2, (range.max as QueryValue.IntValue).value)
    }

    @Test
    fun `test in comparison with multiple values`() {
        val query = parseQuery("priority:0,1,2")
        assertNotNull(query.filter)
        assertIs<InNode>(query.filter)
        val inNode = query.filter as InNode
        assertEquals(QueryField.PRIORITY, inNode.field)
        assertEquals(3, inNode.values.size)
    }

    @Test
    fun `test label field uses HasNode`() {
        val query = parseQuery("label:frontend")
        assertNotNull(query.filter)
        assertIs<HasNode>(query.filter)
        val hasNode = query.filter as HasNode
        assertEquals(QueryField.LABELS, hasNode.field)
    }

    @Test
    fun `test text search without field`() {
        val query = parseQuery("authentication")
        assertNotNull(query.filter)
        assertIs<ContainsNode>(query.filter)
        val contains = query.filter as ContainsNode
        assertNull(contains.field)  // Search all fields
        assertEquals("authentication", contains.value)
        assertFalse(contains.caseSensitive)
    }

    @Test
    fun `test AND operation explicit`() {
        val query = parseQuery("status:open AND priority:0")
        assertNotNull(query.filter)
        assertIs<AndNode>(query.filter)
        val and = query.filter as AndNode
        assertIs<EqualsNode>(and.left)
        assertIs<EqualsNode>(and.right)
    }

    @Test
    fun `test AND operation implicit`() {
        val query = parseQuery("status:open priority:0")
        assertNotNull(query.filter)
        assertIs<AndNode>(query.filter)
        val and = query.filter as AndNode
        assertIs<EqualsNode>(and.left)
        assertIs<EqualsNode>(and.right)
    }

    @Test
    fun `test OR operation`() {
        val query = parseQuery("status:open OR status:blocked")
        assertNotNull(query.filter)
        assertIs<OrNode>(query.filter)
        val or = query.filter as OrNode
        assertIs<EqualsNode>(or.left)
        assertIs<EqualsNode>(or.right)
    }

    @Test
    fun `test NOT operation`() {
        val query = parseQuery("NOT status:closed")
        assertNotNull(query.filter)
        assertIs<NotNode>(query.filter)
        val not = query.filter as NotNode
        assertIs<EqualsNode>(not.child)
    }

    @Test
    fun `test operator precedence - NOT before AND`() {
        val query = parseQuery("NOT status:closed AND priority:0")
        assertNotNull(query.filter)
        assertIs<AndNode>(query.filter)
        val and = query.filter as AndNode
        assertIs<NotNode>(and.left)  // NOT binds tighter
        assertIs<EqualsNode>(and.right)
    }

    @Test
    fun `test operator precedence - AND before OR`() {
        val query = parseQuery("status:open AND priority:0 OR priority:1")
        assertNotNull(query.filter)
        assertIs<OrNode>(query.filter)
        val or = query.filter as OrNode
        assertIs<AndNode>(or.left)  // AND binds tighter
        assertIs<EqualsNode>(or.right)
    }

    @Test
    fun `test parentheses grouping`() {
        val query = parseQuery("(status:open OR status:in_progress) AND priority:0")
        assertNotNull(query.filter)
        assertIs<AndNode>(query.filter)
        val and = query.filter as AndNode
        assertIs<OrNode>(and.left)  // Grouped by parentheses
        assertIs<EqualsNode>(and.right)
    }

    @Test
    fun `test quoted string value`() {
        val query = parseQuery("title:\"bug fix\"")
        assertNotNull(query.filter)
        assertIs<EqualsNode>(query.filter)
        val equals = query.filter as EqualsNode
        assertIs<QueryValue.StringValue>(equals.value)
        assertEquals("bug fix", (equals.value as QueryValue.StringValue).value)
    }

    @Test
    fun `test braced string value`() {
        val query = parseQuery("label:{multi word label}")
        assertNotNull(query.filter)
        assertIs<HasNode>(query.filter)
        val hasNode = query.filter as HasNode
        assertIs<QueryValue.StringValue>(hasNode.value)
        assertEquals("multi word label", (hasNode.value as QueryValue.StringValue).value)
    }

    @Test
    fun `test sort directive single field`() {
        val query = parseQuery("sort by: priority asc")
        assertNull(query.filter)
        assertEquals(1, query.sort.size)
        assertEquals(QueryField.PRIORITY, query.sort[0].field)
        assertEquals(SortDirection.ASC, query.sort[0].direction)
    }

    @Test
    fun `test sort directive multiple fields`() {
        val query = parseQuery("sort by: priority asc, updated desc")
        assertNull(query.filter)
        assertEquals(2, query.sort.size)
        assertEquals(QueryField.PRIORITY, query.sort[0].field)
        assertEquals(SortDirection.ASC, query.sort[0].direction)
        assertEquals(QueryField.UPDATED_AT, query.sort[1].field)
        assertEquals(SortDirection.DESC, query.sort[1].direction)
    }

    @Test
    fun `test sort with default direction`() {
        val query = parseQuery("sort by: priority")
        assertEquals(1, query.sort.size)
        assertEquals(SortDirection.ASC, query.sort[0].direction)  // Default
    }

    @Test
    fun `test filter with sort`() {
        val query = parseQuery("status:open sort by: priority asc")
        assertNotNull(query.filter)
        assertIs<EqualsNode>(query.filter)
        assertEquals(1, query.sort.size)
    }

    @Test
    fun `test field alias - pri for priority`() {
        val query = parseQuery("pri:0")
        assertNotNull(query.filter)
        assertIs<EqualsNode>(query.filter)
        val equals = query.filter as EqualsNode
        assertEquals(QueryField.PRIORITY, equals.field)
    }

    @Test
    fun `test field alias - type for issue_type`() {
        val query = parseQuery("type:bug")
        assertNotNull(query.filter)
        assertIs<EqualsNode>(query.filter)
        val equals = query.filter as EqualsNode
        assertEquals(QueryField.ISSUE_TYPE, equals.field)
    }

    @Test
    fun `test relative date - today`() {
        val query = parseQuery("created:today")
        assertNotNull(query.filter)
        assertIs<EqualsNode>(query.filter)
        val equals = query.filter as EqualsNode
        assertIs<QueryValue.RelativeDate>(equals.value)
        assertEquals(RelativeDateSpec.TODAY, (equals.value as QueryValue.RelativeDate).spec)
    }

    @Test
    fun `test relative date - next-week`() {
        val query = parseQuery("due:next-week")
        assertNotNull(query.filter)
        assertIs<EqualsNode>(query.filter)
        val equals = query.filter as EqualsNode
        assertIs<QueryValue.RelativeDate>(equals.value)
        assertEquals(RelativeDateSpec.NEXT_WEEK, (equals.value as QueryValue.RelativeDate).spec)
    }

    @Test
    fun `test complex query with all operators`() {
        val query = parseQuery("(status:open OR status:in_progress) AND priority:0..2 NOT assignee:null sort by: priority asc, updated desc")
        assertNotNull(query.filter)
        assertIs<AndNode>(query.filter)
        assertEquals(2, query.sort.size)
    }

    @Test
    fun `test unknown field error`() {
        val tokens = Lexer("unknown_field:value").tokenize().getOrThrow()
        val result = Parser(tokens).parse()
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertIs<ParserException>(error)
        assertContains(error.message ?: "", "Unknown field")
    }

    @Test
    fun `test unknown field with suggestions`() {
        val tokens = Lexer("priorit:0").tokenize().getOrThrow()
        val result = Parser(tokens).parse()
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertIs<ParserException>(error)
        assertContains(error.message ?: "", "priority")  // Should suggest correct field
    }

    @Test
    fun `test missing value after colon`() {
        val tokens = Lexer("status:").tokenize().getOrThrow()
        val result = Parser(tokens).parse()
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertIs<ParserException>(error)
    }

    @Test
    fun `test missing closing parenthesis`() {
        val tokens = Lexer("(status:open").tokenize().getOrThrow()
        val result = Parser(tokens).parse()
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertIs<ParserException>(error)
        assertContains(error.message ?: "", "')'")
    }

    @Test
    fun `test unexpected token after query`() {
        // "status:open extra" is actually VALID (implicit AND with text search)
        // Test a truly invalid scenario: token after sort clause
        val tokens = Lexer("status:open sort by: priority asc extra").tokenize().getOrThrow()
        val result = Parser(tokens).parse()
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertIs<ParserException>(error)
        assertContains(error.message ?: "", "Unexpected")
    }

    // Minimal working examples for debugging

    @Test
    fun `minimal example - simple filter`() {
        val query = parseQuery("status:open")
        println("Query: status:open")
        println("Filter: ${query.filter}")
        println("Sort: ${query.sort}")
        assertNotNull(query.filter)
    }

    @Test
    fun `minimal example - AND query`() {
        val query = parseQuery("status:open AND priority:0")
        println("Query: status:open AND priority:0")
        println("Filter AST: ${query.filter}")
        assertIs<AndNode>(query.filter)
    }

    @Test
    fun `minimal example - range query`() {
        val query = parseQuery("priority:0..2")
        println("Query: priority:0..2")
        println("Filter AST: ${query.filter}")
        assertIs<RangeNode>(query.filter)
    }

    @Test
    fun `minimal example - with sort`() {
        val query = parseQuery("status:open sort by: priority asc")
        println("Query: status:open sort by: priority asc")
        println("Filter: ${query.filter}")
        println("Sort directives: ${query.sort}")
        assertNotNull(query.filter)
        assertEquals(1, query.sort.size)
    }
}
