package com.isaakhanimann.journal.data.substances.classes.roa

data class ExtendedReleasePreset(
    val profile: ExtendedReleaseProfile,
    val formulation: String? = null,
    val source: String? = null,
    val note: String? = null
)
