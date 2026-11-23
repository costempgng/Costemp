package com.costemp.core.geometry

import java.util.TreeSet

/**
 * A triangulated surface ready to hand to OpenGL.
 *
 * Positions and normals are parallel `FloatArray`s of `xyz` triples; [indices] is a
 * triangle list. Meshes are immutable once built, so they can be shared between the
 * view model and the render thread without locking.
 */
class Mesh(
    val positions: FloatArray,
    val normals: FloatArray,
    val indices: IntArray,
) {
    val vertexCount: Int get() = positions.size / 3
    val triangleCount: Int get() = indices.size / 3

    init {
        require(positions.size % 3 == 0) { "Positions must be xyz triples" }
        require(normals.size == positions.size) { "Every vertex needs a normal" }
        require(indices.size % 3 == 0) { "Indices must be triangles" }
        require(indices.all { it in 0 until positions.size / 3 }) { "Index out of range" }
    }

    fun bounds(): Bounds = Bounds.of(positions)

    /**
     * Unique undirected edges, as an index buffer for `GL_LINES`.
     *
     * A closed mesh stores each interior edge twice (once per adjacent triangle), so
     * duplicates are removed through a sorted set of packed index pairs.
     */
    fun edgeIndices(): IntArray {
        val edges = TreeSet<Long>()
        var i = 0
        while (i < indices.size) {
            val a = indices[i]
            val b = indices[i + 1]
            val c = indices[i + 2]
            edges.add(pack(a, b))
            edges.add(pack(b, c))
            edges.add(pack(c, a))
            i += 3
        }
        val result = IntArray(edges.size * 2)
        var out = 0
        for (edge in edges) {
            result[out++] = (edge ushr 32).toInt()
            result[out++] = edge.toInt()
        }
        return result
    }

    private fun pack(a: Int, b: Int): Long {
        val low = if (a < b) a else b
        val high = if (a < b) b else a
        return (low.toLong() shl 32) or high.toLong()
    }
}

/** Accumulates primitives into a single [Mesh]. */
class MeshBuilder {
    private val positions = ArrayList<Float>()
    private val normals = ArrayList<Float>()
    private val indices = ArrayList<Int>()

    val vertexCount: Int get() = positions.size / 3

    fun addVertex(position: Vec3, normal: Vec3): Int {
        positions.add(position.x)
        positions.add(position.y)
        positions.add(position.z)
        normals.add(normal.x)
        normals.add(normal.y)
        normals.add(normal.z)
        return positions.size / 3 - 1
    }

    fun addTriangle(a: Int, b: Int, c: Int) {
        indices.add(a)
        indices.add(b)
        indices.add(c)
    }

    fun addQuad(a: Int, b: Int, c: Int, d: Int) {
        addTriangle(a, b, c)
        addTriangle(a, c, d)
    }

    fun addMesh(mesh: Mesh, offset: Vec3 = Vec3.ZERO) {
        val base = vertexCount
        var i = 0
        while (i < mesh.positions.size) {
            positions.add(mesh.positions[i] + offset.x)
            positions.add(mesh.positions[i + 1] + offset.y)
            positions.add(mesh.positions[i + 2] + offset.z)
            normals.add(mesh.normals[i])
            normals.add(mesh.normals[i + 1])
            normals.add(mesh.normals[i + 2])
            i += 3
        }
        for (index in mesh.indices) {
            indices.add(index + base)
        }
    }

    fun build(): Mesh = Mesh(
        positions.toFloatArray(),
        normals.toFloatArray(),
        indices.toIntArray(),
    )
}
