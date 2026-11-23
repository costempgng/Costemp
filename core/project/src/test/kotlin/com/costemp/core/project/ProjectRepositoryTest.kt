package com.costemp.core.project

import com.costemp.core.measurement.BodyProfile
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ProjectRepositoryTest {

    private val root: File = Files.createTempDirectory("costemp-test").toFile()
    private var now = 1_000L
    private val repository = ProjectRepository(root) { now }

    @After
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun `save then load round trips the profile`() {
        val project = Project.create("Rathalos armor", BodyProfile(heightCm = 172.5, chestCircumferenceCm = 101.0), now)
        val stored = repository.save(project)
        assertEquals(1_000L, stored.updatedAt)

        val loaded = repository.load(project.id)
        assertNotNull(loaded)
        assertEquals("Rathalos armor", loaded!!.name)
        assertEquals(172.5, loaded.profile.heightCm, 0.0)
        assertEquals(101.0, loaded.profile.chestCircumferenceCm, 0.0)
    }

    @Test
    fun `saving stamps updatedAt from the clock`() {
        val project = repository.create("Mando")
        now = 5_000L
        val updated = repository.save(project.copy(name = "Mando v2"))
        assertEquals(5_000L, updated.updatedAt)
        assertEquals(1_000L, repository.load(project.id)!!.createdAt)
    }

    @Test
    fun `list is newest first and skips corrupt files`() {
        val first = repository.create("First")
        now = 2_000L
        repository.create("Second")
        File(root, "broken.costemp.json").writeText("{ not json")

        val list = repository.list()
        assertEquals(2, list.size)
        assertEquals("Second", list[0].name)
        assertEquals("First", list[1].name)
        assertFalse(list.any { it.id == first.id && it.name == "Second" })
    }

    @Test
    fun `delete removes the document and reports missing ids`() {
        val project = repository.create("Temp")
        assertTrue(repository.delete(project.id))
        assertFalse(repository.delete(project.id))
        assertNull(repository.load(project.id))
    }

    @Test
    fun `invalid ids are rejected instead of touching the filesystem`() {
        assertFalse(ProjectRepository.isValidId("../etc/passwd"))
        assertFalse(ProjectRepository.isValidId("with space"))
        assertTrue(ProjectRepository.isValidId("a-b_9"))
        assertNull(repository.load("../../escape"))
        assertNull(repository.fluidSave("bad id"))
    }

    @Test
    fun `last opened marker survives reloads and ignores deleted projects`() {
        val project = repository.create("Ongoing")
        repository.rememberLastOpened(project.id)
        assertEquals(project.id, ProjectRepository(root).lastOpenedId())

        repository.delete(project.id)
        assertNull(repository.lastOpenedId())

        repository.rememberLastOpened(null)
        assertNull(repository.lastOpenedId())
    }

    @Test
    fun `imported projects get a new id`() {
        val project = repository.create("Palette")
        val exported = repository.encode(project)
        val imported = repository.decodeImported(exported, now = 9_000L)
        assertNotNull(imported)
        assertTrue(imported!!.id != project.id)
        assertEquals(9_000L, imported.updatedAt)
        assertEquals("Palette", imported.name)
        assertNull(repository.decodeImported("nope"))
    }

    private fun ProjectRepository.fluidSave(id: String): Project? =
        runCatching { save(Project.create(id = id)) }.getOrNull()
}
