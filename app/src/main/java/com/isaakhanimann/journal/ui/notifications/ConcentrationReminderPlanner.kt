package com.isaakhanimann.journal.ui.notifications

import android.content.Context
import com.isaakhanimann.journal.data.room.experiences.ExperienceRepository
import com.isaakhanimann.journal.data.substances.classes.roa.ConcentrationEstimator
import com.isaakhanimann.journal.data.substances.classes.roa.RoaDuration
import com.isaakhanimann.journal.data.substances.repositories.SubstanceRepository
import com.isaakhanimann.journal.ui.tabs.settings.combinations.UserPreferences
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.first

internal const val CONCENTRATION_REMINDER_PREFERENCES = "concentration_reminders"

internal fun concentrationReminderPreferenceKey(
    ingestionId: Int,
    thresholdPercent: Int
): String = "notified_${ingestionId}_threshold_$thresholdPercent"

internal fun concentrationReminderFallbackEndHours(
    ingestionStart: Instant,
    explicitEnd: Instant?,
    roaDuration: RoaDuration?
): Double? {
    if (explicitEnd != null && explicitEnd.isAfter(ingestionStart)) {
        return Duration.between(ingestionStart, explicitEnd).seconds / 3600.0
    }
    val total = roaDuration?.total ?: return null
    val seconds = total.maxInSec ?: total.minInSec ?: return null
    return seconds / 3600.0
}

object ConcentrationReminderPlanner {
    suspend fun rescheduleNext(
        context: Context,
        experienceRepository: ExperienceRepository,
        substanceRepository: SubstanceRepository,
        userPreferences: UserPreferences
    ) {
        if (!userPreferences.concentrationReminderEnabledFlow.first()) {
            ConcentrationReminderScheduler.cancelNextThreshold(context)
            return
        }
        val thresholdPercent =
            userPreferences.concentrationReminderThresholdPercentFlow.first()
        val now = Instant.now()
        val ingestions = experienceRepository.getIngestionsWithCompanions(
            fromInstant = now.minus(180, ChronoUnit.DAYS),
            toInstant = now
        )
        val latestPerSubstanceAndRoute = ingestions
            .filter { !it.ingestion.time.isAfter(now) }
            .groupBy {
                it.ingestion.substanceName to it.ingestion.administrationRoute
            }
            .values
            .mapNotNull { candidates -> candidates.maxByOrNull { it.ingestion.time } }
        val notificationPreferences = context.getSharedPreferences(
            CONCENTRATION_REMINDER_PREFERENCES,
            Context.MODE_PRIVATE
        )

        val nextDelayMillis = latestPerSubstanceAndRoute
            .mapNotNull { ingestionWithCompanion ->
                val ingestion = ingestionWithCompanion.ingestion
                if (notificationPreferences.getBoolean(
                        concentrationReminderPreferenceKey(
                            ingestion.id,
                            thresholdPercent
                        ),
                        false
                    )
                ) {
                    return@mapNotNull null
                }
                val substance = substanceRepository.getSubstance(ingestion.substanceName)
                    ?: return@mapNotNull null
                val fallbackEndHours = concentrationReminderFallbackEndHours(
                    ingestionStart = ingestion.time,
                    explicitEnd = ingestion.endTime,
                    roaDuration = substance.getRoa(ingestion.administrationRoute)?.roaDuration
                )
                val model = ConcentrationEstimator.resolve(
                    recordedModel = substance.getConcentrationModel(
                        ingestion.administrationRoute
                    ),
                    roaDuration = substance
                        .getRoa(ingestion.administrationRoute)
                        ?.roaDuration,
                    ingestionDose = ingestion.dose,
                    ingestionUnits = ingestion.units,
                    fallbackEliminationEndHours = fallbackEndHours,
                    administrationRoute = ingestion.administrationRoute
                ) ?: return@mapNotNull null
                val crossingHours = model.estimatedHoursUntilBelowPeakFraction(
                    peakFraction = thresholdPercent / 100.0,
                    fallbackEliminationEndHours = fallbackEndHours,
                    extendedReleaseProfile = ingestion.extendedReleaseProfile
                ) ?: return@mapNotNull null
                val crossingInstant = ingestion.time.plusMillis((crossingHours * 3_600_000).toLong())
                Duration.between(now, crossingInstant).toMillis().coerceAtLeast(0L) +
                    REMINDER_SCHEDULE_GRACE_MILLIS
            }
            .minOrNull()

        if (nextDelayMillis == null) {
            ConcentrationReminderScheduler.cancelNextThreshold(context)
        } else {
            ConcentrationReminderScheduler.scheduleNextThreshold(context, nextDelayMillis)
        }
    }

    private const val REMINDER_SCHEDULE_GRACE_MILLIS = 30_000L
}
