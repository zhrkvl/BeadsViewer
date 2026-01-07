package me.zkvl.beadsviewer.parser

import java.nio.file.Path

/**
 * Base exception for JSONL parsing errors.
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
}
