package me.zkvl.beadsviewer.query.evaluator

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.model.IssueType
import me.zkvl.beadsviewer.model.Status
import me.zkvl.beadsviewer.query.ast.*
import me.zkvl.beadsviewer.query.lexer.Lexer
import me.zkvl.beadsviewer.query.parser.Parser
import kotlin.test.*
import kotlin.time.Duration.Companion.days

/**
 * Tests for the query evaluator.
 *
 * These tests verify that the evaluator correctly filters and sorts issues
 * and provides minimal working examples for debugging.
 */
class QueryEvaluatorTest {

    private val evaluator = QueryEvaluator()
    private val now = Clock.System.now()

    // Sample test data
    private fun createTestIssues(): List<Issue> {
        return listOf(
            Issue(
                id = "TEST-1",
                title = "Fix authentication bug",
                description = "User login fails with invalid credentials",
                status = Status.OPEN,
                priority = 0,
                issueType = IssueType.Bug,
                assignee = null,
                createdAt = now.minus(5.days),
                updatedAt = now.minus(1.days),
                labels = listOf("frontend", "auth")
            ),
            Issue(
                id = "TEST-2",
                title = "Add dark mode feature",
                description = "Implement dark mode toggle in settings",
                status = Status.IN_PROGRESS,
                priority = 1,
                issueType = IssueType.Feature,
                assignee = "john",
                createdAt = now.minus(10.days),
                updatedAt = now.minus(2.days),
                labels = listOf("frontend", "ui")
            ),
            Issue(
                id = "TEST-3",
                title = "Refactor database queries",
                description = "Optimize slow queries in the dashboard",
                status = Status.BLOCKED,
                priority = 2,
                issueType = IssueType.Task,
                assignee = "jane",
                createdAt = now.minus(15.days),
                updatedAt = now.minus(3.days),
                labels = listOf("backend", "performance")
            ),
            Issue(
                id = "TEST-4",
                title = "Update documentation",
                description = "Add API documentation for new endpoints",
                status = Status.CLOSED,
                priority = 3,
                issueType = IssueType.Task,
                assignee = "john",
                createdAt = now.minus(20.days),
                updatedAt = now.minus(5.days),
                closedAt = now.minus(5.days),
                labels = listOf("docs")
            ),
            Issue(
                id = "TEST-5",
                title = "Fix logout bug",
                description = "Logout button doesn't work on mobile",
                status = Status.OPEN,
                priority = 0,
                issueType = IssueType.Bug,
                assignee = "jane",
                createdAt = now.minus(2.days),
                updatedAt = now.minus(1.days),
                labels = listOf("frontend", "mobile")
            )
        )
    }

    private fun parseAndEvaluate(queryString: String, issues: List<Issue>): List<Issue> {
        val tokens = Lexer(queryString).tokenize().getOrThrow()
        val query = Parser(tokens).parse().getOrThrow()
        return evaluator.filter(issues, query)
    }

    // Basic filtering tests

    @Test
    fun `test filter by status equals`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("status:open", issues)
        assertEquals(2, result.size)
        assertTrue(result.all { it.status == Status.OPEN })
    }

    @Test
    fun `test filter by priority equals`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("priority:0", issues)
        assertEquals(2, result.size)
        assertTrue(result.all { it.priority == 0 })
    }

    @Test
    fun `test filter by priority range`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("priority:0..2", issues)
        assertEquals(4, result.size)  // Priority 0, 1, 2
        assertTrue(result.all { it.priority in 0..2 })
    }

    @Test
    fun `test filter by priority in list`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("priority:0,1", issues)
        assertEquals(3, result.size)
        assertTrue(result.all { it.priority in listOf(0, 1) })
    }

    @Test
    fun `test filter by assignee equals`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("assignee:john", issues)
        assertEquals(2, result.size)
        assertTrue(result.all { it.assignee == "john" })
    }

    @Test
    fun `test filter by assignee null`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("assignee:null", issues)
        assertEquals(1, result.size)
        assertTrue(result.all { it.assignee == null })
    }

    @Test
    fun `test filter by issue type`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("type:bug", issues)
        assertEquals(2, result.size)
        assertTrue(result.all { it.issueType is IssueType.Bug })
    }

    @Test
    fun `test filter by label`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("label:frontend", issues)
        assertEquals(3, result.size)
        assertTrue(result.all { "frontend" in it.labels })
    }

    @Test
    fun `test text search in title`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("authentication", issues)
        assertEquals(1, result.size)
        assertTrue(result[0].title.contains("authentication", ignoreCase = true))
    }

    @Test
    fun `test text search in description`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("login", issues)
        assertEquals(1, result.size)
        assertTrue(result[0].description.contains("login", ignoreCase = true))
    }

    // Logical operator tests

    @Test
    fun `test AND operation explicit`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("status:open AND priority:0", issues)
        assertEquals(2, result.size)
        assertTrue(result.all { it.status == Status.OPEN && it.priority == 0 })
    }

    @Test
    fun `test AND operation implicit`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("status:open priority:0", issues)
        assertEquals(2, result.size)
        assertTrue(result.all { it.status == Status.OPEN && it.priority == 0 })
    }

    @Test
    fun `test OR operation`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("status:open OR status:blocked", issues)
        assertEquals(3, result.size)
        assertTrue(result.all { it.status == Status.OPEN || it.status == Status.BLOCKED })
    }

    @Test
    fun `test NOT operation`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("NOT status:closed", issues)
        assertEquals(4, result.size)
        assertTrue(result.all { it.status != Status.CLOSED })
    }

    @Test
    fun `test complex boolean expression`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("(status:open OR status:in_progress) AND priority:0..1", issues)
        assertEquals(2, result.size)
        assertTrue(result.all {
            (it.status == Status.OPEN || it.status == Status.IN_PROGRESS) && it.priority in 0..1
        })
    }

    // Sorting tests

    @Test
    fun `test sort by priority ascending`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("sort by: priority asc", issues)
        assertEquals(5, result.size)
        assertEquals(listOf(0, 0, 1, 2, 3), result.map { it.priority })
    }

    @Test
    fun `test sort by priority descending`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("sort by: priority desc", issues)
        assertEquals(5, result.size)
        assertEquals(listOf(3, 2, 1, 0, 0), result.map { it.priority })
    }

    @Test
    fun `test sort by created date ascending`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("sort by: created asc", issues)
        assertEquals(5, result.size)
        // Oldest first
        assertEquals("TEST-4", result[0].id)
        assertEquals("TEST-5", result[4].id)
    }

    @Test
    fun `test multi-level sort`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("sort by: priority asc, updated desc", issues)
        assertEquals(5, result.size)
        // First by priority (0, 0, 1, 2, 3)
        // Then by updated (most recent first within same priority)
        assertEquals(listOf(0, 0, 1, 2, 3), result.map { it.priority })
        // Within priority 0, TEST-5 should come before TEST-1 (more recent update)
        val priority0Issues = result.filter { it.priority == 0 }
        assertEquals("TEST-5", priority0Issues[0].id)
        assertEquals("TEST-1", priority0Issues[1].id)
    }

    @Test
    fun `test filter with sort`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("status:open sort by: priority asc", issues)
        assertEquals(2, result.size)
        assertTrue(result.all { it.status == Status.OPEN })
        assertEquals(listOf(0, 0), result.map { it.priority })
    }

    // Edge cases

    @Test
    fun `test empty query returns all issues`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("", issues)
        assertEquals(5, result.size)
    }

    @Test
    fun `test query matching no issues`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("status:hooked", issues)
        assertEquals(0, result.size)
    }

    @Test
    fun `test query with only sort`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("sort by: priority asc", issues)
        assertEquals(5, result.size)
        assertEquals(listOf(0, 0, 1, 2, 3), result.map { it.priority })
    }

    @Test
    fun `test case-insensitive string matching`() {
        val issues = createTestIssues()
        val result1 = parseAndEvaluate("status:OPEN", issues)
        val result2 = parseAndEvaluate("status:open", issues)
        val result3 = parseAndEvaluate("status:Open", issues)
        assertEquals(result1.size, result2.size)
        assertEquals(result1.size, result3.size)
    }

    @Test
    fun `test assignee unassigned alias`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("assignee:unassigned", issues)
        assertEquals(1, result.size)
        assertNull(result[0].assignee)
    }

    // Minimal working examples for debugging

    @Test
    fun `minimal example - simple filter and print results`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("status:open", issues)

        println("\n=== Minimal Example: status:open ===")
        println("Total issues: ${issues.size}")
        println("Filtered issues: ${result.size}")
        println("Results:")
        result.forEach { println("  - ${it.id}: ${it.title} (${it.status})") }

        assertEquals(2, result.size)
    }

    @Test
    fun `minimal example - AND query`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("status:open AND priority:0", issues)

        println("\n=== Minimal Example: status:open AND priority:0 ===")
        println("Filtered issues: ${result.size}")
        result.forEach { println("  - ${it.id}: ${it.title} (status=${it.status}, priority=${it.priority})") }

        assertEquals(2, result.size)
    }

    @Test
    fun `minimal example - range query`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("priority:0..2", issues)

        println("\n=== Minimal Example: priority:0..2 ===")
        println("Filtered issues: ${result.size}")
        result.forEach { println("  - ${it.id}: ${it.title} (priority=${it.priority})") }

        assertEquals(4, result.size)
    }

    @Test
    fun `minimal example - with sorting`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("status:open sort by: priority asc, updated desc", issues)

        println("\n=== Minimal Example: status:open sort by: priority asc, updated desc ===")
        println("Filtered and sorted issues: ${result.size}")
        result.forEach {
            println("  - ${it.id}: ${it.title}")
            println("    priority=${it.priority}, updated=${it.updatedAt}")
        }

        assertEquals(2, result.size)
    }

    @Test
    fun `minimal example - label search`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("label:frontend", issues)

        println("\n=== Minimal Example: label:frontend ===")
        println("Filtered issues: ${result.size}")
        result.forEach { println("  - ${it.id}: ${it.title} (labels=${it.labels.joinToString(", ")})") }

        assertEquals(3, result.size)
    }

    @Test
    fun `minimal example - unassigned issues`() {
        val issues = createTestIssues()
        val result = parseAndEvaluate("assignee:null", issues)

        println("\n=== Minimal Example: assignee:null ===")
        println("Unassigned issues: ${result.size}")
        result.forEach { println("  - ${it.id}: ${it.title} (assignee=${it.assignee})") }

        assertEquals(1, result.size)
    }

    @Test
    fun `end-to-end test - complex real-world query`() {
        val issues = createTestIssues()
        val query = "(status:open OR status:in_progress) AND priority:0..1 label:frontend sort by: priority asc"
        val result = parseAndEvaluate(query, issues)

        println("\n=== Complex Query ===")
        println("Query: $query")
        println("Results: ${result.size}")
        result.forEach {
            println("  - ${it.id}: ${it.title}")
            println("    status=${it.status}, priority=${it.priority}, labels=${it.labels.joinToString(", ")}")
        }

        // Should match: TEST-1 (open, p0, frontend), TEST-2 (in_progress, p1, frontend)
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "TEST-1" })
        assertTrue(result.any { it.id == "TEST-2" })
    }
}
