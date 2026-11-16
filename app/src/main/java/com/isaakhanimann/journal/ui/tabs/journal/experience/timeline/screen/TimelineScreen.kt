/*
 * Copyright (c) 2023. Isaak Hanimann.
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

package com.isaakhanimann.journal.ui.tabs.journal.experience.timeline.screen

import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.localization.i18n
import com.isaakhanimann.journal.data.substances.classes.roa.ConcentrationUnit
import com.isaakhanimann.journal.ui.tabs.journal.experience.components.ExperienceEffectTimelines
import com.isaakhanimann.journal.ui.tabs.journal.experience.timeline.ConcentrationOverlayState
import com.isaakhanimann.journal.ui.tabs.journal.experience.timeline.DataForOneRating
import com.isaakhanimann.journal.ui.tabs.journal.experience.timeline.DataForOneTimedNote
import com.isaakhanimann.journal.ui.theme.JournalTheme
import com.isaakhanimann.journal.ui.theme.horizontalPadding
import com.isaakhanimann.journal.ui.utils.renderComposeViewToBitmap
import com.isaakhanimann.journal.ui.utils.shareBitmap
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun TimelineScreen(viewModel: TimelineScreenViewModel) {
    val timelineScreenModel = TimelineScreenModel(
        title = viewModel.consumerName,
        ingestionElements = viewModel.ingestionElementsFlow.collectAsState().value,
        ratings = viewModel.ratingsFlow.collectAsState().value,
        timedNotes = viewModel.timedNotesFlow.collectAsState().value,
        areSubstanceHeightsIndependent =
            viewModel.areSubstanceHeightsIndependentFlow.collectAsState().value
    )
    TimelineScreen(timelineScreenModel)
}

@Preview(showBackground = true)
@Composable
private fun TimelineScreenPreview(
    @PreviewParameter(TimelineScreenModelPreviewProvider::class)
    timelineScreenModel: TimelineScreenModel
) {
    JournalTheme {
        TimelineScreen(timelineScreenModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(timelineScreenModel: TimelineScreenModel) {
    val context = LocalContext.current
    val currentView = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    var isSharing by remember { mutableStateOf(false) }
    val widthPx = (LocalConfiguration.current.screenWidthDp * LocalDensity.current.density).toInt()
    val shareTimelineContent: @Composable () -> Unit = {
        JournalTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = horizontalPadding,
                        vertical = 12.dp
                    )
                ) {
                    Text(
                        text = timelineScreenModel.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExperienceEffectTimelines(
                        ingestionElements = timelineScreenModel.ingestionElements,
                        dataForRatings = timelineScreenModel.ratings.mapNotNull {
                            val ratingTime = it.time
                            return@mapNotNull if (ratingTime == null) {
                                null
                            } else {
                                DataForOneRating(
                                    time = ratingTime,
                                    option = it.option
                                )
                            }
                        },
                        dataForTimedNotes = timelineScreenModel.timedNotes.filter {
                            it.isPartOfTimeline
                        }
                            .map {
                                DataForOneTimedNote(time = it.time, color = it.color)
                            },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        areSubstanceHeightsIndependent =
                            timelineScreenModel.areSubstanceHeightsIndependent
                    )
                }
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(timelineScreenModel.title) },
                actions = {
                    IconButton(
                        onClick = {
                            if (!isSharing) {
                                isSharing = true
                                coroutineScope.launch {
                                    try {
                                        val activity = context as? androidx.activity.ComponentActivity
                                        if (activity != null) {
                                            val bitmap = renderComposeViewToBitmap(
                                                context = context,
                                                widthPx = widthPx,
                                                lifecycleView = currentView,
                                                content = shareTimelineContent,
                                                postLayoutDelayMs = 300L
                                            )
                                            shareBitmap(context, bitmap)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("TimelineScreen", "error", e)
                                        Toast.makeText(
                                            context,
                                            "${e.localizedMessage}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } finally {
                                        isSharing = false
                                    }
                                }
                            }
                        },
                        enabled = !isSharing
                    ) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = i18n("common_share"),
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(top = 3.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.toFloat()
            var canvasWidth by remember { mutableFloatStateOf(screenWidth) }
            var concentrationOverlayState by remember {
                mutableStateOf<ConcentrationOverlayState?>(null)
            }
            var concentrationUnit by rememberSaveable {
                mutableStateOf(ConcentrationUnit.MG_PER_L)
            }
            var concentrationOverlayHeightPx by remember { mutableFloatStateOf(0f) }
            var isUnitMenuExpanded by remember { mutableStateOf(false) }
            val isOrientationPortrait =
                LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
            if (timelineScreenModel.ingestionElements.isEmpty() &&
                timelineScreenModel.ratings.isEmpty() &&
                timelineScreenModel.timedNotes.isEmpty()
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = i18n("timeline_screen_no_data"),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val density = LocalDensity.current
                val chartHeightPx = with(density) { maxHeight.toPx() }
                val overlayState = concentrationOverlayState
                val overlayTargetYPx = overlayState?.let { state ->
                    (
                        chartHeightPx * state.peakTopFraction -
                            concentrationOverlayHeightPx -
                            with(density) { 8.dp.toPx() }
                        ).coerceAtLeast(with(density) { 8.dp.toPx() })
                } ?: 0f
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    ExperienceEffectTimelines(
                        ingestionElements = timelineScreenModel.ingestionElements,
                        dataForRatings = timelineScreenModel.ratings.mapNotNull {
                            val ratingTime = it.time
                            return@mapNotNull if (ratingTime == null) {
                                null
                            } else {
                                DataForOneRating(
                                    time = ratingTime,
                                    option = it.option
                                )
                            }
                        },
                        dataForTimedNotes = timelineScreenModel.timedNotes.filter {
                            it.isPartOfTimeline
                        }
                            .map {
                                DataForOneTimedNote(time = it.time, color = it.color)
                            },
                        modifier = Modifier
                            .fillMaxHeight(if (isOrientationPortrait) 0.5f else 1f)
                            .width(canvasWidth.dp)
                            .padding(horizontal = horizontalPadding),
                        areSubstanceHeightsIndependent =
                            timelineScreenModel.areSubstanceHeightsIndependent,
                        externalConcentrationUnit = concentrationUnit,
                        onConcentrationUnitChange = { concentrationUnit = it },
                        onConcentrationOverlayChanged = { concentrationOverlayState = it }
                    )
                }
                if (overlayState != null && overlayState.seriesCount > 0) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset {
                                IntOffset(x = 0, y = overlayTargetYPx.roundToInt())
                            }
                            .onSizeChanged { concentrationOverlayHeightPx = it.height.toFloat() }
                            .padding(end = 12.dp)
                            .widthIn(max = 190.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        if (overlayState.hasVerifiedAbsoluteConcentration) {
                            Box {
                                Surface(
                                    onClick = { isUnitMenuExpanded = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "${concentrationUnit.label} ▾",
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                DropdownMenu(
                                    expanded = isUnitMenuExpanded,
                                    onDismissRequest = { isUnitMenuExpanded = false }
                                ) {
                                    ConcentrationUnit.entries.forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text(unit.label) },
                                            onClick = {
                                                concentrationUnit = unit
                                                isUnitMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = i18n("concentration_relative_no_cmax"),
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                        if (overlayState.labels.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                tonalElevation = 3.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 7.dp
                                    ),
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    val isDarkTheme = isSystemInDarkTheme()
                                    overlayState.labels.forEach { label ->
                                        Text(
                                            text = label.text,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = label.color.getComposeColor(isDarkTheme),
                                            textAlign = TextAlign.End,
                                            softWrap = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
            Slider(
                value = canvasWidth,
                onValueChange = { value ->
                    canvasWidth = value
                },
                valueRange = screenWidth..(20f * screenWidth),
                modifier = Modifier.padding(horizontal = 30.dp)
            )
        }
    }
}
