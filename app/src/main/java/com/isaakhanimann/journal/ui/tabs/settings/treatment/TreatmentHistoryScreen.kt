/*
 * Copyright (c) 2026.
 * This file is part of PsychonautWiki Journal.
 *
 * PsychonautWiki Journal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.isaakhanimann.journal.ui.tabs.settings.treatment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isaakhanimann.journal.data.room.experiences.ExperienceRepository
import com.isaakhanimann.journal.data.room.experiences.entities.TreatmentEvent
import com.isaakhanimann.journal.data.room.experiences.entities.TreatmentStatus
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.localization.i18n
import com.isaakhanimann.journal.localization.i18nOrDefault
import com.isaakhanimann.journal.ui.tabs.search.substance.roa.toReadableString
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TreatmentHistoryViewModel @Inject constructor(
    private val repository: ExperienceRepository
) : ViewModel() {
    val events = repository.getTreatmentEventsFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun add(
        substanceName: String,
        route: AdministrationRoute,
        status: TreatmentStatus,
        dose: Double?,
        units: String?,
        reason: String?,
        notes: String?
    ) {
        if (substanceName.isBlank()) return
        viewModelScope.launch {
            repository.insert(
                TreatmentEvent(
                    substanceName = substanceName.trim(),
                    administrationRoute = route,
                    status = status.name,
                    dose = dose,
                    units = units?.trim()?.takeIf { it.isNotEmpty() },
                    reason = reason?.trim()?.takeIf { it.isNotEmpty() },
                    notes = notes?.trim()?.takeIf { it.isNotEmpty() }
                )
            )
        }
    }

    fun delete(event: TreatmentEvent) {
        viewModelScope.launch { repository.delete(event) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TreatmentHistoryScreen(
    navigateBack: () -> Unit,
    viewModel: TreatmentHistoryViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsState()
    var substanceName by remember { mutableStateOf("") }
    var route by remember { mutableStateOf(AdministrationRoute.ORAL) }
    var status by remember { mutableStateOf(TreatmentStatus.ACTIVE) }
    var dose by remember { mutableStateOf("") }
    var units by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var routeMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(i18n("treatment_history_title")) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = i18n("user_agreement_back")
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = i18n("treatment_history_add"),
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = substanceName,
                            onValueChange = { substanceName = it },
                            label = { Text(i18n("treatment_history_substance")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { routeMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        i18nOrDefault(
                                            "route_${route.name.lowercase()}",
                                            route.name.lowercase()
                                        )
                                    )
                                }
                                DropdownMenu(
                                    expanded = routeMenuExpanded,
                                    onDismissRequest = { routeMenuExpanded = false }
                                ) {
                                    AdministrationRoute.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    i18nOrDefault(
                                                        "route_${option.name.lowercase()}",
                                                        option.name.lowercase()
                                                    )
                                                )
                                            },
                                            onClick = {
                                                route = option
                                                routeMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = dose,
                                    onValueChange = { dose = it },
                                    label = { Text(i18n("dose_label")) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = units,
                                    onValueChange = { units = it },
                                    label = { Text(i18n("dose_units_label")) },
                                    singleLine = true,
                                    modifier = Modifier.width(120.dp)
                                )
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TreatmentStatus.entries.forEach { option ->
                                FilterChip(
                                    selected = status == option,
                                    onClick = { status = option },
                                    label = { Text(i18n("treatment_status_${option.name.lowercase()}")) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text(i18n("treatment_history_reason")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(i18n("note_label")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                viewModel.add(
                                    substanceName = substanceName,
                                    route = route,
                                    status = status,
                                    dose = dose.toDoubleOrNull(),
                                    units = units,
                                    reason = reason,
                                    notes = notes
                                )
                                reason = ""
                                notes = ""
                            },
                            enabled = substanceName.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(i18n("treatment_history_save"))
                        }
                    }
                }
            }
            if (events.isEmpty()) {
                item {
                    Text(
                        text = i18n("treatment_history_empty"),
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
            }
            items(events, key = { it.id }) { event ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = event.substanceName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${i18n("treatment_status_${event.treatmentStatus.name.lowercase()}")} · " +
                                    i18nOrDefault(
                                        "route_${event.administrationRoute.name.lowercase()}",
                                        event.administrationRoute.name.lowercase()
                                    )
                            )
                            event.dose?.let {
                                Text("${i18n("dose_label")}: ${it.toReadableString()} ${event.units.orEmpty()}")
                            }
                            event.reason?.let { Text("${i18n("treatment_history_reason")}: $it") }
                            event.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            Text(
                                text = event.effectiveAt.atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.delete(event) }) {
                            Icon(Icons.Default.Delete, contentDescription = i18n("common_delete"))
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.padding(bottom = 24.dp))
            }
        }
    }
}
