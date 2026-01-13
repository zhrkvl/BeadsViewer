package me.zkvl.beadsviewer.parser

import com.intellij.openapi.diagnostic.Logger
import me.zkvl.beadsviewer.model.DataSource
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.model.IssueLoadResult
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.readBytes

/**
 * Repository for managing Beads issues with intelligent caching.
 *
 * This class provides a high-level API for loading issues from SQLite database or JSONL files with:
 * - SQLite-first strategy: Try database first, fallback to JSONL on errors
 * - Hash-based cache invalidation (detects file changes reliably)
 * - Dirty issue tracking (unsaved changes indicator)
 * - Thread-safe concurrent access
 * - Integration with IntelliJ logging
 * - Functional error handling with Result types
 *
 * The cache stores issues in memory and invalidates when the file content changes
 * (detected via SHA-256 hash comparison), not just timestamps. This handles cases
 * like git checkout, file overwrite, etc. more reliably than timestamp-based caching.
 */
class IssueRepository(
    private val jsonlParser: JsonlParser = JsonlParser(),
    private val sqliteParser: SqliteParser = SqliteParser()
) {
    private val logger = Logger.getInstance(IssueRepository::class.java)

    /**
     * Cache entry holding parsed issues, file hash, timestamp, and metadata.
     */
    private data class CacheEntry(
        val result: IssueLoadResult,
        val fileHash: String,
        val timestamp: Long
    )

    /**
     * In-memory cache. Volatile ensures visibility across threads.
     */
    @Volatile
    private var cache: CacheEntry? = null

    /**
     * Loads issues from SQLite database.
     *
     * Reads from .beads/beads.db and returns IssueLoadResult with:
     * - Complete issues list with dependencies, labels, comments
     * - Dirty issue IDs (unsaved changes)
     * - Source metadata (SQLITE)
     *
     * This method does NOT use caching (SQLite is already fast).
     *
     * @param dbFile Path to the beads.db file (typically .beads/beads.db)
     * @return Result.success(IssueLoadResult) if successful, Result.failure(exception) on error
     */
    fun loadFromSqlite(dbFile: Path): Result<IssueLoadResult> {
        logger.info("Loading issues from SQLite: $dbFile")
        val startTime = System.currentTimeMillis()

        return sqliteParser.parseDatabase(dbFile).also { result ->
            val elapsedMs = System.currentTimeMillis() - startTime
            result.onSuccess { loadResult ->
                val dirtyCount = loadResult.dirtyIssueIds.size
                logger.info("Loaded ${loadResult.issues.size} issues from SQLite in ${elapsedMs}ms ($dirtyCount dirty)")
            }.onFailure { error ->
                logger.warn("Failed to load from SQLite: ${error.message}")
            }
        }
    }

    /**
     * Loads issues from JSONL file.
     *
     * This is the traditional JSONL-based loading with no dirty issue tracking
     * (JSONL is always synced by definition).
     *
     * No caching for this method - use loadIssues() for cached loading.
     *
     * @param jsonlFile Path to the issues.jsonl file
     * @return Result.success(IssueLoadResult) if successful, Result.failure(exception) on error
     */
    fun loadFromJsonl(jsonlFile: Path): Result<IssueLoadResult> {
        logger.info("Loading issues from JSONL: $jsonlFile")
        val startTime = System.currentTimeMillis()

        return try {
            val issues = jsonlParser.parseFile(jsonlFile)
            val elapsedMs = System.currentTimeMillis() - startTime
            logger.info("Loaded ${issues.size} issues from JSONL in ${elapsedMs}ms")

            Result.success(
                IssueLoadResult(
                    issues = issues,
                    dirtyIssueIds = emptySet(),  // JSONL is always synced
                    source = DataSource.JSONL,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: ParseException.FileNotFound) {
            logger.info("JSONL file not found: $jsonlFile")
            Result.failure(e)
        } catch (e: ParseException) {
            logger.warn("Failed to parse JSONL: $jsonlFile", e)
            Result.failure(e)
        } catch (e: java.nio.file.NoSuchFileException) {
            logger.info("JSONL file not found: $jsonlFile")
            Result.failure(ParseException.FileNotFound(jsonlFile))
        } catch (e: Exception) {
            logger.error("Unexpected error loading JSONL: $jsonlFile", e)
            Result.failure(ParseException.IoError(jsonlFile, e))
        }
    }

    /**
     * Loads issues with SQLite-first strategy and caching.
     *
     * Strategy:
     * 1. Try loading from SQLite database (.beads/beads.db)
     * 2. If SQLite fails, fallback to JSONL (.beads/issues.jsonl)
     * 3. Cache the result using hash-based invalidation
     *
     * The cache is hash-based: if the file content hasn't changed (same SHA-256 hash),
     * the cached issues are returned immediately without re-parsing.
     *
     * This method is thread-safe and can be called concurrently from multiple threads.
     *
     * @param beadsDir Path to the .beads directory
     * @return Result.success(IssueLoadResult) if successful, Result.failure(exception) on error
     */
    @Synchronized
    fun loadIssues(beadsDir: Path): Result<IssueLoadResult> {
        return try {
            val dbFile = beadsDir.resolve("beads.db")
            val jsonlFile = beadsDir.resolve("issues.jsonl")

            // Determine which file to use for cache validation
            // Prefer SQLite if it exists, otherwise use JSONL
            val cacheKeyFile = if (dbFile.toFile().exists()) dbFile else jsonlFile
            val currentHash = computeFileHash(cacheKeyFile)
            val cached = cache

            // Cache hit: file content hasn't changed
            if (cached != null && cached.fileHash == currentHash) {
                val result = cached.result
                logger.info("Cache hit (${result.issues.size} issues, source: ${result.source})")
                return Result.success(result)
            }

            // Cache miss: file changed or first load
            logger.info("Cache miss, loading from source...")

            // Try SQLite first
            val result = loadFromSqlite(dbFile).getOrNull() ?: run {
                // SQLite failed, fallback to JSONL
                logger.info("SQLite load failed, falling back to JSONL")
                loadFromJsonl(jsonlFile).getOrElse { error ->
                    // Both failed
                    logger.error("Both SQLite and JSONL loading failed")
                    return Result.failure(error)
                }
            }

            // Update cache
            cache = CacheEntry(
                result = result,
                fileHash = currentHash,
                timestamp = System.currentTimeMillis()
            )

            Result.success(result)
        } catch (e: Exception) {
            logger.error("Unexpected error in loadIssues", e)
            Result.failure(ParseException.IoError(beadsDir, e))
        }
    }

    /**
     * Loads issues using streaming/lazy evaluation.
     *
     * Returns a sequence of Result<Issue> for partial success handling.
     * No caching is performed for sequences.
     *
     * @param file Path to the issues.jsonl file
     * @return Sequence of parsing results
     */
    fun loadIssuesSequence(file: Path): Sequence<Result<Issue>> {
        logger.info("Loading issues from $file as sequence (no caching)")
        return jsonlParser.parseFileSequence(file)
    }

    /**
     * Invalidates the cache, forcing the next load to re-parse the file.
     *
     * Call this when you know the file has changed and want to force a refresh,
     * or when cleaning up resources.
     */
    @Synchronized
    fun invalidateCache() {
        cache = null
        logger.info("Cache invalidated")
    }

    /**
     * Returns cached issues if available, null otherwise.
     *
     * This is a non-blocking check that doesn't trigger parsing or I/O.
     *
     * @return Cached issues or null if no valid cache exists
     */
    fun getCachedIssues(): List<Issue>? = cache?.result?.issues

    /**
     * Returns true if issues are cached for the given file.
     *
     * Note: This only checks if a cache exists, not if it's valid for the current
     * file content. Use loadIssues() for automatic cache validation.
     *
     * @param file Path to check
     * @return true if cached, false otherwise
     */
    fun isCached(file: Path): Boolean {
        val cached = cache ?: return false
        return try {
            val currentHash = computeFileHash(file)
            cached.fileHash == currentHash
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns cache statistics for debugging and monitoring.
     *
     * @return Map of statistics (size, timestamp, etc.)
     */
    fun getCacheStats(): Map<String, Any> {
        val cached = cache
        return if (cached != null) {
            val result = cached.result
            mapOf(
                "cached" to true,
                "issueCount" to result.issues.size,
                "dirtyIssueCount" to result.dirtyIssueIds.size,
                "source" to result.source.name,
                "timestamp" to cached.timestamp,
                "ageMs" to (System.currentTimeMillis() - cached.timestamp)
            )
        } else {
            mapOf("cached" to false)
        }
    }

    /**
     * Computes SHA-256 hash of file content for cache validation.
     *
     * Using a hash is more reliable than timestamps because:
     * - Detects actual content changes (git checkout, overwrite, etc.)
     * - Not affected by clock skew or file system timestamp issues
     * - Works correctly when file is replaced with identical content
     *
     * SHA-256 is fast enough for JSONL files (980 issues ≈ 925KB ≈ 30ms to hash).
     *
     * @throws ParseException.FileNotFound if the file doesn't exist
     */
    private fun computeFileHash(file: Path): String {
        if (!java.nio.file.Files.exists(file)) {
            throw ParseException.FileNotFound(file)
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = file.readBytes()
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
