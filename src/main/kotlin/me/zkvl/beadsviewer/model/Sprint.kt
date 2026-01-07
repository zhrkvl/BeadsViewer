package me.zkvl.beadsviewer.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.zkvl.beadsviewer.serialization.InstantSerializer

/**
 * Represents a time-boxed period of work (sprint or iteration).
 *
 * Sprints group issues together with a start/end date and track
 * progress toward completion goals and velocity targets.
 */
@Serializable
data class Sprint(
    /**
     * Unique identifier for the sprint.
     */
    val id: String,

    /**
     * Human-readable name for the sprint (e.g., "Sprint 23", "Q1 2026").
     */
    val name: String,

    /**
     * Start date/time of the sprint.
     */
    @Serializable(with = InstantSerializer::class)
    @SerialName("start_date")
    val startDate: Instant,

    /**
     * End date/time of the sprint.
     */
    @Serializable(with = InstantSerializer::class)
    @SerialName("end_date")
    val endDate: Instant,

    /**
     * List of issue IDs included in this sprint.
     */
    @SerialName("bead_ids")
    val beadIds: List<String> = emptyList(),

    /**
     * Target velocity (story points or issue count) for this sprint.
     */
    @SerialName("velocity_target")
    val velocityTarget: Double = 0.0,

    /**
     * Timestamp when the sprint was created.
     */
    @Serializable(with = InstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant,

    /**
     * Timestamp when the sprint was last updated.
     */
    @Serializable(with = InstantSerializer::class)
    @SerialName("updated_at")
    val updatedAt: Instant
) {
    /**
     * Validates that the sprint data is logically consistent.
     * @throws IllegalArgumentException if validation fails
     */
    fun validate() {
        require(id.isNotBlank()) { "Sprint ID cannot be empty" }
        require(name.isNotBlank()) { "Sprint name cannot be empty" }
        require(endDate >= startDate) {
            "end_date ($endDate) cannot be before start_date ($startDate)"
        }
    }

    /**
     * Returns true if the sprint is currently active (current time is within sprint dates).
     */
    fun isActive(now: Instant = kotlinx.datetime.Clock.System.now()): Boolean {
        return now >= startDate && now <= endDate
    }
}
