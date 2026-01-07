package me.zkvl.beadsviewer.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Categorizes the kind of work represented by an issue.
 *
 * This sealed class supports both known standard types (Bug, Feature, Task, etc.)
 * and extensibility for custom types from the Gastown orchestration system
 * (e.g., "role", "agent", "molecule").
 *
 * Known types:
 * - Bug: A defect or error that needs to be fixed
 * - Feature: New functionality to be implemented
 * - Task: General work item that needs to be done
 * - Epic: Large feature or initiative that spans multiple tasks
 * - Chore: Maintenance work, refactoring, or technical debt
 * - MergeRequest: Code review or merge request tracking
 *
 * Custom types: Any other string value will be wrapped in Custom()
 */
@Serializable(with = IssueTypeSerializer::class)
sealed class IssueType {
    abstract val value: String

    // Standard Beads issue types
    data object Bug : IssueType() {
        override val value = "bug"
    }

    data object Feature : IssueType() {
        override val value = "feature"
    }

    data object Task : IssueType() {
        override val value = "task"
    }

    data object Epic : IssueType() {
        override val value = "epic"
    }

    data object Chore : IssueType() {
        override val value = "chore"
    }

    data object MergeRequest : IssueType() {
        override val value = "merge-request"
    }

    // Extension point for Gastown or other custom types
    data class Custom(override val value: String) : IssueType()

    /**
     * Returns true if this is a known standard type, false for custom types.
     */
    fun isKnownType(): Boolean = this !is Custom

    companion object {
        /**
         * Creates an IssueType from a string value.
         * Returns a known type object if the value matches, otherwise wraps in Custom().
         */
        fun fromString(value: String): IssueType = when (value) {
            "bug" -> Bug
            "feature" -> Feature
            "task" -> Task
            "epic" -> Epic
            "chore" -> Chore
            "merge-request" -> MergeRequest
            else -> Custom(value)
        }
    }
}

/**
 * Custom serializer for IssueType that handles string serialization/deserialization.
 */
object IssueTypeSerializer : KSerializer<IssueType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IssueType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: IssueType) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): IssueType {
        return IssueType.fromString(decoder.decodeString())
    }
}
