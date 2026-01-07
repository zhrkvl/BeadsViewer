package me.zkvl.beadsviewer.state

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import me.zkvl.beadsviewer.model.ViewMode

/**
 * Persistent state component that stores view mode per project.
 *
 * Uses IntelliJ Platform's state management to persist the selected view mode
 * across IDE restarts. State is stored in .idea/workspace.xml (project-level).
 */
@Service(Service.Level.PROJECT)
@State(
    name = "BeadsViewerViewModeState",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class ViewModeStateService : PersistentStateComponent<ViewModeStateService.State> {

    /**
     * Data class holding the persisted state.
     * Must be mutable for IntelliJ Platform's state management.
     */
    data class State(
        var currentViewMode: String = ViewMode.DEFAULT.name
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    /**
     * Returns the current view mode for this project.
     */
    fun getCurrentViewMode(): ViewMode {
        return ViewMode.fromString(myState.currentViewMode)
    }

    /**
     * Sets the current view mode for this project.
     */
    fun setCurrentViewMode(mode: ViewMode) {
        myState.currentViewMode = mode.name
    }

    companion object {
        /**
         * Gets the ViewModeStateService instance for a project.
         */
        fun getInstance(project: Project): ViewModeStateService {
            return project.service<ViewModeStateService>()
        }
    }
}
