package com.costemp.core.geometry

import kotlin.math.sqrt

/** Immutable 3-component vector in a right-handed, Y-up world. Units are centimetres. */
data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Float) = Vec3(x * scale, y * scale, z * scale)
    operator fun div(scale: Float) = Vec3(x / scale, y / scale, z / scale)
    operator fun unaryMinus() = Vec3(-x, -y, -z)

    fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3) = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )

    fun length(): Float = sqrt(dot(this))

    fun normalized(): Vec3 {
        val length = length()
        return if (length < 1e-6f) ZERO else this * (1f / length)
    }

    fun withY(newY: Float) = Vec3(x, newY, z)

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
        val UP = Vec3(0f, 1f, 0f)
        val ONE = Vec3(1f, 1f, 1f)
    }
}

/** An axis-aligned bounding box, used for camera framing and the status bar. */
data class Bounds(val min: Vec3, val max: Vec3) {
    val width: Float get() = max.x - min.x
    val height: Float get() = max.y - min.y
    val depth: Float get() = max.z - min.z
    val center: Vec3 get() = (min + max) * 0.5f

    companion object {
        fun of(points: FloatArray): Bounds {
            require(points.size >= 3) { "Need at least one point" }
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var minZ = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            var maxZ = -Float.MAX_VALUE
            var i = 0
            while (i < points.size) {
                val x = points[i]
                val y = points[i + 1]
                val z = points[i + 2]
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (z < minZ) minZ = z
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
                if (z > maxZ) maxZ = z
                i += 3
            }
            return Bounds(Vec3(minX, minY, minZ), Vec3(maxX, maxY, maxZ))
        }
    }
}

/**
 * Column-major 4x4 matrix, laid out exactly like an OpenGL uniform buffer:
 * element (row, column) lives at `m[column * 4 + row]`.
 */
class Mat4(val m: FloatArray) {
    init {
        require(m.size == 16) { "Mat4 needs 16 floats, got ${m.size}" }
    }

    operator fun times(other: Mat4): Mat4 {
        val result = FloatArray(16)
        for (column in 0 until 4) {
            for (row in 0 until 4) {
                var sum = 0f
                for (k in 0 until 4) {
                    sum += m[k * 4 + row] * other.m[column * 4 + k]
                }
                result[column * 4 + row] = sum
            }
        }
        return Mat4(result)
    }

    /** Transforms a point (w = 1) and drops the resulting w. */
    fun transformPoint(point: Vec3): Vec3 = Vec3(
        m[0] * point.x + m[4] * point.y + m[8] * point.z + m[12],
        m[1] * point.x + m[5] * point.y + m[9] * point.z + m[13],
        m[2] * point.x + m[6] * point.y + m[10] * point.z + m[14],
    )

    companion object {
        fun identity() = Mat4(
            floatArrayOf(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f,
            ),
        )

        /** Right-handed perspective projection with an OpenGL depth range of [-1, 1]. */
        fun perspective(fovYDegrees: Float, aspect: Float, near: Float, far: Float): Mat4 {
            require(aspect > 0f) { "Aspect ratio must be positive" }
            require(near > 0f && far > near) { "Invalid clip planes: $near..$far" }
            val f = 1f / kotlin.math.tan(Math.toRadians(fovYDegrees / 2.0)).toFloat()
            val rangeInverse = 1f / (near - far)
            return Mat4(
                floatArrayOf(
                    f / aspect, 0f, 0f, 0f,
                    0f, f, 0f, 0f,
                    0f, 0f, (far + near) * rangeInverse, -1f,
                    0f, 0f, 2f * far * near * rangeInverse, 0f,
                ),
            )
        }

        /** Right-handed view matrix looking from [eye] towards [target]. */
        fun lookAt(eye: Vec3, target: Vec3, up: Vec3): Mat4 {
            val forward = (target - eye).normalized()
            val side = forward.cross(up).normalized()
            val trueUp = side.cross(forward)
            return Mat4(
                floatArrayOf(
                    side.x, trueUp.x, -forward.x, 0f,
                    side.y, trueUp.y, -forward.y, 0f,
                    side.z, trueUp.z, -forward.z, 0f,
                    -side.dot(eye), -trueUp.dot(eye), forward.dot(eye), 1f,
                ),
            )
        }
    }
}
