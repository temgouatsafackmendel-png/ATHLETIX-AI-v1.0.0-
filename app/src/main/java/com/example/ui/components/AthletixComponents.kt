package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AthletixDanger
import com.example.ui.theme.AthletixPrimaryAccent
import com.example.ui.theme.AthletixSecondaryAccent
import com.example.ui.theme.AthletixWarning

@Composable
fun AthletixCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    } else {
        modifier
    }

    Surface(
        modifier = cardModifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun MetricPill(
    label: String,
    value: String,
    modifier: Modifier
) {
    MetricPill(
        label = label,
        value = value,
        accentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
fun MetricPill(
    label: String,
    value: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
        }
    }
}

@Composable
fun CircularScoreGauge(
    score: Int,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 112.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = 700),
        label = "scoreGauge"
    )
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val progressColor = when {
        score >= 75 -> AthletixPrimaryAccent
        score >= 50 -> AthletixWarning
        else -> AthletixDanger
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val strokeWidth = 10.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = progressColor,
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InteractiveLineChart(
    points: List<Pair<String, Float>>,
    lineColor: Color = AthletixPrimaryAccent,
    unitSuffix: String = "",
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aucune donnée sur cette période",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val values = points.map { it.second }
    val minVal = (values.minOrNull() ?: 0f) * 0.95f
    val maxVal = ((values.maxOrNull() ?: 1f) * 1.05f).coerceAtLeast(minVal + 1f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            val w = size.width
            val h = size.height

            // Horizontal reference lines
            for (i in 0..2) {
                val y = h * (i / 2f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (points.size == 1) {
                val center = Offset(w / 2f, h / 2f)
                drawCircle(color = lineColor, radius = 6.dp.toPx(), center = center)
                return@Canvas
            }

            val stepX = w / (points.size - 1).coerceAtLeast(1)
            val linePath = Path()
            val fillPath = Path()

            points.forEachIndexed { idx, pair ->
                val normY = ((pair.second - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                val x = idx * stepX
                val y = h - (normY * h)
                if (idx == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(w, h)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)
                )
            )
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            points.forEachIndexed { idx, pair ->
                val normY = ((pair.second - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                val x = idx * stepX
                val y = h - (normY * h)
                drawCircle(color = lineColor, radius = 4.5.dp.toPx(), center = Offset(x, y))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.takeLast(6).forEach { (label, v) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${v.toInt()}$unitSuffix",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = label.takeLast(5),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveBarChart(
    bars: List<Pair<String, Float>>,
    barColor: Color = AthletixSecondaryAccent,
    modifier: Modifier = Modifier
) {
    if (bars.isEmpty()) return
    val maxVal = (bars.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            val count = bars.size
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.55f).coerceAtMost(28.dp.toPx())

            bars.forEachIndexed { idx, (_, value) ->
                val ratio = (value / maxVal).coerceIn(0.06f, 1f)
                val barHeight = size.height * ratio
                val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                val top = size.height - barHeight
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            bars.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FirebaseDeclinedNoticeBanner(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("firebase_declined_notice_banner"),
        shape = RoundedCornerShape(14.dp),
        color = AthletixWarning.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, AthletixWarning.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Information Firebase",
                tint = AthletixWarning,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Fonctionnalités IA en mode Aperçu Local : Gemini dans Android Studio requiert Firebase AI Logic (configuration déclinée). Activez Firebase pour connecter Gemini en direct.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun SyncStatusBadge(
    isOffline: Boolean,
    pendingCount: Int,
    onToggleOffline: () -> Unit
) {
    val bgColor = if (isOffline || pendingCount > 0) {
        AthletixWarning.copy(alpha = 0.18f)
    } else {
        AthletixPrimaryAccent.copy(alpha = 0.16f)
    }
    val textColor = if (isOffline || pendingCount > 0) AthletixWarning else AthletixPrimaryAccent
    val icon: ImageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.Verified
    val label = when {
        isOffline -> "Hors-ligne ($pendingCount en file)"
        pendingCount > 0 -> "$pendingCount à synchroniser"
        else -> "Stockage Local Synchronisé"
    }

    Surface(
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.5f)),
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onToggleOffline() }
            .testTag("sync_status_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = textColor
            )
        }
    }
}
