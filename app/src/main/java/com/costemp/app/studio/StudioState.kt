package com.costemp.app.studio

import com.costemp.core.geometry.Mesh
import com.costemp.core.measurement.BodyProfile
import com.costemp.core.measurement.LengthUnit
import com.costemp.core.project.Project
import com.costemp.core.project.ProjectSummary

/** Totals shown in the status bar, measured from the generated mesh. */
data class MeshStats(
    val vertices: Int,
    val triangles: Int,
    val widthCm: Float,
    val heightCm: Float,
    val depthCm: Float,
)

/**
 * Everything the studio UI renders.
 *
 * The view model owns the only mutable copy; composables read this snapshot.
 */
data class StudioState(
    val project: Project? = null,
    val profile: BodyProfile = BodyProfile.DEFAULT,
    val mesh: Mesh? = null,
    val stats: MeshStats? = null,
    val unit: LengthUnit = LengthUnit.CENTIMETRES,
    val tool: StudioTool = StudioTool.ORBIT,
    val wireframe: Boolean = false,
    val grid: Boolean = true,
    val propertiesVisible: Boolean = true,
    /** Incremented to ask the viewport to frame the mannequin again. */
    val cameraResetToken: Int = 0,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val savedProjects: List<ProjectSummary> = emptyList(),
    val statusMessage: String? = null,
) {
    val projectName: String get() = project?.name ?: "No project"
}
