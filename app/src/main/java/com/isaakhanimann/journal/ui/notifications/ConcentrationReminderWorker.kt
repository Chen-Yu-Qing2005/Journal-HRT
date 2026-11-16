package com.isaakhanimann.journal.ui.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.isaakhanimann.journal.data.room.experiences.ExperienceRepository
import com.isaakhanimann.journal.data.substances.repositories.SubstanceRepository
import com.isaakhanimann.journal.ui.tabs.settings.combinations.UserPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ConcentrationReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val experienceRepository: ExperienceRepository,
    private val substanceRepository: SubstanceRepository,
    private val userPreferences: UserPreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            ConcentrationReminderChecker.check(
                context = applicationContext,
                experienceRepository = experienceRepository,
                substanceRepository = substanceRepository,
                userPreferences = userPreferences
            )
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

}
