package com.isaakhanimann.journal

import com.isaakhanimann.journal.data.substances.classes.roa.ConcentrationModel
import com.isaakhanimann.journal.data.substances.classes.roa.ExtendedReleaseProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestExtendedReleaseProfile {

    @Test
    fun releaseFractionFollowsTwoLinearPhases() {
        val profile = ExtendedReleaseProfile(
            initialMinutes = 60.0,
            initialPercent = 50.0,
            subsequentMinutes = 60.0
        )

        assertEquals(0.0, profile.releasedFractionAt(0.0), 1e-12)
        assertEquals(0.25, profile.releasedFractionAt(30.0), 1e-12)
        assertEquals(0.50, profile.releasedFractionAt(60.0), 1e-12)
        assertEquals(0.75, profile.releasedFractionAt(90.0), 1e-12)
        assertEquals(1.0, profile.releasedFractionAt(120.0), 1e-12)
        assertEquals(0.0, profile.remainingFractionAt(120.0), 1e-12)
    }

    @Test
    fun validatesRequiredParameters() {
        assertTrue(
            ExtendedReleaseProfile(60.0, 50.0, 600.0).isValid
        )
        assertTrue(
            ExtendedReleaseProfile(60.0, 100.0, 0.0).isValid
        )
        assertFalse(
            ExtendedReleaseProfile(0.0, 50.0, 600.0).isValid
        )
        assertFalse(
            ExtendedReleaseProfile(60.0, 50.0, 0.0).isValid
        )
        assertFalse(
            ExtendedReleaseProfile(60.0, 101.0, 600.0).isValid
        )
    }

    @Test
    fun extendedReleaseConcentrationHasDelayedInput() {
        val model = ConcentrationModel(
            status = "reference",
            analyte = "test",
            referenceDose = 1.0,
            referenceDoseUnits = "mg",
            cmax = 1.0,
            cmaxMin = null,
            cmaxMax = null,
            cmaxUnits = "mg/L",
            cmaxMgPerL = 1.0,
            cmaxMgPerLMin = null,
            cmaxMgPerLMax = null,
            note = null,
            tmax = 1.0,
            tmaxUnits = "hours",
            halfLife = 2.0,
            halfLifeUnits = "hours"
        )
        val profile = ExtendedReleaseProfile(
            initialMinutes = 60.0,
            initialPercent = 50.0,
            subsequentMinutes = 60.0
        )

        val immediateAtHalfHour = model.estimateMgPerLAt(0.5, 1.0, "mg")!!
        val extendedAtHalfHour = model.estimateMgPerLAt(
            elapsedHours = 0.5,
            ingestionDose = 1.0,
            ingestionUnits = "mg",
            extendedReleaseProfile = profile
        )!!
        assertTrue(extendedAtHalfHour.endInclusive < immediateAtHalfHour.endInclusive)
        assertTrue(extendedAtHalfHour.endInclusive > 0.0)

        val extendedLate = model.estimateMgPerLAt(
            elapsedHours = 5.0,
            ingestionDose = 1.0,
            ingestionUnits = "mg",
            extendedReleaseProfile = profile
        )!!
        val immediateLate = model.estimateMgPerLAt(5.0, 1.0, "mg")!!
        assertTrue(extendedLate.endInclusive > immediateLate.endInclusive)
    }
}
