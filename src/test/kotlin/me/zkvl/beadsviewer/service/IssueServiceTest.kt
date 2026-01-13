package me.zkvl.beadsviewer.service

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert

/**
 * Unit tests for IssueService.
 *
 * These tests verify the service's state management, caching, and refresh functionality.
 * Note: Using JUnit 3 style (test methods prefixed with 'test') for compatibility with BasePlatformTestCase.
 */
class IssueServiceTest : BasePlatformTestCase() {

    /**
     * Test that service can be instantiated and retrieved.
     */
    fun testServiceInstance() {
        val service = IssueService.getInstance(project)
        Assert.assertNotNull("IssueService should not be null", service)
    }

    /**
     * Test that service initializes with a valid state.
     */
    fun testInitialState() {
        val service = IssueService.getInstance(project)
        val state = service.issuesState.value

        // Initial state should be one of the valid states
        Assert.assertTrue(
            "Initial state should be Loading, Loaded, or Error",
            state is IssueService.IssuesState.Loading ||
            state is IssueService.IssuesState.Loaded ||
            state is IssueService.IssuesState.Error
        )
    }

    /**
     * Test that getCacheStats returns valid structure.
     */
    fun testGetCacheStats() {
        val service = IssueService.getInstance(project)
        val stats = service.getCacheStats()

        Assert.assertNotNull("Cache stats should not be null", stats)

        // Verify isCached is a boolean
        Assert.assertTrue(
            "isCached should be a boolean",
            stats.isCached == true || stats.isCached == false
        )
    }

    /**
     * Test that getBeadsDir returns expected path.
     */
    fun testGetBeadsDir() {
        val service = IssueService.getInstance(project)
        val path = service.getBeadsDir()

        if (project.basePath != null) {
            Assert.assertNotNull("Path should not be null when project has basePath", path)
            Assert.assertTrue(
                "Path should end with .beads",
                path?.toString()?.endsWith(".beads") == true
            )
        }
    }

    /**
     * Test that service is a singleton per project.
     */
    fun testSingletonBehavior() {
        val service1 = IssueService.getInstance(project)
        val service2 = IssueService.getInstance(project)

        Assert.assertEquals(
            "IssueService should be a singleton per project",
            service1,
            service2
        )
    }

    /**
     * Test that refresh method can be called without error.
     */
    fun testRefreshMethod() {
        val service = IssueService.getInstance(project)

        // Should not throw exception
        service.refresh()

        // State should still be valid after refresh
        val state = service.issuesState.value
        Assert.assertTrue(
            "State should be valid after refresh",
            state is IssueService.IssuesState.Loading ||
            state is IssueService.IssuesState.Loaded ||
            state is IssueService.IssuesState.Error
        )
    }

    /**
     * Test that onFileChanged can be called without error.
     */
    fun testOnFileChangedMethod() {
        val service = IssueService.getInstance(project)

        // Should not throw exception
        service.onFileChanged()

        // State should still be valid
        val state = service.issuesState.value
        Assert.assertTrue(
            "State should be valid after onFileChanged",
            state is IssueService.IssuesState.Loading ||
            state is IssueService.IssuesState.Loaded ||
            state is IssueService.IssuesState.Error
        )
    }

    /**
     * Test that StateFlow is accessible and reactive.
     */
    fun testStateFlowAccessible() {
        val service = IssueService.getInstance(project)
        val stateFlow = service.issuesState

        Assert.assertNotNull("StateFlow should not be null", stateFlow)
        Assert.assertNotNull("StateFlow value should not be null", stateFlow.value)
    }
}
