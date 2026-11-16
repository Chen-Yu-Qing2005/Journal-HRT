package com.isaakhanimann.journal.ui.tabs.journal.addingestion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.data.substances.ReleaseForm
import com.isaakhanimann.journal.data.substances.classes.roa.ExtendedReleasePreset
import com.isaakhanimann.journal.data.substances.classes.roa.ExtendedReleaseProfile
import com.isaakhanimann.journal.localization.i18n
import com.isaakhanimann.journal.ui.tabs.journal.experience.components.CardWithTitle
import com.isaakhanimann.journal.ui.tabs.search.substance.roa.toReadableString

@Composable
fun ReleaseFormPicker(
    availableForms: List<ReleaseForm>,
    selectedForm: ReleaseForm?,
    onSelect: (ReleaseForm?) -> Unit,
    initialMinutes: String = "",
    initialPercent: String = "",
    subsequentMinutes: String = "",
    onInitialMinutesChange: (String) -> Unit = {},
    onInitialPercentChange: (String) -> Unit = {},
    onSubsequentMinutesChange: (String) -> Unit = {},
    preset: ExtendedReleasePreset? = null,
    wrapInCard: Boolean = true
) {
    if (availableForms.isEmpty() && selectedForm == null) return
    val content: @Composable () -> Unit = {
        ReleaseFormPickerContent(
            selectedForm = selectedForm,
            onSelect = onSelect,
            initialMinutes = initialMinutes,
            initialPercent = initialPercent,
            subsequentMinutes = subsequentMinutes,
            onInitialMinutesChange = onInitialMinutesChange,
            onInitialPercentChange = onInitialPercentChange,
            onSubsequentMinutesChange = onSubsequentMinutesChange,
            preset = preset
        )
    }
    if (wrapInCard) {
        CardWithTitle(title = i18n("release_form_title"), modifier = Modifier.fillMaxWidth()) {
            content()
        }
    } else {
        content()
    }
}

@Composable
private fun ReleaseFormPickerContent(
    selectedForm: ReleaseForm?,
    onSelect: (ReleaseForm?) -> Unit,
    initialMinutes: String,
    initialPercent: String,
    subsequentMinutes: String,
    onInitialMinutesChange: (String) -> Unit,
    onInitialPercentChange: (String) -> Unit,
    onSubsequentMinutesChange: (String) -> Unit,
    preset: ExtendedReleasePreset?
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    i18n("release_form_extended_switch"),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    i18n("release_form_extended_switch_description"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = selectedForm == ReleaseForm.EXTENDED_RELEASE,
                onCheckedChange = { isExtended ->
                    onSelect(
                        if (isExtended) {
                            ReleaseForm.EXTENDED_RELEASE
                        } else {
                            ReleaseForm.IMMEDIATE_RELEASE
                        }
                    )
                }
            )
        }
        Text(i18n("release_form_hint"), style = MaterialTheme.typography.bodySmall)
        if (selectedForm == ReleaseForm.EXTENDED_RELEASE) {
            val profile = ExtendedReleaseProfile.fromNullable(
                initialMinutes = initialMinutes.toDoubleOrNull(),
                initialPercent = initialPercent.toDoubleOrNull(),
                subsequentMinutes = subsequentMinutes.toDoubleOrNull()
            )
            if (preset != null) {
                preset.formulation?.takeIf { it.isNotBlank() }?.let { formulation ->
                    Text(
                        i18n(
                            "release_form_preset_formulation",
                            mapOf("formulation" to formulation)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Text(
                    i18n("release_form_preset_loaded"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = if (preset.formulation.isNullOrBlank()) 8.dp else 2.dp)
                )
                preset.source?.takeIf { it.isNotBlank() }?.let { source ->
                    Text(
                        i18n("release_form_preset_source", mapOf("source" to source)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                preset.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Text(
                        note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                i18n("release_form_extended_description"),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            OutlinedTextField(
                value = initialMinutes,
                onValueChange = onInitialMinutesChange,
                label = { Text(i18n("release_form_extended_x")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            OutlinedTextField(
                value = initialPercent,
                onValueChange = onInitialPercentChange,
                label = { Text(i18n("release_form_extended_y")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            OutlinedTextField(
                value = subsequentMinutes,
                onValueChange = onSubsequentMinutesChange,
                label = { Text(i18n("release_form_extended_z")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            if (profile != null) {
                Text(
                    i18n(
                        "release_form_extended_preview",
                        mapOf(
                            "x" to profile.initialMinutes.toReadableString(),
                            "y" to profile.initialPercent.toReadableString(),
                            "z" to profile.subsequentMinutes.toReadableString(),
                            "total" to profile.totalMinutes.toReadableString()
                        )
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (profile == null) {
                Text(
                    i18n("release_form_extended_invalid"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Text(
                i18n("release_form_no_reference"),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
