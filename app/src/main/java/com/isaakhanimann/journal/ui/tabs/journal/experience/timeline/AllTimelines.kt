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

package com.isaakhanimann.journal.ui.tabs.journal.experience.timeline

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.isaakhanimann.journal.data.room.experiences.entities.AdaptiveColor
import com.isaakhanimann.journal.data.room.experiences.entities.ShulginRatingOption
import com.isaakhanimann.journal.data.substances.classes.roa.ConcentrationUnit
import com.isaakhanimann.journal.localization.i18n
import com.isaakhanimann.journal.ui.tabs.journal.experience.components.DataForOneEffectLine
import com.isaakhanimann.journal.ui.tabs.journal.experience.components.TimeDisplayOption
import com.isaakhanimann.journal.ui.tabs.journal.experience.components.getDurationText
import com.isaakhanimann.journal.ui.tabs.journal.experience.timeline.drawables.AxisDrawable
import com.isaakhanimann.journal.ui.tabs.journal.experience.timeline.drawables.TimeRangeDrawable
import com.isaakhanimann.journal.ui.tabs.journal.experience.timeline.drawables.timelines.monotoneCurveThrough
import com.isaakhanimann.journal.ui.utils.TimeFormat
import com.isaakhanimann.journal.ui.utils.getStringOfPattern
import com.isaakhanimann.journal.ui.utils.getShortTimeText
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class ConcentrationOverlayState(
    val labels: List<ConcentrationOverlayLabel>,
    val peakTopFraction: Float,
    val hasVerifiedAbsoluteConcentration: Boolean,
    val seriesCount: Int
)

data class ConcentrationOverlayLabel(
    val text: String,
    val color: AdaptiveColor
)


@Preview(showBackground = true)
@Composable
fun AllTimelinesPreview(
    @PreviewParameter(
        TimelinesPreviewProvider::class,
    ) dataForEffectLines: List<DataForOneEffectLine>
) {
    AllTimelines(
        model = AllTimelinesModel(
            dataForLines = dataForEffectLines,
            dataForRatings = listOf(
                DataForOneRating(
                    time = Instant.now().minus(3, ChronoUnit.HOURS),
                    option = ShulginRatingOption.MINUS
                ),
                DataForOneRating(
                    time = Instant.now().minus(2, ChronoUnit.HOURS),
                    option = ShulginRatingOption.TWO_PLUS
                ),
                DataForOneRating(
                    time = Instant.now().minus(1, ChronoUnit.HOURS),
                    option = ShulginRatingOption.THREE_PLUS
                ),
                DataForOneRating(
                    time = Instant.now().plus(2, ChronoUnit.HOURS),
                    option = ShulginRatingOption.FOUR_PLUS
                )
            ),
            timedNotes = listOf(
                DataForOneTimedNote(
                    time = Instant.now().minus(30, ChronoUnit.MINUTES),
                    color = AdaptiveColor.PURPLE
                ),
                DataForOneTimedNote(
                    time = Instant.now().plus(30, ChronoUnit.MINUTES),
                    color = AdaptiveColor.BLUE
                ),
            ),
        ),
        isShowingCurrentTime = true,
        timeDisplayOption = TimeDisplayOption.REGULAR,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}


@Composable
fun AllTimelines(
    model: AllTimelinesModel,
    isShowingCurrentTime: Boolean,
    timeDisplayOption: TimeDisplayOption,
    modifier: Modifier = Modifier,
    externalConcentrationUnit: ConcentrationUnit? = null,
    onConcentrationUnitChange: ((ConcentrationUnit) -> Unit)? = null,
    onConcentrationOverlayChanged: ((ConcentrationOverlayState) -> Unit)? = null,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val density = LocalDensity.current
    val axisLabelSize = MaterialTheme.typography.labelMedium.fontSize
    val concentrationLabelSize = MaterialTheme.typography.labelSmall.fontSize * 1.4f
    val axisLabelTextPaint = remember(density) {
        Paint().apply {
            color =
                if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = density.run { axisLabelSize.toPx() }
        }
    }

    val dragTimeTextSize = MaterialTheme.typography.titleMedium
    val textMeasurer = rememberTextMeasurer()
    val ratingSize = MaterialTheme.typography.labelLarge.fontSize
    val ratingTextPaint = remember(density) {
        Paint().apply {
            color =
                if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = density.run { ratingSize.toPx() }
        }
    }
    var currentTime by remember {
        mutableStateOf(Instant.now())
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        currentTime = Instant.now()
    }
    LaunchedEffect(Unit) {
        while (true) {
            val fiveSeconds = 5000L
            delay(fiveSeconds)
            currentTime = Instant.now()
        }
    }
    var dragPoint by remember { mutableStateOf<Offset?>(null) }
    var canvasWidthPx by remember { mutableFloatStateOf(0f) }
    var internalConcentrationUnit by rememberSaveable {
        mutableStateOf(ConcentrationUnit.MG_PER_L)
    }
    val concentrationUnit = externalConcentrationUnit ?: internalConcentrationUnit
    var isUnitMenuExpanded by remember { mutableStateOf(false) }
    var areConcentrationDetailsExpanded by rememberSaveable { mutableStateOf(false) }
    val concentrationCurves = remember(model) {
        model.concentrationSeries.associate { series ->
            series.substanceName to series.normalizedCurve(
                startTime = model.startTime,
                widthInSeconds = model.widthInSeconds,
                sampleCount = 256
            )
        }
    }
    val substancesWithPlottedConcentration = remember(concentrationCurves) {
        concentrationCurves.filterValues { it.size >= 2 }.keys
    }
    val showConcentrationDetails =
        model.concentrationSeries.size == 1 || areConcentrationDetailsExpanded
    val concentrationReferenceTime = dragPoint?.let { point ->
        if (canvasWidthPx > 0f) {
            val pixelsPerSecond = canvasWidthPx / model.widthInSeconds
            model.startTime.plusSeconds(
                (point.x.coerceIn(0f, canvasWidthPx) / pixelsPerSecond).toLong()
            )
        } else {
            currentTime
        }
    } ?: currentTime
    val overlayConcentrationLabels = model.concentrationSeries.mapNotNull { series ->
        series.labelAt(concentrationReferenceTime, concentrationUnit)?.let { label ->
            ConcentrationOverlayLabel(text = label, color = series.color)
        }
    }
    val peakTopFraction = model.concentrationSeries.mapNotNull { series ->
        val points = concentrationCurves[series.substanceName].orEmpty()
        val maximum = points.maxOfOrNull { it.value } ?: return@mapNotNull null
        val displayHeight =
            (series.height / model.overallMaxHeight).coerceIn(0f, 1f)
        1f - sqrt(maximum.coerceIn(0.0, 1.0)).toFloat() * displayHeight
    }.minOrNull()?.coerceIn(0f, 1f) ?: 1f
    LaunchedEffect(
        onConcentrationOverlayChanged,
        overlayConcentrationLabels,
        peakTopFraction,
        model.hasVerifiedAbsoluteConcentration,
        model.concentrationSeries.size
    ) {
        onConcentrationOverlayChanged?.invoke(
            ConcentrationOverlayState(
                labels = overlayConcentrationLabels,
                peakTopFraction = peakTopFraction,
                hasVerifiedAbsoluteConcentration =
                    model.hasVerifiedAbsoluteConcentration,
                seriesCount = model.concentrationSeries.size
            )
        )
    }

    Column(modifier = modifier) {
        if (onConcentrationOverlayChanged == null &&
            model.concentrationSeries.isNotEmpty()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(end = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (model.concentrationSeries.size > 1) {
                    Text(
                        text = i18n(
                            "concentration_details_count",
                            mapOf("count" to model.concentrationSeries.size.toString())
                        ) + if (areConcentrationDetailsExpanded) " ▴" else " ▾",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                areConcentrationDetailsExpanded =
                                    !areConcentrationDetailsExpanded
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                if (model.hasVerifiedAbsoluteConcentration) {
                    Box {
                        Text(
                            text = "${concentrationUnit.label} ▾",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable { isUnitMenuExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        DropdownMenu(
                            expanded = isUnitMenuExpanded,
                            onDismissRequest = { isUnitMenuExpanded = false }
                        ) {
                            ConcentrationUnit.entries.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.label) },
                                    onClick = {
                                        if (onConcentrationUnitChange != null) {
                                            onConcentrationUnitChange(unit)
                                        } else {
                                            internalConcentrationUnit = unit
                                        }
                                        isUnitMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = i18n("concentration_relative_no_cmax"),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Canvas(modifier = Modifier
                .matchParentSize()
                .onSizeChanged { canvasWidthPx = it.width.toFloat() }
                .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { change, _ ->
                    change.consume()
                    dragPoint = change.position
                },
                onDragEnd = {
                    dragPoint = null
                },
                onDragCancel = {
                    dragPoint = null
                }
            )
        }) {
        val canvasWithLabelsHeight = size.height
        val labelsHeight = axisLabelSize.toPx()
        val canvasWidth = size.width
        val concentrationLabelWidth = 0f
        val chartWidth = canvasWidth - concentrationLabelWidth
        val chartHeight = canvasWithLabelsHeight - labelsHeight - strokeWidth
        val pixelsPerSec = chartWidth / model.widthInSeconds

        inset(
            left = 0f,
            top = 0f,
            right = concentrationLabelWidth,
            bottom = labelsHeight + strokeWidth
        ) {
            val canvasHeightWithVerticalLine = size.height
            if (isShowingCurrentTime) {
                drawCurrentTime(
                    startTime = model.startTime,
                    timelineWidthInSeconds = model.widthInSeconds,
                    currentTime = currentTime,
                    pixelsPerSec = pixelsPerSec,
                    isDarkTheme = isDarkTheme,
                    canvasHeightOuter = canvasHeightWithVerticalLine,
                )
            }
            model.groupDrawables.forEach { group ->
                if (group.substanceName !in substancesWithPlottedConcentration) {
                    group.drawTimeLine(
                        drawScope = this,
                        canvasHeight = canvasHeightWithVerticalLine,
                        pixelsPerSec = pixelsPerSec,
                        color = group.color.getComposeColor(isDarkTheme),
                        density = density
                    )
                }
            }
            drawConcentrationCurves(
                series = model.concentrationSeries,
                curves = concentrationCurves,
                widthInSeconds = model.widthInSeconds,
                canvasHeight = canvasHeightWithVerticalLine,
                pixelsPerSec = pixelsPerSec,
                overallMaxHeight = model.overallMaxHeight,
                isDarkTheme = isDarkTheme
            )
            model.dataForRatings.forEach { dataForOneRating ->
                drawRating(
                    startTime = model.startTime,
                    ratingTime = dataForOneRating.time,
                    pixelsPerSec = pixelsPerSec,
                    canvasHeightOuter = canvasHeightWithVerticalLine,
                    rating = dataForOneRating.option,
                    textPaint = ratingTextPaint
                )
            }
            model.timedNotes.forEach { dataForOneTimedNote ->
                drawTimedNote(
                    startTime = model.startTime,
                    noteTime = dataForOneTimedNote.time,
                    color = dataForOneTimedNote.color,
                    pixelsPerSec = pixelsPerSec,
                    canvasHeightOuter = canvasHeightWithVerticalLine,
                    isDarkTheme = isDarkTheme
                )
            }
            dragPoint?.let {
                drawDragPointLineAndTimeLabel(
                    it,
                    chartWidth,
                    isDarkTheme,
                    canvasHeightWithVerticalLine,
                    pixelsPerSec,
                    model,
                    textMeasurer,
                    dragTimeTextSize,
                    timeDisplayOption,
                )
            }
        }
        val canvasConcentrationReferenceTime = dragPoint?.let {
            val seconds = (it.x.coerceIn(0f, chartWidth) / pixelsPerSec).toLong()
            model.startTime.plusSeconds(seconds)
        } ?: currentTime
        if (showConcentrationDetails && onConcentrationOverlayChanged == null) {
            drawConcentrationLabels(
                series = model.concentrationSeries,
                time = canvasConcentrationReferenceTime,
                chartWidth = chartWidth,
                chartHeight = chartHeight,
                availableLabelWidth = min(
                    180.dp.toPx(),
                    chartWidth * 0.56f
                ),
                labelTextSize = concentrationLabelSize.toPx(),
                density = density,
                isDarkTheme = isDarkTheme,
                concentrationUnit = concentrationUnit
            )
        }
        drawAxis(
            axisDrawable = model.axisDrawable,
            pixelsPerSec = pixelsPerSec,
            canvasWidth = chartWidth,
            canvasHeight = canvasWithLabelsHeight,
            textPaint = axisLabelTextPaint
        )
            }
        }
    }
}

private fun DrawScope.drawConcentrationCurves(
    series: List<DataForOneConcentrationSeries>,
    curves: Map<String, List<ConcentrationCurvePoint>>,
    widthInSeconds: Float,
    canvasHeight: Float,
    pixelsPerSec: Float,
    overallMaxHeight: Float,
    isDarkTheme: Boolean
) {
    if (series.isEmpty() || overallMaxHeight <= 0f || widthInSeconds <= 0f) return
    series.forEach { concentrationSeries ->
        val points = curves[concentrationSeries.substanceName].orEmpty()
        if (points.size < 2) return@forEach
        val displayHeight =
            (concentrationSeries.height / overallMaxHeight).coerceIn(0f, 1f)
        val path = Path().apply {
            monotoneCurveThrough(
                points.map { point ->
                    val x = point.timeFraction * widthInSeconds * pixelsPerSec
                    // Keep low residual levels visible without changing the numeric estimate.
                    val visualValue = sqrt(point.value.coerceIn(0.0, 1.0)).toFloat()
                    Offset(
                        x = x,
                        y = canvasHeight - visualValue * displayHeight * canvasHeight
                    )
                }
            )
        }
        drawPath(
            path = path,
            color = concentrationSeries.color.getComposeColor(isDarkTheme),
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

private fun DrawScope.drawConcentrationLabels(
    series: List<DataForOneConcentrationSeries>,
    time: Instant,
    chartWidth: Float,
    chartHeight: Float,
    availableLabelWidth: Float,
    labelTextSize: Float,
    density: androidx.compose.ui.unit.Density,
    isDarkTheme: Boolean,
    concentrationUnit: ConcentrationUnit
) {
    if (series.isEmpty() || chartWidth <= 0f || chartHeight <= 0f) return
    val paint = Paint().apply {
        textAlign = Paint.Align.LEFT
        textSize = labelTextSize
    }
    val horizontalGap = density.run { 5.dp.toPx() }
    val lineHeight = labelTextSize * 1.15f
    val maximumTextWidth =
        (availableLabelWidth - horizontalGap * 3f).coerceAtLeast(labelTextSize)
    var y = labelTextSize + density.run { 4.dp.toPx() }
    series
        .sortedByDescending { it.height }
        .forEach { concentrationSeries ->
            val labelText = concentrationSeries.labelAt(time, concentrationUnit)
                ?: return@forEach
            val wrappedLines = wrapConcentrationLabel(labelText, paint, maximumTextWidth)
            val labelHeight = wrappedLines.size * lineHeight
            if (y + labelHeight > chartHeight) return@forEach
            val textWidth = wrappedLines.maxOf { paint.measureText(it) }
            val textX = (chartWidth - textWidth - horizontalGap * 2f)
                .coerceAtLeast(horizontalGap * 2f)
            val color = concentrationSeries.color.getComposeColor(isDarkTheme)
            paint.color = color.toArgb()
            drawCircle(
                color = color,
                radius = density.run { 2.5.dp.toPx() },
                center = Offset(textX - horizontalGap, y - labelTextSize / 3f)
            )
            wrappedLines.forEachIndexed { index, line ->
                drawContext.canvas.nativeCanvas.drawText(
                    line,
                    textX,
                    y + index * lineHeight,
                    paint
                )
            }
            y += labelHeight + density.run { 8.dp.toPx() }
        }
}

private fun wrapConcentrationLabel(
    label: String,
    paint: Paint,
    maximumWidth: Float
): List<String> {
    if (paint.measureText(label) <= maximumWidth) return listOf(label)
    val lines = mutableListOf<String>()
    var remaining = label.trim()
    while (remaining.isNotEmpty()) {
        if (paint.measureText(remaining) <= maximumWidth) {
            lines.add(remaining)
            break
        }
        var low = 1
        var high = remaining.length
        var fittingCharacters = 1
        while (low <= high) {
            val middle = (low + high) / 2
            val candidate = remaining.substring(0, middle)
            if (paint.measureText(candidate) <= maximumWidth) {
                fittingCharacters = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        val fittingPrefix = remaining.substring(0, fittingCharacters)
        val whitespaceIndex = fittingPrefix.lastIndexOf(' ')
        val breakIndex = if (whitespaceIndex > 0) whitespaceIndex else fittingCharacters
        val line = remaining.substring(0, breakIndex).trimEnd()
        if (line.isNotEmpty()) lines.add(line)
        remaining = remaining.substring(breakIndex).trimStart()
    }
    return lines.ifEmpty { listOf(label) }
}

private fun DrawScope.drawDragPointLineAndTimeLabel(
    dragPoint: Offset,
    canvasWidth: Float,
    isDarkTheme: Boolean,
    canvasHeightWithVerticalLine: Float,
    pixelsPerSec: Float,
    model: AllTimelinesModel,
    textMeasurer: TextMeasurer,
    dragTimeTextSize: TextStyle,
    timeDisplayOption: TimeDisplayOption
) {
    val horizontallyLimitedDragPoint = Offset(
        x = dragPoint.x.coerceIn(0f, canvasWidth),
        y = dragPoint.y
    )
    val dragLineColor = if (isDarkTheme) Color.White else Color.Black
    val textColor = if (isDarkTheme) Color.Black else Color.White
    drawVerticalDragLine(dragLineColor, horizontallyLimitedDragPoint, canvasHeightWithVerticalLine)

    drawDragTimeLabelWithBackground(
        horizontallyLimitedDragPoint,
        pixelsPerSec,
        model,
        textMeasurer,
        canvasWidth,
        canvasHeightWithVerticalLine,
        dragLineColor,
        dragTimeTextSize,
        textColor,
        timeDisplayOption
    )
}

private fun DrawScope.drawDragTimeLabelWithBackground(
    horizontallyLimitedDragPoint: Offset,
    pixelsPerSec: Float,
    model: AllTimelinesModel,
    textMeasurer: TextMeasurer,
    canvasWidth: Float,
    canvasHeightWithVerticalLine: Float,
    dragLineColor: Color,
    dragTimeTextSize: TextStyle,
    textColor: Color,
    timeDisplayOption: TimeDisplayOption
) {
    val secondsAtDragPoint = horizontallyLimitedDragPoint.x / pixelsPerSec
    val timeAtDragPoint = model.startTime.plusSeconds(secondsAtDragPoint.toLong())
    val zone = ZoneId.systemDefault()
    val spansMultipleDays =
        model.startTime.atZone(zone).toLocalDate() !=
            model.startTime.plusSeconds(model.widthInSeconds.toLong())
                .atZone(zone)
                .toLocalDate()
    val timeLabel = when (timeDisplayOption) {
        TimeDisplayOption.RELATIVE_TO_NOW -> {
            val now = Instant.now()
            val isInPast = timeAtDragPoint < now
            if (isInPast) {
                getDurationText(fromInstant = timeAtDragPoint, toInstant = now) + " ago"
            } else {
                "in " + getDurationText(fromInstant = timeAtDragPoint, toInstant = now)
            }
        }

        TimeDisplayOption.RELATIVE_TO_START -> {
            getDurationText(
                fromInstant = model.startTime,
                toInstant = timeAtDragPoint
            ) + " in"
        }

        TimeDisplayOption.TIME_BETWEEN -> if (spansMultipleDays) {
            timeAtDragPoint.getStringOfPattern("MM/dd ${TimeFormat.timePattern}")
        } else {
            timeAtDragPoint.getShortTimeText()
        }
        TimeDisplayOption.REGULAR -> if (spansMultipleDays) {
            timeAtDragPoint.getStringOfPattern("MM/dd ${TimeFormat.timePattern}")
        } else {
            timeAtDragPoint.getShortTimeText()
        }
    }
    val safeCanvasWidth = canvasWidth.coerceAtLeast(1f)
    val safeCanvasHeight = canvasHeightWithVerticalLine.coerceAtLeast(1f)
    val textPadding = 8.dp.toPx()
    val tooltipTopPadding = 72.dp.toPx()
    val maxTextWidth = min(safeCanvasWidth * 0.62f, 240.dp.toPx())
        .toInt()
        .coerceAtLeast(1)
    val measuredText = textMeasurer.measure(
        text = timeLabel,
        style = dragTimeTextSize.copy(fontSize = 18.sp, color = textColor),
        constraints = Constraints(maxWidth = maxTextWidth),
        softWrap = true
    )
    val textSize = measuredText.size
    val rectSize = textSize.toSize().times(1.35f).let {
        it.copy(
            width = min(it.width, safeCanvasWidth),
            height = min(it.height, safeCanvasHeight)
        )
    }
    val rectTopLeft = Offset(
        x = (safeCanvasWidth - rectSize.width - textPadding).coerceAtLeast(0f),
        y = tooltipTopPadding.coerceAtMost(
            (safeCanvasHeight - rectSize.height).coerceAtLeast(0f)
        )
    )
    drawRoundRect(
        color = dragLineColor,
        size = rectSize,
        topLeft = rectTopLeft,
        cornerRadius = CornerRadius(x = 15f, y = 15f)
    )
    drawText(
        textLayoutResult = measuredText,
        topLeft = Offset(
            x = rectTopLeft.x + (rectSize.width - textSize.width) / 2,
            y = rectTopLeft.y + (rectSize.height - textSize.height) / 2
        ),
        color = textColor,
    )
}

private fun DrawScope.drawVerticalDragLine(
    dragLineColor: Color,
    horizontallyLimitedDragPoint: Offset,
    canvasHeightWithVerticalLine: Float
) {
    drawLine(
        color = dragLineColor,
        start = Offset(x = horizontallyLimitedDragPoint.x, y = canvasHeightWithVerticalLine),
        end = Offset(x = horizontallyLimitedDragPoint.x, y = 0f),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round
    )
}

fun DrawScope.drawCurrentTime(
    startTime: Instant,
    timelineWidthInSeconds: Float,
    currentTime: Instant,
    pixelsPerSec: Float,
    isDarkTheme: Boolean,
    canvasHeightOuter: Float,
) {
    val endTime = startTime.plusSeconds(timelineWidthInSeconds.toLong())
    if (startTime.isBefore(currentTime) && endTime.isAfter(currentTime)) {
        val timeStartInSec = Duration.between(startTime, currentTime).seconds
        val timeStartX = timeStartInSec * pixelsPerSec
        val color = (if (isDarkTheme) Color.White else Color.Black).copy(alpha = 0.5f)
        drawLine(
            color = color,
            start = Offset(x = timeStartX, y = canvasHeightOuter),
            end = Offset(x = timeStartX, y = 0f),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

fun DrawScope.drawTimeRange(
    timeRangeDrawable: TimeRangeDrawable,
    canvasHeight: Float,
    pixelsPerSec: Float,
    color: Color,
) {
    val startX = timeRangeDrawable.ingestionStartInSeconds * pixelsPerSec
    val endX = timeRangeDrawable.ingestionEndInSeconds * pixelsPerSec
    val minLineHeight = 12.dp.toPx()
    val horizontalLineWidth = 8.dp.toPx()
    val offset = timeRangeDrawable.intersectionCountWithPreviousRanges * horizontalLineWidth
    val horizontalLineHeight = minLineHeight / 2 + offset
    val verticalLineHeight = minLineHeight + offset
    val verticalLineTopY = canvasHeight - verticalLineHeight
    val horizontalLineY = canvasHeight - horizontalLineHeight
    val verticalLineStrokeWidth = 4.dp.toPx()
    drawLine(
        color = color,
        start = Offset(x = startX, y = verticalLineTopY),
        end = Offset(x = startX, y = canvasHeight),
        strokeWidth = verticalLineStrokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(x = startX, y = horizontalLineY),
        end = Offset(x = endX, y = horizontalLineY),
        strokeWidth = horizontalLineWidth,
        cap = StrokeCap.Butt
    )
    drawLine(
        color = color,
        start = Offset(x = endX, y = verticalLineTopY),
        end = Offset(x = endX, y = canvasHeight),
        strokeWidth = verticalLineStrokeWidth,
        cap = StrokeCap.Round
    )
}

fun DrawScope.drawRating(
    startTime: Instant,
    ratingTime: Instant,
    pixelsPerSec: Float,
    canvasHeightOuter: Float,
    rating: ShulginRatingOption,
    textPaint: Paint
) {
    val timeStartInSec = Duration.between(startTime, ratingTime).seconds
    val timeStartX = timeStartInSec * pixelsPerSec
    val lineHeight = textPaint.textSize
    val lines = rating.verticalSign.split("\n")
    val signHeight = lines.size * lineHeight
    val verticalLineHeight = (canvasHeightOuter - 2 * lineHeight - signHeight) / 2
    drawLine(
        color = Color.Gray,
        start = Offset(x = timeStartX, y = 0f),
        end = Offset(x = timeStartX, y = verticalLineHeight),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
    )
    var y = verticalLineHeight + 1.5f * lineHeight
    drawContext.canvas.nativeCanvas.apply {
        for (line in lines) {
            drawText(
                line,
                timeStartX,
                y,
                textPaint
            )
            y += lineHeight
        }
    }
    drawLine(
        color = Color.Gray,
        start = Offset(x = timeStartX, y = y),
        end = Offset(x = timeStartX, y = canvasHeightOuter),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round
    )
}

fun DrawScope.drawTimedNote(
    startTime: Instant,
    noteTime: Instant,
    pixelsPerSec: Float,
    canvasHeightOuter: Float,
    color: AdaptiveColor,
    isDarkTheme: Boolean
) {
    val timeStartInSec = Duration.between(startTime, noteTime).seconds
    val timeStartX = timeStartInSec * pixelsPerSec
    val strokeWidth = 3.dp.toPx()
    drawLine(
        color = color.getComposeColor(isDarkTheme = isDarkTheme),
        start = Offset(x = timeStartX, y = 0f),
        end = Offset(x = timeStartX, y = canvasHeightOuter),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(strokeWidth, strokeWidth * 2))
    )
}

fun DrawScope.drawAxis(
    axisDrawable: AxisDrawable,
    pixelsPerSec: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    textPaint: Paint
) {
    val fullHours = axisDrawable.getFullHours(
        pixelsPerSec = pixelsPerSec,
        widthInPixels = canvasWidth,
        minimumSpacingPx = 40.dp.toPx(),
        minimumDateSpacingPx = 64.dp.toPx()
    )
    drawContext.canvas.nativeCanvas.apply {
        fullHours.forEach { fullHour ->
            drawText(
                fullHour.label,
                fullHour.distanceFromStart,
                canvasHeight,
                textPaint
            )
        }
    }
}
