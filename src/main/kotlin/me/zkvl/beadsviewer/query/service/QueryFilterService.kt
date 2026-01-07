package me.zkvl.beadsviewer.query.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import me.zkvl.beadsviewer.model.Issue
import me.zkvl.beadsviewer.query.ast.Query
import me.zkvl.beadsviewer.query.evaluator.QueryEvaluator
import me.zkvl.beadsviewer.query.lexer.Lexer
import me.zkvl.beadsviewer.query.lexer.LexerException
import me.zkvl.beadsviewer.query.parser.Parser
import me.zkvl.beadsviewer.query.parser.ParserException
import me.zkvl.beadsviewer.service.IssueService

/**
 * Service for filtering issues by query language.
 *
 * This service:
 * - Maintains reactive state of filtered issues
 * - Combines IssueService.issuesState with query state
 * - Handles query parsing and evaluation
 * - Provides error feedback for invalid queries
 *
 * The service automatically reacts to:
 * - Issue data changes (from IssueService)
 * - Query changes (from setQuery/clearQuery)
 *
 * Example usage:
 * ```kotlin
 * val service = QueryFilterService.getInstance(project)
 *
 * // Set a query
 * service.setQuery("status:open AND priority:0")
 *
 * // Subscribe to filtered results
 * service.filteredState.collect { state ->
 *     when (state) {
 *         is FilteredIssuesState.Filtered -> {
 *             println("Found ${state.issues.size} issues")
 *         }
 *         is FilteredIssuesState.Error -> {
 *             println("Error: ${state.message}")
 *         }
 *     }
 * }
 * ```
 */
@Service(Service.Level.PROJECT)
class QueryFilterService(private val project: Project) {
    private val logger = Logger.getInstance(QueryFilterService::class.java)
    private val issueService = IssueService.getInstance(project)
    private val evaluator = QueryEvaluator()

    // Coroutine scope tied to service lifecycle
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Current query string and parsed query
    private val _queryString = MutableStateFlow<String?>(null)
    val queryString: StateFlow<String?> = _queryString.asStateFlow()

    private val _parsedQuery = MutableStateFlow<ParsedQueryState>(ParsedQueryState.NoQuery)
    val parsedQuery: StateFlow<ParsedQueryState> = _parsedQuery.asStateFlow()

    // Reactive state for filtered issues
    private val _filteredState = MutableStateFlow<FilteredIssuesState>(FilteredIssuesState.NoFilter)
    val filteredState: StateFlow<FilteredIssuesState> = _filteredState.asStateFlow()

    /**
     * Sealed class representing parsed query state.
     */
    sealed class ParsedQueryState {
        /** No query active */
        data object NoQuery : ParsedQueryState()

        /** Successfully parsed query */
        data class Valid(val query: Query) : ParsedQueryState()

        /** Parse error */
        data class Invalid(val error: String, val position: Int?) : ParsedQueryState()
    }

    /**
     * Sealed class representing filtered issues state.
     */
    sealed class FilteredIssuesState {
        /** No query active - use unfiltered issues */
        data object NoFilter : FilteredIssuesState()

        /** Query active and filtering in progress */
        data object Loading : FilteredIssuesState()

        /** Query active and filtering complete */
        data class Filtered(
            val issues: List<Issue>,
            val query: String,
            val totalCount: Int  // Total issues before filtering
        ) : FilteredIssuesState()

        /** Query parsing/evaluation error */
        data class Error(
            val message: String,
            val position: Int?
        ) : FilteredIssuesState()
    }

    init {
        logger.info("QueryFilterService initialized for project: ${project.name}")

        // Combine issue state and query state
        scope.launch {
            combine(
                issueService.issuesState,
                _parsedQuery
            ) { issuesState, queryState ->
                Pair(issuesState, queryState)
            }.collect { (issuesState, queryState) ->
                updateFilteredState(issuesState, queryState)
            }
        }
    }

    /**
     * Set the query string and trigger filtering.
     *
     * If the query is blank, clears the query.
     * Otherwise, parses and applies the query.
     *
     * @param queryString The query string
     */
    fun setQuery(queryString: String) {
        if (queryString.isBlank()) {
            clearQuery()
            return
        }

        logger.info("Setting query: $queryString")
        _queryString.value = queryString

        // Parse query
        val parseResult = parseQuery(queryString)
        _parsedQuery.value = parseResult
    }

    /**
     * Clear the query and return to unfiltered state.
     */
    fun clearQuery() {
        logger.info("Clearing query")
        _queryString.value = null
        _parsedQuery.value = ParsedQueryState.NoQuery
        _filteredState.value = FilteredIssuesState.NoFilter
    }

    /**
     * Get the current query string.
     *
     * @return The current query string, or null if no query is active
     */
    fun getCurrentQuery(): String? = _queryString.value

    /**
     * Parse query string into AST.
     */
    private fun parseQuery(queryString: String): ParsedQueryState {
        // Lex
        val lexResult = Lexer(queryString).tokenize()
        if (lexResult.isFailure) {
            val error = lexResult.exceptionOrNull()
            logger.warn("Lexer error: ${error?.message}")
            return ParsedQueryState.Invalid(
                error?.message ?: "Unknown lexer error",
                (error as? LexerException)?.position
            )
        }

        val tokens = lexResult.getOrThrow()

        // Parse
        val parseResult = Parser(tokens).parse()
        if (parseResult.isFailure) {
            val error = parseResult.exceptionOrNull()
            logger.warn("Parser error: ${error?.message}")
            return ParsedQueryState.Invalid(
                error?.message ?: "Unknown parser error",
                (error as? ParserException)?.position
            )
        }

        val query = parseResult.getOrThrow()
        logger.info("Query parsed successfully: filter=${query.filter != null}, sort=${query.sort.size} directives")
        return ParsedQueryState.Valid(query)
    }

    /**
     * Update filtered state based on issues and query.
     */
    private fun updateFilteredState(
        issuesState: IssueService.IssuesState,
        queryState: ParsedQueryState
    ) {
        when {
            // No query active
            queryState is ParsedQueryState.NoQuery -> {
                _filteredState.value = FilteredIssuesState.NoFilter
            }

            // Query parsing error
            queryState is ParsedQueryState.Invalid -> {
                _filteredState.value = FilteredIssuesState.Error(
                    queryState.error,
                    queryState.position
                )
            }

            // Issues loading
            issuesState is IssueService.IssuesState.Loading -> {
                _filteredState.value = FilteredIssuesState.Loading
            }

            // Issues error
            issuesState is IssueService.IssuesState.Error -> {
                _filteredState.value = FilteredIssuesState.Error(
                    "Failed to load issues: ${issuesState.message}",
                    null
                )
            }

            // Filter issues
            issuesState is IssueService.IssuesState.Loaded &&
            queryState is ParsedQueryState.Valid -> {
                try {
                    val filtered = evaluator.filter(issuesState.issues, queryState.query)

                    _filteredState.value = FilteredIssuesState.Filtered(
                        issues = filtered,
                        query = _queryString.value ?: "",
                        totalCount = issuesState.issues.size
                    )

                    logger.info("Filtered ${filtered.size}/${issuesState.issues.size} issues")
                } catch (e: Exception) {
                    logger.error("Evaluation error", e)
                    _filteredState.value = FilteredIssuesState.Error(
                        "Query evaluation error: ${e.message}",
                        null
                    )
                }
            }
        }
    }

    companion object {
        /**
         * Get the QueryFilterService instance for a project.
         *
         * @param project The project
         * @return The QueryFilterService instance
         */
        fun getInstance(project: Project): QueryFilterService = project.service()
    }
}
