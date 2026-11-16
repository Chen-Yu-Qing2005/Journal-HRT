/*
 * Copyright (c) 2026.
 * This file is part of PsychonautWiki Journal.
 *
 * PsychonautWiki Journal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.isaakhanimann.journal.data.substances.classes.roa

import java.util.Locale
import kotlin.math.pow

enum class ConcentrationUnit(
    val label: String,
    private val multiplierFromMgPerL: Double,
    private val decimalPlaces: Int
) {
    PG_PER_ML("pg/mL", 1_000_000.0, 1),
    NG_PER_ML("ng/mL", 1_000.0, 3),
    MG_PER_ML("mg/mL", 0.001, 5),
    MG_PER_L("mg/L", 1.0, 5);

    fun formatRange(range: ClosedFloatingPointRange<Double>): String? {
        val minimum = convert(range.start.coerceAtLeast(0.0))
        val maximum = convert(range.endInclusive.coerceAtLeast(0.0))
        val minimumVisible = 10.0.pow(-decimalPlaces)
        if (maximum < minimumVisible) return null
        return if (minimum == maximum) {
            "≈ ${formatValue(minimum)} $label"
        } else if (minimum < minimumVisible) {
            "≈ ≤ ${formatValue(maximum)} $label"
        } else {
            "≈ ${formatValue(minimum)}–${formatValue(maximum)} $label"
        }
    }

    private fun convert(mgPerL: Double): Double = mgPerL * multiplierFromMgPerL

    private fun formatValue(value: Double): String {
        if (value <= 0.0) return "0"
        val raw = String.format(Locale.US, "%.${decimalPlaces}f", value)
        return if (decimalPlaces == 0) raw else raw.trimEnd('0').trimEnd('.')
    }
}
