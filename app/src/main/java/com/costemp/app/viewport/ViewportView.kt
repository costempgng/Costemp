package com.costemp.app.viewport

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.costemp.app.studio.StudioTool
import com.costemp.core.geometry.Mesh

/**
 * The 3D viewport surface.
 *
 * Gestures follow the rail tool: one finger orbits, pans or dollies. Two fingers always
 * pan and pinch always zooms, whichever tool is active, and a double tap frames the
 * mannequin. All gestures are incremental, so they compose with the tool selection
 * instead of fighting it.
 */
class ViewportView(context: Context) : GLSurfaceView(context) {

    private val renderer = MannequinRenderer()
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val tapDetector = GestureDetector(context, TapListener())

    var tool: StudioTool = StudioTool.ORBIT

    private var submittedMesh: Mesh? = null
    private var appliedCameraToken = -1
    private var lastX = 0f
    private var lastY = 0f
    private var centroidX = 0f
    private var centroidY = 0f

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 24, 0)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
    }

    fun submitMesh(mesh: Mesh) {
        if (submittedMesh === mesh) return
        submittedMesh = mesh
        renderer.submitMesh(mesh)
    }

    fun setWireframe(enabled: Boolean) {
        renderer.wireframe = enabled
    }

    fun setGrid(enabled: Boolean) {
        renderer.showGrid = enabled
    }

    /** Frames the mannequin whenever the view model increments the reset token. */
    fun applyCameraToken(token: Int) {
        if (token == appliedCameraToken) return
        val firstApplication = appliedCameraToken < 0
        appliedCameraToken = token
        if (!firstApplication || token > 0) renderer.resetCamera()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        tapDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                centroidX = centroid(event, horizontal = true)
                centroidY = centroid(event, horizontal = false)
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount > 1) {
                    val x = centroid(event, horizontal = true)
                    val y = centroid(event, horizontal = false)
                    if (!scaleDetector.isInProgress) {
                        renderer.pan(x - centroidX, y - centroidY, height)
                    }
                    centroidX = x
                    centroidY = y
                } else {
                    val deltaX = event.x - lastX
                    val deltaY = event.y - lastY
                    when (tool) {
                        StudioTool.ORBIT -> renderer.orbit(deltaX, deltaY)
                        StudioTool.PAN -> renderer.pan(deltaX, deltaY, height)
                        StudioTool.ZOOM -> renderer.zoom(1f - deltaY / height.coerceAtLeast(1))
                    }
                    lastX = event.x
                    lastY = event.y
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val remaining = if (event.actionIndex == 0) 1 else 0
                if (remaining < event.pointerCount) {
                    lastX = event.getX(remaining)
                    lastY = event.getY(remaining)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    private fun centroid(event: MotionEvent, horizontal: Boolean): Float {
        if (event.pointerCount == 0) return 0f
        var sum = 0f
        for (index in 0 until event.pointerCount) {
            sum += if (horizontal) event.getX(index) else event.getY(index)
        }
        return sum / event.pointerCount
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            renderer.zoom(detector.scaleFactor)
            return true
        }
    }

    private inner class TapListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(event: MotionEvent): Boolean {
            renderer.resetCamera()
            return true
        }
    }
}
