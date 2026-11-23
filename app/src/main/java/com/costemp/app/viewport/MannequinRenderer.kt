package com.costemp.app.viewport

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.costemp.core.geometry.Mesh
import com.costemp.core.geometry.OrbitCamera
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Draws the measurement-driven mannequin with OpenGL ES 3.0.
 *
 * Two programs are used: a Lambert + rim lit shader for solids and a flat shader for
 * the ground grid and wireframe. Geometry is uploaded once per mesh and lives in
 * VBOs, so orbiting only costs a uniform update per frame.
 *
 * The camera is shared with the touch handler and guarded by [cameraLock]; GL buffers
 * are only touched on the GL thread.
 */
class MannequinRenderer : GLSurfaceView.Renderer {

    private val camera = OrbitCamera()
    private val cameraLock = Any()

    @Volatile
    var wireframe: Boolean = false

    @Volatile
    var showGrid: Boolean = true

    @Volatile
    private var pendingMesh: Mesh? = null

    private var uploadedMesh: Mesh? = null
    private var framedOnce = false

    private var viewportWidth = 1
    private var viewportHeight = 1

    private var litProgram = 0
    private var litMvp = -1
    private var litView = -1
    private var litBaseColor = -1
    private var litRimColor = -1
    private var litPosition = -1
    private var litNormal = -1

    private var lineProgram = 0
    private var lineMvp = -1
    private var lineColor = -1
    private var linePosition = -1

    private var positionBuffer = 0
    private var normalBuffer = 0
    private var indexBuffer = 0
    private var edgeBuffer = 0
    private var indexCount = 0
    private var edgeCount = 0

    private var gridBuffer = 0
    private var gridVertexCount = 0
    private var axisBuffer = 0
    private var axisVertexCount = 0

    // ------------------------------------------------------------------ public API

    fun submitMesh(mesh: Mesh) {
        pendingMesh = mesh
    }

    fun resetCamera() {
        synchronized(cameraLock) {
            camera.reset()
            uploadedMesh?.let { camera.fit(it.bounds()) }
            framedOnce = true
        }
    }

    fun orbit(deltaX: Float, deltaY: Float) {
        synchronized(cameraLock) { camera.orbit(deltaX, deltaY) }
    }

    fun pan(deltaX: Float, deltaY: Float, viewportHeightPx: Int) {
        synchronized(cameraLock) { camera.pan(deltaX, deltaY, viewportHeightPx.toFloat()) }
    }

    fun zoom(scale: Float) {
        synchronized(cameraLock) { camera.dolly(scale) }
    }

    // ------------------------------------------------------------------ GL callbacks

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.031f, 0.031f, 0.039f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        GLES30.glLineWidth(1f)

        litProgram = linkProgram(LIT_VERTEX_SHADER, LIT_FRAGMENT_SHADER)
        litMvp = GLES30.glGetUniformLocation(litProgram, "uMvp")
        litView = GLES30.glGetUniformLocation(litProgram, "uView")
        litBaseColor = GLES30.glGetUniformLocation(litProgram, "uBaseColor")
        litRimColor = GLES30.glGetUniformLocation(litProgram, "uRimColor")
        litPosition = GLES30.glGetAttribLocation(litProgram, "aPosition")
        litNormal = GLES30.glGetAttribLocation(litProgram, "aNormal")

        lineProgram = linkProgram(LINE_VERTEX_SHADER, LINE_FRAGMENT_SHADER)
        lineMvp = GLES30.glGetUniformLocation(lineProgram, "uMvp")
        lineColor = GLES30.glGetUniformLocation(lineProgram, "uColor")
        linePosition = GLES30.glGetAttribLocation(lineProgram, "aPosition")

        // A new context invalidates every buffer handle.
        positionBuffer = 0
        normalBuffer = 0
        indexBuffer = 0
        edgeBuffer = 0
        uploadedMesh = null
        gridBuffer = 0
        axisBuffer = 0
        buildGridGeometry()
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
        GLES30.glViewport(0, 0, viewportWidth, viewportHeight)
    }

    override fun onDrawFrame(unused: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        pendingMesh?.let { mesh ->
            if (mesh !== uploadedMesh) {
                uploadMesh(mesh)
                uploadedMesh = mesh
                pendingMesh = null
                if (!framedOnce) {
                    synchronized(cameraLock) { camera.fit(mesh.bounds()) }
                    framedOnce = true
                }
            }
        }

        val aspect = viewportWidth.toFloat() / viewportHeight.toFloat()
        val mvp: com.costemp.core.geometry.Mat4
        val view: com.costemp.core.geometry.Mat4
        synchronized(cameraLock) {
            mvp = camera.viewProjection(aspect)
            view = camera.viewMatrix()
        }

        if (showGrid) {
            drawLines(gridBuffer, gridVertexCount, mvp, 0.16f, 0.16f, 0.20f, 1f)
        }
        drawLines(axisBuffer, axisVertexCount, mvp, 0.61f, 0.42f, 1.0f, 0.9f)

        if (uploadedMesh != null && indexCount > 0) {
            drawMesh(mvp, view)
        }
        if (wireframe && edgeCount > 0) {
            drawLines(edgeBuffer, edgeCount, mvp, 0.60f, 0.48f, 0.95f, 1f)
        }
    }

    // ------------------------------------------------------------------ drawing

    private fun drawMesh(mvp: com.costemp.core.geometry.Mat4, view: com.costemp.core.geometry.Mat4) {
        GLES30.glUseProgram(litProgram)
        GLES30.glUniformMatrix4fv(litMvp, 1, false, mvp.m, 0)
        GLES30.glUniformMatrix4fv(litView, 1, false, view.m, 0)
        GLES30.glUniform3f(litBaseColor, 0.50f, 0.52f, 0.60f)
        GLES30.glUniform3f(litRimColor, 0.62f, 0.42f, 1.0f)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, positionBuffer)
        GLES30.glEnableVertexAttribArray(litPosition)
        GLES30.glVertexAttribPointer(litPosition, 3, GLES30.GL_FLOAT, false, 0, 0)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, normalBuffer)
        GLES30.glEnableVertexAttribArray(litNormal)
        GLES30.glVertexAttribPointer(litNormal, 3, GLES30.GL_FLOAT, false, 0, 0)

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBuffer)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, 0)

        GLES30.glDisableVertexAttribArray(litPosition)
        GLES30.glDisableVertexAttribArray(litNormal)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    private fun drawLines(
        buffer: Int,
        vertices: Int,
        mvp: com.costemp.core.geometry.Mat4,
        red: Float,
        green: Float,
        blue: Float,
        alpha: Float,
    ) {
        if (buffer == 0 || vertices == 0) return
        GLES30.glUseProgram(lineProgram)
        GLES30.glUniformMatrix4fv(lineMvp, 1, false, mvp.m, 0)
        GLES30.glUniform4f(lineColor, red, green, blue, alpha)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffer)
        GLES30.glEnableVertexAttribArray(linePosition)
        GLES30.glVertexAttribPointer(linePosition, 3, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, vertices)
        GLES30.glDisableVertexAttribArray(linePosition)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    // ------------------------------------------------------------------ uploads

    private fun uploadMesh(mesh: Mesh) {
        positionBuffer = uploadFloats(mesh.positions, positionBuffer, GLES30.GL_ARRAY_BUFFER)
        normalBuffer = uploadFloats(mesh.normals, normalBuffer, GLES30.GL_ARRAY_BUFFER)
        indexBuffer = uploadInts(mesh.indices, indexBuffer)
        indexCount = mesh.indices.size
        val edges = mesh.edgeIndices()
        edgeBuffer = uploadInts(edges, edgeBuffer)
        edgeCount = edges.size
    }

    private fun buildGridGeometry() {
        // A 25 cm grid over an 8 m square, with the two centre lines drawn separately
        // as coloured axes.
        val grid = ArrayList<Float>()
        val extent = 400f
        val step = 25f
        var coordinate = -extent
        while (coordinate <= extent) {
            if (coordinate != 0f) {
                grid.add(-extent); grid.add(0f); grid.add(coordinate)
                grid.add(extent); grid.add(0f); grid.add(coordinate)
                grid.add(coordinate); grid.add(0f); grid.add(-extent)
                grid.add(coordinate); grid.add(0f); grid.add(extent)
            }
            coordinate += step
        }
        gridVertexCount = grid.size / 3
        gridBuffer = uploadFloats(grid.toFloatArray(), gridBuffer, GLES30.GL_ARRAY_BUFFER)

        val axes = floatArrayOf(
            -extent, 0f, 0f, extent, 0f, 0f,
            0f, 0f, -extent, 0f, 0f, extent,
        )
        axisVertexCount = axes.size / 3
        axisBuffer = uploadFloats(axes, axisBuffer, GLES30.GL_ARRAY_BUFFER)
    }

    private fun uploadFloats(data: FloatArray, existing: Int, target: Int): Int {
        val direct = ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        direct.put(data).position(0)
        val handle = if (existing != 0) existing else generateBuffer()
        GLES30.glBindBuffer(target, handle)
        GLES30.glBufferData(target, data.size * 4, direct, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(target, 0)
        return handle
    }

    private fun uploadInts(data: IntArray, existing: Int): Int {
        val direct: IntBuffer = ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
        direct.put(data).position(0)
        val handle = if (existing != 0) existing else generateBuffer()
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, handle)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, data.size * 4, direct, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
        return handle
    }

    private fun generateBuffer(): Int {
        val handle = IntArray(1)
        GLES30.glGenBuffers(1, handle, 0)
        return handle[0]
    }

    // ------------------------------------------------------------------ shaders

    private fun linkProgram(vertexSource: String, fragmentSource: String): Int {
        val vertex = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fragment = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertex)
        GLES30.glAttachShader(program, fragment)
        GLES30.glLinkProgram(program)
        val status = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(program)
            GLES30.glDeleteProgram(program)
            throw IllegalStateException("Program link failed: $log")
        }
        GLES30.glDetachShader(program, vertex)
        GLES30.glDetachShader(program, fragment)
        GLES30.glDeleteShader(vertex)
        GLES30.glDeleteShader(fragment)
        return program
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw IllegalStateException("Shader compile failed: $log")
        }
        return shader
    }

    private companion object {
        const val LIT_VERTEX_SHADER = """#version 300 es
uniform mat4 uMvp;
uniform mat4 uView;
in vec3 aPosition;
in vec3 aNormal;
out vec3 vNormal;
out vec3 vEyePosition;
void main() {
    vec4 eyePosition = uView * vec4(aPosition, 1.0);
    vEyePosition = eyePosition.xyz;
    vNormal = mat3(uView) * aNormal;
    gl_Position = uMvp * vec4(aPosition, 1.0);
}
"""

        const val LIT_FRAGMENT_SHADER = """#version 300 es
precision mediump float;
uniform vec3 uBaseColor;
uniform vec3 uRimColor;
in vec3 vNormal;
in vec3 vEyePosition;
out vec4 fragColor;
void main() {
    vec3 normal = normalize(vNormal);
    vec3 viewDirection = normalize(-vEyePosition);
    vec3 keyLight = normalize(vec3(0.45, 0.82, 0.55));
    vec3 fillLight = normalize(vec3(-0.65, 0.25, -0.45));
    float key = max(dot(normal, keyLight), 0.0);
    float fill = max(dot(normal, fillLight), 0.0) * 0.32;
    float rim = pow(1.0 - max(dot(normal, viewDirection), 0.0), 2.6);
    vec3 color = uBaseColor * (0.20 + 0.85 * key + fill) + uRimColor * rim * 0.55;
    fragColor = vec4(color, 1.0);
}
"""

        const val LINE_VERTEX_SHADER = """#version 300 es
uniform mat4 uMvp;
in vec3 aPosition;
void main() {
    gl_Position = uMvp * vec4(aPosition, 1.0);
}
"""

        const val LINE_FRAGMENT_SHADER = """#version 300 es
precision mediump float;
uniform vec4 uColor;
out vec4 fragColor;
void main() {
    fragColor = uColor;
}
"""
    }
}
