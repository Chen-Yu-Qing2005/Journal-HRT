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

package com.isaakhanimann.journal.ui.tabs.journal

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.isaakhanimann.journal.data.achievement.AchievementEvaluator
import com.isaakhanimann.journal.data.achievement.AchievementGetToast
import com.isaakhanimann.journal.data.room.experiences.relations.ExperienceWithIngestionsCompanionsAndRatings
import com.isaakhanimann.journal.data.substances.repositories.SubstanceRepository
import com.isaakhanimann.journal.localization.i18n
import com.isaakhanimann.journal.localization.i18nOrDefault
import com.isaakhanimann.journal.ui.tabs.journal.components.ExperienceRow
import com.isaakhanimann.journal.ui.tabs.journal.components.JournalSatelliteFab
import com.isaakhanimann.journal.ui.main.navigation.routes.IngestionSaveMode
import com.isaakhanimann.journal.ui.main.MainTabTopAppBar
import com.isaakhanimann.journal.ui.main.bottomBarNestedScroll
import com.isaakhanimann.journal.ui.main.bottomBarOverlayDp
import com.isaakhanimann.journal.ui.main.bottomBarOverlayPadding
import com.isaakhanimann.journal.ui.tabs.stats.EmptyScreenDisclaimer
import com.isaakhanimann.journal.ui.theme.JournalTheme
import com.isaakhanimann.journal.ui.theme.horizontalPadding
import com.isaakhanimann.journal.ui.utils.JournalHrtLinks
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun JournalScreen(
    navigateToExperiencePopNothing: (experienceId: Int) -> Unit,
    navigateToAddIngestion: (IngestionSaveMode) -> Unit,
    navigateToCalendar: () -> Unit,
    navigateToQuickTimedNote: (experienceId: Int) -> Unit,
    navigateToTreatmentHistory: () -> Unit = {},
    viewModel: JournalViewModel = hiltViewModel()
) {
    val experiences = viewModel.experiences.collectAsState().value

    val achievements by viewModel.achievementsFlow.collectAsState(initial = null as List<String>?)
    val ingestions by viewModel.ingestionsFlow.collectAsState(initial = emptyList())
    val activeTreatmentNames by viewModel.activeTreatmentNamesFlow.collectAsState()
    val ownerUserName = viewModel.ownerUserNameFlow.collectAsState().value
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(ingestions, achievements, ownerUserName) {
        // Skip until DataStore has loaded: with the initial emptyList() a cold start
        // re-triggered already-unlocked achievements (toast + event) before the
        // persisted list arrived.
        val achieved = achievements ?: return@LaunchedEffect
        val ownerName = ownerUserName ?: return@LaunchedEffect
        for (definition in viewModel.achievementDefinitions) {
            if (
                !achieved.contains(definition.registerName) &&
                AchievementEvaluator.evaluate(definition, ingestions, ownerName)
            ) {
                viewModel.addAchievement(definition.registerName)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.maybeMigrate()
    }

    JournalScreen(
        navigateToExperiencePopNothing = navigateToExperiencePopNothing,
        navigateToAddIngestion = {
            viewModel.resetAddIngestionTimes()
            navigateToAddIngestion(it)
        },
        navigateToCalendar = navigateToCalendar,
        isFavoriteEnabled = viewModel.isFavoriteEnabledFlow.collectAsState().value,
        onChangeIsFavorite = viewModel::onChangeFavorite,
        isTimeRelativeToNow = viewModel.isTimeRelativeToNow.value,
        onChangeIsRelative = viewModel::onChangeRelative,
        searchText = viewModel.searchTextFlow.collectAsState().value,
        onChangeSearchText = viewModel::search,
        isSearchEnabled = viewModel.isSearchEnabled.value,
        onChangeIsSearchEnabled = viewModel::onChangeOfIsSearchEnabled,
        experiences = experiences,
        ingestions = ingestions,
        substanceRepository = viewModel.substanceRepository,
        ownerUserName = ownerUserName ?: "You",
        navigateToQuickTimedNote = navigateToQuickTimedNote,
        navigateToTreatmentHistory = navigateToTreatmentHistory,
        onOpenRepository = {
            uriHandler.openUri(JournalHrtLinks.REPOSITORY)
        },
        activeTreatmentNames = activeTreatmentNames
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    navigateToExperiencePopNothing: (experienceId: Int) -> Unit,
    navigateToAddIngestion: (IngestionSaveMode) -> Unit,
    navigateToCalendar: () -> Unit,
    isFavoriteEnabled: Boolean,
    onChangeIsFavorite: (Boolean) -> Unit,
    isTimeRelativeToNow: Boolean,
    onChangeIsRelative: (Boolean) -> Unit,
    searchText: String,
    onChangeSearchText: (String) -> Unit,
    isSearchEnabled: Boolean,
    onChangeIsSearchEnabled: (Boolean) -> Unit,
    experiences: List<ExperienceWithIngestionsCompanionsAndRatings>,
    ingestions: List<com.isaakhanimann.journal.data.room.experiences.entities.Ingestion> = emptyList(),
    substanceRepository: SubstanceRepository?,
    ownerUserName: String,
    navigateToQuickTimedNote: (experienceId: Int) -> Unit = {},
    navigateToTreatmentHistory: () -> Unit = {},
    onOpenRepository: () -> Unit = {},
    activeTreatmentNames: List<String> = emptyList()
) {
    val latestExperienceId = experiences
        .firstOrNull { it.ingestionsWithCompanions.isNotEmpty() }
        ?.experience?.id
    var isSatelliteExpanded by rememberSaveable { mutableStateOf(false) }
    var isChoosingIngestionSaveMode by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isSatelliteExpanded) {
        isSatelliteExpanded = false
    }

    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Box(modifier = Modifier.fillMaxSize()) {
        com.isaakhanimann.journal.ui.utils.JournalAmbientBackground()
        Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier
            .bottomBarNestedScroll()
            .nestedScroll(topBarScrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MainTabTopAppBar(
                title = i18n("journal"),
                scrollBehavior = topBarScrollBehavior,
                actions = {
                    IconButton(onClick = navigateToCalendar) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = i18n("journal_navigate_to_calendar")
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            JournalSatelliteFab(
                isExpanded = isSatelliteExpanded,
                onExpandedChange = { isSatelliteExpanded = it },
                onSingleClick = { isChoosingIngestionSaveMode = true },
                isTimeRelativeToNow = isTimeRelativeToNow,
                onChangeIsRelative = onChangeIsRelative,
                isFavoriteEnabled = isFavoriteEnabled,
                onChangeIsFavorite = onChangeIsFavorite,
                isSearchEnabled = isSearchEnabled,
                onChangeIsSearchEnabled = onChangeIsSearchEnabled,
                latestExperienceId = latestExperienceId,
                navigateToQuickTimedNote = navigateToQuickTimedNote
            )
        }
    ) { padding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AchievementGetToast(
                modifier = Modifier.align(Alignment.TopEnd)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                AnimatedVisibility(visible = isSearchEnabled) {
                    Column {
                        val focusManager = LocalFocusManager.current
                        TextField(
                            value = searchText,
                            onValueChange = onChangeSearchText,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = i18n("common_search")
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (searchText.isNotEmpty()) {
                                            onChangeSearchText("")
                                        } else {
                                            onChangeIsSearchEnabled(false)
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = if (searchText.isNotEmpty()) {
                                            i18n("common_close")
                                        } else {
                                            i18n("journal_search_off")
                                        }
                                    )
                                }
                            },
                            label = { Text(text = i18n("journal_search_by_title_or_substance")) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPadding, vertical = 8.dp),
                            shape = MaterialTheme.shapes.large,
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                            }),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                            singleLine = true
                        )
                        if (experiences.isEmpty() && isSearchEnabled && searchText.isNotEmpty()) {
                            if (isFavoriteEnabled) {
                                Column(modifier = Modifier.padding(horizontalPadding)) {
                                    Text(
                                        text = i18n("journal_no_results"),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = i18n(
                                            "journal_no_favorite_experience_titles_match_search"
                                        ),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            } else {
                                Column(modifier = Modifier.padding(horizontalPadding)) {
                                    Text(
                                        text = i18n("journal_no_results"),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = i18n("journal_no_experience_titles_match_search"),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
                val listState = rememberLazyListState()
                val isScrollUpButtonShown by remember {
                    derivedStateOf {
                        listState.firstVisibleItemIndex > 0
                    }
                }
                Box(contentAlignment = Alignment.TopEnd) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = bottomBarOverlayPadding()
                    ) {
                        if (!isSearchEnabled) {
                            item {
                                TodayMedicationPanel(
                                    ingestions = ingestions,
                                    activeTreatmentNames = activeTreatmentNames,
                                    onOpenTreatmentHistory = navigateToTreatmentHistory,
                                    onOpenRepository = onOpenRepository
                                )
                            }
                        }
                        if (experiences.isNotEmpty()) {
                            item {
                                HorizontalDivider()
                            }
                        }
                        items(
                            experiences,
                            key = { it.experience.id }
                        ) { experienceWithIngestions ->
                            if (substanceRepository != null) {
                                ExperienceRow(
                                    experienceWithIngestions,
                                    navigateToExperienceScreen = {
                                        navigateToExperiencePopNothing(
                                            experienceWithIngestions.experience.id
                                        )
                                    },
                                    isTimeRelativeToNow = isTimeRelativeToNow,
                                    substanceRepository = substanceRepository,
                                    ownerUserName = ownerUserName
                                )
                            } else {
                                JournalExperiencePreviewContent(experienceWithIngestions)
                            }
                            HorizontalDivider()
                        }
                    }
                    this@Column.AnimatedVisibility(visible = isScrollUpButtonShown) {
                        val scope = rememberCoroutineScope()
                        ElevatedButton(
                            modifier = Modifier.padding(all = horizontalPadding),
                            onClick = {
                                scope.launch {
                                    listState.scrollToItem(index = 0)
                                }
                            }) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = i18nOrDefault("scroll_to_top", "Scroll to top"))
                        }
                    }
                }
            }
            if (experiences.isEmpty() && ingestions.isEmpty() &&
                activeTreatmentNames.isEmpty() && !isSearchEnabled
            ) {
                if (isFavoriteEnabled) {
                    EmptyScreenDisclaimer(
                        title = i18n("journal_no_favorites"),
                        description = i18n("journal_no_favorites_description")
                    )
                } else {
                    EmptyScreenDisclaimer(
                        title = i18n("journal_empty_title"),
                        description = i18n("journal_empty_description")
                    )
                }
            }
            AnimatedVisibility(
                visible = isSatelliteExpanded,
                enter = fadeIn(animationSpec = tween(durationMillis = 180)),
                exit = fadeOut(animationSpec = tween(durationMillis = 150))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.24f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isSatelliteExpanded = false
                        }
                )
            }
        }
        if (isChoosingIngestionSaveMode) {
            AlertDialog(
                onDismissRequest = { isChoosingIngestionSaveMode = false },
                title = {
                    Text(i18n("add_ingestion_save_mode_title"))
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(i18n("add_ingestion_save_mode_description"))
                        TextButton(
                            onClick = {
                                isChoosingIngestionSaveMode = false
                                navigateToAddIngestion(IngestionSaveMode.SEPARATE)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(i18n("add_ingestion_mode_separate"))
                                Text(
                                    text = i18n(
                                        "add_ingestion_mode_separate_description"
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                isChoosingIngestionSaveMode = false
                                navigateToAddIngestion(IngestionSaveMode.MERGE)
                            },
                            enabled = latestExperienceId != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(i18n("add_ingestion_mode_merge"))
                                Text(
                                    text = i18n(
                                        if (latestExperienceId == null) {
                                            "add_ingestion_mode_merge_unavailable"
                                        } else {
                                            "add_ingestion_mode_merge_description"
                                        }
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { isChoosingIngestionSaveMode = false }
                    ) {
                        Text(i18n("common_cancel"))
                    }
                }
            )
        }
        }
    }
}

@Composable
private fun TodayMedicationPanel(
    ingestions: List<com.isaakhanimann.journal.data.room.experiences.entities.Ingestion>,
    activeTreatmentNames: List<String>,
    onOpenTreatmentHistory: () -> Unit,
    onOpenRepository: () -> Unit
) {
    val today = LocalDate.now()
    val todayIngestions = ingestions.filter {
        it.time.atZone(ZoneId.systemDefault()).toLocalDate() == today
    }
    val todayNames = todayIngestions.map { it.substanceName }.distinct()
    val lastIngestion = todayIngestions.maxByOrNull { it.time }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = i18n("today_medication_title"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = today.format(DateTimeFormatter.ISO_LOCAL_DATE),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (todayNames.isEmpty()) {
                    i18n("today_no_ingestions")
                } else {
                    i18n(
                        "today_ingestions",
                        mapOf("substances" to todayNames.joinToString("、"))
                    )
                }
            )
            Text(
                text = if (activeTreatmentNames.isEmpty()) {
                    i18n("today_no_active_treatments")
                } else {
                    i18n(
                        "today_active_treatments",
                        mapOf("substances" to activeTreatmentNames.joinToString("、"))
                    )
                }
            )
            lastIngestion?.let {
                Text(
                    text = i18n(
                        "today_last_dose",
                        mapOf(
                            "time" to it.time.atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("HH:mm"))
                        )
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onOpenTreatmentHistory) {
                Text(
                    text = i18n("today_open_treatment_history"),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            TextButton(
                onClick = onOpenRepository,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = i18n("repository_home"),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun JournalExperiencePreviewContent(
    experience: ExperienceWithIngestionsCompanionsAndRatings
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = 8.dp)) {
        Text(text = experience.experience.title, style = MaterialTheme.typography.titleSmall)
        Text(
            text = experience.ingestionsWithCompanions.joinToString { it.ingestion.substanceName },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JournalScreenPreview(
    @PreviewParameter(JournalScreenPreviewProvider::class)
    experiences: List<ExperienceWithIngestionsCompanionsAndRatings>
) {
    JournalTheme {
        JournalScreen(
            navigateToExperiencePopNothing = {},
            navigateToAddIngestion = { _ -> },
            navigateToCalendar = {},
            isFavoriteEnabled = false,
            onChangeIsFavorite = {},
            isTimeRelativeToNow = true,
            onChangeIsRelative = {},
            searchText = "",
            onChangeSearchText = {},
            isSearchEnabled = false,
            onChangeIsSearchEnabled = {},
            experiences = experiences,
            substanceRepository = null,
            ownerUserName = "You"
        )
    }
}
