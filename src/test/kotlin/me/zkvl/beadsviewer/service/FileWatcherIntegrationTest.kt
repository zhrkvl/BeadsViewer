package me.zkvl.beadsviewer.service

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert

/**
 * Integration tests for file watching functionality.
 *
 * These tests verify that the FileWatcherService is properly registered and accessible.
 * Note: Using JUnit 3 style (test methods prefixed with 'test') for compatibility with BasePlatformTestCase.
 *
 * Note: Full integration testing with VFS events is complex in the test environment.
 * These tests focus on verifying the service infrastructure is correctly set up.
 */
class FileWatcherIntegrationTest : BasePlatformTestCase() {

    /**
     * Test that FileWatcherService can be instantiated.
     */
    fun testServiceInstance() {
        val service = FileWatcherService.getInstance(project)
        Assert.assertNotNull("FileWatcherService should not be null", service)
    }

    /**
     * Test that FileWatcherService implements BulkFileListener.
     */
    fun testImplementsBulkFileListener() {
        val service = FileWatcherService.getInstance(project)
        Assert.assertTrue(
            "FileWatcherService should implement BulkFileListener",
            service is com.intellij.openapi.vfs.newvfs.BulkFileListener
        )
    }

    /**
     * Test that IssueService is accessible from FileWatcherService context.
     */
    fun testIssueServiceAccessible() {
        val fileWatcherService = FileWatcherService.getInstance(project)
        val issueService = IssueService.getInstance(project)

        Assert.assertNotNull("FileWatcherService should not be null", fileWatcherService)
        Assert.assertNotNull("IssueService should not be null", issueService)
    }

    /**
     * Test that services are properly scoped to project.
     */
    fun testProjectScoping() {
        val fileWatcherService1 = FileWatcherService.getInstance(project)
        val fileWatcherService2 = FileWatcherService.getInstance(project)

        Assert.assertEquals(
            "FileWatcherService should be a singleton per project",
            fileWatcherService1,
            fileWatcherService2
        )
    }
}
