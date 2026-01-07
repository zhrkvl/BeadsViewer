package me.zkvl.beadsviewer.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import me.zkvl.beadsviewer.serialization.InstantSerializer

/**
 * Represents a single point in a burndown chart.
 *
 * Burndown charts track progress over time by showing how many issues
 * remain to be completed versus how many have been completed.
 */
@Serializable
data class BurndownPoint(
    /**
     * Date/time of this measurement point.
     */
    @Serializable(with = InstantSerializer::class)
    val date: Instant,

    /**
     * Number of issues remaining (not completed) at this point.
     */
    val remaining: Int,

    /**
     * Number of issues completed by this point.
     */
    val completed: Int
) {
    /**
     * Validates that the burndown point data is logically consistent.
     * @throws IllegalArgumentException if validation fails
     */
    fun validate() {
        require(remaining >= 0) { "remaining ($remaining) cannot be negative" }
        require(completed >= 0) { "completed ($completed) cannot be negative" }
    }
}
