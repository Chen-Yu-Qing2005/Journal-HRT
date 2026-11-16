/*
 * Copyright (c) 2022. Isaak Hanimann.
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

package com.isaakhanimann.journal.ui.tabs.journal.addingestion.route

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.repositories.SubstanceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = ChooseRouteViewModel.Factory::class)
class ChooseRouteViewModel @AssistedInject constructor(
    val substanceRepo: SubstanceRepository,
    @Assisted val substanceName: String
) : ViewModel() {
    val substance = substanceRepo.getSubstance(substanceName)

    var showOtherRoutes by mutableStateOf(false)
    val pwRoutes = substance?.roas?.map { it.route } ?: emptyList()
    val methodChoiceGroup = substance?.administrationMethodGroup
    val methodChoices: List<AdministrationMethodChoice> =
        methodChoiceGroup?.methods
            ?.filter { method ->
                method.route in (substanceRepo.getSubstance(method.substanceName)
                    ?.roas
                    ?.map { it.route }
                    .orEmpty())
            }
            ?.map { method ->
                AdministrationMethodChoice(
                    targetSubstanceName = method.substanceName,
                    route = method.route,
                    methodLabel = method.label,
                    activeIngredient = method.activeIngredient,
                    note = method.note
                )
            }
            .orEmpty()
    val isGroupedMethodPicker: Boolean =
        methodChoiceGroup != null && methodChoices.size > 1
    val usesWuXingPalette: Boolean =
        methodChoiceGroup?.id == "estradiol-methods"
    val groupTitle: String? = methodChoiceGroup?.title
    private val otherRoutes = AdministrationRoute.entries.filter { route ->
        !pwRoutes.contains(route)
    }
    val otherRoutesChunked = otherRoutes.chunked(2)

    var isShowingInjectionDialog by mutableStateOf(false)
    var currentChoice by mutableStateOf<AdministrationMethodChoice?>(null)

    @AssistedFactory
    interface Factory {
        fun create(substanceName: String): ChooseRouteViewModel
    }
}
