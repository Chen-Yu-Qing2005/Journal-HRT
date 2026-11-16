package com.isaakhanimann.journal

import com.isaakhanimann.journal.data.room.experiences.entities.AdaptiveColor
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.classes.roa.ConcentrationEstimator
import com.isaakhanimann.journal.data.substances.classes.roa.ConcentrationInputModel
import com.isaakhanimann.journal.data.substances.classes.roa.ConcentrationModel
import com.isaakhanimann.journal.data.substances.classes.roa.ConcentrationUnit
import com.isaakhanimann.journal.data.substances.classes.roa.DurationRange
import com.isaakhanimann.journal.data.substances.classes.roa.DurationUnits
import com.isaakhanimann.journal.data.substances.classes.roa.RoaDuration
import com.isaakhanimann.journal.data.substances.parse.SubstanceParser
import com.isaakhanimann.journal.ui.tabs.journal.experience.components.DataForOneEffectLine
import com.isaakhanimann.journal.ui.tabs.journal.experience.timeline.AllTimelinesModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TestConcentrationModel {

    @Test
    fun scalesReferenceCmaxByDose() {
        val model = ConcentrationModel(
            status = "reference",
            analyte = "estradiol",
            referenceDose = 1.0,
            referenceDoseUnits = "mg estradiol",
            cmax = 144.0,
            cmaxMin = null,
            cmaxMax = null,
            cmaxUnits = "pg/mL",
            cmaxMgPerL = 0.000144,
            cmaxMgPerLMin = null,
            cmaxMgPerLMax = null,
            note = null
        )

        assertEquals(
            "≈ 0.00029 mg/L",
            model.estimatePeakLabel(ingestionDose = 2.0, ingestionUnits = "mg")
        )
    }

    @Test
    fun keepsReferenceRange() {
        val model = ConcentrationModel(
            status = "reference_range",
            analyte = "medroxyprogesterone acetate",
            referenceDose = 150.0,
            referenceDoseUnits = "mg",
            cmax = null,
            cmaxMin = 1.0,
            cmaxMax = 7.0,
            cmaxUnits = "ng/mL",
            cmaxMgPerL = null,
            cmaxMgPerLMin = 0.001,
            cmaxMgPerLMax = 0.007,
            note = null
        )

        assertEquals(
            "≈ 0.001–0.007 mg/L",
            model.estimatePeakLabel(ingestionDose = 150.0, ingestionUnits = "mg")
        )
    }

    @Test
    fun rejectsMismatchedUnitsAndUnreportedModels() {
        val model = ConcentrationModel(
            status = "reference",
            analyte = "test",
            referenceDose = 1.0,
            referenceDoseUnits = "mg",
            cmax = 1.0,
            cmaxMin = null,
            cmaxMax = null,
            cmaxUnits = "ng/mL",
            cmaxMgPerL = 0.001,
            cmaxMgPerLMin = null,
            cmaxMgPerLMax = null,
            note = null
        )

        assertNull(model.estimatePeakLabel(1.0, "µg"))
        assertNull(model.copy(status = "not_reported").estimatePeakLabel(1.0, "mg"))
    }

    @Test
    fun estimatesConcentrationAtDraggedTime() {
        val model = ConcentrationModel(
            status = "reference",
            analyte = "estradiol",
            referenceDose = 1.0,
            referenceDoseUnits = "mg",
            cmax = null,
            cmaxMin = null,
            cmaxMax = null,
            cmaxUnits = null,
            cmaxMgPerL = 0.000288,
            cmaxMgPerLMin = null,
            cmaxMgPerLMax = null,
            note = null,
            tmax = 2.0,
            tmaxUnits = "hours",
            halfLife = 8.0,
            halfLifeUnits = "hours"
        )

        assertEquals(0.0, model.estimateMgPerLAt(0.0, 1.0, "mg")!!.start, 1e-12)
        assertEquals(
            0.000288,
            model.estimateMgPerLAt(2.0, 1.0, "mg")!!.start,
            1e-12
        )
        assertEquals(
            model.estimateMgPerLAt(10.0, 1.0, "mg")!!.start,
            model.estimateMgPerLAt(18.0, 1.0, "mg")!!.start * 2.0,
            0.000001
        )
    }

    @Test
    fun parserReadsTmaxAndHalfLife() {
        val substance = SubstanceParser().parseSubstance(
            """
            {
              "name": "Estradiol",
              "concentrationModels": {
                "oral": {
                  "status": "reference",
                  "analyte": "estradiol",
                  "referenceDose": 3,
                  "referenceDoseUnits": "mg estradiol valerate",
                  "cmaxMgPerL": 0.0000733,
                  "cmaxUnits": "pg/mL",
                  "tmaxMedian": 6,
                  "tmaxMin": 1.5,
                  "tmaxMax": 12,
                  "tmaxUnits": "hours",
                  "halfLife": 13,
                  "halfLifeUnits": "hours"
                }
              }
            }
            """.trimIndent()
        )

        val model = substance?.getConcentrationModel(AdministrationRoute.ORAL)
        assertNotNull(model)
        assertEquals(6.0, model!!.tmaxMedian!!, 0.0)
        assertEquals(1.5, model.tmaxMin!!, 0.0)
        assertEquals(12.0, model.tmaxMax!!, 0.0)
        assertEquals(13.0, model.halfLife!!, 0.0)
    }

    @Test
    fun switchesConcentrationUnitsWithoutChangingTheEstimate() {
        val model = ConcentrationModel(
            status = "reference",
            analyte = "estradiol",
            referenceDose = 1.0,
            referenceDoseUnits = "mg",
            cmax = null,
            cmaxMin = null,
            cmaxMax = null,
            cmaxUnits = null,
            cmaxMgPerL = 0.000288,
            cmaxMgPerLMin = null,
            cmaxMgPerLMax = null,
            note = null
        )

        assertEquals(
            "≈ 288 pg/mL",
            model.estimatePeakLabel(1.0, "mg", ConcentrationUnit.PG_PER_ML)
        )
        assertEquals(
            "≈ 0.288 ng/mL",
            model.estimatePeakLabel(1.0, "mg", ConcentrationUnit.NG_PER_ML)
        )
        assertNull(model.estimatePeakLabel(1.0, "mg", ConcentrationUnit.MG_PER_ML))
        assertEquals(
            "≈ 0.00029 mg/L",
            model.estimatePeakLabel(1.0, "mg", ConcentrationUnit.MG_PER_L)
        )
    }

    @Test
    fun calculatesWhenReminderThresholdIsCrossed() {
        val model = ConcentrationModel(
            status = "reference",
            analyte = "test",
            referenceDose = 1.0,
            referenceDoseUnits = "mg",
            cmax = null,
            cmaxMin = null,
            cmaxMax = null,
            cmaxUnits = null,
            cmaxMgPerL = 0.001,
            cmaxMgPerLMin = null,
            cmaxMgPerLMax = null,
            note = null,
            tmax = 2.0,
            tmaxUnits = "hours",
            halfLife = 8.0,
            halfLifeUnits = "hours"
        )

        val crossingHours = model.estimatedHoursUntilBelowPeakFraction(0.10)!!
        val fractionAtCrossing = model.estimateMgPerLAt(
            crossingHours,
            1.0,
            "mg"
        )!!.start / 0.001
        assertTrue(fractionAtCrossing in 0.099999..0.100001)
    }

    @Test
    fun veryLowConcentrationUsesZeroInsteadOfScientificNotation() {
        val model = ConcentrationModel(
            status = "reference",
            analyte = "test",
            referenceDose = 1.0,
            referenceDoseUnits = "mg",
            cmax = null,
            cmaxMin = null,
            cmaxMax = null,
            cmaxUnits = null,
            cmaxMgPerL = 1.0,
            cmaxMgPerLMin = null,
            cmaxMgPerLMax = null,
            note = null,
            tmax = 1.0,
            tmaxUnits = "minutes",
            halfLife = 1.0,
            halfLifeUnits = "minutes"
        )

        val label = model.estimateConcentrationLabelAt(
            elapsedHours = 1.0,
            ingestionDose = 1.0,
            ingestionUnits = "mg",
            concentrationUnit = ConcentrationUnit.MG_PER_ML
        )
        assertNull(label)
    }

    @Test
    fun combinesDosesOfTheSameSubstanceIntoOneConcentrationSeries() {
        val model = ConcentrationModel(
            status = "reference",
            analyte = "test",
            referenceDose = 1.0,
            referenceDoseUnits = "mg",
            cmax = null,
            cmaxMin = null,
            cmaxMax = null,
            cmaxUnits = null,
            cmaxMgPerL = 1.0,
            cmaxMgPerLMin = null,
            cmaxMgPerLMax = null,
            note = null,
            tmax = 1.0,
            tmaxUnits = "hours",
            halfLife = 2.0,
            halfLifeUnits = "hours"
        )
        val start = java.time.Instant.parse("2026-11-14T12:00:00Z")
        fun line(time: java.time.Instant) = DataForOneEffectLine(
            substanceName = "Test",
            route = AdministrationRoute.ORAL,
            roaDuration = null,
            height = 1f,
            horizontalWeight = 1f,
            color = AdaptiveColor.BLUE,
            startTime = time,
            endTime = time.plusSeconds(8 * 3600),
            concentrationModel = model,
            concentrationDose = 1.0,
            concentrationDoseUnits = "mg"
        )
        val timeline = AllTimelinesModel(
            dataForLines = listOf(
                line(start),
                line(start.plusSeconds(3600))
            ),
            dataForRatings = emptyList(),
            timedNotes = emptyList()
        )

        assertEquals(1, timeline.concentrationSeries.size)
        val range = timeline.concentrationSeries.single()
            .estimateMgPerLRangeAt(start.plusSeconds(2 * 3600))!!
        assertEquals(1.818995681, range.endInclusive, 1e-8)
        assertEquals(
            "≈ 1.819 mg/L",
            timeline.concentrationSeries.single().labelAt(
                start.plusSeconds(2 * 3600),
                ConcentrationUnit.MG_PER_L
            )
        )
    }

    @Test
    fun fallsBackToCmaxWhenCurrentValueIsBelowDisplayPrecision() {
        val model = ConcentrationModel(
            status = "reference",
            analyte = "test",
            referenceDose = 1.0,
            referenceDoseUnits = "mg",
            cmax = null,
            cmaxMin = null,
            cmaxMax = null,
            cmaxUnits = null,
            cmaxMgPerL = 0.00001,
            cmaxMgPerLMin = null,
            cmaxMgPerLMax = null,
            note = null,
            tmax = 1.0,
            tmaxUnits = "hours",
            halfLife = 1.0,
            halfLifeUnits = "hours"
        )
        val start = java.time.Instant.parse("2026-11-14T12:00:00Z")
        val timeline = AllTimelinesModel(
            dataForLines = listOf(
                DataForOneEffectLine(
                    substanceName = "Test",
                    route = AdministrationRoute.ORAL,
                    roaDuration = null,
                    height = 1f,
                    horizontalWeight = 1f,
                    color = AdaptiveColor.BLUE,
                    startTime = start,
                    endTime = start.plusSeconds(8 * 3600),
                    concentrationModel = model,
                    concentrationDose = 1.0,
                    concentrationDoseUnits = "mg"
                )
            ),
            dataForRatings = emptyList(),
            timedNotes = emptyList()
        )

        assertEquals(
            "Cmax ≈ 0.00001 mg/L",
            timeline.concentrationSeries.single().labelAt(
                start.plusSeconds(20 * 3600),
                ConcentrationUnit.MG_PER_L
            )
        )
    }

    @Test
    fun createsRelativeExposureForSubstanceWithoutReferenceModel() {
        val duration = RoaDuration(
            onset = DurationRange(0.5f, 1f, DurationUnits.HOURS),
            comeup = DurationRange(1f, 3f, DurationUnits.HOURS),
            peak = DurationRange(2f, 4f, DurationUnits.HOURS),
            offset = null,
            total = DurationRange(8f, 12f, DurationUnits.HOURS),
            afterglow = null
        )

        val model = ConcentrationEstimator.resolve(
            recordedModel = null,
            roaDuration = duration,
            ingestionDose = 25.0,
            ingestionUnits = "mg",
            fallbackEliminationEndHours = null
        )

        assertNotNull(model)
        assertTrue(model!!.isRelativeEstimate)
        assertEquals(3.0, model.tmax!!, 0.0)
        assertEquals(
            100.0,
            model.estimatePeakMgPerLRange(25.0, "mg")!!.endInclusive,
            0.0
        )
    }

    @Test
    fun convertsIncompleteReferenceModelToRelativeExposure() {
        val incompleteModel = ConcentrationModel(
            status = "not_reported",
            analyte = "test",
            referenceDose = null,
            referenceDoseUnits = null,
            cmax = null,
            cmaxMin = null,
            cmaxMax = null,
            cmaxUnits = null,
            cmaxMgPerL = null,
            cmaxMgPerLMin = null,
            cmaxMgPerLMax = null,
            note = null,
            tmax = 2.0,
            tmaxUnits = "hours",
            halfLife = 6.0,
            halfLifeUnits = "hours"
        )

        val model = ConcentrationEstimator.resolve(
            recordedModel = incompleteModel,
            roaDuration = null,
            ingestionDose = 10.0,
            ingestionUnits = "mg",
            fallbackEliminationEndHours = null
        )

        assertNotNull(model)
        assertTrue(model!!.isRelativeEstimate)
        assertEquals(2.0, model.tmax!!, 0.0)
        assertEquals(6.0, model.halfLife!!, 0.0)
    }

    @Test
    fun relativeSeriesShowsPercentWithoutClaimingMassUnits() {
        val model = ConcentrationModel.relativeEstimate(
            referenceDose = 1.0,
            referenceDoseUnits = "mg",
            tmaxHours = 1.0,
            halfLifeHours = 2.0
        )
        val start = java.time.Instant.parse("2026-11-14T12:00:00Z")
        val timeline = AllTimelinesModel(
            dataForLines = listOf(
                DataForOneEffectLine(
                    substanceName = "Test",
                    route = AdministrationRoute.ORAL,
                    roaDuration = null,
                    height = 1f,
                    horizontalWeight = 1f,
                    color = AdaptiveColor.BLUE,
                    startTime = start,
                    endTime = start.plusSeconds(8 * 3600),
                    concentrationModel = model,
                    concentrationDose = 1.0,
                    concentrationDoseUnits = "mg"
                )
            ),
            dataForRatings = emptyList(),
            timedNotes = emptyList()
        )

        val series = timeline.concentrationSeries.single()
        assertTrue(series.usesRelativeScale)
        assertEquals(
            "≈ 100%",
            series.labelAt(
                start.plusSeconds(3600),
                ConcentrationUnit.MG_PER_L
            )
        )
    }

    @Test
    fun batemanCurveRisesSmoothlyAndTurnsAtTmax() {
        val model = referenceModel(tmaxHours = 2.0, halfLifeHours = 8.0)
        val atStart = model.estimateMgPerLAt(0.0, 1.0, "mg")!!.start
        val veryEarly = model.estimateMgPerLAt(0.1, 1.0, "mg")!!.start
        val early = model.estimateMgPerLAt(0.5, 1.0, "mg")!!.start
        val rising = model.estimateMgPerLAt(1.5, 1.0, "mg")!!.start
        val peak = model.estimateMgPerLAt(2.0, 1.0, "mg")!!.start
        val falling = model.estimateMgPerLAt(4.0, 1.0, "mg")!!.start

        assertEquals(0.0, atStart, 1e-12)
        assertTrue(veryEarly > atStart)
        assertTrue(veryEarly < peak * 0.5)
        assertTrue(early < peak)
        assertTrue(rising > early)
        assertEquals(0.001, peak, 1e-12)
        assertTrue(falling < peak)
    }

    @Test
    fun batemanTerminalSlopeMatchesHalfLifeAfterAbsorption() {
        val model = referenceModel(tmaxHours = 2.0, halfLifeHours = 8.0)
        val atHundredHours = model.estimateMgPerLAt(100.0, 1.0, "mg")!!.start
        val eightHoursLater = model.estimateMgPerLAt(108.0, 1.0, "mg")!!.start

        assertEquals(0.5, eightHoursLater / atHundredHours, 1e-8)
    }

    @Test
    fun zeroOrderDepotDelaysExposureWhenReleaseDataExists() {
        val immediate = referenceModel(tmaxHours = 1.0, halfLifeHours = 2.0)
        val depot = immediate.copy(
            inputModel = ConcentrationInputModel.ZERO_ORDER.name.lowercase(),
            releaseDuration = 4.0,
            releaseDurationUnits = "hours"
        )

        val immediateAtHalfHour = immediate.estimateMgPerLAt(0.5, 1.0, "mg")!!.start
        val depotAtHalfHour = depot.estimateMgPerLAt(0.5, 1.0, "mg")!!.start
        val immediateAtSixHours = immediate.estimateMgPerLAt(6.0, 1.0, "mg")!!.start
        val depotAtSixHours = depot.estimateMgPerLAt(6.0, 1.0, "mg")!!.start

        assertTrue(depotAtHalfHour < immediateAtHalfHour)
        assertTrue(depotAtSixHours > immediateAtSixHours)
    }

    @Test
    fun incompleteDepotDefinitionFallsBackToBateman() {
        val bateman = referenceModel(tmaxHours = 2.0, halfLifeHours = 8.0)
        val incompleteDepot = bateman.copy(
            inputModel = ConcentrationInputModel.WEIBULL.name.lowercase()
        )

        assertEquals(
            bateman.estimateMgPerLAt(3.0, 1.0, "mg")!!.start,
            incompleteDepot.estimateMgPerLAt(3.0, 1.0, "mg")!!.start,
            1e-12
        )
    }

    @Test
    fun intravenousRouteUsesImmediateBolusDecay() {
        val recordedModel = referenceModel(tmaxHours = null, halfLifeHours = 2.0)
        val resolved = ConcentrationEstimator.resolve(
            recordedModel = recordedModel,
            roaDuration = null,
            ingestionDose = 1.0,
            ingestionUnits = "mg",
            fallbackEliminationEndHours = null,
            administrationRoute = AdministrationRoute.INTRAVENOUS
        )!!

        assertEquals(0.001, resolved.estimateMgPerLAt(0.0, 1.0, "mg")!!.start, 1e-12)
        assertEquals(0.0005, resolved.estimateMgPerLAt(2.0, 1.0, "mg")!!.start, 1e-12)
    }

    @Test
    fun parserReadsDepotReleaseParameters() {
        val substance = SubstanceParser().parseSubstance(
            """
            {
              "name": "Depot test",
              "concentrationModels": {
                "subcutaneous": {
                  "status": "reference",
                  "referenceDose": 1,
                  "referenceDoseUnits": "mg",
                  "cmaxMgPerL": 0.001,
                  "tmaxMedian": 24,
                  "tmaxUnits": "hours",
                  "halfLife": 48,
                  "halfLifeUnits": "hours",
                  "kinetics": "weibull",
                  "releaseLag": 2,
                  "releaseLagUnits": "hours",
                  "releaseDuration": 14,
                  "releaseDurationUnits": "days",
                  "releaseShape": 1.7
                }
              }
            }
            """.trimIndent()
        )

        val model = substance!!.getConcentrationModel(AdministrationRoute.SUBCUTANEOUS)!!
        assertEquals(ConcentrationInputModel.WEIBULL, model.resolvedInputModel)
        assertEquals(2.0, model.releaseLag!!, 0.0)
        assertEquals(14.0, model.releaseDuration!!, 0.0)
        assertEquals(1.7, model.releaseShape!!, 0.0)
    }

    private fun referenceModel(
        tmaxHours: Double?,
        halfLifeHours: Double
    ): ConcentrationModel = ConcentrationModel(
        status = "reference",
        analyte = "test",
        referenceDose = 1.0,
        referenceDoseUnits = "mg",
        cmax = null,
        cmaxMin = null,
        cmaxMax = null,
        cmaxUnits = null,
        cmaxMgPerL = 0.001,
        cmaxMgPerLMin = null,
        cmaxMgPerLMax = null,
        note = null,
        tmax = tmaxHours,
        tmaxUnits = tmaxHours?.let { "hours" },
        halfLife = halfLifeHours,
        halfLifeUnits = "hours"
    )
}
