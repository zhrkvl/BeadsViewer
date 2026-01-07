package me.zkvl.beadsviewer.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Categorizes the type of relationship between issues.
 *
 * This sealed class supports both known standard types and extensibility for custom types.
 *
 * Known types:
 * - Blocks: The issue blocks another issue from being completed
 * - Related: The issue is related to another issue but doesn't block it
 * - ParentChild: Hierarchical relationship (epic -> task, feature -> subtask)
 * - DiscoveredFrom: The issue was discovered while working on another issue
 * - Supersedes: The issue supersedes/replaces another issue
 * - RepliesTo: The issue is a reply to another issue
 *
 * Custom types: Any other string value will be wrapped in Custom()
 */
@Serializable(with = DependencyTypeSerializer::class)
sealed class DependencyType {
    abstract val value: String

    // Standard dependency types
    data object Blocks : DependencyType() {
        override val value = "blocks"
    }

    data object Related : DependencyType() {
        override val value = "related"
    }

    data object ParentChild : DependencyType() {
        override val value = "parent-child"
    }

    data object DiscoveredFrom : DependencyType() {
        override val value = "discovered-from"
    }

    data object Supersedes : DependencyType() {
        override val value = "supersedes"
    }

    data object RepliesTo : DependencyType() {
        override val value = "replies-to"
    }

    // Extension point for custom types
    data class Custom(override val value: String) : DependencyType()

    /**
     * Returns true if this dependency type represents a blocking relationship.
     * Only Blocks type is considered blocking for dependency graph analysis.
     * Empty string is also treated as blocking for backward compatibility.
     */
    fun isBlocking(): Boolean = this is Blocks || value.isEmpty()

    companion object {
        /**
         * Creates a DependencyType from a string value.
         * Returns a known type object if the value matches, otherwise wraps in Custom().
         */
        fun fromString(value: String): DependencyType = when (value) {
            "blocks", "" -> Blocks  // Empty string treated as blocks for backward compatibility
            "related" -> Related
            "parent-child" -> ParentChild
            "discovered-from" -> DiscoveredFrom
            "supersedes" -> Supersedes
            "replies-to" -> RepliesTo
            else -> Custom(value)
        }
    }
}

/**
 * Custom serializer for DependencyType that handles string serialization/deserialization.
 */
object DependencyTypeSerializer : KSerializer<DependencyType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("DependencyType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DependencyType) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): DependencyType {
        return DependencyType.fromString(decoder.decodeString())
    }
}
