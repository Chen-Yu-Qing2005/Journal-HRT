/*
 * Copyright (c) 2026.
 * This file is part of PsychonautWiki Journal.
 *
 * PsychonautWiki Journal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.isaakhanimann.journal.data.room.experiences.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import java.time.Instant

enum class TreatmentStatus {
    PLANNED,
    ACTIVE,
    PAUSED,
    STOPPED,
    AS_NEEDED;

    companion object {
        fun fromName(name: String?): TreatmentStatus =
            entries.firstOrNull { it.name == name } ?: ACTIVE
    }
}

/**
 * Append-only treatment history. The latest event for a substance and route is the
 * current treatment state; earlier events remain visible as dose or status changes.
 */
@Entity
data class TreatmentEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val substanceName: String,
    val administrationRoute: AdministrationRoute,
    val status: String,
    val dose: Double? = null,
    val units: String? = null,
    val reason: String? = null,
    val notes: String? = null,
    val effectiveAt: Instant = Instant.now(),
    val creationDate: Instant = Instant.now()
) {
    val treatmentStatus: TreatmentStatus
        get() = TreatmentStatus.fromName(status)
}
