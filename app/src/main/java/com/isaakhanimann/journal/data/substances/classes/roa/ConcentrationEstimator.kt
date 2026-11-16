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

/**
 * Resolves a displayable concentration curve for every ingestion.
 *
 * Verified reference models retain their absolute mass units. When Cmax or the
 * kinetics required for a curve are missing, this creates a relative-exposure
 * model whose peak is 100% of the recorded dose at the modeled Tmax. The
 * fallback is intentionally explicit and must never be presented as a measured
 * serum concentration.
 */
object ConcentrationEstimator {
    private const val DEFAULT_TMAX_HOURS = 1.0
    private const val DEFAULT_HALF_LIFE_HOURS = 4.0

    fun resolve(
        recordedModel: ConcentrationModel?,
        roaDuration: RoaDuration?,
        ingestionDose: Double?,
        ingestionUnits: String?,
        fallbackEliminationEndHours: Double?,
        administrationRoute: AdministrationRoute? = null
    ): ConcentrationModel? {
        val dose = ingestionDose?.takeIf { it > 0.0 } ?: return null
        val timing = roaDuration.estimateTiming(
            fallbackEliminationEndHours = fallbackEliminationEndHours
        )
        val routeAdjustedModel =
            recordedModel?.withInputModelForRoute(administrationRoute)
        val absoluteCandidate = routeAdjustedModel?.withFallbackKinetics(
            fallbackTmaxHours = timing.tmaxHours,
            fallbackEliminationEndHours = timing.eliminationEndHours
        )
        if (absoluteCandidate != null &&
            absoluteCandidate.hasVerifiedAbsoluteScale &&
            absoluteCandidate.canEstimateCurve(
                ingestionDose = dose,
                ingestionUnits = ingestionUnits,
                fallbackEliminationEndHours = timing.eliminationEndHours
            )
        ) {
            return absoluteCandidate
        }

        return if (routeAdjustedModel != null) {
            routeAdjustedModel.asRelativeEstimate(
                referenceDose = dose,
                referenceDoseUnits = ingestionUnits,
                fallbackTmaxHours = timing.tmaxHours,
                fallbackEliminationEndHours = timing.eliminationEndHours
            )
        } else {
            val tmaxHours = timing.tmaxHours ?: DEFAULT_TMAX_HOURS
            val halfLifeHours = timing.eliminationEndHours
                ?.takeIf { it > tmaxHours }
                ?.let { (it - tmaxHours) / 4.322 }
                ?: DEFAULT_HALF_LIFE_HOURS
            ConcentrationModel.relativeEstimate(
                referenceDose = dose,
                referenceDoseUnits = ingestionUnits,
                tmaxHours = tmaxHours,
                halfLifeHours = halfLifeHours
            ).withInputModelForRoute(administrationRoute)
        }
    }

    private data class EstimatedTiming(
        val tmaxHours: Double?,
        val eliminationEndHours: Double?
    )

    private fun RoaDuration?.estimateTiming(
        fallbackEliminationEndHours: Double?
    ): EstimatedTiming {
        if (this == null) {
            return EstimatedTiming(
                tmaxHours = null,
                eliminationEndHours = fallbackEliminationEndHours
            )
        }
        val tmaxHours = peak.midpointHours()
            ?: comeup?.maxInSec?.div(3600.0)
            ?: offset?.minInSec?.div(3600.0)
        val durationEndHours = total.preferredEndHours()
            ?: offset.preferredEndHours()
            ?: afterglow?.maxInSec?.div(3600.0)
        return EstimatedTiming(
            tmaxHours = tmaxHours,
            eliminationEndHours = listOfNotNull(
                fallbackEliminationEndHours,
                durationEndHours
            ).maxOrNull()
        )
    }

    private fun DurationRange?.midpointHours(): Double? {
        if (this == null) return null
        val minimum = minInSec?.div(3600.0)
        val maximum = maxInSec?.div(3600.0)
        return when {
            minimum != null && maximum != null -> (minimum + maximum) / 2.0
            minimum != null -> minimum
            else -> maximum
        }
    }

    private fun DurationRange?.preferredEndHours(): Double? =
        this?.maxInSec?.div(3600.0) ?: this?.minInSec?.div(3600.0)
}
