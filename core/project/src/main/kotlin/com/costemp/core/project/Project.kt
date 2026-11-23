package com.costemp.core.project

import com.costemp.core.measurement.BodyProfile
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A saved Costemp project.
 *
 * The project is the document the studio edits: a name plus the measurement set that
 * drives the mannequin. Geometry is always regenerated from [profile], so projects stay
 * tiny and remain readable as plain JSON.
 */
@Serializable
data class Project(
    val id: String,
    val name: String,
    val profile: BodyProfile,
    val createdAt: Long,
    val updatedAt: Long,
) {
    /** The shortened record shown in the project list. */
    fun summary(): ProjectSummary = ProjectSummary(
        id = id,
        name = name,
        heightCm = profile.heightCm,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        const val DEFAULT_NAME = "Untitled Costemp"

        fun create(
            name: String = DEFAULT_NAME,
            profile: BodyProfile = BodyProfile.DEFAULT,
            now: Long = System.currentTimeMillis(),
            id: String = UUID.randomUUID().toString(),
        ): Project {
            val trimmed = name.trim().ifEmpty { DEFAULT_NAME }
            return Project(
                id = id,
                name = trimmed,
                profile = profile,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}

@Serializable
data class ProjectSummary(
    val id: String,
    val name: String,
    val heightCm: Double,
    val createdAt: Long,
    val updatedAt: Long,
)
