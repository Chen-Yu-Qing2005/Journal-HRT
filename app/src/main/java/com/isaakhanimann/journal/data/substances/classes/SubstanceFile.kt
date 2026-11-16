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

package com.isaakhanimann.journal.data.substances.classes

class SubstanceFile(categories: List<Category>, substances: List<Substance>) {
    val categories: List<Category> = categories
    val substances: List<Substance> = substances
    val substancesMap: Map<String, Substance> = substances.associateBy { it.name }
    val categoriesMap: Map<String, Category> = categories.associateBy { it.name }
    val visibleSubstances: List<Substance> by lazy {
        val groupOwners = substances
            .filter { it.administrationMethodGroup != null }
            .map { it.name }
            .toSet()
        val groupedTargets = substances
            .flatMap { it.administrationMethodGroup?.methods.orEmpty() }
            .map { it.substanceName }
            .toSet()
        val hiddenTargets = groupedTargets - groupOwners
        substances.filter { it.name !in hiddenTargets }
    }
    val allSubstancesWithCategories: List<SubstanceWithCategories> by lazy {
        visibleSubstances.map { substance ->
            SubstanceWithCategories(
                substance = substance,
                categories = substance.categories.mapNotNull { categoriesMap[it] }
            )
        }
    }
}
