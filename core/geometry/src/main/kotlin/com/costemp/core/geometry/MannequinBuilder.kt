package com.costemp.core.geometry

import com.costemp.core.measurement.MannequinProportions

/**
 * Builds the Costemp reference mannequin from a [com.costemp.core.measurement.BodyProfile].
 *
 * ### Assembly
 * The body is a union of watertight primitives, each positioned from
 * [MannequinProportions]:
 *
 *  - **torso** — a loft through pelvis, hip, waist, chest and shoulder rings, so the
 *    silhouette really follows the tape measurements;
 *  - **head / neck** — ellipsoid plus a tapering frustum;
 *  - **arms** — upper arm and forearm frustums with spherical joints, mirrored in X;
 *  - **legs** — thigh and calf frustums plus a foot ellipsoid, mirrored in X.
 *
 * The feet rest on `y = 0` and the crown lands exactly on `y = height`, which the unit
 * tests assert. No vertex is welded between parts: the parts overlap, which keeps the
 * builder simple and produces a solid silhouette from every angle.
 */
object MannequinBuilder {

    private const val TORSO_SEGMENTS = 32
    private const val LIMB_SEGMENTS = 16

    fun build(proportions: MannequinProportions): Mesh {
        val builder = MeshBuilder()

        // Torso: rounded pelvis, hips, waist, chest and a shoulder shelf.
        val hipY = proportions.hipY
        val shoulderY = proportions.shoulderY
        builder.addMesh(
            Meshes.loft(
                listOf(
                    LoftRing(hipY - 6f, proportions.hipHalfWidth * 0.82f, proportions.hipHalfDepth * 0.86f),
                    LoftRing(hipY, proportions.hipHalfWidth, proportions.hipHalfDepth),
                    LoftRing(
                        lerp(hipY, proportions.waistY, 0.5f),
                        proportions.hipHalfWidth * 0.95f + proportions.waistHalfWidth * 0.05f,
                        proportions.hipHalfDepth * 0.95f + proportions.waistHalfDepth * 0.05f,
                    ),
                    LoftRing(proportions.waistY, proportions.waistHalfWidth, proportions.waistHalfDepth),
                    LoftRing(
                        lerp(proportions.waistY, proportions.chestY, 0.55f),
                        proportions.waistHalfWidth * 0.45f + proportions.chestHalfWidth * 0.55f,
                        proportions.waistHalfDepth * 0.45f + proportions.chestHalfDepth * 0.55f,
                    ),
                    LoftRing(proportions.chestY, proportions.chestHalfWidth, proportions.chestHalfDepth),
                    LoftRing(shoulderY, proportions.shoulderHalfWidth * 0.86f, proportions.chestHalfDepth * 0.92f),
                    LoftRing(shoulderY + 1.5f, proportions.shoulderHalfWidth * 0.72f, proportions.chestHalfDepth * 0.80f),
                ),
                TORSO_SEGMENTS,
            ),
        )

        // Neck frustum, slightly narrower at the top.
        builder.addMesh(
            Meshes.cylinder(
                Vec3(0f, shoulderY - 1f, 0f),
                Vec3(0f, proportions.neckTopY, 0f),
                proportions.neckRadius * 1.08f,
                proportions.neckRadius,
                LIMB_SEGMENTS,
            ),
        )

        // Head.
        builder.addMesh(
            Meshes.ellipsoid(
                proportions.headHalfWidth,
                proportions.headLength / 2f,
                proportions.headHalfDepth,
                rings = 14,
                segments = 24,
            ),
            Vec3(0f, proportions.headCenterY, 0f),
        )

        // Arms.
        for (side in listOf(-1f, 1f)) {
            val shoulder = Vec3(side * proportions.shoulderJointX, shoulderY - 1.5f, 0f)
            val elbow = Vec3(
                side * (proportions.shoulderJointX + proportions.upperArmLength * 0.16f),
                shoulderY - 1.5f - proportions.upperArmLength,
                0f,
            )
            val wrist = Vec3(
                side * (proportions.shoulderJointX +
                    proportions.upperArmLength * 0.16f +
                    proportions.forearmLength * 0.05f),
                elbow.y - proportions.forearmLength,
                0f,
            )
            val handCenter = Vec3(wrist.x, wrist.y - proportions.handLength * 0.5f, 0f)

            builder.addMesh(
                Meshes.ellipsoid(
                    proportions.upperArmRadius * 1.22f,
                    proportions.upperArmRadius * 1.22f,
                    proportions.upperArmRadius * 1.22f,
                    rings = 8,
                    segments = LIMB_SEGMENTS,
                ),
                shoulder,
            )
            builder.addMesh(
                Meshes.limb(shoulder, elbow, proportions.upperArmRadius, proportions.forearmRadius, LIMB_SEGMENTS),
            )
            builder.addMesh(
                Meshes.limb(elbow, wrist, proportions.forearmRadius, proportions.forearmRadius * 0.72f, LIMB_SEGMENTS),
            )
            builder.addMesh(
                Meshes.ellipsoid(
                    proportions.handHalfWidth,
                    proportions.handLength * 0.5f,
                    proportions.handHalfWidth * 0.55f,
                    rings = 10,
                    segments = 16,
                ),
                handCenter,
            )
        }

        // Legs and feet.
        for (side in listOf(-1f, 1f)) {
            val hip = Vec3(side * proportions.hipJointX, hipY - 2f, 0f)
            val knee = Vec3(side * proportions.hipJointX * 1.05f, proportions.kneeY, 0f)
            val ankle = Vec3(side * proportions.hipJointX * 1.05f, proportions.ankleY, 0f)

            builder.addMesh(
                Meshes.limb(hip, knee, proportions.thighRadius, proportions.thighRadius * 0.78f, LIMB_SEGMENTS),
            )
            builder.addMesh(
                Meshes.limb(
                    knee,
                    ankle,
                    proportions.calfRadius * 1.08f,
                    proportions.calfRadius * 0.62f,
                    LIMB_SEGMENTS,
                ),
            )
            builder.addMesh(
                Meshes.ellipsoid(
                    proportions.calfRadius * 0.68f,
                    proportions.footHeight / 2f,
                    proportions.footLength / 2f,
                    rings = 10,
                    segments = 16,
                ),
                Vec3(
                    ankle.x,
                    proportions.footHeight / 2f,
                    proportions.footLength * 0.22f,
                ),
            )
        }

        return builder.build()
    }

    /** Convenience overload used by the view model. */
    fun build(proportionsOf: com.costemp.core.measurement.BodyProfile): Mesh =
        build(MannequinProportions.from(proportionsOf))

    private fun lerp(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction
}
