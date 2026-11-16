package com.isaakhanimann.journal

import com.isaakhanimann.journal.data.substances.RegulatorySchedule
import org.junit.Assert.assertEquals
import org.junit.Test

class TestRegulatorySchedule {

    @Test
    fun mapsKnownControlledMedicinesWithoutCollapsingJurisdictions() {
        assertEquals(
            listOf(RegulatorySchedule.CN_PSYCHOTROPIC_I),
            RegulatorySchedule.forSubstanceName("Methylphenidate")
        )
        assertEquals(
            listOf(RegulatorySchedule.CN_PSYCHOTROPIC_II),
            RegulatorySchedule.forSubstanceName("Alprazolam")
        )
        assertEquals(
            listOf(
                RegulatorySchedule.CN_NARCOTIC,
                RegulatorySchedule.CN_PSYCHOTROPIC_I,
                RegulatorySchedule.CN_PSYCHOTROPIC_II
            ),
            RegulatorySchedule.forSubstanceName("Oxycodone")
        )
        assertEquals(
            emptyList<RegulatorySchedule>(),
            RegulatorySchedule.forSubstanceName("Lemborexant")
        )
        assertEquals(
            listOf(RegulatorySchedule.CN_NARCOTIC),
            RegulatorySchedule.forSubstanceName("Fentanyl")
        )
        assertEquals(
            listOf(RegulatorySchedule.CN_NON_MEDICAL),
            RegulatorySchedule.forSubstanceName(
                substanceName = "Clonazolam",
                categories = listOf("benzodiazepine", "depressant")
            )
        )
        assertEquals(
            listOf(RegulatorySchedule.CN_NON_MEDICAL),
            RegulatorySchedule.forSubstanceName(
                substanceName = "Research fentanyl analogue",
                categories = listOf("opioid")
            )
        )
        assertEquals(
            listOf(RegulatorySchedule.CN_NON_MEDICAL),
            RegulatorySchedule.forSubstanceName(
                substanceName = "LSD",
                categories = listOf("psychedelic")
            )
        )
        assertEquals(
            listOf(RegulatorySchedule.CN_PSYCHOTROPIC_II),
            RegulatorySchedule.forSubstanceName(
                substanceName = "Caffeine",
                categories = listOf("stimulant")
            )
        )
    }

    @Test
    fun usesTheLatestMedicinalScheduleWithoutCategoryFallbacks() {
        assertEquals(
            listOf(RegulatorySchedule.CN_PSYCHOTROPIC_I),
            RegulatorySchedule.forSubstanceName("Amphetamine")
        )
        assertEquals(
            listOf(RegulatorySchedule.CN_PSYCHOTROPIC_I),
            RegulatorySchedule.forSubstanceName("Triazolam")
        )
        assertEquals(
            listOf(RegulatorySchedule.CN_PSYCHOTROPIC_II),
            RegulatorySchedule.forSubstanceName("Dextromethorphan")
        )
        assertEquals(
            listOf(RegulatorySchedule.CN_NON_MEDICAL),
            RegulatorySchedule.forSubstanceName("Clorazepate")
        )
        assertEquals(
            emptyList<RegulatorySchedule>(),
            RegulatorySchedule.forSubstanceName(
                substanceName = "Pyrazolam",
                categories = listOf("benzodiazepine")
            )
        )
        assertEquals(
            emptyList<RegulatorySchedule>(),
            RegulatorySchedule.forSubstanceName(
                substanceName = "Armodafinil",
                categories = listOf("stimulant", "eugeroic")
            )
        )
    }

    @Test
    fun marksOnlyTheThreeOfficialNonMedicalClassesByStructure() {
        assertEquals(
            listOf(RegulatorySchedule.CN_NON_MEDICAL),
            RegulatorySchedule.forSubstanceName("Cyclopentyl-Fentanyl")
        )
        assertEquals(
            listOf(RegulatorySchedule.CN_NON_MEDICAL),
            RegulatorySchedule.forSubstanceName("Isotonitazepyne")
        )
        assertEquals(
            listOf(RegulatorySchedule.CN_NON_MEDICAL),
            RegulatorySchedule.forSubstanceName(
                substanceName = "JWH-210",
                categories = listOf("cannabinoid", "research-chemical")
            )
        )
        assertEquals(
            emptyList<RegulatorySchedule>(),
            RegulatorySchedule.forSubstanceName(
                substanceName = "Cannabidiol",
                categories = listOf("cannabinoid")
            )
        )
    }
}
