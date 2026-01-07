package me.zkvl.beadsviewer.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.zkvl.beadsviewer.serialization.InstantSerializer

/**
 * Represents a comment on an issue.
 *
 * Comments provide a way to discuss issues, add context, track decisions,
 * and document progress without modifying the issue description.
 */
@Serializable
data class Comment(
    /**
     * Unique identifier for the comment.
     */
    val id: Long,

    /**
     * ID of the issue this comment belongs to.
     */
    @SerialName("issue_id")
    val issueId: String,

    /**
     * Username of the comment author.
     */
    val author: String,

    /**
     * Comment text content (may contain markdown).
     */
    val text: String,

    /**
     * Timestamp when the comment was created.
     */
    @Serializable(with = InstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant
)
