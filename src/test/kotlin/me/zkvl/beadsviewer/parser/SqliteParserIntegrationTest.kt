package me.zkvl.beadsviewer.parser

import me.zkvl.beadsviewer.model.DataSource
import me.zkvl.beadsviewer.model.Status
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DisplayName("SqliteParser Integration Tests")
class SqliteParserIntegrationTest {

    @TempDir
    lateinit var tempDir: Path

    private val parser = SqliteParser()
    private lateinit var dbFile: Path

    @BeforeEach
    fun setUp() {
        dbFile = tempDir.resolve("test-beads.db")
        createTestDatabase(dbFile)
    }

    @Test
    @DisplayName("should parse issues from SQLite database")
    fun testParseIssuesFromDatabase() {
        val result = parser.parseDatabase(dbFile)

        assertTrue(result.isSuccess, "Should successfully parse database")
        val loadResult = result.getOrThrow()

        assertEquals(3, loadResult.issues.size, "Should load 3 issues")
        assertEquals(DataSource.SQLITE, loadResult.source, "Source should be SQLITE")
        assertTrue(loadResult.timestamp > 0, "Timestamp should be set")
    }

    @Test
    @DisplayName("should correctly parse issue fields")
    fun testParseIssueFields() {
        val result = parser.parseDatabase(dbFile)
        val issues = result.getOrThrow().issues

        val issue = issues.find { it.id == "TEST-001" }
        assertNotNull(issue, "Issue TEST-001 should exist")
        assertEquals("First Test Issue", issue.title)
        assertEquals("This is a test description", issue.description)
        assertEquals(Status.OPEN, issue.status)
        assertEquals(1, issue.priority)
        assertEquals("user1", issue.createdBy)
        assertNotNull(issue.createdAt)
    }

    @Test
    @DisplayName("should parse dependencies correctly")
    fun testParseDependencies() {
        val result = parser.parseDatabase(dbFile)
        val issues = result.getOrThrow().issues

        val issue = issues.find { it.id == "TEST-002" }
        assertNotNull(issue, "Issue TEST-002 should exist")
        assertEquals(1, issue.dependencies.size, "Should have 1 dependency")
        assertEquals("TEST-001", issue.dependencies[0].dependsOnId, "Should depend on TEST-001")
    }

    @Test
    @DisplayName("should parse labels correctly")
    fun testParseLabels() {
        val result = parser.parseDatabase(dbFile)
        val issues = result.getOrThrow().issues

        val issue = issues.find { it.id == "TEST-001" }
        assertNotNull(issue, "Issue TEST-001 should exist")
        assertEquals(2, issue.labels.size, "Should have 2 labels")
        assertTrue(issue.labels.contains("urgent"), "Should have 'urgent' label")
        assertTrue(issue.labels.contains("backend"), "Should have 'backend' label")
    }

    @Test
    @DisplayName("should parse comments correctly")
    fun testParseComments() {
        val result = parser.parseDatabase(dbFile)
        val issues = result.getOrThrow().issues

        val issue = issues.find { it.id == "TEST-001" }
        assertNotNull(issue, "Issue TEST-001 should exist")
        assertEquals(1, issue.comments.size, "Should have 1 comment")
        assertEquals("This is a test comment", issue.comments[0].text)
        assertEquals("user1", issue.comments[0].author)
    }

    @Test
    @DisplayName("should track dirty issues")
    fun testParseDirtyIssues() {
        val result = parser.parseDatabase(dbFile)
        val loadResult = result.getOrThrow()

        assertEquals(1, loadResult.dirtyIssueIds.size, "Should have 1 dirty issue")
        assertTrue(loadResult.dirtyIssueIds.contains("TEST-002"), "TEST-002 should be dirty")
    }

    @Test
    @DisplayName("should handle empty database gracefully")
    fun testEmptyDatabase() {
        val emptyDbFile = tempDir.resolve("empty.db")
        createEmptyDatabase(emptyDbFile)

        val result = parser.parseDatabase(emptyDbFile)

        assertTrue(result.isSuccess, "Should successfully parse empty database")
        val loadResult = result.getOrThrow()
        assertEquals(0, loadResult.issues.size, "Should have 0 issues")
        assertEquals(0, loadResult.dirtyIssueIds.size, "Should have 0 dirty issues")
    }

    @Test
    @DisplayName("should handle missing file gracefully")
    fun testMissingFile() {
        val missingFile = tempDir.resolve("nonexistent.db")

        val result = parser.parseDatabase(missingFile)

        assertTrue(result.isFailure, "Should fail for missing file")
        assertTrue(result.exceptionOrNull() is ParseException.FileNotFound, "Should throw FileNotFound")
    }

    @Test
    @DisplayName("should handle legacy database without dirty_issues table")
    fun testLegacyDatabaseWithoutDirtyIssuesTable() {
        val legacyDbFile = tempDir.resolve("legacy.db")
        createLegacyDatabase(legacyDbFile)

        val result = parser.parseDatabase(legacyDbFile)

        assertTrue(result.isSuccess, "Should successfully parse legacy database")
        val loadResult = result.getOrThrow()
        assertEquals(1, loadResult.issues.size, "Should load 1 issue")
        assertEquals(0, loadResult.dirtyIssueIds.size, "Should have 0 dirty issues (table missing)")
    }

    /**
     * Creates a test SQLite database with sample data.
     * Includes issues, dependencies, labels, comments, and dirty_issues tables.
     */
    private fun createTestDatabase(file: Path) {
        val connection = DriverManager.getConnection("jdbc:sqlite:$file")
        connection.use { conn ->
            val stmt = conn.createStatement()

            // Create issues table
            stmt.executeUpdate("""
                CREATE TABLE issues (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT,
                    status TEXT NOT NULL,
                    priority INTEGER NOT NULL,
                    issue_type TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    created_by TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    assignee TEXT,
                    due_date TEXT,
                    defer_until TEXT,
                    closed_at TEXT,
                    deleted_at TEXT,
                    parent_id TEXT,
                    estimated_minutes INTEGER,
                    actual_minutes INTEGER,
                    points INTEGER,
                    rank INTEGER,
                    sprint TEXT,
                    milestone TEXT,
                    notes TEXT,
                    molecule_type TEXT,
                    is_gate INTEGER,
                    is_pinned INTEGER,
                    completion_criteria TEXT,
                    expected_outcome TEXT,
                    gate_id TEXT,
                    root_cause TEXT,
                    resolution TEXT,
                    reproducible_steps TEXT,
                    environment TEXT,
                    affected_version TEXT,
                    target_version TEXT,
                    severity TEXT,
                    pr_title TEXT,
                    pr_number TEXT,
                    pr_url TEXT,
                    merge_strategy TEXT,
                    base_branch TEXT,
                    target_branch TEXT,
                    requested_reviewers TEXT,
                    convoy_task_id TEXT,
                    convoy_id TEXT,
                    convoy_position INTEGER,
                    convoy_blocker_ids TEXT,
                    is_convoy_leader INTEGER,
                    patrol_frequency TEXT,
                    last_patrol_at TEXT,
                    next_patrol_at TEXT
                )
            """.trimIndent())

            // Insert test issues
            stmt.executeUpdate("""
                INSERT INTO issues (
                    id, title, description, status, priority, issue_type,
                    created_at, created_by, updated_at
                ) VALUES
                ('TEST-001', 'First Test Issue', 'This is a test description', 'open', 1, 'task',
                 '2026-01-07T10:00:00Z', 'user1', '2026-01-07T10:00:00Z'),
                ('TEST-002', 'Second Test Issue', 'Another test', 'in_progress', 2, 'bug',
                 '2026-01-07T11:00:00Z', 'user2', '2026-01-07T11:00:00Z'),
                ('TEST-003', 'Third Test Issue', 'Yet another test', 'closed', 3, 'feature',
                 '2026-01-07T12:00:00Z', 'user1', '2026-01-07T12:00:00Z')
            """.trimIndent())

            // Create dependencies table
            stmt.executeUpdate("""
                CREATE TABLE dependencies (
                    issue_id TEXT NOT NULL,
                    depends_on_id TEXT NOT NULL,
                    type TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    created_by TEXT NOT NULL,
                    PRIMARY KEY (issue_id, depends_on_id)
                )
            """.trimIndent())

            stmt.executeUpdate("""
                INSERT INTO dependencies (issue_id, depends_on_id, type, created_at, created_by)
                VALUES ('TEST-002', 'TEST-001', 'blocks', '2026-01-07T11:00:00Z', 'user2')
            """.trimIndent())

            // Create labels table
            stmt.executeUpdate("""
                CREATE TABLE labels (
                    issue_id TEXT NOT NULL,
                    label TEXT NOT NULL,
                    PRIMARY KEY (issue_id, label)
                )
            """.trimIndent())

            stmt.executeUpdate("""
                INSERT INTO labels (issue_id, label) VALUES
                ('TEST-001', 'urgent'),
                ('TEST-001', 'backend')
            """.trimIndent())

            // Create comments table
            stmt.executeUpdate("""
                CREATE TABLE comments (
                    id TEXT PRIMARY KEY,
                    issue_id TEXT NOT NULL,
                    author TEXT NOT NULL,
                    text TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
            """.trimIndent())

            stmt.executeUpdate("""
                INSERT INTO comments (id, issue_id, author, text, created_at)
                VALUES ('CMT-001', 'TEST-001', 'user1', 'This is a test comment', '2026-01-07T10:30:00Z')
            """.trimIndent())

            // Create dirty_issues table
            stmt.executeUpdate("""
                CREATE TABLE dirty_issues (
                    issue_id TEXT PRIMARY KEY
                )
            """.trimIndent())

            stmt.executeUpdate("""
                INSERT INTO dirty_issues (issue_id) VALUES ('TEST-002')
            """.trimIndent())

            stmt.close()
        }
    }

    /**
     * Creates an empty SQLite database with schema but no data.
     */
    private fun createEmptyDatabase(file: Path) {
        val connection = DriverManager.getConnection("jdbc:sqlite:$file")
        connection.use { conn ->
            val stmt = conn.createStatement()

            // Create minimal schema
            stmt.executeUpdate("""
                CREATE TABLE issues (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT,
                    status TEXT NOT NULL,
                    priority INTEGER NOT NULL,
                    issue_type TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    created_by TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    deleted_at TEXT
                )
            """.trimIndent())

            stmt.close()
        }
    }

    /**
     * Creates a legacy SQLite database without dirty_issues table.
     * Tests backward compatibility.
     */
    private fun createLegacyDatabase(file: Path) {
        val connection = DriverManager.getConnection("jdbc:sqlite:$file")
        connection.use { conn ->
            val stmt = conn.createStatement()

            // Create issues table without dirty_issues
            stmt.executeUpdate("""
                CREATE TABLE issues (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT,
                    status TEXT NOT NULL,
                    priority INTEGER NOT NULL,
                    issue_type TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    created_by TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    deleted_at TEXT
                )
            """.trimIndent())

            stmt.executeUpdate("""
                INSERT INTO issues (
                    id, title, description, status, priority, issue_type,
                    created_at, created_by, updated_at
                ) VALUES
                ('LEGACY-001', 'Legacy Issue', 'Old database', 'open', 1, 'task',
                 '2026-01-07T10:00:00Z', 'user1', '2026-01-07T10:00:00Z')
            """.trimIndent())

            stmt.close()
        }
    }
}
