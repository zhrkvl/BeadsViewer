package me.zkvl.beadsviewer.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Serializable
data class BeadsIssue(
    val id: String,
    val title: String,
    val description: String,
    val status: String,
    val priority: Int,
    val issue_type: String,
    val created_at: String,
    val created_by: String,
    val updated_at: String
)

@DisplayName("JSONL Parsing Tests")
class JsonlParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    @DisplayName("Should parse JSONL line with kotlinx.serialization")
    fun testParseJsonlLine() {
        val jsonlLine = """{"id":"TEST-001","title":"Test Issue","description":"Test desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"tester","updated_at":"2026-01-07T10:00:00Z","dependencies":[]}"""

        val issue = json.decodeFromString<BeadsIssue>(jsonlLine)

        assertEquals("TEST-001", issue.id)
        assertEquals("Test Issue", issue.title)
        assertEquals("open", issue.status)
        assertEquals(1, issue.priority)
    }

    @Test
    @DisplayName("Should parse minimal JSONL with required fields")
    fun testParseMinimalJsonl() {
        val minimalJsonl = """{"id":"MIN-001","title":"Minimal","description":"Desc","status":"open","priority":2,"issue_type":"bug","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}"""

        val issue = json.decodeFromString<BeadsIssue>(minimalJsonl)

        assertNotNull(issue)
        assertEquals("MIN-001", issue.id)
        assertEquals("bug", issue.issue_type)
    }
}
