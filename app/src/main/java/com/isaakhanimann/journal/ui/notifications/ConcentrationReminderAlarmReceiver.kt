package com.isaakhanimann.journal.ui.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.isaakhanimann.journal.data.room.experiences.ExperienceRepository
import com.isaakhanimann.journal.data.substances.repositories.SubstanceRepository
import com.isaakhanimann.journal.ui.tabs.settings.combinations.UserPreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ConcentrationReminderAlarmReceiver : BroadcastReceiver() {
    @Inject
    lateinit var experienceRepository: ExperienceRepository

    @Inject
    lateinit var substanceRepository: SubstanceRepository

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ConcentrationReminderChecker.check(
                    context = context.applicationContext,
                    experienceRepository = experienceRepository,
                    substanceRepository = substanceRepository,
                    userPreferences = userPreferences
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
