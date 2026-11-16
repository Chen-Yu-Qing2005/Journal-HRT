/*
 * Copyright (c) 2022-2023. Isaak Hanimann.
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

package com.isaakhanimann.journal.ui.tabs.journal.addingestion.time

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.data.room.experiences.entities.AdaptiveColor
import com.isaakhanimann.journal.data.room.experiences.relations.ExperienceWithIngestions
import com.isaakhanimann.journal.data.substances.ReleaseForm
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.localization.i18n
import com.isaakhanimann.journal.ui.tabs.journal.experience.components.CardWithTitle
import com.isaakhanimann.journal.ui.tabs.journal.experience.rating.FloatingDoneButton
import com.isaakhanimann.journal.ui.theme.horizontalPadding
import java.time.LocalDateTime
import kotlinx.coroutines.launch

@Composable
fun FinishIngestionScreen(
    dismissAddIngestionScreens: () -> Unit,
    viewModel: FinishIngestionScreenViewModel
) {
    val localDateTime = viewModel.localDateTimeStartFlow.collectAsState().value
    val view = androidx.compose.ui.platform.LocalView.current
    FinishIngestionScreen(
        createSaveAndDismissAfter = {
            viewModel.createSaveAndDismissAfter(
                dismiss = dismissAddIngestionScreens,
                lifecycleView = view
            )
        },
        ingestionTimePickerOption = viewModel.ingestionTimePickerOptionFlow.collectAsState().value,
        onChangeTimePickerOption = viewModel::onChangeTimePickerOption,
        onChangeStartDateOrTime = viewModel::onChangeStartDateOrTime,
        localDateTimeStart = localDateTime,
        localDateTimeEnd = viewModel.localDateTimeEndFlow.collectAsState().value,
        onChangeEndDateOrTime = viewModel::onChangeEndDateOrTime,
        onSelectDurationPreset = viewModel::onSelectDurationPreset,
        isLoadingColor = viewModel.isLoadingColor,
        isSubstanceMissing = viewModel.isSubstanceMissing,
        isShowingColorPicker = viewModel.isShowingColorPicker,
        selectedColor = viewModel.selectedColor,
        onChangeColor = { viewModel.selectedColor = it },
        alreadyUsedColors = viewModel.alreadyUsedColorsFlow.collectAsState().value,
        otherColors = viewModel.otherColorsFlow.collectAsState().value,
        previousNotes = viewModel.previousNotesFlow.collectAsState().value,
        note = viewModel.note,
        onNoteChange = {
            viewModel.note = it
        },
        experiencesInRange = viewModel.experiencesInRangeFlow.collectAsState().value,
        selectedExperience = viewModel.selectedExperienceFlow.collectAsState().value,
        onChangeOfSelectedExperience = viewModel::onChangeOfSelectedExperience,
        onChangeMergeWithExistingExperience =
            viewModel::onChangeMergeWithExistingExperience,
        substanceName = viewModel.substanceName,
        getSubstanceDisplayName = viewModel.substanceRepo::getDisplayName,
        enteredTitle = viewModel.enteredTitle,
        onChangeOfEnteredTitle = viewModel::changeTitle,
        isEnteredTitleOk = viewModel.isEnteredTitleOk,
        consumerName = viewModel.consumerName,
        onChangeOfConsumerName = viewModel::changeConsumerName,
        consumerNamesSorted = viewModel.sortedConsumerNamesFlow.collectAsState().value,
        availableReleaseForms = viewModel.availableReleaseForms,
        releaseForm = viewModel.releaseForm,
        onChangeReleaseForm = viewModel::changeReleaseForm,
        extendedReleaseInitialMinutes = viewModel.extendedReleaseInitialMinutesText,
        extendedReleaseInitialPercent = viewModel.extendedReleaseInitialPercentText,
        extendedReleaseSubsequentMinutes = viewModel.extendedReleaseSubsequentMinutesText,
        onChangeExtendedReleaseInitialMinutes = viewModel::changeExtendedReleaseInitialMinutes,
        onChangeExtendedReleaseInitialPercent = viewModel::changeExtendedReleaseInitialPercent,
        onChangeExtendedReleaseSubsequentMinutes =
            viewModel::changeExtendedReleaseSubsequentMinutes,
        isExtendedReleaseConfigurationValid = viewModel.isExtendedReleaseConfigurationValid,
        showApplicationMetadata = viewModel.hasApplicationMetadata,
        isInjectionRoute = viewModel.administrationRoute.isInjectionMethod,
        applicationSite = viewModel.applicationSite,
        onChangeApplicationSite = viewModel::changeApplicationSite,
        batchNumber = viewModel.batchNumber,
        onChangeBatchNumber = viewModel::changeBatchNumber,
        productExpiryDateText = viewModel.productExpiryDateText,
        onChangeProductExpiryDate = viewModel::changeProductExpiryDate,
        isApplicationMetadataValid = viewModel.isApplicationMetadataValid
    )
}

@Preview
@Composable
fun FinishIngestionScreenPreview() {
    val alreadyUsedColors = listOf(AdaptiveColor.BLUE, AdaptiveColor.PINK)
    val otherColors = AdaptiveColor.entries.filter { color ->
        !alreadyUsedColors.contains(color)
    }
    FinishIngestionScreen(
        createSaveAndDismissAfter = {},
        ingestionTimePickerOption = IngestionTimePickerOption.POINT_IN_TIME,
        onChangeTimePickerOption = {},
        onChangeStartDateOrTime = {},
        localDateTimeStart = LocalDateTime.now(),
        localDateTimeEnd = LocalDateTime.now(),
        onChangeEndDateOrTime = {},
        isLoadingColor = false,
        isSubstanceMissing = false,
        isShowingColorPicker = true,
        selectedColor = AdaptiveColor.BLUE,
        onChangeColor = {},
        alreadyUsedColors = alreadyUsedColors,
        otherColors = otherColors,
        previousNotes = listOf(
            "My previous note where I make some remarks",
            "Another previous note and this one is very long, such that it doesn't fit on one line"
        ),
        note = "",
        onNoteChange = {},
        experiencesInRange = emptyList(),
        selectedExperience = null,
        onChangeOfSelectedExperience = {},
        onChangeMergeWithExistingExperience = {},
        substanceName = "LSD",
        enteredTitle = "This is my title",
        onChangeOfEnteredTitle = {},
        isEnteredTitleOk = true,
        consumerName = "",
        onChangeOfConsumerName = {},
        consumerNamesSorted = listOf("Isaak", "Marc", "Eve"),
        showApplicationMetadata = false,
        isInjectionRoute = false,
        applicationSite = "",
        onChangeApplicationSite = {},
        batchNumber = "",
        onChangeBatchNumber = {},
        productExpiryDateText = "",
        onChangeProductExpiryDate = {},
        isApplicationMetadataValid = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishIngestionScreen(
    createSaveAndDismissAfter: () -> Unit,
    ingestionTimePickerOption: IngestionTimePickerOption,
    onChangeTimePickerOption: (option: IngestionTimePickerOption) -> Unit,
    onChangeStartDateOrTime: (LocalDateTime) -> Unit,
    localDateTimeStart: LocalDateTime,
    onChangeEndDateOrTime: (LocalDateTime) -> Unit,
    onSelectDurationPreset: (Long) -> Unit = {},
    localDateTimeEnd: LocalDateTime,
    isLoadingColor: Boolean,
    isSubstanceMissing: Boolean,
    isShowingColorPicker: Boolean,
    selectedColor: AdaptiveColor,
    onChangeColor: (AdaptiveColor) -> Unit,
    alreadyUsedColors: List<AdaptiveColor>,
    otherColors: List<AdaptiveColor>,
    previousNotes: List<String>,
    note: String,
    onNoteChange: (String) -> Unit,
    experiencesInRange: List<ExperienceWithIngestions>,
    selectedExperience: ExperienceWithIngestions?,
    onChangeOfSelectedExperience: (ExperienceWithIngestions?) -> Unit,
    onChangeMergeWithExistingExperience: (Boolean) -> Unit,
    substanceName: String,
    getSubstanceDisplayName: (String) -> String = { it },
    enteredTitle: String,
    onChangeOfEnteredTitle: (String) -> Unit,
    isEnteredTitleOk: Boolean,
    consumerName: String,
    onChangeOfConsumerName: (String) -> Unit,
    consumerNamesSorted: List<String>,
    availableReleaseForms: List<ReleaseForm> = emptyList(),
    releaseForm: ReleaseForm? = null,
    onChangeReleaseForm: (ReleaseForm?) -> Unit = {},
    extendedReleaseInitialMinutes: String = "",
    extendedReleaseInitialPercent: String = "",
    extendedReleaseSubsequentMinutes: String = "",
    onChangeExtendedReleaseInitialMinutes: (String) -> Unit = {},
    onChangeExtendedReleaseInitialPercent: (String) -> Unit = {},
    onChangeExtendedReleaseSubsequentMinutes: (String) -> Unit = {},
    isExtendedReleaseConfigurationValid: Boolean = true,
    showApplicationMetadata: Boolean = false,
    isInjectionRoute: Boolean = false,
    applicationSite: String = "",
    onChangeApplicationSite: (String) -> Unit = {},
    batchNumber: String = "",
    onChangeBatchNumber: (String) -> Unit = {},
    productExpiryDateText: String = "",
    onChangeProductExpiryDate: (String) -> Unit = {},
    isApplicationMetadataValid: Boolean = true
) {
    val focusManager = LocalFocusManager.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        i18n(
                            "substances_ingestion",
                            replacements = mapOf(
                                "substance" to getSubstanceDisplayName(substanceName)
                            )
                        )
                    )
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isLoadingColor && !isSubstanceMissing && substanceName.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingDoneButton(
                    onDone = createSaveAndDismissAfter,
                    modifier = Modifier.imePadding(),
                    enabled = isExtendedReleaseConfigurationValid && isApplicationMetadataValid
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            LinearProgressIndicator(
                progress = { 0.9f },
                modifier = Modifier.fillMaxWidth()
            )
            if (isSubstanceMissing) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = 8.dp)
                ) {
                    Text(
                        text = i18n("finish_substance_missing"),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding)
            ) {
                Spacer(modifier = Modifier.height(3.dp))
                CardWithTitle(title = i18n("common_time")) {
                    TimePointOrRangePicker(
                        onChangeTimePickerOption = onChangeTimePickerOption,
                        ingestionTimePickerOption = ingestionTimePickerOption,
                        localDateTimeStart = localDateTimeStart,
                        onChangeStartDateOrTime = onChangeStartDateOrTime,
                        localDateTimeEnd = localDateTimeEnd,
                        onChangeEndDateOrTime = onChangeEndDateOrTime,
                        showFutureQuickButtons = true,
                        onSelectDurationPreset = onSelectDurationPreset
                    )
                }
                if (showApplicationMetadata) {
                    CardWithTitle(
                        title = i18n("ingestion_application_details"),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            OutlinedTextField(
                                value = applicationSite,
                                onValueChange = onChangeApplicationSite,
                                label = {
                                    Text(
                                        i18n(
                                            if (isInjectionRoute) {
                                                "ingestion_injection_site"
                                            } else {
                                                "ingestion_application_site"
                                            }
                                        )
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = batchNumber,
                                onValueChange = onChangeBatchNumber,
                                label = { Text(i18n("ingestion_batch_number")) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = productExpiryDateText,
                                onValueChange = onChangeProductExpiryDate,
                                label = { Text(i18n("ingestion_product_expiry")) },
                                placeholder = { Text("YYYY-MM-DD") },
                                singleLine = true,
                                isError = !isApplicationMetadataValid,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                CardWithTitle(
                    title = i18n("common_experience"),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var isShowingDropDownMenu by remember { mutableStateOf(false) }
                    val shouldMergeWithExistingExperience = selectedExperience != null
                    if (experiencesInRange.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = i18n("finish_merge_into_existing_experience"),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = i18n("finish_merge_existing_explanation"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = shouldMergeWithExistingExperience,
                                onCheckedChange = onChangeMergeWithExistingExperience
                            )
                        }
                    }
                    if (!shouldMergeWithExistingExperience) {
                        Text(
                            text = i18n("finish_create_separate_experience"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (shouldMergeWithExistingExperience) {
                    Box(
                        modifier = Modifier
                            .wrapContentSize(Alignment.TopEnd)
                    ) {
                        OutlinedButton(
                            onClick = { isShowingDropDownMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val selectedExperienceTitle = selectedExperience?.experience?.title
                            Text(
                                text = if (selectedExperienceTitle !=
                                    null
                                ) {
                                    i18n(
                                        "finish_part_of_experience",
                                        mapOf(
                                            "experience" to selectedExperienceTitle
                                        )
                                    )
                                } else {
                                    i18n("finish_part_of_new_experience")
                                }
                            )
                        }
                        DropdownMenu(
                            expanded = isShowingDropDownMenu,
                            onDismissRequest = { isShowingDropDownMenu = false }
                        ) {
                            experiencesInRange.forEach { experienceWithIngestions ->
                                val experience = experienceWithIngestions.experience
                                DropdownMenuItem(
                                    text = { Text(experience.title) },
                                    onClick = {
                                        onChangeOfSelectedExperience(experienceWithIngestions)
                                        isShowingDropDownMenu = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(i18n("finish_new_experience")) },
                                onClick = {
                                    onChangeOfSelectedExperience(null)
                                    isShowingDropDownMenu = false
                                }
                            )
                        }
                    }
                    }
                    AnimatedVisibility(visible = selectedExperience == null) {
                        OutlinedTextField(
                            value = enteredTitle,
                            onValueChange = onChangeOfEnteredTitle,
                            singleLine = true,
                            label = { Text(text = i18n("common_new_experience_title")) },
                            isError = !isEnteredTitleOk,
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                            }),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done,
                                capitalization = KeyboardCapitalization.Words
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 3.dp)
                        )
                    }
                }
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = horizontalPadding,
                            vertical = 3.dp
                        )
                    ) {
                        Text(
                            text = i18n(
                                "consumed_by",
                                mapOf(
                                    "name" to consumerName.ifBlank { i18n("you") }
                                )
                            ),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (consumerNamesSorted.isNotEmpty() || consumerName.isNotBlank()) {
                            var areConsumerNamesExpanded by remember { mutableStateOf(false) }
                            TextButton(onClick = { areConsumerNamesExpanded = true }) {
                                Text(text = i18n("finish_choose_other_consumer"))
                            }
                            DropdownMenu(
                                expanded = areConsumerNamesExpanded,
                                onDismissRequest = { areConsumerNamesExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(i18n("you")) },
                                    onClick = {
                                        onChangeOfConsumerName("")
                                        areConsumerNamesExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = i18n("stats_consumer")
                                        )
                                    }
                                )
                                consumerNamesSorted.forEach { consumerName ->
                                    DropdownMenuItem(
                                        text = { Text(consumerName) },
                                        onClick = {
                                            onChangeOfConsumerName(consumerName)
                                            areConsumerNamesExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = i18n("stats_consumer")
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        var showNewConsumerTextField by remember { mutableStateOf(false) }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = showNewConsumerTextField,
                                onCheckedChange = {
                                    showNewConsumerTextField = !showNewConsumerTextField
                                }
                            )
                            Text(i18n("common_enter_new_consumer"))
                        }
                        AnimatedVisibility(visible = showNewConsumerTextField) {
                            OutlinedTextField(
                                value = consumerName,
                                onValueChange = onChangeOfConsumerName,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = i18n("stats_consumer")
                                    )
                                },
                                keyboardActions = KeyboardActions(onDone = {
                                    focusManager.clearFocus()
                                }),
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    imeAction = ImeAction.Done,
                                    capitalization = KeyboardCapitalization.Words
                                ),
                                placeholder = { Text(i18n("common_new_consumer_name")) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
                CardWithTitle(title = i18n("finish_ingestion_note")) {
                    NoteSection(
                        previousNotes,
                        note,
                        onNoteChange
                    )
                }
                if (isShowingColorPicker) {
                    CardWithTitle(
                        title = i18n("finish_substance_color", mapOf("substance" to substanceName)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ColorPicker(
                            selectedColor = selectedColor,
                            onChangeOfColor = onChangeColor,
                            alreadyUsedColors = alreadyUsedColors,
                            otherColors = otherColors
                        )
                    }
                }
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteSection(previousNotes: List<String>, note: String, onNoteChange: (String) -> Unit) {
    var isShowingSuggestions by remember { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = BringIntoViewRequester()
    Column {
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            label = { Text(text = i18n("common_notes")) },
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                isShowingSuggestions = false
            }),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.Sentences
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusEvent { focusState ->
                    if (focusState.isFocused && focusState.hasFocus) {
                        isShowingSuggestions = true
                        coroutineScope.launch {
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                }

        )
        if (previousNotes.isNotEmpty() && isShowingSuggestions) {
            Column {
                previousNotes.forEach {
                    Row(
                        Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                            .clickable {
                                focusManager.clearFocus()
                                isShowingSuggestions = false
                                onNoteChange(it)
                            }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = i18n("common_copy"),
                                Modifier.size(15.dp)
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(
                                text = it,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
