/*
 * Copyright (c) 2022. Isaak Hanimann.
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

package com.isaakhanimann.journal.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isaakhanimann.journal.ui.tabs.settings.combinations.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _starPromptCount = MutableStateFlow<Int?>(null)
    val starPromptCountFlow: StateFlow<Int?> = _starPromptCount.asStateFlow()

    init {
        viewModelScope.launch {
            _starPromptCount.value =
                userPreferences.recordAppLaunchAndGetStarPromptCount()
        }
    }

    fun dismissStarPrompt() {
        _starPromptCount.value = null
    }

    // Acceptance is deliberately process-scoped: every cold launch requires a
    // fresh decision and the choice is never persisted across sessions.
    private val _isAccepted = MutableStateFlow(false)
    val isAcceptedFlow: StateFlow<Boolean> = _isAccepted.asStateFlow()

    val selectedLanguageFlow: StateFlow<String?> = userPreferences.selectedLanguageFlow.stateIn(
        initialValue = null,
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000)
    )

    val isAppLockEnabledFlow: StateFlow<Boolean> = userPreferences.isAppLockEnabledFlow.stateIn(
        initialValue = false,
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000)
    )

    val isRootTabTransitionsEnabledFlow: StateFlow<Boolean> =
        userPreferences.isRootTabTransitionsEnabledFlow.stateIn(
            initialValue = true,
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000)
        )

    val isBottomBarPinnedFlow: StateFlow<Boolean> =
        userPreferences.isBottomBarPinnedFlow.stateIn(
            initialValue = false,
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000)
        )

    // Unlock state lives for the process lifetime: once authenticated, the app stays
    // unlocked until it is killed. Reset when the lock is disabled by the user.
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlockedFlow: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun markUnlocked() {
        _isUnlocked.value = true
    }

    fun accept() {
        _isAccepted.value = true
    }
}
