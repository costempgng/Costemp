package com.costemp.core.measurement

/**
 * Units used across Costemp. All engine storage is metric (centimetres); imperial
 * values are only a presentation concern and are converted at the edges.
 */
enum class LengthUnit(val symbol: String, val centimetresPerUnit: Double) {
    CENTIMETRES("cm", 1.0),
    INCHES("in", 2.54);

    /** Converts a stored centimetre value into this unit. */
    fun fromCentimetres(valueCm: Double): Double = valueCm / centimetresPerUnit

    /** Converts a value expressed in this unit back to centimetres. */
    fun toCentimetres(value: Double): Double = value * centimetresPerUnit

    /** Formats a centimetre value with one decimal, in this unit. */
    fun format(valueCm: Double): String = formatNumber(fromCentimetres(valueCm))

    companion object {
        fun formatNumber(value: Double): String {
            val rounded = Math.round(value * 10.0) / 10.0
            return if (rounded == Math.floor(rounded) && !rounded.isInfinite()) {
                rounded.toLong().toString()
            } else {
                String.format(java.util.Locale.US, "%.1f", rounded)
            }
        }
    }
}
