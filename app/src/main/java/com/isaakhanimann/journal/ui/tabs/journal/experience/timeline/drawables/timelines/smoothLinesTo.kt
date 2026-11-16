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

package com.isaakhanimann.journal.ui.tabs.journal.experience.timeline.drawables.timelines

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.abs
import kotlin.math.min

fun Path.startSmoothLineTo(
    smoothnessBetween0And1: Float,
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float
) {
    val diff = endX - startX
    val controlX = startX + (diff * smoothnessBetween0And1)
    quadraticTo(controlX, startY, endX, endY)
}

fun Path.endSmoothLineTo(
    smoothnessBetween0And1: Float,
    startX: Float,
    endX: Float,
    endY: Float
) {
    val diff = endX - startX
    val controlX = endX - (diff * smoothnessBetween0And1)
    quadraticTo(controlX, endY, endX, endY)
}

/**
 * Draws a smooth curve through every supplied point without overshooting between
 * monotonic sections. Timing points therefore remain exact instead of being replaced
 * by decorative control points.
 */
fun Path.monotoneCurveThrough(points: List<Offset>) {
    if (points.isEmpty()) return
    val curvePoints = mutableListOf<Offset>()
    points.forEach { point ->
        val previous = curvePoints.lastOrNull()
        if (previous != null && abs(point.x - previous.x) <= 0.001f) {
            curvePoints[curvePoints.lastIndex] = Offset(
                x = previous.x,
                y = maxOf(previous.y, point.y)
            )
        } else {
            curvePoints.add(point)
        }
    }
    moveTo(curvePoints.first().x, curvePoints.first().y)
    if (curvePoints.size == 1) return
    if (curvePoints.size == 2) {
        lineTo(curvePoints.last().x, curvePoints.last().y)
        return
    }
    val deltas = curvePoints.zipWithNext { first, second -> second.x - first.x }
    if (deltas.any { it <= 0.001f }) {
        curvePoints.drop(1).forEach { lineTo(it.x, it.y) }
        return
    }
    val slopes = curvePoints.zipWithNext { first, second ->
        (second.y - first.y) / (second.x - first.x)
    }
    val tangents = FloatArray(curvePoints.size)
    tangents[0] = slopes.first()
    tangents[tangents.lastIndex] = slopes.last()
    for (index in 1 until curvePoints.lastIndex) {
        val left = slopes[index - 1]
        val right = slopes[index]
        tangents[index] = if (left * right <= 0f) {
            0f
        } else {
            val leftDelta = deltas[index - 1]
            val rightDelta = deltas[index]
            val firstWeight = 2f * rightDelta + leftDelta
            val secondWeight = rightDelta + 2f * leftDelta
            (firstWeight + secondWeight) /
                (firstWeight / left + secondWeight / right)
        }
    }
    tangents.indices.forEach { index ->
        val tangentLimit = if (index == 0) {
            3f * abs(slopes.first())
        } else if (index == tangents.lastIndex) {
            3f * abs(slopes.last())
        } else {
            3f * min(abs(slopes[index - 1]), abs(slopes[index]))
        }
        tangents[index] = tangents[index].coerceIn(-tangentLimit, tangentLimit)
    }
    for (index in slopes.indices) {
        val sectionDelta = deltas[index]
        val firstTangent = tangents[index]
        val secondTangent = tangents[index + 1]
        val start = curvePoints[index]
        val end = curvePoints[index + 1]
        cubicTo(
            x1 = start.x + sectionDelta / 3f,
            y1 = start.y + firstTangent * sectionDelta / 3f,
            x2 = end.x - sectionDelta / 3f,
            y2 = end.y - secondTangent * sectionDelta / 3f,
            x3 = end.x,
            y3 = end.y
        )
    }
}
