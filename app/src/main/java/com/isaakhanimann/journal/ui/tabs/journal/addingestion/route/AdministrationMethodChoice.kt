package com.isaakhanimann.journal.ui.tabs.journal.addingestion.route

import com.isaakhanimann.journal.data.substances.AdministrationRoute

data class AdministrationMethodChoice(
    val targetSubstanceName: String,
    val route: AdministrationRoute,
    val methodLabel: String? = null,
    val activeIngredient: String? = null,
    val note: String? = null
)
