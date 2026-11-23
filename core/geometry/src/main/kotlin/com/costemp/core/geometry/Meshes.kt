package com.costemp.core.geometry

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** One horizontal cross-section used by [Meshes.loft]. */
data class LoftRing(val y: Float, val halfWidth: Float, val halfDepth: Float)

/**
 * Parametric primitives used to assemble the mannequin.
 *
 * All shaded geometry is generated with smooth normals; the viewport renders with
 * back-face culling disabled so a closed primitive never shows holes when a limb is
 * rotated later.
 */
object Meshes {

    private const val TWO_PI = (Math.PI * 2.0).toFloat()

    /**
     * Ellipsoid centred on the origin.
     *
     * Vertices are laid out as latitude rings from the north pole to the south pole,
     * with `segments` samples per ring. The analytic gradient of
     * `x²/rx² + y²/ry² + z²/rz² = 1` gives the exact surface normal,
     * `normalise(x/rx², y/ry², z/rz²)`.
     */
    fun ellipsoid(
        radiusX: Float,
        radiusY: Float,
        radiusZ: Float,
        rings: Int = 16,
        segments: Int = 24,
    ): Mesh {
        require(rings >= 2 && segments >= 3) { "Ellipsoid needs at least 2 rings and 3 segments" }
        val builder = MeshBuilder()
        for (ring in 0..rings) {
            val phi = Math.PI * ring / rings
            val sinPhi = sin(phi).toFloat()
            val cosPhi = cos(phi).toFloat()
            for (segment in 0..segments) {
                val theta = TWO_PI * segment / segments
                val x = radiusX * sinPhi * cos(theta)
                val y = radiusY * cosPhi
                val z = radiusZ * sinPhi * sin(theta)
                val normal = Vec3(
                    x / (radiusX * radiusX),
                    y / (radiusY * radiusY),
                    z / (radiusZ * radiusZ),
                ).normalized()
                builder.addVertex(Vec3(x, y, z), normal)
            }
        }
        val stride = segments + 1
        for (ring in 0 until rings) {
            for (segment in 0 until segments) {
                val a = ring * stride + segment
                val b = a + stride
                builder.addQuad(a, b, b + 1, a + 1)
            }
        }
        return builder.build()
    }

    /**
     * Frustum between [start] and [end] with independent end radii.
     *
     * An orthonormal frame is built around the axis so the result can point in any
     * direction. When `rStart == rEnd` this is a cylinder.
     */
    fun cylinder(
        start: Vec3,
        end: Vec3,
        rStart: Float,
        rEnd: Float,
        segments: Int = 20,
        capped: Boolean = true,
    ): Mesh {
        require(segments >= 3) { "Cylinder needs at least 3 segments" }
        val axis = (end - start).normalized()
        val helper = if (abs(axis.y) > 0.99f) Vec3(1f, 0f, 0f) else Vec3(0f, 1f, 0f)
        val side = axis.cross(helper).normalized()
        val forward = axis.cross(side).normalized()
        val slope = (rStart - rEnd) / (end - start).length().coerceAtLeast(1e-4f)

        val builder = MeshBuilder()
        val startCenter = builder.addVertex(start, -axis)
        val endCenter = builder.addVertex(end, axis)
        val ringStart = IntArray(segments + 1)
        val ringEnd = IntArray(segments + 1)

        for (segment in 0..segments) {
            val angle = TWO_PI * segment / segments
            val direction = side * cos(angle) + forward * sin(angle)
            val normal = (direction + axis * slope).normalized()
            ringStart[segment] = builder.addVertex(start + direction * rStart, normal)
            ringEnd[segment] = builder.addVertex(end + direction * rEnd, normal)
        }
        for (segment in 0 until segments) {
            // Side quad, wound counter-clockwise when seen from outside.
            builder.addQuad(ringStart[segment], ringEnd[segment], ringEnd[segment + 1], ringStart[segment + 1])
        }
        if (capped) {
            for (segment in 0 until segments) {
                builder.addTriangle(startCenter, ringStart[segment + 1], ringStart[segment])
                builder.addTriangle(endCenter, ringEnd[segment], ringEnd[segment + 1])
            }
        }
        return builder.build()
    }

    /**
     * A limb segment: a frustum with a sphere at each end, so joints stay rounded and
     * the seam between two limbs is hidden.
     */
    fun limb(start: Vec3, end: Vec3, rStart: Float, rEnd: Float, segments: Int = 16): Mesh {
        val builder = MeshBuilder()
        builder.addMesh(cylinder(start, end, rStart, rEnd, segments, capped = false))
        builder.addMesh(ellipsoid(rStart, rStart, rStart, rings = 8, segments = segments), start)
        builder.addMesh(ellipsoid(rEnd, rEnd, rEnd, rings = 8, segments = segments), end)
        return builder.build()
    }

    /**
     * Skins a stack of elliptical cross-sections into a closed solid.
     *
     * Rings are assumed to be ordered by increasing `y`. Side normals come from the
     * ellipse gradient `(x / halfWidth², z / halfDepth²)`; the vertical component is
     * left at zero, which is correct on the widest points and visually stable at the
     * shallow slopes a torso uses.
     */
    fun loft(rings: List<LoftRing>, segments: Int = 28): Mesh {
        require(rings.size >= 2) { "Loft needs at least two rings" }
        require(segments >= 3) { "Loft needs at least three segments" }
        val builder = MeshBuilder()

        val rows = ArrayList<IntArray>()
        for (ring in rings) {
            val row = IntArray(segments + 1)
            for (segment in 0..segments) {
                val angle = TWO_PI * segment / segments
                val x = ring.halfWidth * cos(angle)
                val z = ring.halfDepth * sin(angle)
                val normal = Vec3(
                    x / (ring.halfWidth * ring.halfWidth),
                    0f,
                    z / (ring.halfDepth * ring.halfDepth),
                ).normalized()
                row[segment] = builder.addVertex(Vec3(x, ring.y, z), normal)
            }
            rows.add(row)
        }
        for (index in 0 until rows.size - 1) {
            val lower = rows[index]
            val upper = rows[index + 1]
            for (segment in 0 until segments) {
                builder.addQuad(lower[segment], lower[segment + 1], upper[segment + 1], upper[segment])
            }
        }

        val bottom = rings.first()
        val top = rings.last()
        val bottomCenter = builder.addVertex(Vec3(0f, bottom.y, 0f), Vec3(0f, -1f, 0f))
        val topCenter = builder.addVertex(Vec3(0f, top.y, 0f), Vec3(0f, 1f, 0f))
        for (segment in 0 until segments) {
            builder.addTriangle(bottomCenter, rows[0][segment], rows[0][segment + 1])
            builder.addTriangle(topCenter, rows[rows.size - 1][segment + 1], rows[rows.size - 1][segment])
        }
        return builder.build()
    }
}
