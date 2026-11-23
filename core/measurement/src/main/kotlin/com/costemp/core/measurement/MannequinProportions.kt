package com.costemp.core.measurement

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * An ellipse cross-section, used to turn a tape-measure circumference into the
 * width/depth radii the mannequin mesh needs.
 *
 * ### Algorithm
 * The perimeter of an ellipse with semi-axes `a >= b > 0` has no closed form, so
 * Costemp uses Ramanujan's second approximation:
 *
 *     C ~= pi * ( 3(a + b) - sqrt( (3a + b)(a + 3b) ) )
 *
 * It is accurate to better than 1e-5 relative for moderate eccentricity, which is far
 * beyond tape-measure precision.
 *
 * To go the other way (circumference -> radii) we fix the depth ratio `b = ratio * a`
 * and solve `C(a) = target`. `C` is strictly increasing in `a`, so a bisection on
 * `[0, target]` converges deterministically without needing a derivative. 60 steps
 * shrink the bracket by 2^60, i.e. to floating point precision.
 */
data class Ellipse(val semiWidth: Double, val semiDepth: Double) {
    val circumference: Double
        get() = PI * (3.0 * (semiWidth + semiDepth) - sqrt((3.0 * semiWidth + semiDepth) * (semiWidth + 3.0 * semiDepth)))

    companion object {
        /**
         * Finds the ellipse whose circumference equals [targetCircumference] while its
         * depth stays [depthRatio] times its width.
         */
        fun forCircumference(targetCircumference: Double, depthRatio: Double): Ellipse {
            require(targetCircumference > 0.0) { "Circumference must be positive" }
            require(depthRatio > 0.0) { "Depth ratio must be positive" }

            fun circumferenceFor(width: Double): Double =
                Ellipse(width, width * depthRatio).circumference

            var low = 0.0
            var high = targetCircumference
            repeat(60) {
                val mid = (low + high) / 2.0
                if (circumferenceFor(mid) < targetCircumference) low = mid else high = mid
            }
            val width = (low + high) / 2.0
            return Ellipse(width, width * depthRatio)
        }
    }
}

/**
 * Anthropometric proportions derived from a [BodyProfile].
 *
 * All positions are centimetres in a Y-up world with the floor at `y = 0`, the body
 * centred on `x = z = 0` and the character facing `+Z`. Vertical landmark heights use
 * the classic 7.5-head figure and standard fractions of stature:
 *
 *  - head length          = height / 7.5
 *  - shoulder (acromion)  = height - 1.5 * head length
 *  - chest (nipple line)  = 0.72 * height
 *  - waist (natural)      = 0.615 * height
 *  - hip (greater troch.) = 0.52 * height
 *  - knee                 = 0.48 * inseam
 */
data class MannequinProportions(
    val height: Float,
    val headLength: Float,
    val headHalfWidth: Float,
    val headHalfDepth: Float,
    val headCenterY: Float,
    val neckRadius: Float,
    val neckTopY: Float,
    val shoulderY: Float,
    val shoulderHalfWidth: Float,
    val chestY: Float,
    val chestHalfWidth: Float,
    val chestHalfDepth: Float,
    val waistY: Float,
    val waistHalfWidth: Float,
    val waistHalfDepth: Float,
    val hipY: Float,
    val hipHalfWidth: Float,
    val hipHalfDepth: Float,
    val shoulderJointX: Float,
    val upperArmLength: Float,
    val forearmLength: Float,
    val handLength: Float,
    val upperArmRadius: Float,
    val forearmRadius: Float,
    val handHalfWidth: Float,
    val hipJointX: Float,
    val kneeY: Float,
    val ankleY: Float,
    val thighRadius: Float,
    val calfRadius: Float,
    val footLength: Float,
    val footHeight: Float,
) {
    companion object {
        private const val HEAD_TO_HEIGHT = 7.5
        private const val CHEST_DEPTH_RATIO = 0.72
        private const val WAIST_DEPTH_RATIO = 0.76
        private const val HIP_DEPTH_RATIO = 0.80

        fun from(profile: BodyProfile): MannequinProportions {
            val height = profile.heightCm
            val headLength = height / HEAD_TO_HEIGHT
            val headWidth = headLength * 0.70
            val headDepth = headWidth * 1.28
            val neckRadius = profile.neckCircumferenceCm / (2.0 * PI)

            val chest = Ellipse.forCircumference(profile.chestCircumferenceCm, CHEST_DEPTH_RATIO)
            val waist = Ellipse.forCircumference(profile.waistCircumferenceCm, WAIST_DEPTH_RATIO)
            val hip = Ellipse.forCircumference(profile.hipCircumferenceCm, HIP_DEPTH_RATIO)

            val shoulderHalfWidth = profile.shoulderWidthCm / 2.0
            val upperArmRadius = (profile.chestCircumferenceCm * 0.30) / (2.0 * PI)
            val forearmRadius = upperArmRadius * 0.76

            return MannequinProportions(
                height = height.toFloat(),
                headLength = headLength.toFloat(),
                headHalfWidth = (headWidth / 2.0).toFloat(),
                headHalfDepth = (headDepth / 2.0).toFloat(),
                headCenterY = (height - headLength / 2.0).toFloat(),
                neckRadius = neckRadius.toFloat(),
                neckTopY = (height - headLength * 1.02).toFloat(),
                shoulderY = (height - headLength * 1.5).toFloat(),
                shoulderHalfWidth = shoulderHalfWidth.toFloat(),
                chestY = (height * 0.72).toFloat(),
                chestHalfWidth = chest.semiWidth.toFloat(),
                chestHalfDepth = chest.semiDepth.toFloat(),
                waistY = (height * 0.615).toFloat(),
                waistHalfWidth = waist.semiWidth.toFloat(),
                waistHalfDepth = waist.semiDepth.toFloat(),
                hipY = (height * 0.52).toFloat(),
                hipHalfWidth = hip.semiWidth.toFloat(),
                hipHalfDepth = hip.semiDepth.toFloat(),
                shoulderJointX = (shoulderHalfWidth * 0.92).toFloat(),
                upperArmLength = (profile.armLengthCm * 0.45).toFloat(),
                forearmLength = (profile.armLengthCm * 0.40).toFloat(),
                handLength = (profile.armLengthCm * 0.15).toFloat(),
                upperArmRadius = upperArmRadius.toFloat(),
                forearmRadius = forearmRadius.toFloat(),
                handHalfWidth = (upperArmRadius * 0.95).toFloat(),
                hipJointX = (hip.semiWidth * 0.48).toFloat(),
                kneeY = (profile.inseamCm * 0.48).toFloat(),
                ankleY = (height * 0.045).toFloat(),
                thighRadius = (hip.semiWidth * 0.42).toFloat(),
                calfRadius = (hip.semiWidth * 0.28).toFloat(),
                footLength = (height * 0.15).toFloat(),
                footHeight = (height * 0.032).toFloat(),
            )
        }

        /** Convenience for tests and callers that only need a landmark height. */
        fun landmarkHeight(profile: BodyProfile, fractionOfHeight: Double): Float =
            (profile.heightCm * fractionOfHeight).toFloat()

        fun assertEqualsWithin(a: Double, b: Double, tolerance: Double): Boolean = abs(a - b) <= tolerance
    }
}
