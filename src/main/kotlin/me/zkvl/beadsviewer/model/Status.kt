package me.zkvl.beadsviewer.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the current state of an issue.
 *
 * Status values:
 * - OPEN: Issue is created but not yet being worked on
 * - IN_PROGRESS: Issue is currently being worked on
 * - BLOCKED: Issue cannot proceed due to dependencies or other blockers
 * - CLOSED: Issue has been completed or resolved
 * - TOMBSTONE: Issue has been permanently deleted/archived
 * - HOOKED: Issue is waiting for external event or trigger
 */
@Serializable
enum class Status {
    @SerialName("open")
    OPEN,

    @SerialName("in_progress")
    IN_PROGRESS,

    @SerialName("blocked")
    BLOCKED,

    @SerialName("closed")
    CLOSED,

    @SerialName("tombstone")
    TOMBSTONE,

    @SerialName("hooked")
    HOOKED;

    /**
     * Returns true if this status represents a closed state.
     */
    fun isClosed(): Boolean = this == CLOSED

    /**
     * Returns true if this status represents an active state (open or in progress).
     */
    fun isOpen(): Boolean = this == OPEN || this == IN_PROGRESS

    /**
     * Returns true if this status represents a permanently deleted/archived state.
     */
    fun isTombstone(): Boolean = this == TOMBSTONE
}
