package me.zkvl.beadsviewer.model

/**
 * Represents the source from which issues were loaded.
 *
 * BeadsViewer can load issues from multiple sources:
 * - SQLITE: Local SQLite database (.beads/beads.db) - shows latest unsaved changes
 * - JSONL: Git-tracked JSONL file (.beads/issues.jsonl) - shows synced state
 *
 * The plugin prefers SQLITE when available (for real-time updates), but falls back
 * to JSONL if the database is unavailable or encounters errors.
 */
enum class DataSource {
    /**
     * Issues loaded from SQLite database (.beads/beads.db).
     * This is the preferred source as it shows the latest state including unsaved changes.
     */
    SQLITE,

    /**
     * Issues loaded from JSONL file (.beads/issues.jsonl).
     * This is the fallback source and represents the git-synced state.
     */
    JSONL
}
