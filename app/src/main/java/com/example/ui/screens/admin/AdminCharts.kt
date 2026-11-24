package com.example.ui.screens.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyMetricPoint
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.DividerMint
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LightMintBackground
import com.example.ui.theme.MainText
import com.example.ui.theme.NeutralSlate
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.WarningAmber

@Composable
fun MetricBarChartCard(
    title: String,
    subtitle: String,
    points: List<DailyMetricPoint>,
    barColor: Color = DeepTeal,
    modifier: Modifier = Modifier
) {
    val maxVal = (points.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerMint),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MainText)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
            Spacer(modifier = Modifier.height(14.dp))

            if (points.isEmpty()) {
                Text(
                    text = "No activity recorded in this window.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    val count = points.size
                    val slotW = size.width / count.coerceAtLeast(1)
                    val barW = (slotW * 0.56f).coerceIn(8f, 38f)
                    val chartH = size.height - 14f

                    // Baseline
                    drawLine(
                        color = DividerMint,
                        start = Offset(0f, chartH),
                        end = Offset(size.width, chartH),
                        strokeWidth = 2f
                    )

                    points.forEachIndexed { idx, pt ->
                        val ratio = pt.count.toFloat() / maxVal.toFloat()
                        val h = (chartH * 0.90f * ratio).coerceAtLeast(if (pt.count > 0) 8f else 2f)
                        val x = idx * slotW + (slotW - barW) / 2f
                        val y = chartH - h
                        drawRoundRect(
                            color = if (pt.count > 0) barColor else DividerMint,
                            topLeft = Offset(x, y),
                            size = Size(barW, h),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                val labelIndices = if (points.size <= 7) {
                    points.indices.toList()
                } else {
                    listOf(0, points.size / 2, points.lastIndex)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    labelIndices.forEach { idx ->
                        Text(
                            text = "${points[idx].dateUtc} (${points[idx].count})",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GrowthLineChartCard(
    title: String,
    subtitle: String,
    points: List<DailyMetricPoint>,
    lineColor: Color = EmeraldGreen,
    modifier: Modifier = Modifier
) {
    val maxVal = (points.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerMint),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MainText)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
            Spacer(modifier = Modifier.height(14.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                if (points.isNotEmpty()) {
                    val stepX = if (points.size > 1) size.width / (points.size - 1) else size.width
                    val path = Path()
                    val coords = points.mapIndexed { idx, pt ->
                        val x = if (points.size == 1) size.width / 2f else idx * stepX
                        val y = size.height - 16f - ((pt.count.toFloat() / maxVal) * (size.height - 32f))
                        Offset(x, y)
                    }
                    coords.forEachIndexed { idx, offset ->
                        if (idx == 0) path.moveTo(offset.x, offset.y)
                        else path.lineTo(offset.x, offset.y)
                    }
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )
                    coords.forEach { pt ->
                        drawCircle(color = PureWhite, radius = 9f, center = pt)
                        drawCircle(color = DeepTeal, radius = 6f, center = pt)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEach { pt ->
                    Text("${pt.dateUtc}: ${pt.count}", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                }
            }
        }
    }
}

@Composable
fun OutcomeDistributionCard(
    realCount: Int,
    suspiciousCount: Int,
    fakeCount: Int,
    unverifiedCount: Int,
    modifier: Modifier = Modifier
) {
    val total = (realCount + suspiciousCount + fakeCount + unverifiedCount).coerceAtLeast(1)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = LightMintBackground),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Verification Result Distribution", style = MaterialTheme.typography.titleSmall, color = MainText)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(50))
            ) {
                val segments = listOf(
                    realCount to EmeraldGreen,
                    suspiciousCount to WarningAmber,
                    fakeCount to ErrorRed,
                    unverifiedCount to NeutralSlate
                )
                var xOffset = 0f
                segments.forEach { (count, color) ->
                    val segW = (count.toFloat() / total.toFloat()) * size.width
                    if (segW > 0f) {
                        drawRect(color = color, topLeft = Offset(xOffset, 0f), size = Size(segW, size.height))
                        xOffset += segW
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendPill("Real", realCount, EmeraldGreen)
                LegendPill("Suspicious", suspiciousCount, WarningAmber)
                LegendPill("Fake", fakeCount, ErrorRed)
                LegendPill("Unverified", unverifiedCount, NeutralSlate)
            }
        }
    }
}

@Composable
private fun LegendPill(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text("$label ($count)", style = MaterialTheme.typography.labelSmall, color = MainText)
    }
}
