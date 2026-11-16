package com.isaakhanimann.journal.ui.tabs.journal.addingestion.interactions

import com.isaakhanimann.journal.data.room.experiences.ExperienceRepository
import com.isaakhanimann.journal.data.room.experiences.entities.Ingestion
import com.isaakhanimann.journal.data.substances.classes.roa.ConcentrationEstimator
import com.isaakhanimann.journal.data.substances.classes.roa.RoaDuration
import com.isaakhanimann.journal.data.substances.repositories.SubstanceRepository
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Finds substances that are still present across every saved experience.
 *
 * Experience boundaries are organizational only. A dose remains relevant to interaction checks
 * even when it is stored in a different experience, so this resolver deliberately scans the whole
 * ingestion table before grouping by substance.
 *
 * The operational residual threshold is 0.1% of the modeled reference peak. Irreversible effects
 * such as enzyme inhibition can outlast plasma exposure and are not inferred from concentration
 * alone.
 */
@Singleton
class ActiveIngestionResolver @Inject constructor(
    private val experienceRepo: ExperienceRepository,
    private val substanceRepo: SubstanceRepository
) {
    suspend fun getActiveIngestions(now: Instant = Instant.now()): List<Ingestion> =
        experienceRepo.getSortedIngestionsFlow()
            .first()
            .asSequence()
            .filter { !it.time.isAfter(now) }
            .groupBy { it.substanceName }
            .values
            .mapNotNull { ingestions ->
                ingestions
                    .filter { isActiveAt(it, now) }
                    .maxByOrNull { it.time }
            }
            .sortedByDescending { it.time }

    private fun isActiveAt(ingestion: Ingestion, now: Instant): Boolean {
        val substance = substanceRepo.getSubstance(ingestion.substanceName)
        val roaDuration = substance
            ?.getRoa(ingestion.administrationRoute)
            ?.roaDuration
        val fallbackEndHours = fallbackEliminationEndHours(
            ingestion = ingestion,
            roaDuration = roaDuration
        )
        val model = ConcentrationEstimator.resolve(
            recordedModel = substance?.getConcentrationModel(
                ingestion.administrationRoute
            ),
            roaDuration = roaDuration,
            ingestionDose = ingestion.dose,
            ingestionUnits = ingestion.units,
            fallbackEliminationEndHours = fallbackEndHours,
            administrationRoute = ingestion.administrationRoute
        )
        if (model != null) {
            val elapsedHours =
                Duration.between(ingestion.time, now).seconds / 3600.0
            val relativeRange = model.estimateRelativePercentAt(
                elapsedHours = elapsedHours,
                ingestionDose = ingestion.dose,
                ingestionUnits = ingestion.units,
                fallbackEliminationEndHours = fallbackEndHours,
                extendedReleaseProfile = ingestion.extendedReleaseProfile
            )
            if (relativeRange != null) {
                return relativeRange.endInclusive >= ACTIVE_REFERENCE_PEAK_PERCENT
            }
        }

        val durationEnd = roaDuration?.total?.let {
            it.maxInSec ?: it.minInSec
        }?.toLong()
        val expectedEnd = ingestion.endTime ?: durationEnd?.let(ingestion.time::plusSeconds)
        return expectedEnd?.isAfter(now) == true
    }

    private fun fallbackEliminationEndHours(
        ingestion: Ingestion,
        roaDuration: RoaDuration?
    ): Double? {
        val explicitHours = ingestion.endTime?.let {
            Duration.between(ingestion.time, it).seconds / 3600.0
        }?.takeIf { it > 0.0 }
        val durationHours = roaDuration?.total?.let {
            it.maxInSec?.div(3600.0) ?: it.minInSec?.div(3600.0)
        }?.takeIf { it > 0.0 }
        return listOfNotNull(explicitHours, durationHours).maxOrNull()
    }

    private companion object {
        const val ACTIVE_REFERENCE_PEAK_PERCENT = 0.1
    }
}
