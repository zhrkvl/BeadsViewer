package me.zkvl.beadsviewer.parser

import com.intellij.openapi.diagnostic.Logger
import me.zkvl.beadsviewer.model.*
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import kotlinx.datetime.Instant

/**
 * SQLite parser for Beads database (.beads/beads.db).
 *
 * Reads issues from the SQLite database created by the bd daemon. This provides:
 * - Faster access compared to JSONL parsing
 * - Real-time updates (shows latest unsaved changes)
 * - Dirty issue tracking (issues not yet synced to git)
 *
 * The parser implements graceful error handling - any SQL errors result in
 * Result.failure rather than crashes, allowing fallback to JSONL parsing.
 *
 * Database schema (bd version 0.44.0):
 * - issues: Main issue data (48 columns)
 * - dependencies: Issue relationships
 * - labels: Issue tags
 * - comments: Issue comments
 * - dirty_issues: Unsaved changes tracker
 */
class SqliteParser(
    private val config: ParserConfig = ParserConfig()
) {
    private val logger = Logger.getInstance(SqliteParser::class.java)

    /**
     * Parses all issues from a SQLite database file.
     *
     * This method:
     * 1. Opens a read-only connection to the database
     * 2. Reads issues, dependencies, labels, comments
     * 3. Identifies dirty (unsaved) issues
     * 4. Assembles complete Issue objects
     * 5. Returns Result with IssueLoadResult or error
     *
     * @param file Path to the beads.db file (typically .beads/beads.db)
     * @return Result containing IssueLoadResult on success, or ParseException on failure
     */
    fun parseDatabase(file: Path): Result<IssueLoadResult> {
        // Check if file exists
        if (!file.toFile().exists()) {
            val error = ParseException.FileNotFound(file)
            logger.info("SQLite database not found: $file")
            return Result.failure(error)
        }

        // Try to parse the database with proper resource management
        return try {
            openConnection(file).use { connection ->
                // Parse all components
                val issuesMap = parseIssues(connection)
                val dependenciesMap = parseDependencies(connection)
                val labelsMap = parseLabels(connection)
                val commentsMap = parseComments(connection)
                val dirtyIssueIds = parseDirtyIssues(connection)

                // Assemble complete issues with all relationships
                val completeIssues = issuesMap.values.map { issue ->
                    issue.copy(
                        dependencies = dependenciesMap[issue.id] ?: emptyList(),
                        labels = labelsMap[issue.id] ?: emptyList(),
                        comments = commentsMap[issue.id] ?: emptyList()
                    )
                }

                // Validate if enabled
                if (config.validateOnParse) {
                    completeIssues.forEach { issue ->
                        try {
                            issue.validate()
                        } catch (e: IllegalArgumentException) {
                            logger.warn("Validation failed for issue ${issue.id}: ${e.message}")
                            throw ParseException.ValidationError(issue.id, e)
                        }
                    }
                }

                val result = IssueLoadResult(
                    issues = completeIssues,
                    dirtyIssueIds = dirtyIssueIds,
                    source = DataSource.SQLITE,
                    timestamp = System.currentTimeMillis()
                )

                logger.info("Loaded ${completeIssues.size} issues from SQLite (${dirtyIssueIds.size} dirty)")
                Result.success(result)
            }
        } catch (e: ParseException) {
            logger.warn("Failed to parse SQLite database: ${e.message}")
            Result.failure(e)
        } catch (e: SQLException) {
            val error = ParseException.SqlError(file, "database parsing", e)
            logger.warn("SQL error parsing database: ${e.message}", e)
            Result.failure(error)
        } catch (e: Exception) {
            val error = ParseException.IoError(file, e)
            logger.error("Unexpected error parsing SQLite database", e)
            Result.failure(error)
        }
    }

    /**
     * Opens a read-only connection to the SQLite database.
     *
     * Connection settings:
     * - Read-only mode: prevents accidental writes
     * - No auto-commit: improves read performance
     * - Short timeout: fails fast if database is locked
     *
     * @throws SQLException if connection fails
     */
    private fun openConnection(file: Path): Connection {
        try {
            // JDBC URL with read-only mode
            val url = "jdbc:sqlite:file:${file.toAbsolutePath()}?mode=ro"
            val connection = DriverManager.getConnection(url)
            connection.autoCommit = false  // Improves read performance
            return connection
        } catch (e: SQLException) {
            throw ParseException.SqlError(file, "opening connection", e)
        }
    }

    /**
     * Parses issues from the issues table.
     *
     * SQL: SELECT * FROM issues WHERE deleted_at IS NULL
     *
     * Maps all 48 columns to Issue model fields. Handles:
     * - NULL values for optional fields
     * - ISO8601 timestamp parsing to Instant
     * - Status/Priority/IssueType enums
     *
     * @return Map of issue_id -> Issue (without dependencies/labels/comments)
     */
    private fun parseIssues(connection: Connection): Map<String, Issue> {
        val issuesMap = mutableMapOf<String, Issue>()

        try {
            val stmt = connection.prepareStatement(
                "SELECT * FROM issues WHERE deleted_at IS NULL"
            )

            val rs = stmt.executeQuery()
            while (rs.next()) {
                try {
                    // Core required fields
                    val id = rs.getString("id")
                    val title = rs.getString("title")
                    val statusStr = rs.getString("status")
                    val priority = rs.getInt("priority")
                    val issueTypeStr = rs.getString("issue_type")
                    val createdAtStr = rs.getString("created_at")
                    val updatedAtStr = rs.getString("updated_at")

                    // Parse status - convert to uppercase with underscores
                    val status = try {
                        Status.valueOf(statusStr.uppercase().replace("-", "_"))
                    } catch (e: IllegalArgumentException) {
                        logger.warn("Unknown status '$statusStr' for issue $id, defaulting to OPEN")
                        Status.OPEN
                    }

                    // Parse issue type
                    val issueType = IssueType.fromString(issueTypeStr)

                    // Parse required timestamps
                    val createdAt = parseInstant(createdAtStr)
                        ?: throw IllegalStateException("created_at is required but null for issue $id")
                    val updatedAt = parseInstant(updatedAtStr)
                        ?: throw IllegalStateException("updated_at is required but null for issue $id")

                    // Parse optional fields
                    val issue = Issue(
                        id = id,
                        title = title,
                        description = rs.getStringOrNull("description") ?: "",
                        status = status,
                        priority = priority,
                        issueType = issueType,
                        design = rs.getStringOrNull("design"),
                        acceptanceCriteria = rs.getStringOrNull("acceptance_criteria"),
                        notes = rs.getStringOrNull("notes"),
                        assignee = rs.getStringOrNull("assignee"),
                        estimatedMinutes = rs.getIntOrNull("estimated_minutes"),
                        externalRef = rs.getStringOrNull("external_ref"),
                        sourceRepo = rs.getStringOrNull("source_repo"),
                        createdAt = createdAt,
                        createdBy = rs.getStringOrNull("created_by") ?: "",
                        updatedAt = updatedAt,
                        dueDate = parseInstant(rs.getStringOrNull("due_at")),
                        closedAt = parseInstant(rs.getStringOrNull("closed_at")),
                        closeReason = rs.getStringOrNull("close_reason"),
                        deletedAt = parseInstant(rs.getStringOrNull("deleted_at")),
                        deletedBy = rs.getStringOrNull("deleted_by"),
                        deleteReason = rs.getStringOrNull("delete_reason"),
                        originalType = rs.getStringOrNull("original_type"),
                        compactionLevel = rs.getIntOrNull("compaction_level") ?: 0,
                        compactedAt = parseInstant(rs.getStringOrNull("compacted_at")),
                        compactedAtCommit = rs.getStringOrNull("compacted_at_commit"),
                        originalSize = rs.getIntOrNull("original_size") ?: 0,
                        // Collections will be added later
                        labels = emptyList(),
                        dependencies = emptyList(),
                        comments = emptyList()
                    )

                    issuesMap[id] = issue
                } catch (e: Exception) {
                    // Log error but continue parsing other issues
                    val issueId = try {
                        rs.getString("id")
                    } catch (ex: Exception) {
                        "unknown"
                    }
                    logger.warn("Failed to parse issue $issueId: ${e.message}", e)
                }
            }

            rs.close()
            stmt.close()

            logger.info("Parsed ${issuesMap.size} issues from database")
        } catch (e: SQLException) {
            throw ParseException.SchemaError(
                file = Path.of(".beads/beads.db"),
                expectedTable = "issues",
                cause = e
            )
        }

        return issuesMap
    }

    /**
     * Parses dependencies from the dependencies table.
     *
     * SQL: SELECT issue_id, depends_on_id, type, created_at, created_by FROM dependencies
     *
     * Groups dependencies by issue_id.
     *
     * @return Map of issue_id -> List<Dependency>
     */
    private fun parseDependencies(connection: Connection): Map<String, List<Dependency>> {
        val dependenciesMap = mutableMapOf<String, MutableList<Dependency>>()

        try {
            val stmt = connection.prepareStatement(
                "SELECT issue_id, depends_on_id, type, created_at, created_by FROM dependencies"
            )

            val rs = stmt.executeQuery()
            var count = 0
            while (rs.next()) {
                try {
                    val issueId = rs.getString("issue_id")
                    val dependsOnId = rs.getString("depends_on_id")
                    val typeStr = rs.getStringOrNull("type") ?: "blocks"  // Default to blocks if null
                    val createdAtStr = rs.getString("created_at")
                    val createdBy = rs.getString("created_by")

                    // Parse dependency type
                    val type = DependencyType.fromString(typeStr)

                    // Parse timestamp
                    val createdAt = parseInstant(createdAtStr)
                        ?: throw IllegalStateException("created_at is required for dependency")

                    val dependency = Dependency(
                        issueId = issueId,
                        dependsOnId = dependsOnId,
                        type = type,
                        createdAt = createdAt,
                        createdBy = createdBy
                    )

                    // Group by issue_id
                    dependenciesMap.getOrPut(issueId) { mutableListOf() }.add(dependency)
                    count++
                } catch (e: Exception) {
                    logger.warn("Failed to parse dependency: ${e.message}", e)
                }
            }

            rs.close()
            stmt.close()

            logger.info("Parsed $count dependencies from database")
        } catch (e: SQLException) {
            // If dependencies table doesn't exist or has schema issues, just return empty map
            // This allows graceful degradation for databases without dependencies
            logger.info("Could not parse dependencies table: ${e.message}")
            return emptyMap()
        }

        return dependenciesMap
    }

    /**
     * Parses labels from the labels table.
     *
     * SQL: SELECT issue_id, label FROM labels
     *
     * Groups labels by issue_id.
     *
     * @return Map of issue_id -> List<String>
     */
    private fun parseLabels(connection: Connection): Map<String, List<String>> {
        val labelsMap = mutableMapOf<String, MutableList<String>>()

        try {
            val stmt = connection.prepareStatement(
                "SELECT issue_id, label FROM labels"
            )

            val rs = stmt.executeQuery()
            var count = 0
            while (rs.next()) {
                try {
                    val issueId = rs.getString("issue_id")
                    val label = rs.getString("label")

                    // Group by issue_id
                    labelsMap.getOrPut(issueId) { mutableListOf() }.add(label)
                    count++
                } catch (e: Exception) {
                    logger.warn("Failed to parse label: ${e.message}", e)
                }
            }

            rs.close()
            stmt.close()

            logger.info("Parsed $count labels from database")
        } catch (e: SQLException) {
            // If labels table doesn't exist, just return empty map
            logger.info("Could not parse labels table: ${e.message}")
            return emptyMap()
        }

        return labelsMap
    }

    /**
     * Parses comments from the comments table.
     *
     * SQL: SELECT id, issue_id, author, text, created_at FROM comments ORDER BY created_at
     *
     * Groups comments by issue_id, maintaining chronological order.
     *
     * @return Map of issue_id -> List<Comment>
     */
    private fun parseComments(connection: Connection): Map<String, List<Comment>> {
        val commentsMap = mutableMapOf<String, MutableList<Comment>>()

        try {
            val stmt = connection.prepareStatement(
                "SELECT id, issue_id, author, text, created_at FROM comments ORDER BY created_at"
            )

            val rs = stmt.executeQuery()
            var count = 0
            while (rs.next()) {
                try {
                    val id = rs.getLong("id")
                    val issueId = rs.getString("issue_id")
                    val author = rs.getString("author")
                    val text = rs.getString("text")
                    val createdAtStr = rs.getString("created_at")

                    // Parse timestamp
                    val createdAt = parseInstant(createdAtStr)
                        ?: throw IllegalStateException("created_at is required for comment")

                    val comment = Comment(
                        id = id,
                        issueId = issueId,
                        author = author,
                        text = text,
                        createdAt = createdAt
                    )

                    // Group by issue_id (maintains ORDER BY from query)
                    commentsMap.getOrPut(issueId) { mutableListOf() }.add(comment)
                    count++
                } catch (e: Exception) {
                    logger.warn("Failed to parse comment: ${e.message}", e)
                }
            }

            rs.close()
            stmt.close()

            logger.info("Parsed $count comments from database")
        } catch (e: SQLException) {
            // If comments table doesn't exist, just return empty map
            logger.info("Could not parse comments table: ${e.message}")
            return emptyMap()
        }

        return commentsMap
    }

    /**
     * Parses dirty issues from the dirty_issues table.
     *
     * SQL: SELECT issue_id FROM dirty_issues
     *
     * Returns the set of issue IDs that have unsaved changes (not yet synced to git).
     * These issues should be displayed with a "Not Synced" badge in the UI.
     *
     * Gracefully handles missing table (legacy databases) by returning empty set.
     *
     * @return Set of issue IDs that are dirty
     */
    private fun parseDirtyIssues(connection: Connection): Set<String> {
        val dirtyIssues = mutableSetOf<String>()

        try {
            val stmt = connection.prepareStatement(
                "SELECT issue_id FROM dirty_issues"
            )

            val rs = stmt.executeQuery()
            while (rs.next()) {
                val issueId = rs.getString("issue_id")
                dirtyIssues.add(issueId)
            }

            rs.close()
            stmt.close()

            logger.info("Found ${dirtyIssues.size} dirty issues (not synced)")
        } catch (e: SQLException) {
            // If dirty_issues table doesn't exist (legacy databases), just return empty set
            // This is normal for older database versions
            logger.info("Could not parse dirty_issues table (legacy database?): ${e.message}")
            return emptySet()
        }

        return dirtyIssues
    }

    /**
     * Helper: Parse ISO8601 timestamp string to Instant.
     * Handles both with and without timezone formats.
     */
    private fun parseInstant(timestamp: String?): Instant? {
        if (timestamp == null) return null
        return try {
            Instant.parse(timestamp)
        } catch (e: Exception) {
            logger.warn("Failed to parse timestamp: $timestamp", e)
            null
        }
    }

    /**
     * Helper: Safely get nullable string from ResultSet.
     * Returns null if column is SQL NULL.
     */
    private fun ResultSet.getStringOrNull(columnName: String): String? {
        val value = getString(columnName)
        return if (wasNull()) null else value
    }

    /**
     * Helper: Safely get nullable int from ResultSet.
     * Returns null if column is SQL NULL.
     */
    private fun ResultSet.getIntOrNull(columnName: String): Int? {
        val value = getInt(columnName)
        return if (wasNull()) null else value
    }
}
