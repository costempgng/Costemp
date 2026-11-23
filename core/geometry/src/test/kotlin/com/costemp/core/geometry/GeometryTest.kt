package com.costemp.core.geometry

import com.costemp.core.measurement.BodyProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryTest {

    @Test
    fun `vector math is right handed`() {
        val x = Vec3(1f, 0f, 0f)
        val y = Vec3(0f, 1f, 0f)
        val z = x.cross(y)
        assertEquals(0f, z.x, 0f)
        assertEquals(0f, z.y, 0f)
        assertEquals(1f, z.z, 0f)
        assertEquals(0f, x.dot(y), 0f)
        assertEquals(5f, Vec3(3f, 0f, 4f).length(), 1e-5f)
    }

    @Test
    fun `perspective matrix matches the gl convention`() {
        val projection = Mat4.perspective(90f, 1f, 1f, 101f)
        assertEquals(1f, projection.m[0], 1e-5f) // f / aspect with 90 degrees
        assertEquals(1f, projection.m[5], 1e-5f)
        assertEquals(-1f, projection.m[11], 0f)
        // A point on the near plane maps to NDC z = -1.
        val nearPoint = projection.transformPoint(Vec3(0f, 0f, -1f))
        assertEquals(-1f, nearPoint.z, 1e-5f)
    }

    @Test
    fun `lookAt places the target in front of the camera`() {
        val view = Mat4.lookAt(Vec3(0f, 0f, 10f), Vec3.ZERO, Vec3.UP)
        val target = view.transformPoint(Vec3.ZERO)
        assertEquals(0f, target.x, 1e-5f)
        assertEquals(0f, target.y, 1e-5f)
        assertEquals(-10f, target.z, 1e-5f)
    }

    @Test
    fun `ellipsoid has unit normals and expected bounds`() {
        val mesh = Meshes.ellipsoid(3f, 5f, 2f, rings = 12, segments = 18)
        val bounds = mesh.bounds()
        assertEquals(-3f, bounds.min.x, 1e-4f)
        assertEquals(3f, bounds.max.x, 1e-4f)
        assertEquals(-5f, bounds.min.y, 1e-4f)
        assertEquals(5f, bounds.max.y, 1e-4f)

        var index = 0
        while (index < mesh.normals.size) {
            val normal = Vec3(mesh.normals[index], mesh.normals[index + 1], mesh.normals[index + 2])
            assertEquals(1f, normal.length(), 1e-3f)
            index += 3
        }
    }

    @Test
    fun `cylinder produces unique wireframe edges`() {
        val mesh = Meshes.cylinder(Vec3.ZERO, Vec3(0f, 10f, 0f), 2f, 2f, segments = 6)
        val edges = mesh.edgeIndices()
        // Side: 7 distinct verticals (the sextant seam keeps a duplicate vertex, so the
        // closing vertical is its own edge), 12 ring edges (6 top + 6 bottom) and 6
        // quad diagonals = 25. Each cap fan adds 7 spokes (to every ring vertex,
        // including the duplicate seam one) = 14. Total 39 unique edges.
        assertEquals(39 * 2, edges.size)
        assertEquals(0, edges.size % 2)
    }

    @Test
    fun `mannequin rests on the floor and reaches the profile height`() {
        val mesh = MannequinBuilder.build(BodyProfile(heightCm = 178.0))
        val bounds = mesh.bounds()
        assertEquals(0f, bounds.min.y, 0.001f)
        assertEquals(178f, bounds.max.y, 0.001f)
        assertTrue(bounds.width > 30f)
        assertTrue(bounds.depth > 10f)
        assertTrue(mesh.triangleCount > 4000)
        assertTrue(mesh.vertexCount > 2000)
    }

    @Test
    fun `mannequin respects circumference changes`() {
        val slim = MannequinBuilder.build(BodyProfile(chestCircumferenceCm = 82.0, waistCircumferenceCm = 70.0))
        val broad = MannequinBuilder.build(BodyProfile(chestCircumferenceCm = 124.0, waistCircumferenceCm = 112.0))
        assertTrue(broad.bounds().width > slim.bounds().width)
        assertEquals(slim.vertexCount, broad.vertexCount)
    }

    @Test
    fun `orbiting keeps the camera on its sphere and clamps elevation`() {
        val camera = OrbitCamera()
        val initialEye = camera.eye()
        assertEquals(camera.distance, (initialEye - camera.target).length(), 1e-2f)

        camera.orbit(300f, 4000f)
        assertTrue(camera.elevationDegrees <= 85f)
        assertTrue(camera.elevationDegrees >= -85f)
        assertEquals(camera.distance, (camera.eye() - camera.target).length(), 1e-2f)
    }

    @Test
    fun `fit frames the whole body`() {
        val mesh = MannequinBuilder.build(BodyProfile())
        val camera = OrbitCamera()
        camera.fit(mesh.bounds())
        assertEquals(87.5f, camera.target.y, 0.5f)
        assertTrue(camera.distance > 200f)
        assertTrue(camera.distance < 900f)
    }

    @Test
    fun `panning and dollying move the camera as documented`() {
        val camera = OrbitCamera()
        val before = camera.target
        camera.pan(100f, 0f, 1000f)
        assertTrue(camera.target.x < before.x)
        assertEquals(before.y, camera.target.y, 1e-4f)

        val distanceBefore = camera.distance
        camera.dolly(2f)
        assertEquals(distanceBefore / 2f, camera.distance, 1e-4f)
    }
}
