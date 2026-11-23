# Geometry notes

All engine coordinates are centimetres in a right-handed, Y-up world. The mannequin
stands on `y = 0`, is centred on `x = z = 0`, and faces `+Z`.

## Circumference to cross-section

A tape measurement is a perimeter; the mesh needs radii. For an ellipse with semi-axes
`a >= b > 0` Costemp uses Ramanujan's second approximation:

```
C ≈ π · ( 3(a + b) − √( (3a + b)(a + 3b) ) )
```

Because `C(a)` is strictly increasing once the depth ratio `b = r·a` is fixed, the inverse
is solved by bisection on `[0, C]` (60 iterations, `gnu` tolerance far below measuring
precision). `Ellipse.forCircumference` is unit-tested against the forward formula, and
its depth ratio is preserved exactly.

Depth ratios: chest `0.72`, waist `0.76`, hips `0.80`. Measured widths therefore decide
the silhouette instead of a guessed constant.

## Vertical proportions

The mannequin follows the classic 7.5-head figure plus stature fractions
(`MannequinProportions`):

| landmark | height |
| --- | --- |
| head length | `H / 7.5` |
| shoulder | `H − 1.5 · head` |
| chest | `0.72 H` |
| waist | `0.615 H` |
| hip | `0.52 H` |
| knee | `0.48 · inseam` |

`headCentreY + headLength/2 = H` and the feet rest on `y = 0`, so the mesh bounds equal the
profile height exactly; both are asserted in `GeometryTest`.

## Primitive construction

- **Ellipsoid** — latitude/longitude parameterisation. Normals are the analytic gradient
  of `x²/a² + y²/b² + z²/c² = 1`, normalised, so no smoothing pass is needed.
- **Frustum (`cylinder`)** — an orthonormal frame is built around the axis, allowing limbs
  in any direction. Side normals blend the radial direction with the taper slope.
- **Limb** — a frustum plus a sphere at each end, hiding the joint seam between segments.
- **Loft** — ellipses stacked along Y are skinned into a closed solid with top/bottom fans.
  Side normals come from the ellipse gradient, which is correct at the widest points.

## Mannequin assembly

`MannequinBuilder` unions the torso loft (pelvis, hip, waist, chest and shoulder rings),
neck, head, both arms (deltoid + upper arm + forearm + hand) and both legs (thigh + calf +
foot). Parts overlap rather than being welded: the silhouette is solid from every angle
and the builder stays simple.

## Camera

`OrbitCamera` is a turntable: the eye sits on a sphere around a target, parameterised by
azimuth, elevation and distance. Pixels map to world units by
`2 · distance · tan(fov/2) / viewportHeight`, which makes panning resolution independent.
It lives in `core/geometry` and is unit-tested.

## Wireframe

`Mesh.edgeIndices` packs each undirected index pair into a `Long`, collects them in a
sorted set to drop the duplicate stored by adjacent triangles, and emits a `GL_LINES`
index buffer. The same edge buffer is drawn over the shaded mesh when wireframe is on.
