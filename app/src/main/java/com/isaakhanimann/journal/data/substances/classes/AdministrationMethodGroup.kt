package com.isaakhanimann.journal.data.substances.classes

import com.isaakhanimann.journal.data.substances.AdministrationRoute

/**
 * A presentation-level grouping of related products.
 *
 * The referenced [AdministrationMethod.substanceName] remains the authoritative substance for
 * dosing, concentration modelling and journal storage. This lets a group show a single entry
 * without pretending that different formulations are chemically identical.
 */
data class AdministrationMethodGroup(
    val id: String,
    val title: String,
    val methods: List<AdministrationMethod>
)

data class AdministrationMethod(
    val label: String,
    val substanceName: String,
    val route: AdministrationRoute,
    val activeIngredient: String? = null,
    val note: String? = null
)
