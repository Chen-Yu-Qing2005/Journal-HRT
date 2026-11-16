package com.isaakhanimann.journal.data.substances.classes.roa

import kotlin.math.max

/**
 * User-defined two-stage zero-order release profile.
 *
 * The first [initialMinutes] releases [initialPercent] of the dose, then the remaining
 * fraction is released over [subsequentMinutes].
 */
data class ExtendedReleaseProfile(
    val initialMinutes: Double,
    val initialPercent: Double,
    val subsequentMinutes: Double
) {
    val isValid: Boolean
        get() = initialMinutes > 0.0 &&
            initialPercent in 0.0..100.0 &&
            (initialPercent >= 100.0 || subsequentMinutes > 0.0)

    val totalMinutes: Double
        get() = if (initialPercent >= 100.0) initialMinutes else initialMinutes + subsequentMinutes

    fun releasedFractionAt(elapsedMinutes: Double): Double {
        if (!isValid || elapsedMinutes <= 0.0) return 0.0
        if (elapsedMinutes >= totalMinutes) return 1.0
        val initialFraction = initialPercent / 100.0
        if (elapsedMinutes <= initialMinutes) {
            return initialFraction * (elapsedMinutes / initialMinutes)
        }
        val remainingFraction = 1.0 - initialFraction
        val secondElapsed = elapsedMinutes - initialMinutes
        return (initialFraction + remainingFraction * (secondElapsed / subsequentMinutes))
            .coerceIn(0.0, 1.0)
    }

    fun remainingFractionAt(elapsedMinutes: Double): Double =
        max(0.0, 1.0 - releasedFractionAt(elapsedMinutes))

    companion object {
        fun fromNullable(
            initialMinutes: Double?,
            initialPercent: Double?,
            subsequentMinutes: Double?
        ): ExtendedReleaseProfile? {
            if (initialMinutes == null || initialPercent == null) {
                return null
            }
            val subsequent = when {
                initialPercent >= 100.0 -> subsequentMinutes ?: 0.0
                subsequentMinutes == null -> return null
                else -> subsequentMinutes
            }
            return ExtendedReleaseProfile(
                initialMinutes = initialMinutes,
                initialPercent = initialPercent,
                subsequentMinutes = subsequent
            ).takeIf { it.isValid }
        }
    }
}
