package com.costemp.core.geometry

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Turntable camera used by the 3D viewport.
 *
 * The camera orbits a [target] point on a sphere described by [azimuthDegrees],
 * [elevationDegrees] and [distance], which makes it trivial to keep the mannequin
 * centred while the user spins around it.
 *
 * It lives in the geometry module (not the UI) because it is pure math and is covered
 * by unit tests: framing, orbits, panning and projection all behave deterministically.
 */
class OrbitCamera(
    var azimuthDegrees: Float = 24f,
    var elevationDegrees: Float = 6f,
    var distance: Float = 380f,
    var target: Vec3 = Vec3(0f, 90f, 0f),
    var fovYDegrees: Float = 45f,
) {
    var nearPlane: Float = 1f
    var farPlane: Float = 40000f

    fun eye(): Vec3 {
        val azimuth = Math.toRadians(azimuthDegrees.toDouble())
        val elevation = Math.toRadians(elevationDegrees.toDouble())
        val horizontal = distance * cos(elevation).toFloat()
        return target + Vec3(
            horizontal * sin(azimuth).toFloat(),
            distance * sin(elevation).toFloat(),
            horizontal * cos(azimuth).toFloat(),
        )
    }

    fun viewMatrix(): Mat4 = Mat4.lookAt(eye(), target, Vec3.UP)

    fun projectionMatrix(aspect: Float): Mat4 =
        Mat4.perspective(fovYDegrees, aspect.coerceAtLeast(0.1f), nearPlane, farPlane)

    fun viewProjection(aspect: Float): Mat4 = projectionMatrix(aspect) * viewMatrix()

    /** Rotates around the target. Positive dx spins right, positive dy looks up. */
    fun orbit(deltaX: Float, deltaY: Float, degreesPerPixel: Float = 0.35f) {
        azimuthDegrees = (azimuthDegrees - deltaX * degreesPerPixel) % 360f
        elevationDegrees = (elevationDegrees + deltaY * degreesPerPixel).coerceIn(-85f, 85f)
    }

    /** Pinch zoom: [scale] > 1 moves closer. */
    fun dolly(scale: Float) {
        if (scale <= 0f) return
        distance = (distance / scale).coerceIn(20f, 3000f)
    }

    /**
     * Pans the target in the camera plane.
     *
     * One screen pixel covers `2 * distance * tan(fov/2) / viewportHeight` world units.
     * Dragging right must move the model right, so the target moves left along the
     * camera's right vector; dragging down moves the target up.
     */
    fun pan(deltaXPixels: Float, deltaYPixels: Float, viewportHeightPixels: Float) {
        if (viewportHeightPixels <= 0f) return
        val worldPerPixel =
            2f * distance * tan(Math.toRadians(fovYDegrees / 2.0)).toFloat() / viewportHeightPixels
        val forward = (target - eye()).normalized()
        val right = forward.cross(Vec3.UP).normalized()
        val up = right.cross(forward).normalized()
        target += right * (-deltaXPixels * worldPerPixel) + up * (deltaYPixels * worldPerPixel)
    }

    /** Frames [bounds] so the whole model is comfortably inside the viewport. */
    fun fit(bounds: Bounds) {
        target = Vec3(0f, bounds.center.y, 0f)
        val vertical = bounds.height.coerceAtLeast(10f)
        val halfFov = Math.toRadians(fovYDegrees / 2.0)
        distance = (vertical / (2.0 * tan(halfFov)) * 1.9).toFloat().coerceIn(40f, 3000f)
        azimuthDegrees = 24f
        elevationDegrees = 6f
    }

    /** Neutral three-quarter view used when a project opens. */
    fun reset() {
        azimuthDegrees = 24f
        elevationDegrees = 6f
        distance = 380f
        target = Vec3(0f, 90f, 0f)
    }
}
