package me.zkvl.beadsviewer.state

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.zkvl.beadsviewer.ui.theme.ThemeMode

/**
 * Persistent state component that stores theme mode per project.
 *
 * Uses IntelliJ Platform's state management to persist the selected theme mode
 * across IDE restarts. State is stored in .idea/workspace.xml (project-level).
 */
@Service(Service.Level.PROJECT)
@State(
    name = "BeadsViewerThemeState",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class ThemeStateService : PersistentStateComponent<ThemeStateService.State> {

    /**
     * Data class holding the persisted state.
     * Must be mutable for IntelliJ Platform's state management.
     */
    data class State(
        var currentThemeMode: String = ThemeMode.SYSTEM.name
    )

    private var myState = State()
    private val _themeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)

    /**
     * StateFlow for observing theme mode changes reactively.
     */
    val themeModeFlow: StateFlow<ThemeMode> = _themeModeFlow.asStateFlow()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
        _themeModeFlow.value = getCurrentThemeMode()
    }

    /**
     * Returns the current theme mode for this project.
     */
    fun getCurrentThemeMode(): ThemeMode {
        return try {
            ThemeMode.valueOf(myState.currentThemeMode)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    /**
     * Sets the current theme mode for this project.
     */
    fun setCurrentThemeMode(mode: ThemeMode) {
        myState.currentThemeMode = mode.name
        _themeModeFlow.value = mode
    }

    companion object {
        /**
         * Gets the ThemeStateService instance for a project.
         */
        fun getInstance(project: Project): ThemeStateService {
            return project.service<ThemeStateService>()
        }
    }
}
