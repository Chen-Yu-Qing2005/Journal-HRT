package com.isaakhanimann.journal

import androidx.compose.ui.graphics.Color
import com.isaakhanimann.journal.ui.tabs.search.CategoryModel
import com.isaakhanimann.journal.ui.tabs.search.SubstanceModel
import com.isaakhanimann.journal.ui.tabs.search.browse.SubstanceBrowseGroup
import com.isaakhanimann.journal.ui.tabs.search.browse.buildSubstanceBrowseSections
import com.isaakhanimann.journal.ui.tabs.search.browse.primaryBrowseGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class TestSubstanceBrowseSections {

    @Test
    fun assignsPsychiatricHrtAndRecreationalSectionsByPriority() {
        val substances = listOf(
            substance("Sertraline", "antidepressant", "ssri"),
            substance("Estradiol", "hormone", "estrogen"),
            substance("Alprazolam", "benzodiazepine", "depressant"),
            substance("Clonazolam", "benzodiazepine", "depressant"),
            substance("LSD", "psychedelic"),
            substance("Ketamine", "dissociative", "hallucinogen"),
            substance("Morphine", "opioid", "depressant"),
            substance("Caffeine", "stimulant"),
            substance("Cannabis", "cannabinoid"),
            substance("Alcohol", "depressant"),
            substance("4-MTA", "research-chemical", "entactogen", "tentative"),
            substance("Donepezil", "antidementia"),
            substance("Unclassified")
        )

        assertEquals(SubstanceBrowseGroup.PSYCHIATRIC, primaryBrowseGroup(substances[0]))
        assertEquals(SubstanceBrowseGroup.HRT, primaryBrowseGroup(substances[1]))
        assertEquals(SubstanceBrowseGroup.PSYCHIATRIC, primaryBrowseGroup(substances[2]))
        assertEquals(SubstanceBrowseGroup.BENZODIAZEPINE, primaryBrowseGroup(substances[3]))
        assertEquals(SubstanceBrowseGroup.PSYCHEDELIC, primaryBrowseGroup(substances[4]))
        assertEquals(SubstanceBrowseGroup.DISSOCIATIVE, primaryBrowseGroup(substances[5]))
        assertEquals(SubstanceBrowseGroup.OPIOID, primaryBrowseGroup(substances[6]))
        assertEquals(SubstanceBrowseGroup.STIMULANT, primaryBrowseGroup(substances[7]))
        assertEquals(SubstanceBrowseGroup.CANNABINOID, primaryBrowseGroup(substances[8]))
        assertEquals(SubstanceBrowseGroup.DEPRESSANT, primaryBrowseGroup(substances[9]))
        assertEquals(SubstanceBrowseGroup.OTHER, primaryBrowseGroup(substances[10]))
        assertEquals(SubstanceBrowseGroup.OTHER, primaryBrowseGroup(substances[11]))
        assertEquals(SubstanceBrowseGroup.OTHER, primaryBrowseGroup(substances[12]))

        val sections = buildSubstanceBrowseSections(substances)
        assertEquals(substances.size, sections.sumOf { it.substances.size })
        assertEquals(
            listOf(
                SubstanceBrowseGroup.PSYCHIATRIC,
                SubstanceBrowseGroup.HRT,
                SubstanceBrowseGroup.DISSOCIATIVE,
                SubstanceBrowseGroup.PSYCHEDELIC,
                SubstanceBrowseGroup.OPIOID,
                SubstanceBrowseGroup.BENZODIAZEPINE,
                SubstanceBrowseGroup.STIMULANT,
                SubstanceBrowseGroup.CANNABINOID,
                SubstanceBrowseGroup.DEPRESSANT,
                SubstanceBrowseGroup.OTHER
            ),
            sections.map { it.group }
        )
    }

    private fun substance(name: String, vararg categories: String) = SubstanceModel(
        name = name,
        displayName = name,
        commonNames = emptyList(),
        categories = categories.map { CategoryModel(rawName = it, color = Color.Gray) },
        hasSaferUse = false,
        hasInteractions = false
    )
}
