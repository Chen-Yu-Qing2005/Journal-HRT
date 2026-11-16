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

package com.isaakhanimann.journal.data.substances.classes

import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.ReleaseForm
import com.isaakhanimann.journal.data.substances.RegulatorySchedule
import com.isaakhanimann.journal.data.substances.classes.roa.DurationRange
import com.isaakhanimann.journal.data.substances.classes.roa.DurationUnits
import com.isaakhanimann.journal.data.substances.classes.roa.ExtendedReleasePreset
import com.isaakhanimann.journal.data.substances.classes.roa.ExtendedReleaseProfile
import com.isaakhanimann.journal.data.substances.classes.roa.Roa
import com.isaakhanimann.journal.data.substances.classes.roa.RoaDuration
import com.isaakhanimann.journal.data.substances.classes.roa.ConcentrationModel
import com.isaakhanimann.journal.ui.utils.getInteractionExplanationURLForSubstance

data class Substance(
    val name: String,
    val localizedName: String?,
    val commonNames: List<String>,
    val url: String,
    val isApproved: Boolean,
    val tolerance: Tolerance?,
    val crossTolerances: List<String>,
    val addictionPotential: String?,
    val toxicities: List<String>,
    val categories: List<String>,
    val summary: String?,
    val effectsSummary: String?,
    val dosageRemark: String?,
    val generalRisks: String?,
    val longtermRisks: String?,
    val saferUse: List<String>,
    val interactions: Interactions?,
    val roas: List<Roa>,
    val concentrationModels: Map<AdministrationRoute, ConcentrationModel> = emptyMap(),
    val metabolism: String? = null,
    val metabolismSources: List<String> = emptyList(),
    val oralReleaseForms: List<ReleaseForm> = emptyList(),
    val extendedReleasePreset: ExtendedReleasePreset? = null,
    val administrationMethodGroup: AdministrationMethodGroup? = null,
    val regulatorySchedules: List<RegulatorySchedule> = emptyList()
) {
    fun getRoa(
        route: AdministrationRoute,
        releaseForm: ReleaseForm? = null,
        extendedReleaseProfile: ExtendedReleaseProfile? = null
    ): Roa? {
        val roa = roas.firstOrNull { it.route == route } ?: return null
        if (releaseForm != ReleaseForm.EXTENDED_RELEASE) return roa
        val profile = extendedReleaseProfile?.takeIf { it.isValid }
            ?: getExtendedReleasePreset(route)?.profile
            ?: return null
        val initialMinutes = profile.initialMinutes.toFloat()
        val totalMinutes = profile.totalMinutes.toFloat()
        val originalTotal = roa.roaDuration?.total
        val originalTotalMin = originalTotal?.min ?: 0f
        val originalTotalMax = originalTotal?.max ?: originalTotalMin
        val totalMin = (originalTotalMin + totalMinutes).coerceAtLeast(totalMinutes)
        val totalMax = (originalTotalMax + totalMinutes).coerceAtLeast(totalMin)
        return roa.copy(
            roaDuration = RoaDuration(
                onset = DurationRange(0f, initialMinutes, DurationUnits.MINUTES),
                comeup = DurationRange(initialMinutes, totalMinutes, DurationUnits.MINUTES),
                peak = DurationRange(initialMinutes, totalMinutes, DurationUnits.MINUTES),
                offset = DurationRange(totalMinutes, totalMax, DurationUnits.MINUTES),
                total = DurationRange(totalMin, totalMax, DurationUnits.MINUTES),
                afterglow = roa.roaDuration?.afterglow
            )
        )
    }

    fun getReleaseForms(route: AdministrationRoute): List<ReleaseForm> =
        if (route == AdministrationRoute.ORAL) oralReleaseForms else emptyList()

    fun getExtendedReleasePreset(route: AdministrationRoute): ExtendedReleasePreset? =
        if (route == AdministrationRoute.ORAL) {
            extendedReleasePreset?.takeIf { it.profile.isValid }
        } else {
            null
        }

    fun getConcentrationModel(route: AdministrationRoute): ConcentrationModel? =
        concentrationModels[route]

    val hasInteractions: Boolean get() {
        return if (interactions == null) {
            false
        } else {
            interactions.uncertain.isNotEmpty() ||
                interactions.unsafe.isNotEmpty() ||
                interactions.dangerous.isNotEmpty()
        }
    }

    val isHallucinogen
        get() = categories.any {
            val hallucinogens = setOf(
                "hallucinogen",
                "psychedelic",
                "dissociative",
                "deliriant"
            )
            hallucinogens.contains(it.lowercase())
        }
    val isStimulant
        get() = categories.any {
            val stimulants = setOf(
                "stimulant"
            )
            stimulants.contains(it.lowercase())
        }

    val interactionExplanationURL get() = getInteractionExplanationURLForSubstance(url)

    val displayName: String
        get() = localizedName ?: name
}
