package me.zkvl.beadsviewer.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.zkvl.beadsviewer.serialization.InstantSerializer
import me.zkvl.beadsviewer.serialization.NullableInstantSerializer

/**
 * Represents a trackable work item in the Beads system.
 *
 * Issues are the core data structure for tracking tasks, bugs, features, and other work items.
 * They support dependencies, comments, labels, and rich metadata for project management.
 */
@Serializable
data class Issue(
    // ===== Core Fields =====

    /**
     * Unique identifier for the issue (e.g., "BeadsViewer-zw5").
     */
    val id: String,

    /**
     * Short summary of the issue.
     */
    val title: String,

    /**
     * Detailed description of the issue (may contain markdown).
     * Optional - tombstoned issues may not have descriptions.
     */
    val description: String = "",

    /**
     * Current state of the issue.
     */
    val status: Status,

    /**
     * Priority level (0 = highest/critical, 4 = lowest/backlog).
     */
    val priority: Int,

    /**
     * Type/category of the issue.
     */
    @SerialName("issue_type")
    val issueType: IssueType,

    // ===== Optional Text Fields =====

    /**
     * Design notes or specifications (optional).
     */
    val design: String? = null,

    /**
     * Acceptance criteria or definition of done (optional).
     */
    @SerialName("acceptance_criteria")
    val acceptanceCriteria: String? = null,

    /**
     * Additional notes or context (optional).
     */
    val notes: String? = null,

    // ===== Optional Metadata =====

    /**
     * Username of the person assigned to this issue (optional).
     */
    val assignee: String? = null,

    /**
     * Estimated time to complete in minutes (optional).
     */
    @SerialName("estimated_minutes")
    val estimatedMinutes: Int? = null,

    /**
     * External reference (e.g., URL, ticket ID) (optional).
     */
    @SerialName("external_ref")
    val externalRef: String? = null,

    /**
     * Source repository if tracking cross-repo issues (optional).
     */
    @SerialName("source_repo")
    val sourceRepo: String? = null,

    // ===== Timestamps =====

    /**
     * Timestamp when the issue was created.
     */
    @Serializable(with = InstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant,

    /**
     * Username of the person who created the issue.
     */
    @SerialName("created_by")
    val createdBy: String = "",

    /**
     * Timestamp when the issue was last updated.
     */
    @Serializable(with = InstantSerializer::class)
    @SerialName("updated_at")
    val updatedAt: Instant,

    /**
     * Due date for the issue (optional).
     */
    @Serializable(with = NullableInstantSerializer::class)
    @SerialName("due_date")
    val dueDate: Instant? = null,

    /**
     * Timestamp when the issue was closed (optional).
     */
    @Serializable(with = NullableInstantSerializer::class)
    @SerialName("closed_at")
    val closedAt: Instant? = null,

    /**
     * Reason why the issue was closed (optional).
     */
    @SerialName("close_reason")
    val closeReason: String? = null,

    // ===== Tombstone Fields =====

    /**
     * Timestamp when the issue was deleted/tombstoned (optional).
     */
    @Serializable(with = NullableInstantSerializer::class)
    @SerialName("deleted_at")
    val deletedAt: Instant? = null,

    /**
     * User who deleted the issue (optional).
     */
    @SerialName("deleted_by")
    val deletedBy: String? = null,

    /**
     * Reason for deletion (optional).
     */
    @SerialName("delete_reason")
    val deleteReason: String? = null,

    /**
     * Original issue type before tombstoning (optional).
     */
    @SerialName("original_type")
    val originalType: String? = null,

    // ===== Compaction Fields (for future use) =====

    /**
     * Compaction level (0 = full, higher = more compacted).
     */
    @SerialName("compaction_level")
    val compactionLevel: Int = 0,

    /**
     * Timestamp when the issue was compacted (optional).
     */
    @Serializable(with = NullableInstantSerializer::class)
    @SerialName("compacted_at")
    val compactedAt: Instant? = null,

    /**
     * Git commit hash where compaction occurred (optional).
     */
    @SerialName("compacted_at_commit")
    val compactedAtCommit: String? = null,

    /**
     * Original size in bytes before compaction (optional).
     */
    @SerialName("original_size")
    val originalSize: Int = 0,

    // ===== Collections =====

    /**
     * List of labels/tags for categorization and filtering.
     */
    val labels: List<String> = emptyList(),

    /**
     * List of dependencies (what this issue depends on or blocks).
     */
    val dependencies: List<Dependency> = emptyList(),

    /**
     * List of comments on this issue.
     */
    val comments: List<Comment> = emptyList()
) {
    /**
     * Validates issue data for logical consistency.
     * @throws IllegalArgumentException if validation fails
     *
     * Note: Timestamp validations are lenient to handle microsecond-precision clock issues
     * where timestamps might be recorded in reverse order due to system clock resolution.
     */
    fun validate() {
        require(id.isNotBlank()) { "Issue ID cannot be empty" }
        require(title.isNotBlank()) { "Issue title cannot be empty" }

        // Note: Skip timestamp order validation - real-world data can have microsecond-precision
        // issues where timestamps are recorded slightly out of order due to clock resolution.
        // For example: closed_at=2025-12-22T20:16:53.033025Z vs created_at=2025-12-22T20:16:53.033119Z
        // This is not a data integrity issue, just clock precision.

        estimatedMinutes?.let { minutes ->
            require(minutes >= 0) {
                "estimated_minutes ($minutes) cannot be negative"
            }
        }
        require(priority in 0..4) {
            "priority ($priority) must be between 0 and 4"
        }
    }

    /**
     * Returns true if this issue is closed.
     */
    fun isClosed(): Boolean = status.isClosed()

    /**
     * Returns true if this issue is open (open or in_progress).
     */
    fun isOpen(): Boolean = status.isOpen()

    /**
     * Returns the number of blocking dependencies (issues that block this one).
     */
    fun blockedByCount(): Int = dependencies.count { it.isBlocking() && it.issueId == id }

    /**
     * Returns the number of issues this issue blocks.
     */
    fun blocksCount(): Int = dependencies.count { it.isBlocking() && it.dependsOnId == id }
}
