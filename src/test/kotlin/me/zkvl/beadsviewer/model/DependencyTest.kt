package me.zkvl.beadsviewer.model

import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("Dependency Model Tests")
class DependencyTest {

    @Test
    @DisplayName("isBlocking() should return true for Blocks type")
    fun testIsBlockingReturnsTrueForBlocks() {
        val dep = Dependency(
            issueId = "A",
            dependsOnId = "B",
            type = DependencyType.Blocks,
            createdAt = Instant.parse("2026-01-07T10:00:00Z"),
            createdBy = "test"
        )
        assertTrue(dep.isBlocking())
    }

    @Test
    @DisplayName("isBlocking() should return false for Related type")
    fun testIsBlockingReturnsFalseForRelated() {
        val dep = Dependency(
            issueId = "A",
            dependsOnId = "B",
            type = DependencyType.Related,
            createdAt = Instant.parse("2026-01-07T10:00:00Z"),
            createdBy = "test"
        )
        assertFalse(dep.isBlocking())
    }

    @Test
    @DisplayName("isBlocking() should return false for ParentChild type")
    fun testIsBlockingReturnsFalseForParentChild() {
        val dep = Dependency(
            issueId = "A",
            dependsOnId = "B",
            type = DependencyType.ParentChild,
            createdAt = Instant.parse("2026-01-07T10:00:00Z"),
            createdBy = "test"
        )
        assertFalse(dep.isBlocking())
    }

    @Test
    @DisplayName("isBlocking() should return false for DiscoveredFrom type")
    fun testIsBlockingReturnsFalseForDiscoveredFrom() {
        val dep = Dependency(
            issueId = "A",
            dependsOnId = "B",
            type = DependencyType.DiscoveredFrom,
            createdAt = Instant.parse("2026-01-07T10:00:00Z"),
            createdBy = "test"
        )
        assertFalse(dep.isBlocking())
    }

    @Test
    @DisplayName("isBlocking() should handle custom types as non-blocking")
    fun testIsBlockingReturnsFalseForCustomTypes() {
        val dep = Dependency(
            issueId = "A",
            dependsOnId = "B",
            type = DependencyType.Custom("custom-type"),
            createdAt = Instant.parse("2026-01-07T10:00:00Z"),
            createdBy = "test"
        )
        assertFalse(dep.isBlocking())
    }
}
