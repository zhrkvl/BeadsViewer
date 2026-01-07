package me.zkvl.beadsviewer.parser

/**
 * Configuration options for the JSONL parser.
 *
 * Allows customizing parser behavior for different use cases
 * (e.g., strict validation vs. lenient parsing for recovery).
 */
data class ParserConfig(
    /**
     * If true, validates each issue after parsing using Issue.validate().
     * Disabling this improves performance but skips logical consistency checks.
     * Default: true (fail-fast on invalid data).
     */
    val validateOnParse: Boolean = true,

    /**
     * If true, allows relaxed JSON syntax (e.g., unquoted keys, trailing commas).
     * Useful for recovery from slightly malformed JSONL files.
     * Default: false (strict JSON parsing).
     */
    val lenientMode: Boolean = false,

    /**
     * If true, coerces incorrect types when possible (e.g., string "0" to int 0).
     * Helps parse data from mixed sources or format migrations.
     * Default: true (forgiving type handling).
     */
    val coerceInputValues: Boolean = true
)
