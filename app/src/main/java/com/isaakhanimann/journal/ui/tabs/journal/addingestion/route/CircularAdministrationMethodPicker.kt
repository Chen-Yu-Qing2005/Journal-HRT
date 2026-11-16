package com.isaakhanimann.journal.ui.tabs.journal.addingestion.route

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.localization.i18n
import com.isaakhanimann.journal.localization.i18nOrDefault
import com.isaakhanimann.journal.ui.utils.administrationRouteKey
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class WuXingStyle(
    val backgroundColor: Color,
    val contentColor: Color = Color.White
)

private val wuXingStyles = listOf(
    WuXingStyle(backgroundColor = Color(0xFFB08A35)),
    WuXingStyle(backgroundColor = Color(0xFF2F7D4D)),
    WuXingStyle(backgroundColor = Color(0xFF315F9B)),
    WuXingStyle(backgroundColor = Color(0xFFC14335)),
    WuXingStyle(backgroundColor = Color(0xFF94642D))
)

@Composable
fun CircularAdministrationMethodPicker(
    title: String,
    methods: List<AdministrationMethodChoice>,
    onMethodTapped: (AdministrationMethodChoice) -> Unit,
    useWuXingPalette: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    val ringColor = MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = modifier.padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val diameter = minOf(maxWidth, maxHeight).coerceAtMost(390.dp)
            val nodeDiameter = when {
                diameter < 300.dp -> 82.dp
                diameter < 350.dp -> 92.dp
                else -> 104.dp
            }
            val radius = ((diameter - nodeDiameter) / 2 - 9.dp).coerceAtLeast(80.dp)
            val startAngle = -PI / 2.0
            val elementStyles = wuXingStyles
                .takeIf { useWuXingPalette && methods.size == 5 }
                .orEmpty()

            Box(modifier = Modifier.size(diameter)) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radiusPx = radius.toPx()
                    drawCircle(
                        color = ringColor,
                        radius = radiusPx,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    methods.forEachIndexed { index, method ->
                        val angle = startAngle + index * (2.0 * PI / methods.size)
                        val target = Offset(
                            x = center.x + (cos(angle) * radiusPx).toFloat(),
                            y = center.y + (sin(angle) * radiusPx).toFloat()
                        )
                        val elementColor = elementStyles.getOrNull(index)?.backgroundColor
                        drawLine(
                            color = (elementColor ?: method.route.color.getComposeColor(isDarkTheme))
                                .copy(alpha = 0.5f),
                            start = center,
                            end = target,
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                MethodCenterBadge(
                    title = title,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(nodeDiameter * 1.22f)
                )

                methods.forEachIndexed { index, method ->
                    val angle = startAngle + index * (2.0 * PI / methods.size)
                    MethodNode(
                        method = method,
                        elementStyle = elementStyles.getOrNull(index),
                        isDarkTheme = isDarkTheme,
                        onClick = { onMethodTapped(method) },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(
                                x = (cos(angle) * radius.value).dp,
                                y = (sin(angle) * radius.value).dp
                            )
                            .size(nodeDiameter)
                    )
                }
            }
        }
        Text(
            text = i18n("administration_method_group_note"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun MethodCenterBadge(
    title: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MethodNode(
    method: AdministrationMethodChoice,
    elementStyle: WuXingStyle?,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val routeColor = method.route.color.getComposeColor(isDarkTheme)
    val nodeColor = elementStyle?.backgroundColor ?: MaterialTheme.colorScheme.surfaceVariant
    val nodeContentColor =
        elementStyle?.contentColor ?: MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = nodeColor,
        contentColor = nodeContentColor,
        border = BorderStroke(
            3.dp,
            if (elementStyle != null) {
                Color.White.copy(alpha = 0.32f)
            } else {
                routeColor
            }
        ),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = method.methodLabel ?: i18nOrDefault(
                    administrationRouteKey(method.route),
                    method.route.displayText
                ),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            method.activeIngredient?.takeIf { it.isNotBlank() }?.let { ingredient ->
                Text(
                    text = ingredient,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = if (elementStyle == null) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
