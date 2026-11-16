/*
 * Copyright (c) 2022-2023. Isaak Hanimann.
 * This file is part of PsychonautWiki Journal.
 *
 * PsychonautWiki Journal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * PsychonautWiki Journal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PsychonautWiki Journal.  If not, see https://www.gnu.org/licenses/gpl-3.0.en.html.
 */

package com.isaakhanimann.journal.ui.tabs.journal.experience.timeline

import com.isaakhanimann.journal.data.room.experiences.entities.AdaptiveColor
import com.isaakhanimann.journal.data.substances.classes.roa.ConcentrationModel
import com.isaakhanimann.journal.data.substances.classes.roa.ConcentrationUnit
import com.isaakhanimann.journal.data.substances.classes.roa.ExtendedReleaseProfile
import com.isaakhanimann.journal.data.substances.classes.roa.RoaDuration
import com.isaakhanimann.journal.ui.tabs.journal.experience.components.DataForOneEffectLine
import com.isaakhanimann.journal.ui.tabs.journal.experience.timeline.drawables.AxisDrawable
import com.isaakhanimann.journal.ui.tabs.journal.experience.timeline.drawables.GroupDrawable
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Group effect lines by their resolved duration within each substance and
 * administration route. A null duration forms its own bucket (no known curve).
 * RoaDuration and DurationRange are data classes with structural equality.
 *
 * Kotlin groupBy preserves key encounter order: the original substance and
 * route draw order is retained; duration buckets within a route appear in
 * their first-occurrence order.
 */
internal fun groupEffectLinesByProfile(
    dataForLines: List<DataForOneEffectLine>
): List<List<DataForOneEffectLine>> =
    dataForLines.groupBy { it.substanceName }.values.flatMap { substanceLines ->
        substanceLines.groupBy { it.route }.values.flatMap { routeLines ->
            routeLines.groupBy { it.roaDuration }.values
        }
    }

data class DataForOneConcentrationContribution(
    val model: ConcentrationModel,
    val dose: Double,
    val doseUnits: String,
    val startTime: Instant,
    val fallbackEliminationEndHours: Double?,
    val extendedReleaseProfile: ExtendedReleaseProfile? = null
)

data class DataForOneConcentrationSeries(
    val substanceName: String,
    val contributions: List<DataForOneConcentrationContribution>,
    val height: Float,
    val color: AdaptiveColor
) {
    val usesRelativeScale: Boolean
        get() = contributions.any { it.model.isRelativeEstimate }

    val usesVerifiedAbsoluteScale: Boolean
        get() = contributions.isNotEmpty() &&
            contributions.all { it.model.hasVerifiedAbsoluteScale }

    fun estimateMgPerLRangeAt(time: Instant): ClosedFloatingPointRange<Double>? {
        if (contributions.isEmpty()) return null
        val ranges = contributions.mapNotNull { contribution ->
            val elapsedHours =
                Duration.between(contribution.startTime, time).seconds / 3600.0
            contribution.model.estimateMgPerLAt(
                elapsedHours = elapsedHours,
                ingestionDose = contribution.dose,
                ingestionUnits = contribution.doseUnits,
                fallbackEliminationEndHours = contribution.fallbackEliminationEndHours,
                extendedReleaseProfile = contribution.extendedReleaseProfile
            )
        }
        if (ranges.size != contributions.size) return null
        return ranges.sumOf { it.start }..ranges.sumOf { it.endInclusive }
    }

    fun estimateRelativePercentRangeAt(
        time: Instant
    ): ClosedFloatingPointRange<Double>? {
        if (contributions.isEmpty()) return null
        val ranges = contributions.mapNotNull { contribution ->
            val elapsedHours =
                Duration.between(contribution.startTime, time).seconds / 3600.0
            contribution.model.estimateRelativePercentAt(
                elapsedHours = elapsedHours,
                ingestionDose = contribution.dose,
                ingestionUnits = contribution.doseUnits,
                fallbackEliminationEndHours = contribution.fallbackEliminationEndHours,
                extendedReleaseProfile = contribution.extendedReleaseProfile
            )
        }
        if (ranges.size != contributions.size) return null
        return ranges.sumOf { it.start }..ranges.sumOf { it.endInclusive }
    }

    fun labelAt(
        time: Instant,
        concentrationUnit: ConcentrationUnit
    ): String? {
        if (usesRelativeScale) {
            return estimateRelativePercentRangeAt(time)?.let(::formatPercentRange)
        }
        estimateMgPerLRangeAt(time)?.let { currentRange ->
            concentrationUnit.formatRange(currentRange)?.let { return it }
        }
        val peakRanges = contributions.mapNotNull { contribution ->
            contribution.model.estimatePeakMgPerLRange(
                ingestionDose = contribution.dose,
                ingestionUnits = contribution.doseUnits
            )
        }
        if (peakRanges.isEmpty() || peakRanges.size != contributions.size) return null
        val totalPeak =
            peakRanges.sumOf { it.start }..peakRanges.sumOf { it.endInclusive }
        return concentrationUnit.formatRange(totalPeak)?.let { "Cmax $it" }
    }

    fun normalizedCurve(
        startTime: Instant,
        widthInSeconds: Float,
        sampleCount: Int = 128
    ): List<ConcentrationCurvePoint> {
        if (contributions.isEmpty() || widthInSeconds <= 0f) return emptyList()
        val safeSampleCount = sampleCount.coerceAtLeast(32)
        val samples = (0..safeSampleCount).map { index ->
            val fraction = index.toFloat() / safeSampleCount
            val time = startTime.plusSeconds((widthInSeconds * fraction).toLong())
            val range = estimateMgPerLRangeAt(time) ?: return emptyList()
            val midpoint = (range.start + range.endInclusive) / 2.0
            ConcentrationCurvePoint(
                timeFraction = fraction,
                value = midpoint.coerceAtLeast(0.0),
                upperBound = range.endInclusive.coerceAtLeast(0.0)
            )
        }
        val maximum = samples.maxOfOrNull { it.upperBound } ?: return emptyList()
        if (maximum <= 0.0) return emptyList()
        return samples.map { sample ->
            sample.copy(value = (sample.value / maximum).coerceIn(0.0, 1.0))
        }
    }

    private fun formatPercentRange(
        range: ClosedFloatingPointRange<Double>
    ): String? {
        val minimum = range.start.coerceAtLeast(0.0)
        val maximum = range.endInclusive.coerceAtLeast(0.0)
        if (maximum < 0.01) return null
        return if (maximum - minimum < 0.01) {
            "≈ ${formatPercent((minimum + maximum) / 2.0)}%"
        } else if (minimum < 0.01) {
            "≈ ≤ ${formatPercent(maximum)}%"
        } else {
            "≈ ${formatPercent(minimum)}–${formatPercent(maximum)}%"
        }
    }

    private fun formatPercent(value: Double): String =
        String.format(Locale.US, "%.2f", value)
            .trimEnd('0')
            .trimEnd('.')
}

data class ConcentrationCurvePoint(
    val timeFraction: Float,
    val value: Double,
    val upperBound: Double = value
)

private data class ConcentrationSource(
    val substanceName: String,
    val contribution: DataForOneConcentrationContribution,
    val height: Float,
    val color: AdaptiveColor
)

class AllTimelinesModel(
    dataForLines: List<DataForOneEffectLine>,
    val dataForRatings: List<DataForOneRating>,
    val timedNotes: List<DataForOneTimedNote>,
    areSubstanceHeightsIndependent: Boolean = false,
) {
    val startTime: Instant
    val widthInSeconds: Float
    val groupDrawables: List<GroupDrawable>
    val axisDrawable: AxisDrawable
    val overallMaxHeight: Float
    val concentrationSeries: List<DataForOneConcentrationSeries>
    val hasVerifiedAbsoluteConcentration: Boolean

    data class RoaGroup(
        val substanceName: String,
        val color: AdaptiveColor,
        val roaDuration: RoaDuration?,
        val weightedLines: List<WeightedLine>
    )

    init {
        val ratingTimes = dataForRatings.map { it.time }
        val ingestionTimes = dataForLines.map { it.startTime }
        val noteTimes = timedNotes.map { it.time }
        val allStartTimeCandidates = ratingTimes + ingestionTimes + noteTimes
        startTime = allStartTimeCandidates.minOrNull() ?: Instant.EPOCH
        val roaGroups = groupEffectLinesByProfile(dataForLines).map { lines ->
            RoaGroup(
                substanceName = lines.first().substanceName,
                color = lines.first().color,
                roaDuration = lines.first().roaDuration,
                weightedLines = lines.map {
                    WeightedLine(
                        startTime = it.startTime,
                        endTime = it.endTime,
                        horizontalWeight = it.horizontalWeight,
                        height = it.height
                    )
                }
            )
        }
        val groupDrawables = roaGroups.map { group ->
            GroupDrawable(
                startTimeGraph = startTime,
                substanceName = group.substanceName,
                color = group.color,
                roaDuration = group.roaDuration,
                weightedLines = group.weightedLines,
                areSubstanceHeightsIndependent = areSubstanceHeightsIndependent
            )
        }
        val overallMaxHeight = groupDrawables.maxOfOrNull { it.nonNormalisedHeight } ?: 1f
        groupDrawables.forEach { it.normaliseHeight(overallMaxHeight) }
        this.groupDrawables = groupDrawables
        this.overallMaxHeight = overallMaxHeight
        val concentrationSources = dataForLines.mapNotNull { line ->
            val model = line.concentrationModel ?: return@mapNotNull null
            val dose = line.concentrationDose ?: return@mapNotNull null
            val doseUnits = line.concentrationDoseUnits ?: return@mapNotNull null
            ConcentrationSource(
                substanceName = line.substanceName,
                contribution = DataForOneConcentrationContribution(
                    model = model,
                    dose = dose,
                    doseUnits = doseUnits,
                    startTime = line.startTime,
                    fallbackEliminationEndHours = line.eliminationEndHours(),
                    extendedReleaseProfile = line.extendedReleaseProfile
                ),
                height = line.height,
                color = line.color
            )
        }
        concentrationSeries = concentrationSources
            .groupBy { it.substanceName }
            .values
            .map { sources ->
                DataForOneConcentrationSeries(
                    substanceName = sources.first().substanceName,
                    contributions = sources.map { it.contribution },
                    height = sources.maxOf { it.height },
                    color = sources.last().color
                )
            }
        hasVerifiedAbsoluteConcentration =
            concentrationSeries.any { it.usesVerifiedAbsoluteScale }
        val maxWidthOfGroups: Float = groupDrawables.maxOfOrNull {
            it.endOfLineRelativeToStartInSeconds
        } ?: 0f
        val latestRating = ratingTimes.maxOrNull()
        val maxWidthRating: Float = if (latestRating != null) {
            Duration.between(startTime, latestRating).seconds.toFloat()
        } else {
            0f
        }
        val latestNote = noteTimes.maxOrNull()
        val maxWidthNote: Float = if (latestNote != null) {
            Duration.between(startTime, latestNote).seconds.toFloat()
        } else {
            0f
        }
        val concentrationEndSeconds = concentrationSources.map { source ->
            val crossingHours =
                source.contribution.model.estimatedHoursUntilBelowPeakFraction(
                    peakFraction = CONCENTRATION_TAIL_PEAK_FRACTION,
                    fallbackEliminationEndHours =
                        source.contribution.fallbackEliminationEndHours,
                    extendedReleaseProfile =
                        source.contribution.extendedReleaseProfile
                )
            if (crossingHours == null) {
                0.0
            } else {
                val offsetSeconds = Duration.between(
                    startTime,
                    source.contribution.startTime
                ).seconds.coerceAtLeast(0L)
                offsetSeconds.toDouble() + crossingHours * 3600.0
            }
        }
        val maxWidthOfConcentration =
            concentrationEndSeconds.maxOrNull()?.toFloat() ?: 0f
        val maxCandidates = listOf(
            maxWidthOfGroups,
            maxWidthRating,
            maxWidthNote,
            maxWidthOfConcentration,
            2.hours.inWholeSeconds.toFloat()
        )
        widthInSeconds = maxCandidates.max() + 10.minutes.inWholeSeconds.toFloat()
        axisDrawable = AxisDrawable(startTime, widthInSeconds)
    }
}

private const val CONCENTRATION_TAIL_PEAK_FRACTION = 0.001

private fun DataForOneEffectLine.eliminationEndHours(): Double? {
    val explicitEnd = endTime
    if (explicitEnd != null && explicitEnd.isAfter(startTime)) {
        return Duration.between(startTime, explicitEnd).seconds / 3600.0
    }
    val totalDuration = roaDuration?.total ?: return null
    val totalSeconds = totalDuration.maxInSec ?: totalDuration.minInSec ?: return null
    return totalSeconds / 3600.0
}
