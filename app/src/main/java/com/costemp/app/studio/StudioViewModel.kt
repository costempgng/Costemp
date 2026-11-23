package com.costemp.app.studio

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.costemp.core.geometry.MannequinBuilder
import com.costemp.core.measurement.BodyProfile
import com.costemp.core.measurement.LengthUnit
import com.costemp.core.measurement.MeasurementKind
import com.costemp.core.project.Project
import com.costemp.core.project.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * Owns the open project, its measurement history and the generated mannequin.
 *
 * State transitions are synchronous so the mesh in the viewport always matches the
 * numbers in the properties panel; only file I/O is dispatched to the IO dispatcher.
 */
class StudioViewModel(private val repository: ProjectRepository) : ViewModel() {

    private val _state = MutableStateFlow(StudioState())
    val state: StateFlow<StudioState> = _state.asStateFlow()

    private val undoStack = ArrayDeque<BodyProfile>()
    private val redoStack = ArrayDeque<BodyProfile>()
    private var lastUndoStamp = 0L
    private var autosaveJob: Job? = null

    init {
        openStartupProject()
    }

    // ---------------------------------------------------------------- project lifecycle

    private fun openStartupProject() {
        val remembered = repository.lastOpenedId()?.let(repository::load)
        val project = remembered
            ?: repository.list().firstOrNull()?.let { repository.load(it.id) }
            ?: repository.create(Project.DEFAULT_NAME)
        repository.rememberLastOpened(project.id)
        undoStack.clear()
        redoStack.clear()
        present(project, status = null)
    }

    fun newProject(name: String) {
        val created = repository.create(name)
        repository.rememberLastOpened(created.id)
        undoStack.clear()
        redoStack.clear()
        present(created, status = "Created “${created.name}”")
    }

    fun openProject(id: String) {
        val project = repository.load(id) ?: return
        repository.rememberLastOpened(project.id)
        undoStack.clear()
        redoStack.clear()
        present(project, status = "Opened “${project.name}”")
    }

    fun deleteProject(id: String) {
        if (!repository.delete(id)) return
        val current = _state.value.project
        if (current?.id == id) {
            val fallback = repository.list().firstOrNull()?.let { repository.load(it.id) }
                ?: repository.create(Project.DEFAULT_NAME)
            repository.rememberLastOpened(fallback.id)
            undoStack.clear()
            redoStack.clear()
            present(fallback, status = "Project deleted")
        } else {
            _state.update { it.copy(savedProjects = repository.list()) }
        }
    }

    fun renameProject(name: String) {
        val project = _state.value.project ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed == project.name) return
        val renamed = project.copy(name = trimmed)
        persist(renamed, status = "Renamed to “$trimmed”")
    }

    fun saveNow() {
        val project = _state.value.project ?: return
        persist(project, status = "Saved “${project.name}”")
    }

    fun exportProjectJson(): String? {
        val project = _state.value.project ?: return null
        return repository.encode(project.copy(profile = _state.value.profile))
    }

    fun importProjectJson(json: String): Boolean {
        val imported = repository.decodeImported(json) ?: run {
            _state.update { it.copy(statusMessage = "That file is not a Costemp project") }
            return false
        }
        val stored = repository.save(imported)
        repository.rememberLastOpened(stored.id)
        undoStack.clear()
        redoStack.clear()
        present(stored, status = "Imported “${stored.name}”")
        return true
    }

    // ---------------------------------------------------------------- measurements

    /**
     * Applies a measurement edit.
     *
     * Values are clamped to the physically plausible range. Undo entries are coalesced
     * while a slider/typing burst is in progress, so one undo reverts a whole edit
     * rather than a single keystroke.
     */
    fun updateMeasurement(kind: MeasurementKind, valueCm: Double) {
        val project = _state.value.project ?: return
        val current = _state.value.profile
        val clamped = kind.clamp(valueCm)
        if (kotlin.math.abs(clamped - kind.read(current)) < 1e-4) return

        pushUndo(current)
        val updated = project.copy(profile = current.with(kind, clamped))
        present(updated)
        scheduleAutosave()
    }

    fun undo() {
        val project = _state.value.project ?: return
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_state.value.profile)
        lastUndoStamp = 0L
        present(project.copy(profile = previous), status = "Undo")
        scheduleAutosave()
    }

    fun redo() {
        val project = _state.value.project ?: return
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_state.value.profile)
        lastUndoStamp = 0L
        present(project.copy(profile = next), status = "Redo")
        scheduleAutosave()
    }

    fun resetMeasurements() {
        val project = _state.value.project ?: return
        pushUndo(_state.value.profile)
        present(project.copy(profile = BodyProfile.DEFAULT), status = "Measurements reset to the reference body")
        scheduleAutosave()
    }

    // ---------------------------------------------------------------- view controls

    fun setUnit(unit: LengthUnit) = _state.update { it.copy(unit = unit) }

    fun setTool(tool: StudioTool) = _state.update { it.copy(tool = tool) }

    fun toggleWireframe() = _state.update { it.copy(wireframe = !it.wireframe) }

    fun toggleGrid() = _state.update { it.copy(grid = !it.grid) }

    fun toggleProperties() = _state.update { it.copy(propertiesVisible = !it.propertiesVisible) }

    fun requestCameraReset() = _state.update { it.copy(cameraResetToken = it.cameraResetToken + 1) }

    fun showStatus(message: String) = _state.update { it.copy(statusMessage = message) }

    // ---------------------------------------------------------------- internals

    private fun pushUndo(profile: BodyProfile) {
        val now = System.currentTimeMillis()
        val coalesce = undoStack.isNotEmpty() && now - lastUndoStamp < 800L
        if (!coalesce) {
            undoStack.addLast(profile)
            while (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        }
        lastUndoStamp = now
        redoStack.clear()
    }

    private fun persist(project: Project, status: String?) {
        val stored = repository.save(project)
        repository.rememberLastOpened(stored.id)
        present(stored, status = status)
    }

    private fun scheduleAutosave() {
        val project = _state.value.project ?: return
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(700)
            val stored = withContext(Dispatchers.IO) { repository.save(project) }
            repository.rememberLastOpened(stored.id)
            _state.update { it.copy(savedProjects = repository.list()) }
        }
    }

    /** Rebuilds the mannequin and publishes a consistent state snapshot. */
    private fun present(project: Project, status: String? = null) {
        val profile = project.profile
        val issues = profile.validate()
        val mesh = runCatching { MannequinBuilder.build(profile) }.getOrNull()
        val stats = mesh?.let {
            val bounds = it.bounds()
            MeshStats(
                vertices = it.vertexCount,
                triangles = it.triangleCount,
                widthCm = bounds.width,
                heightCm = bounds.height,
                depthCm = bounds.depth,
            )
        }
        _state.update { current ->
            current.copy(
                project = project,
                profile = profile,
                mesh = mesh,
                stats = stats,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                savedProjects = repository.list(),
                statusMessage = status ?: issues.firstOrNull()?.message,
            )
        }
    }

    companion object {
        private const val MAX_HISTORY = 120

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                StudioViewModel(
                    ProjectRepository(File(context.filesDir, "projects")),
                )
            }
        }
    }
}
