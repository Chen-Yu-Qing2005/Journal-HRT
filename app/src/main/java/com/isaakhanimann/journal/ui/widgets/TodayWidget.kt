/*
 * Copyright (c) 2026.
 * This file is part of PsychonautWiki Journal.
 *
 * PsychonautWiki Journal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.isaakhanimann.journal.ui.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import com.isaakhanimann.journal.MainActivity
import com.isaakhanimann.journal.R
import com.isaakhanimann.journal.data.room.experiences.ExperienceRepository
import com.isaakhanimann.journal.data.room.experiences.JournalDataEvents
import com.isaakhanimann.journal.data.room.experiences.entities.TreatmentStatus
import com.isaakhanimann.journal.localization.I18n
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

internal data class TodayWidgetSummary(
    val activeTreatmentNames: List<String>,
    val todayIngestionNames: List<String>,
    val lastIngestionTime: Instant?
)

private object TodayWidgetData {
    suspend fun load(repository: ExperienceRepository): TodayWidgetSummary {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val startOfDay = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val ingestions = repository.getIngestionsWithCompanions(startOfDay, now.plusSeconds(1))
            .map { it.ingestion }
        val latestTreatments = repository.getAllTreatmentEvents()
            .groupBy { it.substanceName to it.administrationRoute }
            .values
            .mapNotNull { events -> events.maxByOrNull { it.effectiveAt } }
            .filter { it.treatmentStatus == TreatmentStatus.ACTIVE }
        return TodayWidgetSummary(
            activeTreatmentNames = latestTreatments.map { it.substanceName }.distinct(),
            todayIngestionNames = ingestions.map { it.substanceName }.distinct(),
            lastIngestionTime = ingestions.maxOfOrNull { it.time }
        )
    }
}

class TodayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pending = goAsync()
        TodayWidgetSync.requestRefresh(appWidgetIds, onComplete = { pending.finish() })
    }

    companion object {
        internal fun render(context: Context, summary: TodayWidgetSummary): RemoteViews {
            val localized = { key: String -> I18n.translate(context, key) }
            val activeNames = summary.activeTreatmentNames.take(3).joinToString(", ")
            val todayNames = summary.todayIngestionNames.take(3).joinToString(", ")
            val active = if (activeNames.isBlank()) {
                localized("today_no_active_treatments")
            } else {
                localized("today_active_treatments").replace("{substances}", activeNames)
            }
            val recorded = if (todayNames.isBlank()) {
                localized("today_no_ingestions")
            } else {
                localized("today_ingestions").replace("{substances}", todayNames)
            }
            val latest = summary.lastIngestionTime
                ?.atZone(ZoneId.systemDefault())
                ?.format(DateTimeFormatter.ofPattern("HH:mm"))
                ?: ""
            val openIntent = Intent(context, MainActivity::class.java).apply {
                data = Uri.parse("journal://open/journal")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                310_000,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return RemoteViews(context.packageName, R.layout.widget_today).apply {
                setTextViewText(R.id.widget_today_title, localized("today_medication_title"))
                setTextViewText(R.id.widget_today_active, active)
                setTextViewText(R.id.widget_today_recorded, recorded)
                setTextViewText(
                    R.id.widget_today_latest,
                    if (latest.isEmpty()) "" else localized("today_last_dose")
                        .replace("{time}", latest)
                )
                setOnClickPendingIntent(R.id.widget_today_root, pendingIntent)
            }
        }
    }
}

private class TodayRefreshTargets(
    val allWidgets: Boolean,
    val ids: List<Int>,
    val completions: List<() -> Unit>
)

object TodayWidgetSync {
    private const val TAG = "TodayWidgetSync"
    private const val SETTLE_MS = 500L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val lock = Any()
    private var started = false
    private var allWidgets = false
    private val ids = mutableSetOf<Int>()
    private val completions = mutableListOf<() -> Unit>()

    @OptIn(FlowPreview::class)
    fun start(context: Context, repository: ExperienceRepository, languageChanges: Flow<Unit>) {
        val appContext = context.applicationContext
        synchronized(lock) {
            if (started) return
            started = true
        }
        scope.launch {
            for (ignored in requests) {
                val targets = claim()
                try {
                    refresh(appContext, repository, targets)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Today widget refresh failed", e)
                } finally {
                    targets.completions.forEach { runCatching(it) }
                }
            }
        }
        scope.launch {
            JournalDataEvents.journalChangeSignal.debounce(SETTLE_MS).collect {
                requestRefresh()
            }
        }
        scope.launch { languageChanges.collect { requestRefresh() } }
        scope.launch { I18n.stringsChanged.collect { requestRefresh() } }
        requestRefresh()
    }

    fun requestRefresh(appWidgetIds: IntArray? = null, onComplete: (() -> Unit)? = null) {
        synchronized(lock) {
            if (appWidgetIds == null) allWidgets = true
            else ids.addAll(appWidgetIds.toList())
            if (onComplete != null) completions.add(onComplete)
        }
        requests.trySend(Unit)
    }

    private fun claim(): TodayRefreshTargets = synchronized(lock) {
        val result = TodayRefreshTargets(allWidgets, ids.toList(), completions.toList())
        allWidgets = false
        ids.clear()
        completions.clear()
        result
    }

    private suspend fun refresh(
        context: Context,
        repository: ExperienceRepository,
        targets: TodayRefreshTargets
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val widgetIds = if (targets.allWidgets) {
            manager.getAppWidgetIds(ComponentName(context, TodayWidgetProvider::class.java))
                .toList()
        } else {
            targets.ids
        }
        if (widgetIds.isEmpty()) return
        val summary = TodayWidgetData.load(repository)
        widgetIds.forEach { id ->
            manager.updateAppWidget(id, TodayWidgetProvider.render(context, summary))
        }
    }
}
