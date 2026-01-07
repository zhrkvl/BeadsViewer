package me.zkvl.beadsviewer.parser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("IssueRepository Tests")
class IssueRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private val repository = IssueRepository()

    @Test
    @DisplayName("should cache parsed issues on first load")
    fun testCacheParsedIssuesOnFirstLoad() {
        val file = tempDir.resolve("issues.jsonl")
        file.writeText("""
            {"id":"TEST-001","title":"Issue","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}
        """.trimIndent())

        // First load - cache miss
        val result1 = repository.loadIssues(file)
        assertTrue(result1.isSuccess)
        assertEquals(1, result1.getOrNull()?.size)

        // Verify cached
        assertNotNull(repository.getCachedIssues())
        assertEquals(1, repository.getCachedIssues()?.size)
    }

    @Test
    @DisplayName("should return cached issues on second load (cache hit)")
    fun testReturnCachedIssuesOnSecondLoad() {
        val file = tempDir.resolve("issues.jsonl")
        file.writeText("""
            {"id":"TEST-001","title":"Issue","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}
        """.trimIndent())

        val result1 = repository.loadIssues(file)
        assertTrue(result1.isSuccess)

        // Second load - cache hit (same content)
        val result2 = repository.loadIssues(file)
        assertTrue(result2.isSuccess)
        assertEquals(1, result2.getOrNull()?.size)

        // Both should return the same data
        assertEquals(result1.getOrNull(), result2.getOrNull())
    }

    @Test
    @DisplayName("should invalidate cache when file content changes")
    fun testInvalidateCacheWhenFileContentChanges() {
        val file = tempDir.resolve("issues.jsonl")
        file.writeText("""
            {"id":"TEST-001","title":"Issue","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}
        """.trimIndent())

        val result1 = repository.loadIssues(file)
        assertEquals(1, result1.getOrNull()?.size)

        // Modify file content
        file.writeText("""
            {"id":"TEST-001","title":"Issue","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}
            {"id":"TEST-002","title":"Issue2","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}
        """.trimIndent())

        // Re-load - cache should be invalidated and file re-parsed
        val result2 = repository.loadIssues(file)
        assertEquals(2, result2.getOrNull()?.size)
    }

    @Test
    @DisplayName("invalidateCache() should clear cached data")
    fun testInvalidateCacheClearsCachedData() {
        val file = tempDir.resolve("issues.jsonl")
        file.writeText("""
            {"id":"TEST-001","title":"Issue","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}
        """.trimIndent())

        repository.loadIssues(file)
        assertNotNull(repository.getCachedIssues())

        repository.invalidateCache()
        assertNull(repository.getCachedIssues())
    }

    @Test
    @DisplayName("isCached() should return true for cached file with same content")
    fun testIsCachedReturnsTrueForCachedFile() {
        val file = tempDir.resolve("issues.jsonl")
        file.writeText("""
            {"id":"TEST-001","title":"Issue","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}
        """.trimIndent())

        assertFalse(repository.isCached(file))

        repository.loadIssues(file)

        assertTrue(repository.isCached(file))
    }

    @Test
    @DisplayName("isCached() should return false after file content changes")
    fun testIsCachedReturnsFalseAfterFileChanges() {
        val file = tempDir.resolve("issues.jsonl")
        file.writeText("""
            {"id":"TEST-001","title":"Issue","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}
        """.trimIndent())

        repository.loadIssues(file)
        assertTrue(repository.isCached(file))

        // Modify file
        file.writeText("""
            {"id":"TEST-002","title":"Issue2","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}
        """.trimIndent())

        assertFalse(repository.isCached(file))
    }

    @Test
    @DisplayName("getCacheStats() should return correct statistics")
    fun testGetCacheStatsReturnsCorrectStats() {
        val file = tempDir.resolve("issues.jsonl")
        file.writeText("""
            {"id":"TEST-001","title":"Issue","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}
        """.trimIndent())

        // No cache initially
        var stats = repository.getCacheStats()
        assertEquals(false, stats["cached"])

        // Load and check stats
        repository.loadIssues(file)
        stats = repository.getCacheStats()
        assertEquals(true, stats["cached"])
        assertEquals(1, stats["issueCount"])
        assertNotNull(stats["timestamp"])
        assertNotNull(stats["ageMs"])
    }

    @Test
    @DisplayName("should handle parsing errors gracefully")
    fun testHandleParsingErrorsGracefully() {
        val file = tempDir.resolve("issues.jsonl")
        file.writeText("""
            {invalid json}
        """.trimIndent())

        val result = repository.loadIssues(file)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ParseException.InvalidJson)
    }

    @Test
    @DisplayName("loadIssuesSequence should not cache")
    fun testLoadIssuesSequenceDoesNotCache() {
        val file = tempDir.resolve("issues.jsonl")
        file.writeText("""
            {"id":"TEST-001","title":"Issue","description":"Desc","status":"open","priority":1,"issue_type":"task","created_at":"2026-01-07T10:00:00Z","created_by":"user","updated_at":"2026-01-07T10:00:00Z"}
        """.trimIndent())

        val sequence = repository.loadIssuesSequence(file)
        val results = sequence.toList()

        assertEquals(1, results.size)
        assertTrue(results[0].isSuccess)

        // Should not be cached
        assertNull(repository.getCachedIssues())
    }
}
