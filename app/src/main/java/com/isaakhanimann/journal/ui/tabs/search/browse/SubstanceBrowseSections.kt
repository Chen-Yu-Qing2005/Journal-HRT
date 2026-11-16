package com.isaakhanimann.journal.ui.tabs.search.browse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.localization.i18n
import com.isaakhanimann.journal.ui.tabs.search.SubstanceModel
import com.isaakhanimann.journal.ui.theme.horizontalPadding

enum class SubstanceBrowseGroup(val id: String, val titleKey: String) {
    PSYCHIATRIC("psychiatric", "substance_section_psychiatric"),
    HRT("hrt", "substance_section_hrt"),
    DISSOCIATIVE("dissociative", "substance_section_dissociative"),
    PSYCHEDELIC("psychedelic", "substance_section_psychedelic"),
    OPIOID("opioid", "substance_section_opioid"),
    BENZODIAZEPINE("benzodiazepine", "substance_section_benzodiazepine"),
    STIMULANT("stimulant", "substance_section_stimulant"),
    CANNABINOID("cannabinoid", "substance_section_cannabinoid"),
    DEPRESSANT("depressant", "substance_section_depressant"),
    OTHER("other", "substance_section_other")
}

data class SubstanceBrowseSection(
    val group: SubstanceBrowseGroup,
    val substances: List<SubstanceModel>
)

private val commonPsychiatricMedicationNames = setOf(
    "escitalopram",
    "sertraline",
    "citalopram",
    "paroxetine",
    "fluvoxamine",
    "fluoxetine",
    "venlafaxine",
    "desvenlafaxine",
    "duloxetine",
    "vortioxetine",
    "agomelatine",
    "trazodone",
    "mirtazapine",
    "bupropion",
    "tianeptine",
    "aripiprazole",
    "paliperidone",
    "lurasidone",
    "ziprasidone",
    "amisulpride",
    "clozapine",
    "quetiapine",
    "olanzapine",
    "risperidone",
    "haloperidol",
    "lithium",
    "lamotrigine",
    "valproic acid",
    "valpromide",
    "carbamazepine",
    "buspirone",
    "hydroxyzine",
    "gabapentin",
    "pregabalin",
    "atomoxetine",
    "methylphenidate",
    "lisdexamfetamine",
    "clonidine",
    "guanfacine",
    "alprazolam",
    "clonazepam",
    "diazepam",
    "lorazepam",
    "temazepam",
    "midazolam",
    "flunitrazepam",
    "zolpidem",
    "zopiclone",
    "eszopiclone",
    "suvorexant",
    "lemborexant",
    "daridorexant",
    "ramelteon",
    "tasimelteon"
)

private val hrtCategories = setOf(
    "hormone",
    "estrogen",
    "progestogen",
    "progestin",
    "antiandrogen",
    "gnrh",
    "gonadotropin-releasing-hormone"
)

private val hrtSubstanceNames = setOf(
    "estradiol",
    "estradiol gel",
    "estradiol patch",
    "estradiol (sublingual)",
    "bicalutamide",
    "cyproterone acetate",
    "dydrogesterone",
    "goserelin 3.6 mg implant",
    "goserelin 10.8 mg implant",
    "histrelin 50 mg implant",
    "medroxyprogesterone acetate",
    "progesterone",
    "spironolactone"
)

fun buildSubstanceBrowseSections(
    substances: List<SubstanceModel>
): List<SubstanceBrowseSection> =
    substances
        .groupBy(::primaryBrowseGroup)
        .toList()
        .sortedBy { (group, _) -> group.ordinal }
        .map { (group, groupedSubstances) ->
            SubstanceBrowseSection(
                group = group,
                substances = groupedSubstances.sortedBy { it.displayName.lowercase() }
            )
        }

internal fun primaryBrowseGroup(substance: SubstanceModel): SubstanceBrowseGroup {
    val categories = substance.categories.map { it.rawName.lowercase() }.toSet()
    val name = substance.name.lowercase()
    return when {
        name in commonPsychiatricMedicationNames -> SubstanceBrowseGroup.PSYCHIATRIC

        name in hrtSubstanceNames ||
            categories.any { it in hrtCategories } -> SubstanceBrowseGroup.HRT

        "dissociative" in categories -> SubstanceBrowseGroup.DISSOCIATIVE
        "psychedelic" in categories || "hallucinogen" in categories ->
            SubstanceBrowseGroup.PSYCHEDELIC

        "opioid" in categories -> SubstanceBrowseGroup.OPIOID
        "benzodiazepine" in categories -> SubstanceBrowseGroup.BENZODIAZEPINE
        "stimulant" in categories -> SubstanceBrowseGroup.STIMULANT
        "cannabinoid" in categories -> SubstanceBrowseGroup.CANNABINOID
        "depressant" in categories -> SubstanceBrowseGroup.DEPRESSANT
        else -> SubstanceBrowseGroup.OTHER
    }
}

@Composable
fun SubstanceBrowseDrawer(
    section: SubstanceBrowseSection,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    rowContent: @Composable (SubstanceModel) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = horizontalPadding,
                    vertical = 12.dp
                ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = i18n(section.group.titleKey),
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = section.substances.size.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Default.ExpandLess
                        } else {
                            Icons.Default.ExpandMore
                        },
                        contentDescription = null
                    )
                }
            }
        }
        AnimatedVisibility(visible = isExpanded) {
            Column {
                section.substances.forEachIndexed { index, substance ->
                    rowContent(substance)
                    if (index < section.substances.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
        HorizontalDivider()
    }
}
