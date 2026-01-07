package me.zkvl.beadsviewer.serialization

import kotlinx.serialization.json.Json
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.model.Status
import me.zkvl.beadsviewer.model.IssueType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DisplayName("JSONL Parsing Tests")
class JsonlParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    @DisplayName("Should parse JSONL line with kotlinx.serialization")
    fun testParseJsonlLine() {
        val jsonlLine = """{"id":"TEST-001","title":"Test Issue","description":"Test desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"tester","updated_at":"2026-01-07T10:00:00Z","dependencies":[]}"""

        val issue = json.decodeFromString<Issue>(jsonlLine)

        assertEquals("TEST-001", issue.id)
        assertEquals("Test Issue", issue.title)
        assertEquals(Status.OPEN, issue.status)
        assertEquals(1, issue.priority)
        assertEquals(IssueType.Task, issue.issueType)
    }

    @Test
    @DisplayName("Should parse minimal JSONL with required fields")
    fun testParseMinimalJsonl() {
        val minimalJsonl = """{"id":"MIN-001","title":"Minimal","description":"Desc","status":"open","priority":2,"issue_type":"bug","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}"""

        val issue = json.decodeFromString<Issue>(minimalJsonl)

        assertNotNull(issue)
        assertEquals("MIN-001", issue.id)
        assertEquals(IssueType.Bug, issue.issueType)
    }

    @Test
    @DisplayName("Should parse JSONL with all optional fields")
    fun testParseJsonlWithOptionalFields() {
        val jsonlLine = """{"id":"FULL-001","title":"Full Issue","description":"Description","status":"closed","priority":0,"issue_type":"feature","assignee":"john","due_date":"2026-01-10T10:00:00Z","closed_at":"2026-01-08T10:00:00Z","created_at":"2026-01-07T10:00:00Z","created_by":"jane","updated_at":"2026-01-08T10:00:00Z","labels":["urgent","backend"],"estimated_minutes":120,"design":"Design notes","acceptance_criteria":"Must work","notes":"Some notes","external_ref":"EXT-123"}"""

        val issue = json.decodeFromString<Issue>(jsonlLine)

        assertEquals("FULL-001", issue.id)
        assertEquals("Full Issue", issue.title)
        assertEquals(Status.CLOSED, issue.status)
        assertEquals("john", issue.assignee)
        assertNotNull(issue.dueDate)
        assertNotNull(issue.closedAt)
        assertEquals(2, issue.labels.size)
        assertEquals(120, issue.estimatedMinutes)
        assertEquals("Design notes", issue.design)
    }

    @Test
    @DisplayName("Should parse custom issue types (Gastown)")
    fun testParseCustomIssueTypes() {
        val jsonlLine = """{"id":"CUSTOM-001","title":"Agent Issue","description":"Desc","status":"open","priority":1,"issue_type":"agent","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}"""

        val issue = json.decodeFromString<Issue>(jsonlLine)

        assertEquals("CUSTOM-001", issue.id)
        assertEquals("agent", issue.issueType.value)
    }
}
