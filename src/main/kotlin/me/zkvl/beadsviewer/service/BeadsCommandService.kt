package me.zkvl.beadsviewer.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Project-level service for executing bd commands from the plugin.
 *
 * This service:
 * - Executes bd CLI commands via ProcessBuilder
 * - Provides command builders for different field types
 * - Handles shell escaping for safe command execution
 * - Exposes reactive state for UI feedback
 */
@Service(Service.Level.PROJECT)
class BeadsCommandService(private val project: Project) : Disposable {
    private val logger = Logger.getInstance(BeadsCommandService::class.java)

    // Command execution state
    private val _commandState = MutableStateFlow<CommandState>(CommandState.Idle)
    val commandState: StateFlow<CommandState> = _commandState.asStateFlow()

    /**
     * Sealed class representing the state of command execution.
     */
    sealed class CommandState {
        /** No command is currently executing. */
        object Idle : CommandState()

        /** A command is currently executing. */
        data class Executing(val command: String) : CommandState()

        /** Command executed successfully. */
        data class Success(val command: String, val output: String) : CommandState()

        /** Command failed with an error. */
        data class Error(val command: String, val message: String) : CommandState()
    }

    /**
     * Updates an issue field via bd update command.
     *
     * @param issueId The issue ID to update
     * @param field The field name to update
     * @param value The new value for the field
     * @return Result containing the command output or error
     */
    suspend fun updateIssue(
        issueId: String,
        field: String,
        value: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val command = buildUpdateCommand(issueId, field, value)
        executeCommand(command)
    }

    /**
     * Adds a comment to an issue.
     *
     * @param issueId The issue ID to add a comment to
     * @param commentText The comment text
     * @return Result containing the command output or error
     */
    suspend fun addComment(
        issueId: String,
        commentText: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val escapedText = escapeShellArgument(commentText)
        val command = "bd comment $issueId \"$escapedText\""
        executeCommand(command)
    }

    /**
     * Updates issue status.
     *
     * @param issueId The issue ID to update
     * @param status The new status value
     * @return Result containing the command output or error
     */
    suspend fun updateStatus(
        issueId: String,
        status: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val command = "bd update $issueId --status $status"
        executeCommand(command)
    }

    /**
     * Updates issue status using Status enum.
     *
     * @param issueId The issue ID to update
     * @param status The new Status enum value
     * @return Result containing the command output or error
     */
    suspend fun updateIssueStatus(
        issueId: String,
        status: me.zkvl.beadsviewer.model.Status
    ): Result<String> {
        // Convert Status enum to lowercase string for bd CLI
        val statusString = status.name.lowercase()
        return updateStatus(issueId, statusString)
    }

    /**
     * Updates issue priority.
     *
     * @param issueId The issue ID to update
     * @param priority The new priority (0-4)
     * @return Result containing the command output or error
     */
    suspend fun updatePriority(
        issueId: String,
        priority: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        val command = "bd update $issueId --priority $priority"
        executeCommand(command)
    }

    /**
     * Builds bd update command with proper escaping.
     *
     * @param issueId The issue ID
     * @param field The field to update
     * @param value The new value
     * @return The complete bd command string
     */
    private fun buildUpdateCommand(
        issueId: String,
        field: String,
        value: String
    ): String {
        val escapedValue = escapeShellArgument(value)
        return when (field) {
            "title" -> "bd update $issueId --title \"$escapedValue\""
            "description" -> "bd update $issueId --description \"$escapedValue\""
            "notes" -> "bd update $issueId --notes \"$escapedValue\""
            "design" -> "bd update $issueId --design \"$escapedValue\""
            "acceptance" -> "bd update $issueId --acceptance \"$escapedValue\""
            "assignee" -> "bd update $issueId --assignee \"$escapedValue\""
            "estimate" -> "bd update $issueId --estimate $value"
            else -> throw IllegalArgumentException("Unknown field: $field")
        }
    }

    /**
     * Executes a bd command using ProcessBuilder.
     *
     * @param command The command to execute
     * @return Result containing output or error
     */
    private fun executeCommand(command: String): Result<String> {
        _commandState.value = CommandState.Executing(command)
        logger.info("Executing: $command")

        return try {
            val basePath = project.basePath
                ?: return Result.failure(IllegalStateException("Project path not found"))

            // Execute command in project directory
            val processBuilder = ProcessBuilder()
                .command("bash", "-c", command)
                .directory(File(basePath))
                .redirectErrorStream(true)

            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                _commandState.value = CommandState.Success(command, output)
                logger.info("Command succeeded: $command")
                Result.success(output)
            } else {
                val errorMsg = "Command failed with exit code $exitCode: $output"
                _commandState.value = CommandState.Error(command, errorMsg)
                logger.error(errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = "Failed to execute command: ${e.message}"
            _commandState.value = CommandState.Error(command, errorMsg)
            logger.error("Command execution failed", e)
            Result.failure(e)
        }
    }

    /**
     * Escapes shell argument for safe command execution.
     * Escapes: backslashes, quotes, dollar signs, backticks, newlines.
     *
     * @param arg The argument to escape
     * @return The escaped argument
     */
    private fun escapeShellArgument(arg: String): String {
        return arg
            .replace("\\", "\\\\")  // Escape backslashes
            .replace("\"", "\\\"")  // Escape quotes
            .replace("\$", "\\\$")  // Escape dollar signs
            .replace("`", "\\`")    // Escape backticks
            .replace("\n", "\\n")   // Escape newlines
    }

    override fun dispose() {
        // Cleanup if needed
    }

    companion object {
        /**
         * Get the BeadsCommandService instance for a project.
         *
         * @param project The project to get the service for
         * @return The project's BeadsCommandService instance
         */
        fun getInstance(project: Project): BeadsCommandService = project.service()
    }
}
