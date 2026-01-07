package me.zkvl.beadsviewer.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.zkvl.beadsviewer.serialization.InstantSerializer

/**
 * Represents a dependency relationship between two issues.
 *
 * Dependencies define how issues are related and which issues block others.
 * For example, if issue A depends on issue B, then B must be completed before A can be completed.
 */
@Serializable
data class Dependency(
    /**
     * ID of the issue that has this dependency.
     */
    @SerialName("issue_id")
    val issueId: String,

    /**
     * ID of the issue that this issue depends on.
     */
    @SerialName("depends_on_id")
    val dependsOnId: String,

    /**
     * Type of dependency relationship.
     */
    val type: DependencyType,

    /**
     * Timestamp when the dependency was created.
     */
    @Serializable(with = InstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant,

    /**
     * Username of the person who created the dependency.
     */
    @SerialName("created_by")
    val createdBy: String
) {
    /**
     * Returns true if this dependency represents a blocking relationship.
     */
    fun isBlocking(): Boolean = type.isBlocking()
}
