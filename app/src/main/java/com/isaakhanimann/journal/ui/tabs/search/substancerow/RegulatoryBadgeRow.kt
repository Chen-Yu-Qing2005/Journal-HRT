package com.isaakhanimann.journal.ui.tabs.search.substancerow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isaakhanimann.journal.data.substances.RegulatorySchedule
import com.isaakhanimann.journal.localization.i18n

private val PsychotropicGreen = Color(0xFF087E4B)
private val NarcoticBlue = Color(0xFF0668B3)

@Composable
fun RegulatoryBadgeRow(
    schedules: List<RegulatorySchedule>,
    modifier: Modifier = Modifier
) {
    if (schedules.isEmpty()) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        schedules.distinct().forEach { schedule ->
            RegulatoryBadge(schedule)
        }
    }
}

@Composable
private fun RegulatoryBadge(schedule: RegulatorySchedule) {
    when (schedule) {
        RegulatorySchedule.CN_PSYCHOTROPIC_I -> PsychotropicDrugBadge(
            scheduleClass = "Ⅰ",
            description = i18n("regulatory_cn_psychotropic_i")
        )

        RegulatorySchedule.CN_PSYCHOTROPIC_II -> PsychotropicDrugBadge(
            scheduleClass = "Ⅱ",
            description = i18n("regulatory_cn_psychotropic_ii")
        )

        RegulatorySchedule.CN_NARCOTIC -> NarcoticDrugBadge(
            description = i18n("regulatory_cn_narcotic")
        )

        RegulatorySchedule.CN_NON_MEDICAL -> TextBadge(
            text = i18n("regulatory_cn_non_medical"),
            containerColor = Color(0xFF303030),
            contentColor = Color.White
        )
    }
}

@Composable
private fun PsychotropicDrugBadge(
    scheduleClass: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(36.dp)
                .clearAndSetSemantics { contentDescription = description },
            shape = RoundedCornerShape(2.dp),
            color = PsychotropicGreen,
            border = BorderStroke(1.dp, Color(0xFFE7E7E7))
        ) {
            Box {
                Canvas(Modifier.matchParentSize()) {
                    val divider = 1.5.dp.toPx()
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
                        strokeWidth = divider
                    )
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                        strokeWidth = divider
                    )
                }
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.weight(1f)) {
                        IconCharacter("精")
                        IconCharacter("神")
                    }
                    Row(Modifier.weight(1f)) {
                        IconCharacter("药")
                        IconCharacter("品")
                    }
                }
            }
        }
        Text(
            text = scheduleClass,
            color = PsychotropicGreen,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun NarcoticDrugBadge(description: String) {
    Surface(
        modifier = Modifier
            .size(36.dp)
            .clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(2.dp),
        color = NarcoticBlue,
        contentColor = Color.White
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White,
                    radius = size.minDimension * 0.37f,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
            Text(
                text = "麻",
                color = Color.White,
                fontSize = 21.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.IconCharacter(character: String) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = character,
            color = Color.White,
            fontSize = 14.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TextBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}
