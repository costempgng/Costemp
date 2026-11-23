package com.costemp.core.project

import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * File-backed project manager.
 *
 * Layout: one pretty-printed JSON document per project inside [root], named
 * `<id>.costemp.json`, plus a tiny `last-opened` file so the studio can reopen the
 * document the user was working on.
 *
 * Ids are validated against `[A-Za-z0-9_-]{1,64}` before they touch the filesystem,
 * which makes path traversal through a modified file impossible.
 */
class ProjectRepository(
    val root: File,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    init {
        if (!root.exists()) root.mkdirs()
    }

    fun list(): List<ProjectSummary> =
        (root.listFiles { file -> file.isFile && file.name.endsWith(SUFFIX) } ?: emptyArray())
            .mapNotNull { file ->
                runCatching { json.decodeFromString(Project.serializer(), file.readText()).summary().takeIf { it.id == file.idFromName() } }
                    .getOrNull()
            }
            .sortedWith(compareByDescending<ProjectSummary> { it.updatedAt }.thenBy { it.name.lowercase() })

    fun exists(id: String): Boolean = fileFor(id)?.exists() == true

    fun load(id: String): Project? {
        val file = fileFor(id) ?: return null
        if (!file.exists()) return null
        return runCatching { json.decodeFromString(Project.serializer(), file.readText()) }.getOrNull()
            ?.takeIf { it.id == id }
    }

    /** Writes [project], stamping a fresh `updatedAt`, and returns the stored copy. */
    fun save(project: Project): Project {
        val file = fileFor(project.id) ?: error("Invalid project id '${project.id}'")
        val stored = project.copy(updatedAt = clock())
        file.writeText(encode(stored))
        return stored
    }

    fun delete(id: String): Boolean {
        val file = fileFor(id) ?: return false
        return file.exists() && file.delete()
    }

    fun lastOpenedId(): String? {
        val marker = File(root, LAST_OPENED)
        if (!marker.exists()) return null
        val id = marker.readText().trim()
        return id.takeIf { isValidId(it) && exists(it) }
    }

    fun rememberLastOpened(id: String?) {
        val marker = File(root, LAST_OPENED)
        if (id == null) {
            marker.delete()
        } else {
            marker.writeText(id)
        }
    }

    /** Serialises a project for export through the system file picker. */
    fun encode(project: Project): String = json.encodeToString(Project.serializer(), project)

    /**
     * Reads an exported project, giving it a fresh id so importing the same file twice
     * creates two documents instead of silently overwriting one.
     */
    fun decodeImported(jsonText: String, now: Long = clock()): Project? {
        val parsed = runCatching { json.decodeFromString(Project.serializer(), jsonText) }.getOrNull() ?: return null
        return parsed.copy(id = UUID.randomUUID().toString(), updatedAt = now)
    }

    /** Adds a new project to the store, making sure the id cannot collide. */
    fun create(name: String, profile: com.costemp.core.measurement.BodyProfile = com.costemp.core.measurement.BodyProfile.DEFAULT): Project {
        val project = Project.create(name = name, profile = profile, now = clock())
        return save(project)
    }

    private fun fileFor(id: String): File? = if (isValidId(id)) File(root, "$id$SUFFIX") else null

    private fun File.idFromName(): String = name.removeSuffix(SUFFIX)

    companion object {
        const val SUFFIX = ".costemp.json"
        private const val LAST_OPENED = "last-opened"
        private val ID_PATTERN = Regex("[A-Za-z0-9_-]{1,64}")
        fun isValidId(id: String): Boolean = ID_PATTERN.matches(id)
    }
}
