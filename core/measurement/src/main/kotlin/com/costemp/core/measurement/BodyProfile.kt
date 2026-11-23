package com.costemp.core.measurement

import kotlinx.serialization.Serializable

/**
 * The measurement set Costemp fits a mannequin and, later, armour shells to.
 *
 * Every value is stored in centimetres so the geometry engine never has to guess a
 * unit. Ranges are validated by [validate]; the UI clamps edits to [MeasurementKind.range].
 */
@Serializable
data class BodyProfile(
    /** Standing height, barefoot, top of head to floor. */
    val heightCm: Double = 175.0,
    /** Circumference around the widest part of the head. */
    val headCircumferenceCm: Double = 57.0,
    /** Circumference around the base of the neck. */
    val neckCircumferenceCm: Double = 38.0,
    /** Biacromial width: shoulder tip to shoulder tip. */
    val shoulderWidthCm: Double = 44.0,
    /** Circumference around the fullest part of the chest. */
    val chestCircumferenceCm: Double = 96.0,
    /** Circumference around the narrowest part of the waist. */
    val waistCircumferenceCm: Double = 80.0,
    /** Circumference around the widest part of the hips. */
    val hipCircumferenceCm: Double = 99.0,
    /** Shoulder to wrist length, arm hanging straight. */
    val armLengthCm: Double = 60.0,
    /** Crotch to floor (inside leg). */
    val inseamCm: Double = 80.0,
) {
    /** Returns the value currently held for [kind]. */
    fun valueOf(kind: MeasurementKind): Double = kind.read(this)

    /** Returns a copy with [kind] replaced by [value] (no clamping — callers validate). */
    fun with(kind: MeasurementKind, value: Double): BodyProfile = kind.write(this, value)

    /** Clamps [value] into the physically plausible range for [kind]. */
    fun clamped(kind: MeasurementKind, value: Double): Double = kind.clamp(value)

    /** Every measurement outside its plausible range, with a human readable reason. */
    fun validate(): List<ValidationIssue> = MeasurementKind.entries.mapNotNull { kind ->
        val value = kind.read(this)
        if (value.isNaN() || value < kind.range.start || value > kind.range.endInclusive) {
            ValidationIssue(
                kind = kind,
                message = "${kind.label} must be between " +
                    "${LengthUnit.formatNumber(kind.range.start)} and " +
                    "${LengthUnit.formatNumber(kind.range.endInclusive)} cm.",
            )
        } else {
            null
        }
    }

    fun isValid(): Boolean = validate().isEmpty()

    companion object {
        /** A neutral adult reference body used for a brand new project. */
        val DEFAULT = BodyProfile()
    }
}

data class ValidationIssue(val kind: MeasurementKind, val message: String)

/**
 * The editable measurement fields. Keeping the metadata in the measurement module
 * means tests cover the ranges and conversions the UI shows.
 */
enum class MeasurementKind(
    val label: String,
    val hint: String,
    val range: ClosedFloatingPointRange<Double>,
) {
    HEIGHT("Height", "Top of head to floor", 60.0..250.0) {
        override fun read(profile: BodyProfile) = profile.heightCm
        override fun write(profile: BodyProfile, value: Double) = profile.copy(heightCm = value)
    },
    HEAD("Head circumference", "Around the widest part of the head", 30.0..80.0) {
        override fun read(profile: BodyProfile) = profile.headCircumferenceCm
        override fun write(profile: BodyProfile, value: Double) = profile.copy(headCircumferenceCm = value)
    },
    NECK("Neck circumference", "Around the base of the neck", 20.0..80.0) {
        override fun read(profile: BodyProfile) = profile.neckCircumferenceCm
        override fun write(profile: BodyProfile, value: Double) = profile.copy(neckCircumferenceCm = value)
    },
    SHOULDERS("Shoulder width", "Shoulder tip to shoulder tip", 20.0..80.0) {
        override fun read(profile: BodyProfile) = profile.shoulderWidthCm
        override fun write(profile: BodyProfile, value: Double) = profile.copy(shoulderWidthCm = value)
    },
    CHEST("Chest circumference", "Around the fullest part of the chest", 40.0..200.0) {
        override fun read(profile: BodyProfile) = profile.chestCircumferenceCm
        override fun write(profile: BodyProfile, value: Double) = profile.copy(chestCircumferenceCm = value)
    },
    WAIST("Waist circumference", "Around the narrowest part of the waist", 35.0..200.0) {
        override fun read(profile: BodyProfile) = profile.waistCircumferenceCm
        override fun write(profile: BodyProfile, value: Double) = profile.copy(waistCircumferenceCm = value)
    },
    HIPS("Hip circumference", "Around the widest part of the hips", 40.0..200.0) {
        override fun read(profile: BodyProfile) = profile.hipCircumferenceCm
        override fun write(profile: BodyProfile, value: Double) = profile.copy(hipCircumferenceCm = value)
    },
    ARM("Arm length", "Shoulder to wrist, arm hanging straight", 20.0..120.0) {
        override fun read(profile: BodyProfile) = profile.armLengthCm
        override fun write(profile: BodyProfile, value: Double) = profile.copy(armLengthCm = value)
    },
    INSEAM("Inseam", "Crotch to floor", 20.0..150.0) {
        override fun read(profile: BodyProfile) = profile.inseamCm
        override fun write(profile: BodyProfile, value: Double) = profile.copy(inseamCm = value)
    };

    abstract fun read(profile: BodyProfile): Double
    abstract fun write(profile: BodyProfile, value: Double): BodyProfile

    fun clamp(value: Double): Double = value.coerceIn(range.start, range.endInclusive)
}
