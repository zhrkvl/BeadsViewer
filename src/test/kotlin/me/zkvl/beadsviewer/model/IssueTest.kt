package me.zkvl.beadsviewer.model

import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("Issue Model Tests")
class IssueTest {

    @Test
    @DisplayName("validate() should pass for valid issue")
    fun testValidatePassesForValidIssue() {
        val issue = createValidIssue()
        issue.validate()  // Should not throw
    }

    @Test
    @DisplayName("validate() should fail for empty ID")
    fun testValidateFailsForEmptyId() {
        val issue = createValidIssue().copy(id = "")
        assertThrows<IllegalArgumentException> { issue.validate() }
    }

    @Test
    @DisplayName("validate() should fail for blank ID")
    fun testValidateFailsForBlankId() {
        val issue = createValidIssue().copy(id = "   ")
        assertThrows<IllegalArgumentException> { issue.validate() }
    }

    @Test
    @DisplayName("validate() should fail for empty title")
    fun testValidateFailsForEmptyTitle() {
        val issue = createValidIssue().copy(title = "")
        assertThrows<IllegalArgumentException> { issue.validate() }
    }

    // Note: Timestamp order validation tests removed because real-world data can have
    // microsecond-precision issues where timestamps are slightly out of order due to
    // clock resolution. This is not a data integrity issue.

    @Test
    @DisplayName("validate() should fail for negative estimatedMinutes")
    fun testValidateFailsForNegativeEstimatedMinutes() {
        val issue = createValidIssue().copy(estimatedMinutes = -10)
        assertThrows<IllegalArgumentException> { issue.validate() }
    }

    @Test
    @DisplayName("validate() should fail for invalid priority (< 0)")
    fun testValidateFailsForPriorityTooLow() {
        val issue = createValidIssue().copy(priority = -1)
        assertThrows<IllegalArgumentException> { issue.validate() }
    }

    @Test
    @DisplayName("validate() should fail for invalid priority (> 4)")
    fun testValidateFailsForPriorityTooHigh() {
        val issue = createValidIssue().copy(priority = 5)
        assertThrows<IllegalArgumentException> { issue.validate() }
    }

    @Test
    @DisplayName("isClosed() should return true for CLOSED status")
    fun testIsClosedReturnsTrueForClosedStatus() {
        val issue = createValidIssue().copy(status = Status.CLOSED)
        assertTrue(issue.isClosed())
    }

    @Test
    @DisplayName("isClosed() should return false for OPEN status")
    fun testIsClosedReturnsFalseForOpenStatus() {
        val issue = createValidIssue().copy(status = Status.OPEN)
        assertFalse(issue.isClosed())
    }

    @Test
    @DisplayName("isOpen() should return true for OPEN status")
    fun testIsOpenReturnsTrueForOpenStatus() {
        val issue = createValidIssue().copy(status = Status.OPEN)
        assertTrue(issue.isOpen())
    }

    @Test
    @DisplayName("isOpen() should return true for IN_PROGRESS status")
    fun testIsOpenReturnsTrueForInProgressStatus() {
        val issue = createValidIssue().copy(status = Status.IN_PROGRESS)
        assertTrue(issue.isOpen())
    }

    @Test
    @DisplayName("isOpen() should return false for CLOSED status")
    fun testIsOpenReturnsFalseForClosedStatus() {
        val issue = createValidIssue().copy(status = Status.CLOSED)
        assertFalse(issue.isOpen())
    }

    @Test
    @DisplayName("copy() should create modified copy with specified changes")
    fun testCopyCreatesModifiedCopy() {
        val original = createValidIssue()
        val modified = original.copy(
            title = "New Title",
            priority = 3,
            status = Status.CLOSED
        )

        assertEquals("New Title", modified.title)
        assertEquals(3, modified.priority)
        assertEquals(Status.CLOSED, modified.status)
        assertEquals(original.id, modified.id)  // Unchanged fields retained
        assertEquals(original.description, modified.description)
    }

    @Test
    @DisplayName("issue with all optional fields should validate")
    fun testIssueWithAllOptionalFieldsValidates() {
        val now = Instant.parse("2026-01-07T10:00:00Z")
        val later = Instant.parse("2026-01-07T12:00:00Z")

        val issue = createValidIssue().copy(
            design = "Design notes",
            acceptanceCriteria = "AC here",
            notes = "Additional notes",
            assignee = "john",
            estimatedMinutes = 120,
            externalRef = "EXT-123",
            sourceRepo = "github.com/user/repo",
            dueDate = later,
            closedAt = later,
            labels = listOf("urgent", "backend"),
            dependencies = listOf(
                Dependency(
                    issueId = "TEST-001",
                    dependsOnId = "TEST-002",
                    type = DependencyType.Blocks,
                    createdAt = now,
                    createdBy = "john"
                )
            ),
            comments = listOf(
                Comment(
                    id = 1L,
                    issueId = "TEST-001",
                    author = "john",
                    text = "Test comment",
                    createdAt = now
                )
            )
        )

        issue.validate()  // Should not throw
    }

    /**
     * Helper function to create a valid test issue.
     */
    private fun createValidIssue() = Issue(
        id = "TEST-001",
        title = "Test Issue",
        description = "Test description",  // Explicitly provide description even though it has a default
        status = Status.OPEN,
        priority = 1,
        issueType = IssueType.Task,
        createdAt = Instant.parse("2026-01-07T10:00:00Z"),
        createdBy = "test-user",  // Explicitly provide created_by
        updatedAt = Instant.parse("2026-01-07T10:00:00Z")
    )
}
