package me.zkvl.beadsviewer.query.ast

import kotlinx.datetime.Instant

/**
 * Represents a value in a query expression.
 *
 * Sealed class for type safety, ensuring values are properly typed
 * before evaluation.
 *
 * Example values:
 * - `"open"` → StringValue("open")
 * - `0` → IntValue(0)
 * - `null` → NullValue
 * - `today` → RelativeDate(TODAY)
 */
sealed class QueryValue {
    /**
     * String value (e.g., "open", "john", "frontend")
     */
    data class StringValue(val value: String) : QueryValue()

    /**
     * Integer value (e.g., 0, 1, 42, -5)
     */
    data class IntValue(val value: Int) : QueryValue()

    /**
     * Absolute timestamp value (e.g., 2024-01-01T00:00:00Z)
     */
    data class TimestampValue(val value: Instant) : QueryValue()

    /**
     * Relative date specification (e.g., today, next-week)
     */
    data class RelativeDate(val spec: RelativeDateSpec) : QueryValue()

    /**
     * Null value for null checks (e.g., assignee:null)
     */
    data object NullValue : QueryValue()

    /**
     * Attempts to coerce this value to match the target field type.
     *
     * @param field The target field for type coercion
     * @return Success with coerced value, or Failure with error message
     */
    fun coerceToFieldType(field: QueryField): Result<QueryValue> {
        // Type coercion rules based on field type
        return when (field.type) {
            FieldType.STRING, FieldType.STRING_NULLABLE -> {
                // String fields accept any value as string
                when (this) {
                    is StringValue -> Result.success(this)
                    is IntValue -> Result.success(StringValue(value.toString()))
                    is TimestampValue -> Result.success(StringValue(value.toString()))
                    is RelativeDate -> Result.success(StringValue(spec.name.lowercase()))
                    is NullValue -> Result.success(this)
                }
            }

            FieldType.STRING_LIST -> {
                // List fields accept string values
                when (this) {
                    is StringValue -> Result.success(this)
                    is IntValue -> Result.success(StringValue(value.toString()))
                    is NullValue -> Result.success(this)
                    else -> Result.failure(IllegalArgumentException(
                        "Cannot use ${this.javaClass.simpleName} for list field ${field.fieldName}"
                    ))
                }
            }

            FieldType.INTEGER, FieldType.INTEGER_NULLABLE -> {
                // Integer fields require integer values
                when (this) {
                    is IntValue -> Result.success(this)
                    is StringValue -> value.toIntOrNull()?.let {
                        Result.success(IntValue(it))
                    } ?: Result.failure(IllegalArgumentException(
                        "Cannot convert '$value' to integer for field ${field.fieldName}"
                    ))
                    is NullValue -> Result.success(this)
                    else -> Result.failure(IllegalArgumentException(
                        "Cannot use ${this.javaClass.simpleName} for integer field ${field.fieldName}"
                    ))
                }
            }

            FieldType.TIMESTAMP, FieldType.TIMESTAMP_NULLABLE -> {
                // Timestamp fields accept timestamps and relative dates
                when (this) {
                    is TimestampValue -> Result.success(this)
                    is RelativeDate -> Result.success(this)
                    is NullValue -> Result.success(this)
                    else -> Result.failure(IllegalArgumentException(
                        "Cannot use ${this.javaClass.simpleName} for timestamp field ${field.fieldName}"
                    ))
                }
            }

            FieldType.ENUM_STATUS, FieldType.ENUM_ISSUE_TYPE -> {
                // Enum fields accept string values
                when (this) {
                    is StringValue -> Result.success(this)
                    is NullValue -> Result.success(this)
                    else -> Result.failure(IllegalArgumentException(
                        "Cannot use ${this.javaClass.simpleName} for enum field ${field.fieldName}"
                    ))
                }
            }
        }
    }
}

/**
 * Relative date specifications (YouTrack-style).
 *
 * These are resolved to absolute timestamps at evaluation time,
 * using the system timezone.
 *
 * Examples:
 * - `created:today` → issues created today
 * - `due:next-week` → issues due next week
 */
enum class RelativeDateSpec {
    /** Today (current date at 00:00) */
    TODAY,

    /** Yesterday (previous date at 00:00) */
    YESTERDAY,

    /** Tomorrow (next date at 00:00) */
    TOMORROW,

    /** Start of current week (Monday 00:00) */
    THIS_WEEK,

    /** Start of last week */
    LAST_WEEK,

    /** Start of next week */
    NEXT_WEEK,

    /** Start of current month (1st day at 00:00) */
    THIS_MONTH,

    /** Start of last month */
    LAST_MONTH,

    /** Start of next month */
    NEXT_MONTH;

    companion object {
        /**
         * Parse relative date spec from string.
         *
         * @param spec The string representation (case-insensitive, with/without hyphens)
         * @return The relative date spec, or null if not recognized
         */
        fun fromString(spec: String): RelativeDateSpec? {
            val normalized = spec.lowercase().replace("-", "_")
            return entries.find { it.name.lowercase() == normalized }
        }
    }
}

/**
 * Field type enumeration for type checking and coercion.
 */
enum class FieldType {
    STRING,
    STRING_NULLABLE,
    STRING_LIST,
    INTEGER,
    INTEGER_NULLABLE,
    TIMESTAMP,
    TIMESTAMP_NULLABLE,
    ENUM_STATUS,
    ENUM_ISSUE_TYPE
}
