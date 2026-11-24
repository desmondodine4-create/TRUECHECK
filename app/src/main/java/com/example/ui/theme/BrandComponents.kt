package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VerificationStatus

@Composable
fun TrueCheckShieldMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    inverted: Boolean = false
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val shieldPath = Path().apply {
            moveTo(w * 0.50f, h * 0.06f)
            lineTo(w * 0.88f, h * 0.21f)
            lineTo(w * 0.88f, h * 0.49f)
            cubicTo(w * 0.88f, h * 0.74f, w * 0.69f, h * 0.89f, w * 0.50f, h * 0.95f)
            cubicTo(w * 0.31f, h * 0.89f, w * 0.12f, h * 0.74f, w * 0.12f, h * 0.49f)
            lineTo(w * 0.12f, h * 0.21f)
            close()
        }

        val shieldBrush = if (inverted) {
            Brush.linearGradient(
                colors = listOf(PureWhite, SoftMint),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(DeepTeal, EmeraldGreen),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        }

        drawPath(path = shieldPath, brush = shieldBrush)

        val checkPath = Path().apply {
            moveTo(w * 0.32f, h * 0.52f)
            lineTo(w * 0.45f, h * 0.65f)
            lineTo(w * 0.70f, h * 0.38f)
        }

        drawPath(
            path = checkPath,
            color = if (inverted) DeepTeal else PureWhite,
            style = Stroke(
                width = w * 0.105f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
fun TrueCheckWordmark(
    modifier: Modifier = Modifier,
    shieldSize: Dp = 32.dp,
    inverted: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        TrueCheckShieldMark(size = shieldSize, inverted = inverted)
        Spacer(modifier = Modifier.width(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "TRUE",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = if (inverted) PureWhite else DarkGreen
            )
            Text(
                text = "CHECK",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = if (inverted) SoftMint else EmeraldGreen
            )
        }
    }
}

@Composable
fun VerificationStatusBadge(
    status: VerificationStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg, label) = when (status) {
        VerificationStatus.REAL -> Triple(SoftMint, DeepTeal, "Verified")
        VerificationStatus.SUSPICIOUS -> Triple(WarningAmberSoft, Color(0xFFB36E0A), "Suspicious")
        VerificationStatus.FAKE -> Triple(ErrorRedSoft, ErrorRed, "Counterfeit")
        VerificationStatus.UNVERIFIED -> Triple(NeutralSlateSoft, NeutralSlate, "Unverified")
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SoftMintBanner(
    text: String,
    modifier: Modifier = Modifier,
    accentColor: Color = DeepTeal,
    backgroundColor: Color = LightMintBackground
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = accentColor
        )
    }
}
