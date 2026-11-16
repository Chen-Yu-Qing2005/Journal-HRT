package com.isaakhanimann.journal.ui.notifications

import android.content.Context
import android.util.Log
import com.isaakhanimann.journal.data.room.experiences.ExperienceRepository
import com.isaakhanimann.journal.data.substances.classes.roa.ConcentrationEstimator
import com.isaakhanimann.journal.data.substances.repositories.SubstanceRepository
import com.isaakhanimann.journal.ui.tabs.settings.combinations.UserPreferences
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.first

object ConcentrationReminderChecker {
    private const val TAG = "ConcentrationReminder"

    suspend fun check(
        context: Context,
        experienceRepository: ExperienceRepository,
        substanceRepository: SubstanceRepository,
        userPreferences: UserPreferences
    ): Int {
        if (!userPreferences.concentrationReminderEnabledFlow.first()) {
            Log.d(TAG, "Skipped: reminders disabled")
            return 0
        }
        val thresholdPercent =
            userPreferences.concentrationReminderThresholdPercentFlow.first()
        val thresholdFraction = thresholdPercent / 100.0
        val now = Instant.now()
        val ingestions = experienceRepository.getIngestionsWithCompanions(
            fromInstant = now.minus(180, ChronoUnit.DAYS),
            toInstant = now
        )
        val ingestionsBySubstance = ingestions
            .filter { !it.ingestion.time.isAfter(now) }
            .groupBy { it.ingestion.substanceName }
            .values
        val notificationPreferences = context.getSharedPreferences(
            CONCENTRATION_REMINDER_PREFERENCES,
            Context.MODE_PRIVATE
        )
        var notificationCount = 0
        Log.d(
            TAG,
            "Checking ${ingestionsBySubstance.size} active substances at threshold=$thresholdPercent%"
        )

        ingestionsBySubstance.forEach { candidates ->
            val latest = candidates.maxByOrNull { it.ingestion.time } ?: return@forEach
            val ingestion = latest.ingestion
            val substance = substanceRepository.getSubstance(ingestion.substanceName)
                ?: return@forEach
            val dose = ingestion.dose ?: return@forEach
            val units = ingestion.units ?: return@forEach
            val fallbackEndHours = concentrationReminderFallbackEndHours(
                ingestionStart = ingestion.time,
                explicitEnd = ingestion.endTime,
                roaDuration = substance.getRoa(ingestion.administrationRoute)?.roaDuration
            )
            val model = ConcentrationEstimator.resolve(
                recordedModel = substance.getConcentrationModel(
                    ingestion.administrationRoute
                ),
                roaDuration = substance.getRoa(ingestion.administrationRoute)?.roaDuration,
                ingestionDose = dose,
                ingestionUnits = units,
                fallbackEliminationEndHours = fallbackEndHours,
                administrationRoute = ingestion.administrationRoute
            ) ?: return@forEach
            val crossingHours = model.estimatedHoursUntilBelowPeakFraction(
                peakFraction = thresholdFraction,
                fallbackEliminationEndHours = fallbackEndHours,
                extendedReleaseProfile = ingestion.extendedReleaseProfile
            ) ?: return@forEach
            val elapsedHours = Duration.between(ingestion.time, now).seconds / 3600.0
            if (elapsedHours < crossingHours) return@forEach

            val peakRange = model.estimatePeakMgPerLRange(dose, units)
                ?: return@forEach
            val currentRanges = candidates.mapNotNull { candidate ->
                val candidateIngestion = candidate.ingestion
                val candidateModel = substance.getConcentrationModel(
                    candidateIngestion.administrationRoute
                ) ?: return@mapNotNull null
                val candidateDose = candidateIngestion.dose ?: return@mapNotNull null
                val candidateUnits = candidateIngestion.units ?: return@mapNotNull null
                val candidateFallbackEndHours = concentrationReminderFallbackEndHours(
                    ingestionStart = candidateIngestion.time,
                    explicitEnd = candidateIngestion.endTime,
                    roaDuration = substance.getRoa(
                        candidateIngestion.administrationRoute
                    )?.roaDuration
                )
                val resolvedCandidateModel = if (candidateModel === model) {
                    candidateModel
                } else {
                    ConcentrationEstimator.resolve(
                        recordedModel = candidateModel,
                        roaDuration = substance.getRoa(
                            candidateIngestion.administrationRoute
                        )?.roaDuration,
                        ingestionDose = candidateDose,
                        ingestionUnits = candidateUnits,
                        fallbackEliminationEndHours = candidateFallbackEndHours,
                        administrationRoute = candidateIngestion.administrationRoute
                    ) ?: return@mapNotNull null
                }
                resolvedCandidateModel.estimateMgPerLAt(
                    elapsedHours = Duration.between(
                        candidateIngestion.time,
                        now
                    ).seconds / 3600.0,
                    ingestionDose = candidateDose,
                    ingestionUnits = candidateUnits,
                    fallbackEliminationEndHours = candidateFallbackEndHours,
                    extendedReleaseProfile = candidateIngestion.extendedReleaseProfile
                )
            }
            if (currentRanges.isEmpty()) return@forEach
            val totalCurrentUpper = currentRanges.sumOf { it.endInclusive }
            if (totalCurrentUpper > peakRange.endInclusive * thresholdFraction) {
                return@forEach
            }

            val notificationKey = concentrationReminderPreferenceKey(
                ingestion.id,
                thresholdPercent
            )
            if (notificationPreferences.getBoolean(notificationKey, false)) {
                return@forEach
            }
            val wasShown = Notifications.showConcentrationReminder(
                context = context,
                ingestionId = ingestion.id,
                experienceId = ingestion.experienceId,
                substanceName = substance.displayName,
                thresholdPercent = thresholdPercent
            )
            if (wasShown) {
                notificationPreferences.edit()
                    .putBoolean(notificationKey, true)
                    .apply()
                notificationCount += 1
                Log.i(TAG, "Posted reminder for ingestion ${ingestion.id}")
            }
        }
        Log.d(TAG, "Check complete: notifications=$notificationCount")
        return notificationCount
    }
}
