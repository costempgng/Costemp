# Architecture

Costemp is split so the modelling engine never depends on the UI. Everything below
`core/` is plain Kotlin/JVM and is covered by unit tests; `app/` holds Android-only code.

```
app/
  studio/StudioViewModel.kt   state, undo/redo, autosave, project lifecycle
  studio/StudioState.kt       immutable snapshot rendered by Compose
  viewport/MannequinRenderer  OpenGL ES 3.0 renderer (grid, mesh, wireframe)
  viewport/ViewportView       GLSurfaceView + gesture handling
  ui/                         Compose workspace (menu, rail, panels, dialogs)
core/measurement/             BodyProfile, ranges, ellipse solving, proportions
core/geometry/                Vec3, Mat4, Mesh, primitives, MannequinBuilder, OrbitCamera
core/project/                 Project documents + ProjectRepository
```

## Data flow

1. `StudioViewModel` owns the open `Project` and publishes a `StudioState`.
2. A measurement edit updates the `BodyProfile`, pushes the previous profile onto the
   undo stack (coalesced within 800 ms), rebuilds the mannequin and republishes state.
3. `ViewportPanel` forwards the new `Mesh` reference to `ViewportView`, which hands it to
   the renderer. The renderer uploads it once per GL thread and keeps it in VBOs.
4. Autosave writes the project to disk 700 ms after the last edit.

The mesh is regenerated from the profile on every change instead of being stored, so a
project file stays tiny and deterministic.

## Threading

- Compose and the view model run on the main thread.
- File I/O is dispatched to `Dispatchers.IO`.
- The renderer touches OpenGL only on the GL thread. Camera mutations from touch are
  guarded by a small lock shared with the renderer.
- The mesh reference is handed over through a `@Volatile` field: immutable arrays mean
  no torn reads.

## Persistence

`ProjectRepository` stores one pretty-printed JSON document per project in
`filesDir/projects/<id>.costemp.json`, plus `last-opened` with the id of the project to
reopen. Ids are validated against `[A-Za-z0-9_-]{1,64}` before touching the filesystem.
Imports get a fresh id so importing the same file twice does not overwrite a project.

## Responsive layout

`CostempApp` measures the window with `BoxWithConstraints`. At 840dp or wider the tool
rail and properties panel are fixed columns; below that the rail floats over the
viewport and properties open in a modal bottom sheet. Both paths drive the same state.
