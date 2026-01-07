package me.zkvl.beadsviewer.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color tokens for the Beads design system.
 *
 * Provides semantic colors for status, priority, and type indicators
 * following IntelliJ Platform design patterns.
 */
object BeadsColors {
    // Status colors
    val StatusOpen = Color(0xFF5C9FE5)        // Blue
    val StatusInProgress = Color(0xFFE5A55C)  // Orange
    val StatusBlocked = Color(0xFFE55C5C)     // Red
    val StatusClosed = Color(0xFF5CE585)      // Green
    val StatusDeferred = Color(0xFF9E9E9E)    // Gray
    val StatusHooked = Color(0xFFB85CE5)      // Purple

    // Priority colors (P0-P4)
    val PriorityP0 = Color(0xFFDB3737)  // Critical red
    val PriorityP1 = Color(0xFFE5825C)  // High orange
    val PriorityP2 = Color(0xFFE5C55C)  // Medium yellow
    val PriorityP3 = Color(0xFF85B8E5)  // Low blue
    val PriorityP4 = Color(0xFF9E9E9E)  // Lowest gray

    // Type colors
    val TypeTask = Color(0xFF5C9FE5)
    val TypeBug = Color(0xFFE55C5C)
    val TypeFeature = Color(0xFF85E55C)
    val TypeEpic = Color(0xFFB85CE5)
    val TypeChore = Color(0xFF9E9E9E)

    /**
     * Returns the color for a given status string.
     */
    fun statusColor(status: String): Color = when (status.lowercase()) {
        "open" -> StatusOpen
        "in_progress" -> StatusInProgress
        "blocked" -> StatusBlocked
        "closed" -> StatusClosed
        "deferred" -> StatusDeferred
        "hooked" -> StatusHooked
        else -> StatusOpen
    }

    /**
     * Returns the color for a given priority (0-4).
     */
    fun priorityColor(priority: Int): Color = when (priority) {
        0 -> PriorityP0
        1 -> PriorityP1
        2 -> PriorityP2
        3 -> PriorityP3
        else -> PriorityP4
    }
}
