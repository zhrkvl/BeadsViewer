package me.zkvl.beadsviewer.parser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIf
import kotlin.io.path.Path
import kotlin.test.assertTrue

/**
 * Performance tests using real reference data.
 * Only runs if reference files exist.
 */
@DisplayName("Performance Tests")
class PerformanceTest {

    @Test
    @EnabledIf("referenceFilesExist")
    @DisplayName("should parse 980 issues in under 1 second")
    fun testParse980IssuesInUnder1Second() {
        val file = Path("references/beads/.beads/issues.jsonl")
        val parser = JsonlParser()

        val startTime = System.currentTimeMillis()
        val issues = parser.parseFile(file)
        val elapsedMs = System.currentTimeMillis() - startTime

        assertTrue(issues.size >= 900, "Expected ~980 issues, got ${issues.size}")
        assertTrue(elapsedMs < 1000, "Parsing took ${elapsedMs}ms, expected <1000ms")

        println("✓ Parsed ${issues.size} issues in ${elapsedMs}ms")
    }

    @Test
    @EnabledIf("referenceFilesExist")
    @DisplayName("should cache 980 issues efficiently (cache hit 10x faster)")
    fun testCache980IssuesEfficiently() {
        val file = Path("references/beads/.beads/issues.jsonl")
        val repository = IssueRepository()

        // First load (cache miss)
        val startTime1 = System.currentTimeMillis()
        val result1 = repository.loadIssues(file)
        val elapsedMs1 = System.currentTimeMillis() - startTime1

        assertTrue(result1.isSuccess)
        val issueCount = result1.getOrNull()?.size ?: 0
        assertTrue(issueCount >= 900, "Expected ~980 issues, got $issueCount")

        // Second load (cache hit)
        val startTime2 = System.currentTimeMillis()
        val result2 = repository.loadIssues(file)
        val elapsedMs2 = System.currentTimeMillis() - startTime2

        assertTrue(result2.isSuccess)

        // Cache hit should be significantly faster (at least 10x)
        assertTrue(elapsedMs2 < elapsedMs1 / 10,
            "Cache hit (${elapsedMs2}ms) should be 10x faster than miss (${elapsedMs1}ms)")

        println("✓ Cache miss: ${elapsedMs1}ms, Cache hit: ${elapsedMs2}ms (${elapsedMs1 / elapsedMs2}x faster)")
    }

    @Test
    @EnabledIf("meshCoreQtExists")
    @DisplayName("should parse MeshCoreQt (134 issues) in under 500ms")
    fun testParseMeshCoreQt() {
        val file = Path("references/MeshCoreQt/.beads/issues.jsonl")
        val parser = JsonlParser()

        val startTime = System.currentTimeMillis()
        val issues = parser.parseFile(file)
        val elapsedMs = System.currentTimeMillis() - startTime

        assertTrue(issues.size >= 100, "Expected ~134 issues, got ${issues.size}")
        assertTrue(elapsedMs < 500, "Parsing took ${elapsedMs}ms, expected <500ms")

        println("✓ Parsed ${issues.size} issues in ${elapsedMs}ms")
    }

    @Test
    @EnabledIf("projectIssuesExist")
    @DisplayName("should parse project's own issues (101 issues) in under 100ms")
    fun testParseProjectOwnIssues() {
        val file = Path(".beads/issues.jsonl")
        val parser = JsonlParser()

        val startTime = System.currentTimeMillis()
        val issues = parser.parseFile(file)
        val elapsedMs = System.currentTimeMillis() - startTime

        assertTrue(issues.size >= 50, "Expected ~101 issues, got ${issues.size}")
        assertTrue(elapsedMs < 100, "Parsing took ${elapsedMs}ms, expected <100ms")

        println("✓ Parsed ${issues.size} issues in ${elapsedMs}ms")
    }

    @Test
    @EnabledIf("referenceFilesExist")
    @DisplayName("should handle 980 issues with streaming (memory efficient)")
    fun testStreamingWith980Issues() {
        val file = Path("references/beads/.beads/issues.jsonl")
        val parser = JsonlParser()

        val startTime = System.currentTimeMillis()
        val results = parser.parseFileSequence(file).toList()
        val elapsedMs = System.currentTimeMillis() - startTime

        val successCount = results.count { it.isSuccess }
        assertTrue(successCount >= 900, "Expected ~980 successful parses, got $successCount")
        assertTrue(elapsedMs < 1500, "Streaming parsing took ${elapsedMs}ms, expected <1500ms")

        println("✓ Streamed ${results.size} issues (${successCount} successful) in ${elapsedMs}ms")
    }

    companion object {
        @JvmStatic
        fun referenceFilesExist(): Boolean {
            return Path("references/beads/.beads/issues.jsonl").toFile().exists()
        }

        @JvmStatic
        fun meshCoreQtExists(): Boolean {
            return Path("references/MeshCoreQt/.beads/issues.jsonl").toFile().exists()
        }

        @JvmStatic
        fun projectIssuesExist(): Boolean {
            return Path(".beads/issues.jsonl").toFile().exists()
        }
    }
}
