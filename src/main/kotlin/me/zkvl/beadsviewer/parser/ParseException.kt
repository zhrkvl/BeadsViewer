package me.zkvl.beadsviewer.parser

import java.nio.file.Path

/**
 * Base exception for parsing errors (JSONL and SQLite).
 *
 * This sealed class hierarchy provides specific exception types for different
 * parsing failures, making error handling more precise and user-friendly.
 */
sealed class ParseException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /**
     * Thrown when the issues.jsonl file cannot be found.
     *
     * This typically indicates that the project doesn't have a .beads/ directory
     * or the file has been deleted.
     */
    class FileNotFound(val file: Path) : ParseException(
        "Beads issues file not found: $file"
    )

    /**
     * Thrown when a line in the JSONL file contains invalid JSON.
     *
     * Includes the line number and content snippet for debugging.
     */
    class InvalidJson(
        val file: Path,
        val lineNumber: Int,
        val line: String,
        cause: Throwable
    ) : ParseException(
        "Invalid JSON at line $lineNumber in $file: ${cause.message}\nLine content: $line",
        cause
    )

    /**
     * Thrown when there's an I/O error reading the file.
     *
     * This could be due to permission errors, disk errors, or other I/O issues.
     */
    class IoError(
        val file: Path,
        cause: Throwable
    ) : ParseException(
        "IO error reading $file: ${cause.message}",
        cause
    )

    /**
     * Thrown when an issue fails validation after parsing.
     *
     * This indicates that the JSON is valid but the data doesn't meet
     * logical consistency requirements (e.g., dates out of order).
     */
    class ValidationError(
        val issueId: String,
        cause: Throwable
    ) : ParseException(
        "Validation failed for issue $issueId: ${cause.message}",
        cause
    )

    /**
     * Thrown when there's an error connecting to or reading from the SQLite database.
     *
     * This could be due to:
     * - Database file missing
     * - Database file corrupted
     * - Database locked by another process
     * - SQL query errors
     * - Schema mismatches
     */
    class SqlError(
        val file: Path,
        val operation: String,
        cause: Throwable
    ) : ParseException(
        "SQLite error during $operation on $file: ${cause.message}",
        cause
    )

    /**
     * Thrown when the database schema doesn't match expected structure.
     *
     * This can happen with:
     * - Legacy databases with old schema
     * - Corrupted database files
     * - Missing required tables or columns
     */
    class SchemaError(
        val file: Path,
        val expectedTable: String? = null,
        val expectedColumn: String? = null,
        cause: Throwable
    ) : ParseException(
        buildString {
            append("Schema mismatch in $file")
            if (expectedTable != null) append(": table '$expectedTable' not found or invalid")
            if (expectedColumn != null) append(": column '$expectedColumn' not found or invalid")
            append(": ${cause.message}")
        },
        cause
    )
}
