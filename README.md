# Costemp

Open-source Android cosplay studio: a measurement-driven mannequin for fitting armour,
sculpting, retopology, papercraft unfolding and EVA foam patterns.

This repository contains the **Phase 1 foundation**: a working studio workspace with a
real project manager, an anthropometric measurement model, and a procedurally generated
3D mannequin rendered with OpenGL ES 3.

## What works today

- **Studio workspace** — dark three-panel layout (menu bar, tool rail, 3D viewport,
  collapsible properties panel, status bar). Phones show the rail as a floating strip and
  the properties in a bottom sheet; tablets from 840dp show all three columns.
- **Measurement-driven mannequin** — nine tape measurements (height, head, neck,
  shoulders, chest, waist, hips, arm length, inseam) drive a watertight mannequin mesh.
  Edits rebuild the mesh live; the status bar reports real triangle/vertex counts and
  bounding dimensions.
- **Project manager** — create, open, rename, delete, import and export projects as
  readable `.costemp.json` files. Edits autosave, the last project reopens on launch.
- **Undo/redo** with edit coalescing, and a reset to the reference body.
- **Viewport navigation** — orbit, pan and zoom tools, two-finger pan, pinch zoom,
  double-tap framing, ground grid and wireframe overlays.
- **Units** — metric storage with a cm/in presentation toggle.

Fabrication phases (armour shells, sculpting, retopology, papercraft unfolding, foam
patterns, print preparation) are **not** implemented yet. The app does not show buttons
for features that do not exist.

## Architecture

```
app/                 Jetpack Compose UI, OpenGL ES viewport, view model
core/measurement/    BodyProfile, measurement ranges, ellipse math, proportions
core/geometry/       Vectors, matrices, meshes, primitives, mannequin builder, camera
core/project/        Project model + file-backed repository
```

The geometry engine and measurement model are pure Kotlin/JVM modules with no Android
dependency, which keeps them unit-testable and lets them be reused by future desktop
tooling. See [docs/architecture.md](docs/architecture.md) and
[docs/geometry.md](docs/geometry.md).

## Build

Requirements: JDK 17 and the Android SDK (platform 35, build-tools 35.0.0).

```bash
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest \
  :core:geometry:test \
  :core:measurement:test \
  :core:project:test             # unit tests
```

Install the debug APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## CI

- `.github/workflows/test.yml` (`test`) builds the debug APK and runs the unit tests for
  every push and pull request.
- `.github/workflows/release.yml` (`release`) builds release APK/AAB on a `v*` tag (or a
  manual dispatch), signs them when the signing secrets are configured, publishes a
  GitHub Release and uploads the artifacts.

Release signing reads `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`,
`RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD` from repository secrets. Without them the
release build is unsigned and the release notes say so.

## Roadmap

1. Foundation — workspace, project manager, measurement model, mannequin *(done)*
2. Armour fitting on the mannequin
3. Sculpting / mesh editing
4. Retopology
5. Papercraft unfolding and export
6. EVA foam patterns
7. 3D print preparation

## License

No license has been specified for this repository.
