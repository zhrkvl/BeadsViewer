package me.zkvl.beadsviewer.parser

import com.intellij.openapi.diagnostic.Logger
import me.zkvl.beadsviewer.model.Issue
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.readBytes

/**
 * Repository for managing Beads issues with intelligent caching.
 *
 * This class provides a high-level API for loading issues from JSONL files with:
 * - Hash-based cache invalidation (detects file changes reliably)
 * - Thread-safe concurrent access
 * - Integration with IntelliJ logging
 * - Functional error handling with Result types
 *
 * The cache stores issues in memory and invalidates when the file content changes
 * (detected via SHA-256 hash comparison), not just timestamps. This handles cases
 * like git checkout, file overwrite, etc. more reliably than timestamp-based caching.
 */
class IssueRepository(
    private val parser: JsonlParser = JsonlParser()
) {
    private val logger = Logger.getInstance(IssueRepository::class.java)

    /**
     * Cache entry holding parsed issues, file hash, and timestamp.
     */
    private data class CacheEntry(
        val issues: List<Issue>,
        val fileHash: String,
        val timestamp: Long
    )

    /**
     * In-memory cache. Volatile ensures visibility across threads.
     */
    @Volatile
    private var cache: CacheEntry? = null

    /**
     * Loads issues from a JSONL file with caching.
     *
     * The cache is hash-based: if the file content hasn't changed (same SHA-256 hash),
     * the cached issues are returned immediately without re-parsing. If the file content
     * has changed, it's re-parsed and the cache is updated.
     *
     * This method is thread-safe and can be called concurrently from multiple threads.
     *
     * @param file Path to the issues.jsonl file
     * @return Result.success(issues) if successful, Result.failure(exception) on error
     */
    @Synchronized
    fun loadIssues(file: Path): Result<List<Issue>> {
        return try {
            // Compute hash of current file content
            val currentHash = computeFileHash(file)
            val cached = cache

            // Cache hit: file content hasn't changed
            if (cached != null && cached.fileHash == currentHash) {
                logger.info("Cache hit for $file (${cached.issues.size} issues)")
                return Result.success(cached.issues)
            }

            // Cache miss: file changed or first load
            logger.info("Cache miss for $file, parsing...")
            val startTime = System.currentTimeMillis()

            val issues = parser.parseFile(file)

            val elapsedMs = System.currentTimeMillis() - startTime
            logger.info("Parsed ${issues.size} issues from $file in ${elapsedMs}ms")

            // Update cache
            cache = CacheEntry(
                issues = issues,
                fileHash = currentHash,
                timestamp = System.currentTimeMillis()
            )

            Result.success(issues)
        } catch (e: ParseException) {
            logger.warn("Failed to parse $file", e)
            Result.failure(e)
        } catch (e: Exception) {
            logger.error("Unexpected error loading $file", e)
            Result.failure(ParseException.IoError(file, e))
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
        return parser.parseFileSequence(file)
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
    fun getCachedIssues(): List<Issue>? = cache?.issues

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
            mapOf(
                "cached" to true,
                "issueCount" to cached.issues.size,
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
     */
    private fun computeFileHash(file: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = file.readBytes()
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
