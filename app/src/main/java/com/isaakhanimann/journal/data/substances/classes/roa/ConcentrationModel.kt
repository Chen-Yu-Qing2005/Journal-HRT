/*
 * Copyright (c) 2026.
 * This file is part of PsychonautWiki Journal.
 *
 * PsychonautWiki Journal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.isaakhanimann.journal.data.substances.classes.roa

import com.isaakhanimann.journal.data.substances.AdministrationRoute
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class ConcentrationInputModel {
    BATEMAN,
    BOLUS,
    ZERO_ORDER,
    WEIBULL,
    HIGUCHI;

    companion object {
        fun from(value: String?): ConcentrationInputModel? = when (
            value?.trim()?.lowercase(Locale.US)
        ) {
            "bateman", "first_order" -> BATEMAN
            "bolus", "intravenous_bolus", "iv_bolus" -> BOLUS
            "zero_order", "zero-order" -> ZERO_ORDER
            "weibull" -> WEIBULL
            "higuchi" -> HIGUCHI
            else -> null
        }
    }
}

data class ConcentrationModel(
    val status: String,
    val analyte: String?,
    val referenceDose: Double?,
    val referenceDoseUnits: String?,
    val cmax: Double?,
    val cmaxMin: Double?,
    val cmaxMax: Double?,
    val cmaxUnits: String?,
    val cmaxMgPerL: Double?,
    val cmaxMgPerLMin: Double?,
    val cmaxMgPerLMax: Double?,
    val note: String?,
    val tmax: Double? = null,
    val tmaxMedian: Double? = null,
    val tmaxMin: Double? = null,
    val tmaxMax: Double? = null,
    val tmaxUnits: String? = null,
    val halfLife: Double? = null,
    val halfLifeMin: Double? = null,
    val halfLifeMax: Double? = null,
    val halfLifeUnits: String? = null,
    val inputModel: String? = null,
    val absorptionHalfLife: Double? = null,
    val absorptionHalfLifeUnits: String? = null,
    val releaseLag: Double? = null,
    val releaseLagUnits: String? = null,
    val releaseDuration: Double? = null,
    val releaseDurationUnits: String? = null,
    val releaseShape: Double? = null
) {
    val isRelativeEstimate: Boolean
        get() = status == RELATIVE_ESTIMATE_STATUS

    val hasVerifiedAbsoluteScale: Boolean
        get() = status == "reference" || status == "reference_range"

    val resolvedInputModel: ConcentrationInputModel
        get() = ConcentrationInputModel.from(inputModel) ?: ConcentrationInputModel.BATEMAN

    fun withInputModelForRoute(route: AdministrationRoute?): ConcentrationModel {
        if (ConcentrationInputModel.from(inputModel) != null) return this
        val resolved = if (route == AdministrationRoute.INTRAVENOUS) {
            ConcentrationInputModel.BOLUS
        } else {
            ConcentrationInputModel.BATEMAN
        }
        return copy(inputModel = resolved.name.lowercase(Locale.US))
    }

    fun estimatePeakLabel(
        ingestionDose: Double?,
        ingestionUnits: String?,
        concentrationUnit: ConcentrationUnit = ConcentrationUnit.MG_PER_L
    ): String? = peakMgPerLRange(
        ingestionDose = ingestionDose,
        ingestionUnits = ingestionUnits
    )?.let(concentrationUnit::formatRange)

    fun estimatePeakMgPerLRange(
        ingestionDose: Double?,
        ingestionUnits: String?
    ): ClosedFloatingPointRange<Double>? = peakMgPerLRange(
        ingestionDose = ingestionDose,
        ingestionUnits = ingestionUnits
    )

    fun canEstimateCurve(
        ingestionDose: Double?,
        ingestionUnits: String?,
        fallbackEliminationEndHours: Double?
    ): Boolean {
        if (peakMgPerLRange(ingestionDose, ingestionUnits) == null) return false
        val tmaxHours = tmaxHours() ?: 0.0
        if (resolvedInputModel != ConcentrationInputModel.BOLUS &&
            tmaxHours <= 0.0
        ) {
            return false
        }
        return halfLifeHours(
            fallbackEliminationEndHours = fallbackEliminationEndHours,
            tmaxHours = tmaxHours
        ) != null
    }

    fun estimateRelativePercentAt(
        elapsedHours: Double,
        ingestionDose: Double?,
        ingestionUnits: String?,
        fallbackEliminationEndHours: Double? = null,
        extendedReleaseProfile: ExtendedReleaseProfile? = null
    ): ClosedFloatingPointRange<Double>? {
        val referencePeak = peakMgPerLRange(
            ingestionDose = referenceDose,
            ingestionUnits = referenceDoseUnits
        ) ?: return null
        val current = estimateMgPerLAt(
            elapsedHours = elapsedHours,
            ingestionDose = ingestionDose,
            ingestionUnits = ingestionUnits,
            fallbackEliminationEndHours = fallbackEliminationEndHours,
            extendedReleaseProfile = extendedReleaseProfile
        ) ?: return null
        val peakMinimum = referencePeak.start.coerceAtLeast(0.000000001)
        val peakMaximum = referencePeak.endInclusive.coerceAtLeast(peakMinimum)
        return (current.start / peakMaximum * 100.0)..
            (current.endInclusive / peakMinimum * 100.0)
    }

    fun asRelativeEstimate(
        referenceDose: Double,
        referenceDoseUnits: String?,
        fallbackTmaxHours: Double?,
        fallbackEliminationEndHours: Double?
    ): ConcentrationModel {
        val tmaxHours = tmaxHours() ?: fallbackTmaxHours ?: 1.0
        val hasHalfLife = halfLife != null || halfLifeMin != null || halfLifeMax != null
        val fallbackHalfLife = if (hasHalfLife) {
            null
        } else if (fallbackEliminationEndHours != null &&
            fallbackEliminationEndHours > tmaxHours
        ) {
            (fallbackEliminationEndHours - tmaxHours) / 4.322
        } else {
            DEFAULT_FALLBACK_HALF_LIFE_HOURS
        }
        return copy(
            status = RELATIVE_ESTIMATE_STATUS,
            referenceDose = referenceDose,
            referenceDoseUnits = referenceDoseUnits ?: this.referenceDoseUnits ?: "dose",
            cmax = 100.0,
            cmaxMin = null,
            cmaxMax = null,
            cmaxUnits = "%",
            cmaxMgPerL = 100.0,
            cmaxMgPerLMin = null,
            cmaxMgPerLMax = null,
            tmax = if (tmaxHours() == null) tmaxHours else tmax,
            tmaxMedian = if (tmaxHours() == null) null else tmaxMedian,
            tmaxMin = if (tmaxHours() == null) null else tmaxMin,
            tmaxMax = if (tmaxHours() == null) null else tmaxMax,
            tmaxUnits = if (tmaxHours() == null) "hours" else tmaxUnits,
            halfLife = if (fallbackHalfLife != null) fallbackHalfLife else halfLife,
            halfLifeMin = if (fallbackHalfLife != null) null else halfLifeMin,
            halfLifeMax = if (fallbackHalfLife != null) null else halfLifeMax,
            halfLifeUnits = if (fallbackHalfLife != null) "hours" else halfLifeUnits,
            note = note ?: "Relative exposure estimate; no verified absolute Cmax is available."
        )
    }

    fun withFallbackKinetics(
        fallbackTmaxHours: Double?,
        fallbackEliminationEndHours: Double?
    ): ConcentrationModel {
        val resolvedTmaxHours = tmaxHours() ?: fallbackTmaxHours
        val hasHalfLife = halfLife != null || halfLifeMin != null || halfLifeMax != null
        val resolvedHalfLifeHours = when {
            hasHalfLife -> null
            resolvedTmaxHours != null &&
                fallbackEliminationEndHours != null &&
                fallbackEliminationEndHours > resolvedTmaxHours ->
                (fallbackEliminationEndHours - resolvedTmaxHours) / 4.322
            else -> null
        }
        if (tmaxHours() != null && resolvedHalfLifeHours == null) return this
        return copy(
            tmax = if (tmaxHours() == null) resolvedTmaxHours else tmax,
            tmaxMedian = if (tmaxHours() == null) null else tmaxMedian,
            tmaxMin = if (tmaxHours() == null) null else tmaxMin,
            tmaxMax = if (tmaxHours() == null) null else tmaxMax,
            tmaxUnits = if (tmaxHours() == null) "hours" else tmaxUnits,
            halfLife = if (resolvedHalfLifeHours != null) resolvedHalfLifeHours else halfLife,
            halfLifeMin = if (resolvedHalfLifeHours != null) null else halfLifeMin,
            halfLifeMax = if (resolvedHalfLifeHours != null) null else halfLifeMax,
            halfLifeUnits = if (resolvedHalfLifeHours != null) "hours" else halfLifeUnits
        )
    }

    /**
     * Estimates a single-dose concentration curve from an explicit input model.
     *
     * Bateman is the default: first-order absorption into one central compartment followed by
     * first-order elimination. Zero-order, Weibull and Higuchi release are used only when a
     * dosage form supplies complete release parameters. Missing or invalid release metadata
     * falls back to Bateman instead of inventing a depot profile.
     */
    fun estimateMgPerLAt(
        elapsedHours: Double,
        ingestionDose: Double?,
        ingestionUnits: String?,
        fallbackEliminationEndHours: Double? = null,
        extendedReleaseProfile: ExtendedReleaseProfile? = null
    ): ClosedFloatingPointRange<Double>? {
        val peak = peakMgPerLRange(ingestionDose, ingestionUnits) ?: return null
        val dose = ingestionDose ?: return null
        if (elapsedHours < 0.0) return 0.0..0.0
        if (resolvedInputModel == ConcentrationInputModel.BOLUS) {
            return estimateBolusMgPerLAt(
                elapsedHours = elapsedHours,
                peak = peak,
                fallbackEliminationEndHours = fallbackEliminationEndHours
            )
        }

        val releaseSchedule = resolveReleaseSchedule(extendedReleaseProfile)
        if (releaseSchedule != null) {
            val inputEndHours = minOf(
                elapsedHours,
                releaseSchedule.totalHours + releaseSchedule.lagHours
            )
            if (inputEndHours <= releaseSchedule.lagHours) return 0.0..0.0
            val inputDurationHours = inputEndHours - releaseSchedule.lagHours
            val steps = (inputDurationHours * 4.0)
                .roundToInt()
                .coerceIn(MIN_RELEASE_INTEGRATION_STEPS, MAX_RELEASE_INTEGRATION_STEPS)
            var minimum = 0.0
            var maximum = 0.0
            var hasContribution = false
            for (step in 0 until steps) {
                val startMinutes = inputDurationHours * 60.0 * step / steps
                val endMinutes = inputDurationHours * 60.0 * (step + 1) / steps
                val releasedFraction =
                    releaseSchedule.releasedFractionAt(endMinutes) -
                        releaseSchedule.releasedFractionAt(startMinutes)
                if (releasedFraction <= 0.0) continue
                val midpointHours =
                    releaseSchedule.lagHours + (startMinutes + endMinutes) / 120.0
                val impulseRange = estimateBatemanMgPerLAt(
                    elapsedHours = elapsedHours - midpointHours,
                    ingestionDose = dose * releasedFraction,
                    ingestionUnits = ingestionUnits,
                    fallbackEliminationEndHours = fallbackEliminationEndHours
                ) ?: return null
                minimum += impulseRange.start
                maximum += impulseRange.endInclusive
                hasContribution = true
            }
            return if (hasContribution) minimum..maximum else 0.0..0.0
        }

        return estimateBatemanMgPerLAt(
            elapsedHours = elapsedHours,
            ingestionDose = ingestionDose,
            ingestionUnits = ingestionUnits,
            fallbackEliminationEndHours = fallbackEliminationEndHours
        )
    }

    private fun estimateBolusMgPerLAt(
        elapsedHours: Double,
        peak: ClosedFloatingPointRange<Double>,
        fallbackEliminationEndHours: Double?
    ): ClosedFloatingPointRange<Double> {
        if (elapsedHours < 0.0) return 0.0..0.0
        val tmaxHours = tmaxHours() ?: 0.0
        val halfLifeRange = halfLifeHours(
            fallbackEliminationEndHours = fallbackEliminationEndHours,
            tmaxHours = tmaxHours
        ) ?: return 0.0..0.0
        val elapsedAfterInput = (elapsedHours - tmaxHours).coerceAtLeast(0.0)
        val shortestHalfLife = halfLifeRange.start.coerceAtLeast(0.001)
        val longestHalfLife = halfLifeRange.endInclusive.coerceAtLeast(shortestHalfLife)
        val minimum = peak.start * 0.5.pow(elapsedAfterInput / shortestHalfLife)
        val maximum = peak.endInclusive * 0.5.pow(elapsedAfterInput / longestHalfLife)
        return minimum..maximum
    }

    private fun estimateBatemanMgPerLAt(
        elapsedHours: Double,
        ingestionDose: Double?,
        ingestionUnits: String?,
        fallbackEliminationEndHours: Double? = null
    ): ClosedFloatingPointRange<Double>? {
        val peak = peakMgPerLRange(ingestionDose, ingestionUnits) ?: return null
        val tmaxHours = tmaxHours() ?: return null
        if (elapsedHours <= 0.0) return 0.0..0.0
        if (tmaxHours <= 0.0) return null
        val halfLifeRange = halfLifeHours(
            fallbackEliminationEndHours = fallbackEliminationEndHours,
            tmaxHours = tmaxHours
        ) ?: return null
        val shortestHalfLife = halfLifeRange.start.coerceAtLeast(0.001)
        val longestHalfLife = halfLifeRange.endInclusive.coerceAtLeast(shortestHalfLife)
        val shortestKe = ln(2.0) / longestHalfLife
        val longestKe = ln(2.0) / shortestHalfLife
        val firstFraction = batemanFractionAt(
            elapsedHours = elapsedHours,
            tmaxHours = tmaxHours,
            absorptionRate = absorptionRateFor(tmaxHours, shortestKe),
            eliminationRate = shortestKe
        )
        val secondFraction = batemanFractionAt(
            elapsedHours = elapsedHours,
            tmaxHours = tmaxHours,
            absorptionRate = absorptionRateFor(tmaxHours, longestKe),
            eliminationRate = longestKe
        )
        val minimum = peak.start * minOf(firstFraction, secondFraction)
        val maximum = peak.endInclusive * maxOf(firstFraction, secondFraction)
        return minimum..maximum
    }

    private fun resolveReleaseSchedule(
        extendedReleaseProfile: ExtendedReleaseProfile?
    ): ReleaseSchedule? {
        if (extendedReleaseProfile != null) {
            if (!extendedReleaseProfile.isValid) return null
            return ReleaseSchedule(
                lagHours = 0.0,
                totalHours = extendedReleaseProfile.totalMinutes / 60.0,
                releasedFractionAt = { elapsedMinutes ->
                    extendedReleaseProfile.releasedFractionAt(elapsedMinutes)
                }
            )
        }
        val inputModel = resolvedInputModel
        if (inputModel !in setOf(
                ConcentrationInputModel.ZERO_ORDER,
                ConcentrationInputModel.WEIBULL,
                ConcentrationInputModel.HIGUCHI
            )
        ) {
            return null
        }
        val totalHours = releaseDuration?.let {
            toHours(it, releaseDurationUnits)
        }?.takeIf { it > 0.0 } ?: return null
        val lagHours = releaseLag?.let {
            toHours(it, releaseLagUnits)
        }?.coerceAtLeast(0.0) ?: 0.0
        val shape = releaseShape?.takeIf { it > 0.0 }
        if (inputModel == ConcentrationInputModel.WEIBULL && shape == null) return null
        return ReleaseSchedule(
            lagHours = lagHours,
            totalHours = totalHours,
            releasedFractionAt = { elapsedMinutes ->
                val elapsedHours = elapsedMinutes / 60.0
                if (elapsedHours <= 0.0) {
                    0.0
                } else {
                    val progress = (elapsedHours / totalHours).coerceIn(0.0, 1.0)
                    when (inputModel) {
                        ConcentrationInputModel.ZERO_ORDER -> progress
                        ConcentrationInputModel.WEIBULL -> {
                            val resolvedShape = shape ?: 1.0
                            val endpoint = 1.0 - exp(-resolvedShape)
                            (
                                (1.0 - exp(
                                    -resolvedShape * progress.pow(resolvedShape)
                                )) / endpoint
                                ).coerceIn(0.0, 1.0)
                        }
                        ConcentrationInputModel.HIGUCHI ->
                            sqrt(progress).coerceIn(0.0, 1.0)
                        else -> progress
                    }
                }
            }
        )
    }

    private fun batemanFractionAt(
        elapsedHours: Double,
        tmaxHours: Double,
        absorptionRate: Double,
        eliminationRate: Double
    ): Double {
        if (elapsedHours <= 0.0) return 0.0
        if (abs(absorptionRate - eliminationRate) < 0.000000001) {
            val scaledTime = eliminationRate * elapsedHours
            return scaledTime * exp(1.0 - scaledTime)
        }
        val peakShape =
            exp(-eliminationRate * tmaxHours) - exp(-absorptionRate * tmaxHours)
        if (peakShape <= 0.000000000000001) return 0.0
        val current =
            exp(-eliminationRate * elapsedHours) - exp(-absorptionRate * elapsedHours)
        return (current / peakShape).coerceAtLeast(0.0)
    }

    private fun absorptionRateFor(
        tmaxHours: Double,
        eliminationRate: Double
    ): Double {
        absorptionHalfLife?.let { value ->
            toHours(value, absorptionHalfLifeUnits)?.takeIf { it > 0.0 }?.let {
                return ln(2.0) / it
            }
        }
        if (tmaxHours <= 0.0) {
            return max(eliminationRate * 10.0, 1.0)
        }
        val criticalTmax = 1.0 / eliminationRate
        val useNormalAbsorption = tmaxHours < criticalTmax * 0.999999
        var lower = if (useNormalAbsorption) {
            eliminationRate * (1.0 + 0.000000001)
        } else {
            eliminationRate * 0.000000001
        }
        var upper = if (useNormalAbsorption) {
            max(eliminationRate * 1000000.0, 1000.0)
        } else {
            eliminationRate * (1.0 - 0.000000001)
        }
        repeat(70) {
            val midpoint = if (useNormalAbsorption) {
                sqrt(lower * upper)
            } else {
                (lower + upper) / 2.0
            }
            val midpointTmax = batemanTmax(
                absorptionRate = midpoint,
                eliminationRate = eliminationRate
            )
            if (midpointTmax > tmaxHours) {
                lower = midpoint
            } else {
                upper = midpoint
            }
        }
        return (lower + upper) / 2.0
    }

    private fun batemanTmax(
        absorptionRate: Double,
        eliminationRate: Double
    ): Double {
        if (abs(absorptionRate - eliminationRate) < 0.000000000001) {
            return 1.0 / eliminationRate
        }
        return ln(absorptionRate / eliminationRate) /
            (absorptionRate - eliminationRate)
    }

    private data class ReleaseSchedule(
        val lagHours: Double,
        val totalHours: Double,
        val releasedFractionAt: (Double) -> Double
    )

    fun estimateConcentrationLabelAt(
        elapsedHours: Double,
        ingestionDose: Double?,
        ingestionUnits: String?,
        fallbackEliminationEndHours: Double? = null,
        concentrationUnit: ConcentrationUnit = ConcentrationUnit.MG_PER_L,
        extendedReleaseProfile: ExtendedReleaseProfile? = null
    ): String? = estimateMgPerLAt(
        elapsedHours = elapsedHours,
        ingestionDose = ingestionDose,
        ingestionUnits = ingestionUnits,
        fallbackEliminationEndHours = fallbackEliminationEndHours,
        extendedReleaseProfile = extendedReleaseProfile
    )?.let(concentrationUnit::formatRange)

    /**
     * Returns the estimated hours after ingestion at which the modeled concentration
     * first falls below [peakFraction] of Cmax. The longest reported half-life is used,
     * so reminders are not sent earlier than the available data supports.
     */
    fun estimatedHoursUntilBelowPeakFraction(
        peakFraction: Double,
        fallbackEliminationEndHours: Double? = null,
        extendedReleaseProfile: ExtendedReleaseProfile? = null
    ): Double? {
        if (peakFraction <= 0.0 || peakFraction >= 1.0) return null
        val tmaxHours = tmaxHours() ?: return null
        val halfLifeRange = halfLifeHours(
            fallbackEliminationEndHours = fallbackEliminationEndHours,
            tmaxHours = tmaxHours
        ) ?: return null
        val longestHalfLife = halfLifeRange.endInclusive.coerceAtLeast(0.001)
        val immediateCrossingHours = when (resolvedInputModel) {
            ConcentrationInputModel.BOLUS ->
                tmaxHours + longestHalfLife * (ln(1.0 / peakFraction) / ln(2.0))
            else -> {
                val eliminationRate = ln(2.0) / longestHalfLife
                val absorptionRate = absorptionRateFor(tmaxHours, eliminationRate)
                var lower = tmaxHours
                var upper = tmaxHours + longestHalfLife * 40.0
                repeat(60) {
                    val midpoint = (lower + upper) / 2.0
                    val fraction = batemanFractionAt(
                        elapsedHours = midpoint,
                        tmaxHours = tmaxHours,
                        absorptionRate = absorptionRate,
                        eliminationRate = eliminationRate
                    )
                    if (fraction > peakFraction) {
                        lower = midpoint
                    } else {
                        upper = midpoint
                    }
                }
                upper
            }
        }
        val releaseSchedule = resolveReleaseSchedule(extendedReleaseProfile)
        return immediateCrossingHours +
            (releaseSchedule?.let { it.lagHours + it.totalHours } ?: 0.0)
    }

    private fun peakMgPerLRange(
        ingestionDose: Double?,
        ingestionUnits: String?
    ): ClosedFloatingPointRange<Double>? {
        if (!hasVerifiedAbsoluteScale && !isRelativeEstimate) return null
        val dose = ingestionDose ?: return null
        val referenceDose = referenceDose ?: return null
        if (dose <= 0 || referenceDose <= 0) return null
        if (!isRelativeEstimate && !unitsMatch(ingestionUnits, referenceDoseUnits)) return null

        val scale = dose / referenceDose
        val minMgPerL = cmaxMgPerLMin ?: cmaxMgPerL
        val maxMgPerL = cmaxMgPerLMax ?: cmaxMgPerL
        if (minMgPerL != null || maxMgPerL != null) {
            return ((minMgPerL ?: maxMgPerL)!! * scale)..
                ((maxMgPerL ?: minMgPerL)!! * scale)
        }
        return estimateFromSourceCmax(scale)
    }

    private fun estimateFromSourceCmax(
        scale: Double
    ): ClosedFloatingPointRange<Double>? {
        val sourceMin = cmax ?: cmaxMin ?: return null
        val sourceMax = cmax ?: cmaxMax ?: return null
        val factor = when (cmaxUnits) {
            "pg/mL" -> 0.000001
            "ng/mL" -> 0.001
            "µg/mL" -> 1.0
            "μg/mL" -> 1.0
            "mg/L" -> 1.0
            else -> return null
        }
        val minMgPerL = sourceMin * factor * scale
        val maxMgPerL = sourceMax * factor * scale
        return minMgPerL..maxMgPerL
    }

    private fun tmaxHours(): Double? {
        val minimum = tmaxMin
        val maximum = tmaxMax
        val value = tmax
            ?: tmaxMedian
            ?: when {
                minimum != null && maximum != null -> (minimum + maximum) / 2.0
                minimum != null -> minimum
                else -> maximum
            }
            ?: return null
        return toHours(value, tmaxUnits)
    }

    private fun halfLifeHours(
        fallbackEliminationEndHours: Double?,
        tmaxHours: Double
    ): ClosedFloatingPointRange<Double>? {
        if (halfLife != null) {
            val hours = toHours(halfLife, halfLifeUnits) ?: return null
            return hours..hours
        }
        val min = halfLifeMin?.let { toHours(it, halfLifeUnits) }
        val max = halfLifeMax?.let { toHours(it, halfLifeUnits) }
        if (min != null || max != null) {
            return (min ?: max)!!..(max ?: min)!!
        }
        val eliminationEnd = fallbackEliminationEndHours ?: return null
        if (eliminationEnd <= tmaxHours) return null
        // The effect timeline supplies a display horizon rather than a measured half-life.
        // Treat the post-peak horizon as roughly five elimination half-lives.
        val fallbackHalfLife = (eliminationEnd - tmaxHours) / 4.322
        return fallbackHalfLife..fallbackHalfLife
    }

    private fun toHours(value: Double, units: String?): Double? = when (
        units?.trim()?.lowercase(Locale.US)
    ) {
        "hour", "hours", "h" -> value
        "day", "days", "d" -> value * 24.0
        "minute", "minutes", "min" -> value / 60.0
        "second", "seconds", "s" -> value / 3600.0
        else -> null
    }

    private fun unitsMatch(
        ingestionUnits: String?,
        referenceUnits: String?
    ): Boolean {
        val ingestionToken = ingestionUnits
            ?.trim()
            ?.substringBefore(" ")
            ?.lowercase(Locale.US)
            ?: return false
        val referenceToken = referenceUnits
            ?.trim()
            ?.substringBefore(" ")
            ?.lowercase(Locale.US)
            ?: return false
        return ingestionToken == referenceToken
    }

    companion object {
        const val RELATIVE_ESTIMATE_STATUS = "relative_estimate"
        private const val DEFAULT_FALLBACK_HALF_LIFE_HOURS = 4.0
        private const val MIN_RELEASE_INTEGRATION_STEPS = 96
        private const val MAX_RELEASE_INTEGRATION_STEPS = 384

        fun relativeEstimate(
            referenceDose: Double,
            referenceDoseUnits: String?,
            tmaxHours: Double,
            halfLifeHours: Double
        ): ConcentrationModel = ConcentrationModel(
            status = RELATIVE_ESTIMATE_STATUS,
            analyte = null,
            referenceDose = referenceDose,
            referenceDoseUnits = referenceDoseUnits ?: "dose",
            cmax = 100.0,
            cmaxMin = null,
            cmaxMax = null,
            cmaxUnits = "%",
            cmaxMgPerL = 100.0,
            cmaxMgPerLMin = null,
            cmaxMgPerLMax = null,
            note = "Relative exposure estimate derived from route timing; not a serum concentration.",
            tmax = tmaxHours,
            tmaxUnits = "hours",
            halfLife = halfLifeHours.coerceAtLeast(0.001),
            halfLifeUnits = "hours",
            inputModel = ConcentrationInputModel.BATEMAN.name.lowercase(Locale.US)
        )
    }

}
