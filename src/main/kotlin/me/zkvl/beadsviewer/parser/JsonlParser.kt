package me.zkvl.beadsviewer.parser

import kotlinx.serialization.json.Json
import me.zkvl.beadsviewer.model.Issue
import java.nio.file.Path
import kotlin.io.path.useLines

/**
 * JSONL parser for Beads issues.jsonl files.
 *
 * Uses streaming line-by-line parsing for memory efficiency with large files (980+ issues).
 * Supports two modes:
 * - parseFile(): Loads all issues into memory (good for <1000 issues)
 * - parseFileSequence(): Lazy sequence for streaming (good for 980+ issues or partial success)
 */
class JsonlParser(
    private val config: ParserConfig = ParserConfig()
) {
    private val json = Json {
        ignoreUnknownKeys = true  // Forward compatibility - ignore fields we don't know about
        isLenient = config.lenientMode
        coerceInputValues = config.coerceInputValues
    }

    /**
     * Parses all issues from a JSONL file into a list.
     *
     * This method loads all issues into memory at once. Use parseFileSequence() for
     * very large files or when you need to handle partial failures gracefully.
     *
     * @param file Path to the issues.jsonl file
     * @return List of parsed issues
     * @throws ParseException.FileNotFound if the file doesn't exist
     * @throws ParseException.InvalidJson if any line contains invalid JSON
     * @throws ParseException.IoError if there's an I/O error reading the file
     * @throws ParseException.ValidationError if validation is enabled and an issue fails validation
     */
    fun parseFile(file: Path): List<Issue> {
        if (!file.toFile().exists()) {
            throw ParseException.FileNotFound(file)
        }

        val issues = mutableListOf<Issue>()
        var lineNumber = 0

        try {
            file.useLines { lines ->
                for (line in lines) {
                    lineNumber++
                    val trimmed = line.trim()

                    // Skip empty lines and whitespace
                    if (trimmed.isEmpty()) {
                        continue
                    }

                    try {
                        val issue = json.decodeFromString<Issue>(trimmed)

                        // Validate if enabled
                        if (config.validateOnParse) {
                            try {
                                issue.validate()
                            } catch (e: IllegalArgumentException) {
                                throw ParseException.ValidationError(issue.id, e)
                            }
                        }

                        issues.add(issue)
                    } catch (e: ParseException) {
                        throw e  // Re-throw our own exceptions
                    } catch (e: Exception) {
                        throw ParseException.InvalidJson(
                            file = file,
                            lineNumber = lineNumber,
                            line = trimmed.take(100),  // Truncate for error message
                            cause = e
                        )
                    }
                }
            }
        } catch (e: ParseException) {
            throw e  // Re-throw our exceptions
        } catch (e: Exception) {
            throw ParseException.IoError(file, e)
        }

        return issues
    }

    /**
     * Parses issues lazily using a sequence.
     *
     * This method returns a sequence of Result<Issue>, allowing you to:
     * - Process issues as they're parsed (streaming)
     * - Handle partial failures (some issues succeed, some fail)
     * - Minimize memory usage for very large files
     *
     * Each element in the sequence is either:
     * - Result.success(issue) if parsing succeeded
     * - Result.failure(exception) if parsing failed for that line
     *
     * @param file Path to the issues.jsonl file
     * @return Sequence of parsing results
     */
    fun parseFileSequence(file: Path): Sequence<Result<Issue>> {
        if (!file.toFile().exists()) {
            return sequenceOf(Result.failure(ParseException.FileNotFound(file)))
        }

        var lineNumber = 0
        return file.toFile().bufferedReader().lineSequence()
            .map { line ->
                lineNumber++
                val trimmed = line.trim()

                // Skip empty lines
                if (trimmed.isEmpty()) {
                    return@map null
                }

                try {
                    val issue = json.decodeFromString<Issue>(trimmed)

                    // Validate if enabled
                    if (config.validateOnParse) {
                        try {
                            issue.validate()
                        } catch (e: IllegalArgumentException) {
                            return@map Result.failure(ParseException.ValidationError(issue.id, e))
                        }
                    }

                    Result.success(issue)
                } catch (e: Exception) {
                    Result.failure(ParseException.InvalidJson(
                        file = file,
                        lineNumber = lineNumber,
                        line = trimmed.take(100),
                        cause = e
                    ))
                }
            }
            .filterNotNull()
    }

    /**
     * Counts the number of issues in a JSONL file without fully parsing them.
     *
     * This is faster than parseFile() when you only need the count.
     *
     * @param file Path to the issues.jsonl file
     * @return Number of non-empty lines (approximate issue count)
     */
    fun countIssues(file: Path): Int {
        if (!file.toFile().exists()) {
            return 0
        }

        return file.useLines { lines ->
            lines.count { it.trim().isNotEmpty() }
        }
    }
}
