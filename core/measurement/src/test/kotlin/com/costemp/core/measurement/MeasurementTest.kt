package com.costemp.core.measurement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementTest {

    @Test
    fun `unit conversion round trips`() {
        val inches = LengthUnit.INCHES.fromCentimetres(175.0)
        assertEquals(68.897, inches, 0.001)
        assertEquals(175.0, LengthUnit.INCHES.toCentimetres(inches), 1e-9)
        assertEquals(175.0, LengthUnit.CENTIMETRES.toCentimetres(175.0), 0.0)
    }

    @Test
    fun `formatting hides trailing zeros`() {
        assertEquals("175", LengthUnit.CENTIMETRES.format(175.0))
        assertEquals("96.5", LengthUnit.CENTIMETRES.format(96.54))
        assertEquals("68.9", LengthUnit.INCHES.format(175.0))
    }

    @Test
    fun `ellipse circumference matches ramanujan for a circle`() {
        val circle = Ellipse(10.0, 10.0)
        assertEquals(2.0 * Math.PI * 10.0, circle.circumference, 1e-9)
    }

    @Test
    fun `ellipse solver recovers target circumference and depth ratio`() {
        val ellipse = Ellipse.forCircumference(96.0, 0.72)
        assertEquals(96.0, ellipse.circumference, 1e-6)
        assertEquals(0.72, ellipse.semiDepth / ellipse.semiWidth, 1e-9)
    }

    @Test
    fun `ellipse solver scales with circumference`() {
        val small = Ellipse.forCircumference(70.0, 0.8)
        val large = Ellipse.forCircumference(140.0, 0.8)
        assertTrue(large.semiWidth > small.semiWidth)
        assertEquals(2.0, large.semiWidth / small.semiWidth, 1e-6)
    }

    @Test
    fun `default profile is valid`() {
        assertTrue(BodyProfile.DEFAULT.validate().isEmpty())
    }

    @Test
    fun `out of range measurements are reported with a message`() {
        val broken = BodyProfile(heightCm = 20.0, waistCircumferenceCm = 500.0)
        val issues = broken.validate()
        assertEquals(2, issues.size)
        assertEquals(MeasurementKind.HEIGHT, issues[0].kind)
        assertTrue(issues[0].message.contains("Height"))
        assertEquals(MeasurementKind.WAIST, issues[1].kind)
        assertFalse(broken.isValid())
    }

    @Test
    fun `kind read and write round trip`() {
        var profile = BodyProfile()
        for (kind in MeasurementKind.entries) {
            profile = profile.with(kind, 42.0)
            assertEquals(42.0, profile.valueOf(kind), 1e-9)
        }
    }

    @Test
    fun `clamp keeps values inside the documented range`() {
        assertEquals(60.0, MeasurementKind.HEIGHT.clamp(10.0), 0.0)
        assertEquals(250.0, MeasurementKind.HEIGHT.clamp(400.0), 0.0)
        assertEquals(175.0, MeasurementKind.HEIGHT.clamp(175.0), 0.0)
    }

    @Test
    fun `proportions follow the seven and a half head canon`() {
        val profile = BodyProfile(heightCm = 180.0)
        val p = MannequinProportions.from(profile)
        assertEquals(24.0f, p.headLength, 1e-4f)
        assertEquals(180.0f, p.headCenterY + p.headLength / 2f, 1e-4f)
        assertTrue(p.shoulderY > p.chestY)
        assertTrue(p.chestY > p.waistY)
        assertTrue(p.waistY > p.hipY)
    }

    @Test
    fun `proportions use chest circumference for torso width`() {
        val slim = MannequinProportions.from(BodyProfile(chestCircumferenceCm = 80.0))
        val broad = MannequinProportions.from(BodyProfile(chestCircumferenceCm = 120.0))
        assertTrue(broad.chestHalfWidth > slim.chestHalfWidth)
        assertTrue(broad.upperArmRadius > slim.upperArmRadius)
    }
}
