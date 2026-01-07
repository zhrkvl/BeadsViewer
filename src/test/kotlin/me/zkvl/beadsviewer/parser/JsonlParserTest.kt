package me.zkvl.beadsviewer.parser

import me.zkvl.beadsviewer.model.Status
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DisplayName("JsonlParser Tests")
class JsonlParserTest {

    @TempDir
    lateinit var tempDir: Path

    private val parser = JsonlParser()

    @Test
    @DisplayName("should parse valid JSONL file with multiple issues")
    fun testParseValidJsonl() {
        val file = tempDir.resolve("issues.jsonl")
        file.writeText("""
            {"id":"TEST-001","title":"First Issue","description":"Desc1","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}
            {"id":"TEST-002","title":"Second Issue","description":"Desc2","status":"closed","priority":2,"issue_type":"bug","created_at":"2026-01-07T11:00:00Z","created_by":"user","updated_at":"2026-01-07T11:00:00Z"}
        """.trimIndent())

        val issues = parser.parseFile(file)

        assertEquals(2, issues.size)
        assertEquals("TEST-001", issues[0].id)
        assertEquals("First Issue", issues[0].title)
        assertEquals(Status.OPEN, issues[0].status)
        assertEquals("TEST-002", issues[1].id)
        assertEquals("Second Issue", issues[1].title)
        assertEquals(Status.CLOSED, issues[1].status)
    }

    @Test
    @DisplayName("should skip empty lines")
    fun testSkipEmptyLines() {
        val file = tempDir.resolve("issues.jsonl")
        file.writeText("""
            {"id":"TEST-001","title":"First","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}

            {"id":"TEST-002","title":"Second","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}

        """.trimIndent())

        val issues = parser.parseFile(file)
        assertEquals(2, issues.size)
    }

    @Test
    @DisplayName("should handle issues with optional fields")
    fun testHandleOptionalFields() {
        val file = tempDir.resolve("issues.jsonl")
        file.writeText("""
            {"id":"TEST-001","title":"Minimal","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}
            {"id":"TEST-002","title":"Full","description":"Desc","status":"closed","priority":1,"issue_type":"task","assignee":"john","due_date":"2026-01-10T10:00:00Z","closed_at":"2026-01-08T10:00:00Z","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-08T10:00:00Z","labels":["urgent","backend"],"dependencies":[],"comments":[]}
        """.trimIndent())

        val issues = parser.parseFile(file)

        assertEquals(2, issues.size)
        assertEquals(null, issues[0].assignee)
        assertEquals(null, issues[0].dueDate)
        assertEquals("john", issues[1].assignee)
        assertNotNull(issues[1].dueDate)
        assertNotNull(issues[1].closedAt)
        assertEquals(2, issues[1].labels.size)
    }

    @Test
    @DisplayName("should throw ParseException.FileNotFound for non-existent file")
    fun testThrowFileNotFoundException() {
        val file = tempDir.resolve("nonexistent.jsonl")
        assertThrows<ParseException.FileNotFound> {
            parser.parseFile(file)
        }
    }

    @Test
    @DisplayName("should throw ParseException.InvalidJson for malformed JSON")
    fun testThrowInvalidJsonException() {
        val file = tempDir.resolve("issues.jsonl")
        file.writeText("""
            {"id":"TEST-001","title":"Valid","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}
            {invalid json here}
        """.trimIndent())

        val exception = assertThrows<ParseException.InvalidJson> {
            parser.parseFile(file)
        }
        assertEquals(2, exception.lineNumber)
    }

    @Test
    @DisplayName("should ignore unknown fields (forward compatibility)")
    fun testIgnoreUnknownFields() {
        val file = tempDir.resolve("issues.jsonl")
        file.writeText("""
            {"id":"TEST-001","title":"Issue","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z","unknown_field":"value","another_unknown":123}
        """.trimIndent())

        val issues = parser.parseFile(file)
        assertEquals(1, issues.size)
        assertEquals("TEST-001", issues[0].id)
    }

    @Test
    @DisplayName("parseFileSequence should handle partial failures gracefully")
    fun testParseFileSequenceHandlesPartialFailures() {
        val file = tempDir.resolve("issues.jsonl")
        file.writeText("""
            {"id":"TEST-001","title":"Valid1","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}
            {invalid}
            {"id":"TEST-003","title":"Valid2","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}
        """.trimIndent())

        val results = parser.parseFileSequence(file).toList()

        assertEquals(3, results.size)
        assertTrue(results[0].isSuccess)
        assertTrue(results[1].isFailure)
        assertTrue(results[2].isSuccess)
    }

    @Test
    @DisplayName("should handle issues with dependencies")
    fun testHandleIssuesWithDependencies() {
        val file = tempDir.resolve("issues.jsonl")
        file.writeText("""
            {"id":"TEST-001","title":"Issue","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z","dependencies":[{"issue_id":"TEST-001","depends_on_id":"TEST-002","type":"blocks","created_at":"2026-01-07T10:00:00Z","created_by":"user"}]}
        """.trimIndent())

        val issues = parser.parseFile(file)

        assertEquals(1, issues.size)
        assertEquals(1, issues[0].dependencies.size)
        assertEquals("TEST-001", issues[0].dependencies[0].issueId)
        assertEquals("TEST-002", issues[0].dependencies[0].dependsOnId)
    }

    @Test
    @DisplayName("countIssues should count non-empty lines")
    fun testCountIssues() {
        val file = tempDir.resolve("issues.jsonl")
        file.writeText("""
            {"id":"TEST-001","title":"First","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}

            {"id":"TEST-002","title":"Second","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}
        """.trimIndent())

        val count = parser.countIssues(file)
        assertEquals(2, count)
    }

    @Test
    @DisplayName("should parse custom issue types")
    fun testParseCustomIssueTypes() {
        val file = tempDir.resolve("issues.jsonl")
        file.writeText("""
            {"id":"TEST-001","title":"Agent","description":"Desc","status":"open","priority":1,"issue_type":"agent","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}
            {"id":"TEST-002","title":"Role","description":"Desc","status":"open","priority":1,"issue_type":"role","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}
        """.trimIndent())

        val issues = parser.parseFile(file)

        assertEquals(2, issues.size)
        // Custom types should be wrapped in IssueType.Custom
        assertEquals("agent", issues[0].issueType.value)
        assertEquals("role", issues[1].issueType.value)
    }
}
