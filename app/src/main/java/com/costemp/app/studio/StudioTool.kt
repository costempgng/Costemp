package com.costemp.app.studio

/** The navigation tools available in the viewport rail. */
enum class StudioTool(val label: String, val hint: String) {
    ORBIT("Orbit", "Drag to orbit around the mannequin"),
    PAN("Pan", "Drag to move the camera"),
    ZOOM("Zoom", "Drag up or down to zoom, pinch on any tool"),
}
