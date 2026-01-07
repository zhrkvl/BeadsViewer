package me.zkvl.beadsviewer.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("Status Enum Tests")
class StatusTest {

    @Test
    @DisplayName("isClosed() should return true only for CLOSED")
    fun testIsClosedReturnsTrueOnlyForClosed() {
        assertTrue(Status.CLOSED.isClosed())
        assertFalse(Status.OPEN.isClosed())
        assertFalse(Status.IN_PROGRESS.isClosed())
        assertFalse(Status.BLOCKED.isClosed())
        assertFalse(Status.TOMBSTONE.isClosed())
        assertFalse(Status.HOOKED.isClosed())
    }

    @Test
    @DisplayName("isOpen() should return true for OPEN and IN_PROGRESS")
    fun testIsOpenReturnsTrueForOpenAndInProgress() {
        assertTrue(Status.OPEN.isOpen())
        assertTrue(Status.IN_PROGRESS.isOpen())
        assertFalse(Status.CLOSED.isOpen())
        assertFalse(Status.BLOCKED.isOpen())
        assertFalse(Status.TOMBSTONE.isOpen())
        assertFalse(Status.HOOKED.isOpen())
    }

    @Test
    @DisplayName("isTombstone() should return true only for TOMBSTONE")
    fun testIsTombstoneReturnsTrueOnlyForTombstone() {
        assertTrue(Status.TOMBSTONE.isTombstone())
        assertFalse(Status.OPEN.isTombstone())
        assertFalse(Status.IN_PROGRESS.isTombstone())
        assertFalse(Status.BLOCKED.isTombstone())
        assertFalse(Status.CLOSED.isTombstone())
        assertFalse(Status.HOOKED.isTombstone())
    }
}
